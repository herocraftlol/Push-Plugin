package com.arenapvp.plugin;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ecoute tous les evenements de jeu necessaires :
 * - mort par joueur ou par chute dans le vide -> attribution de point
 * - interdiction de drop / deplacement des armes du kit
 * - cooldown de tir a l'arc (3s) avec fleche "infinie" fournie virtuellement
 * - deconnexion en cours de partie
 * - utilisation du diamant admin (voir AdminItemListener)
 */
public class GameListener implements Listener {

    private final ArenaPvPPlugin plugin;
    private final ArenaManager manager;

    // Cooldown de tir par joueur (uuid -> true si en cooldown)
    private final Set<UUID> bowOnCooldown = new HashSet<>();

    // Suivi de la derniere entite qui a degat un joueur, pour attribuer le point en cas de mort par chute
    // juste apres avoir ete touche (evite qu'une chute provoquee par un coup d'epee compte comme "suicide")
    private final Map<UUID, UUID> lastDamager = new HashMap<>();
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private static final long ASSIST_WINDOW_MS = 5000L;

    // Joueurs dont la mort par le vide est en cours de traitement : permet d'eviter
    // qu'onDeath() ne recompte un second point quand le damage(1000) qui suit
    // declenche le veritable PlayerDeathEvent.
    private final Set<UUID> processingVoidDeath = new HashSet<>();

    public GameListener(ArenaPvPPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getArenaManager();
    }

    // ---------------- Suivi des degats pour determiner l'auteur d'un kill ----------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Arena arena = manager.getArenaOf(victim.getUniqueId());
        if (arena == null || arena.getState() != Arena.State.RUNNING) return;

        Player damager = resolveDamagerPlayer(event.getDamager());
        if (damager == null) return;
        if (damager.getUniqueId().equals(victim.getUniqueId())) return;

        lastDamager.put(victim.getUniqueId(), damager.getUniqueId());
        lastDamageTime.put(victim.getUniqueId(), System.currentTimeMillis());
    }

    private Player resolveDamagerPlayer(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player p) return p;
        if (entity instanceof Projectile proj) {
            ProjectileSource source = proj.getShooter();
            if (source instanceof Player p) return p;
        }
        return null;
    }

    // ---------------- Mort par un autre joueur ----------------

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Arena arena = manager.getArenaOf(victim.getUniqueId());
        if (arena == null || arena.getState() != Arena.State.RUNNING) return;

        // On annule les drops et garde le format de message simple
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Mort deja entierement traitee par handleVoidDeath (point attribue, message envoye) :
        // on se contente de ne rien dropper / pas de message vanilla, sans recompter de point.
        if (processingVoidDeath.remove(victim.getUniqueId())) {
            event.setDeathMessage(null);
            return;
        }

        int victimTeam = arena.getTeamOf(victim.getUniqueId());
        Player killer = victim.getKiller();

        Integer scoringTeamObj = null;
        if (killer != null && manager.getArenaOf(killer.getUniqueId()) == arena) {
            int killerTeam = arena.getTeamOf(killer.getUniqueId());
            if (killerTeam != victimTeam) {
                scoringTeamObj = killerTeam;
            }
        }

        if (scoringTeamObj != null) {
            event.setDeathMessage(null);
            String killerName = killer.getName();
            manager.broadcastToArena(arena, ChatColor.YELLOW + victim.getName() + ChatColor.GRAY
                    + " a ete elimine par " + ChatColor.YELLOW + killerName + ChatColor.GRAY + ".");
            manager.addPointAndCheckWin(arena, scoringTeamObj, victim);
        } else {
            event.setDeathMessage(null);
            manager.broadcastToArena(arena, ChatColor.YELLOW + victim.getName() + ChatColor.GRAY + " est mort.");
        }

        lastDamager.remove(victim.getUniqueId());
        lastDamageTime.remove(victim.getUniqueId());

        // Respawn immediat dans l'arene (voir onRespawn)
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null) return;

        if (arena.getState() == Arena.State.RUNNING) {
            int team = arena.getTeamOf(player.getUniqueId());
            var spawn = arena.getTeamSpawns().get(team);
            if (spawn != null) {
                event.setRespawnLocation(spawn);
            }
            // On redonne le kit juste apres le respawn effectif
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (player.isOnline()) {
                    manager.giveKit(player, arena);
                }
            }, 1L);
        }
    }

    // ---------------- Chute dans le vide ----------------

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null || arena.getState() != Arena.State.RUNNING) return;
        if (!arena.hasVoidY()) return;

        if (player.getLocation().getY() < arena.getVoidY()) {
            handleVoidDeath(player, arena);
        }
    }

    private void handleVoidDeath(Player player, Arena arena) {
        int victimTeam = arena.getTeamOf(player.getUniqueId());

        UUID damagerUuid = lastDamager.get(player.getUniqueId());
        Long time = lastDamageTime.get(player.getUniqueId());
        Integer scoringTeam = null;

        if (damagerUuid != null && time != null && (System.currentTimeMillis() - time) <= ASSIST_WINDOW_MS) {
            Player damager = plugin.getServer().getPlayer(damagerUuid);
            if (damager != null && manager.getArenaOf(damager.getUniqueId()) == arena) {
                int damagerTeam = arena.getTeamOf(damager.getUniqueId());
                if (damagerTeam != victimTeam) {
                    scoringTeam = damagerTeam;
                }
            }
        }

        // Pas d'auteur recent identifie : on attribue le point a "l'equipe adverse" la plus simple
        // -> dans une arene a 2 equipes, c'est immediat. Dans une arene a 3-4 equipes sans agresseur connu,
        // on n'attribue le point a personne pour rester juste (le joueur est tout de meme elimine).
        if (scoringTeam == null && arena.getTeamCount() == 2) {
            scoringTeam = 1 - victimTeam;
        }

        manager.broadcastToArena(arena, ChatColor.YELLOW + player.getName() + ChatColor.GRAY
                + " est tombe dans le vide.");

        if (scoringTeam != null) {
            manager.addPointAndCheckWin(arena, scoringTeam, player);
        }

        lastDamager.remove(player.getUniqueId());
        lastDamageTime.remove(player.getUniqueId());

        // On marque cette mort comme deja traitee (point deja attribue / message deja envoye)
        // avant de declencher le veritable PlayerDeathEvent via damage(), pour qu'onDeath()
        // ne recompte pas un second point pour la meme chute.
        processingVoidDeath.add(player.getUniqueId());
        player.setHealth(0.0001);
        player.damage(1000.0);
    }

    // ---------------- Anti-drop / anti-deplacement des armes du kit ----------------

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null || arena.getState() != Arena.State.RUNNING) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (isKitItem(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null || arena.getState() != Arena.State.RUNNING) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (isKitItem(current) || isKitItem(cursor)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null || arena.getState() != Arena.State.RUNNING) return;

        if (isKitItem(event.getMainHandItem()) || isKitItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    private boolean isKitItem(ItemStack item) {
        if (item == null) return false;
        return item.getType() == Material.STONE_SWORD || item.getType() == Material.BOW
                || item.getType() == Material.ARROW;
    }

    // ---------------- Arc : cooldown 3s + fleche "infinie" ----------------

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null || arena.getState() != Arena.State.RUNNING) return;

        if (bowOnCooldown.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // On consume une fleche "virtuelle" : pas besoin d'en avoir dans l'inventaire,
        // et on s'assure qu'aucune fleche ne reste a ramasser/droppee.
        // Paper 1.21+ utilise shouldConsumeItem() au lieu de getConsumeItem()
        try {
            if (event.getClass().getMethod("shouldConsumeItem").invoke(event).equals(true)) {
                event.getClass().getMethod("setConsumeItem", boolean.class).invoke(event, false);
            }
        } catch (Exception ignored) {}
        if (event.getProjectile() instanceof Arrow arrow) {
            arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        }

        startBowCooldown(player);

        // Securite : si la fleche du slot 8 a malgre tout ete consommee, on la restaure
        // au tick suivant pour garantir un tir infini.
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (!player.isOnline()) return;
            ItemStack slot8 = player.getInventory().getItem(8);
            if (slot8 == null || slot8.getType() != Material.ARROW) {
                ItemStack arrow = new ItemStack(Material.ARROW, 1);
                org.bukkit.inventory.meta.ItemMeta meta = arrow.getItemMeta();
                meta.setDisplayName(ChatColor.GRAY + "Fleche d'arene");
                arrow.setItemMeta(meta);
                player.getInventory().setItem(8, arrow);
            }
        }, 1L);
    }

    private void startBowCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        bowOnCooldown.add(uuid);
        int seconds = plugin.getConfig().getInt("bow-cooldown-seconds", 3);

        new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    bowOnCooldown.remove(uuid);
                    cancel();
                    return;
                }
                if (remaining <= 0) {
                    bowOnCooldown.remove(uuid);
                    player.setCooldown(Material.BOW, 0);
                    cancel();
                    return;
                }
                // Affiche une jauge de cooldown visuelle native (1.21) sur l'icone de l'arc
                int ticks = remaining * 20;
                player.setCooldown(Material.BOW, ticks);
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ---------------- Deconnexion en cours de partie ----------------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null) return;

        bowOnCooldown.remove(player.getUniqueId());
        lastDamager.remove(player.getUniqueId());
        lastDamageTime.remove(player.getUniqueId());
        processingVoidDeath.remove(player.getUniqueId());

        manager.removePlayerFromArena(player, arena, false);
        manager.broadcastToArena(arena, ChatColor.GRAY + player.getName() + " a quitte la partie.");
    }

    // ---------------- Empecher la faim de baisser pendant les parties (confort) ----------------

    @EventHandler
    public void onFoodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena != null) {
            event.setCancelled(true);
        }
    }
}
