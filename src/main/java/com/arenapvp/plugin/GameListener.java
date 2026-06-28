package com.arenapvp.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
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
 * - mort par joueur ou par chute dans le vide -> attribution de point + reteleportation
 * - interdiction de drop / deplacement des armes du kit ET du diamant admin
 * - cooldown de tir a l'arc (3s) — l'arc a l'enchantement INFINITY, pas besoin de fleche
 * - deconnexion en cours de partie
 * - blocage des actions pendant la pause apres un point
 */
public class GameListener implements Listener {

    private final ArenaPvPPlugin plugin;
    private final ArenaManager manager;

    // Cooldown de tir par joueur
    private final Set<UUID> bowOnCooldown = new HashSet<>();

    // Suivi de la derniere entite qui a inflige des degats, pour les kills par chute
    private final Map<UUID, UUID> lastDamager = new HashMap<>();
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private static final long ASSIST_WINDOW_MS = 5000L;

    // Joueurs dont la mort par le vide est en cours de traitement
    private final Set<UUID> processingVoidDeath = new HashSet<>();

    public GameListener(ArenaPvPPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getArenaManager();
    }

    // ---------------- Suivi des degats ----------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Arena arena = manager.getArenaOf(victim.getUniqueId());
        if (arena == null || arena.getState() != Arena.State.RUNNING) return;

        // Bloquer les degats pendant la pause post-point
        if (manager.isArenaPaused(arena)) {
            event.setCancelled(true);
            return;
        }

        Player damager = resolveDamagerPlayer(event.getDamager());
        if (damager == null) return;
        if (damager.getUniqueId().equals(victim.getUniqueId())) return;
        if (manager.getArenaOf(damager.getUniqueId()) != arena) return;

        lastDamager.put(victim.getUniqueId(), damager.getUniqueId());
        lastDamageTime.put(victim.getUniqueId(), System.currentTimeMillis());

        int damagerTeam = arena.getTeamOf(damager.getUniqueId());
        int victimTeam = arena.getTeamOf(victim.getUniqueId());
        if (damagerTeam != -1 && damagerTeam != victimTeam) {
            manager.addDamage(arena, damagerTeam, event.getFinalDamage());
        }
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

        event.getDrops().clear();
        event.setDroppedExp(0);

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
            plugin.getStatsManager().addKill(killer.getUniqueId(), killerName);
            manager.addPointAndCheckWin(arena, scoringTeamObj, victim);
        } else {
            event.setDeathMessage(null);
            manager.broadcastToArena(arena, ChatColor.YELLOW + victim.getName() + ChatColor.GRAY + " est mort.");
        }

        lastDamager.remove(victim.getUniqueId());
        lastDamageTime.remove(victim.getUniqueId());
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
            // On redonner le kit apres le respawn seulement si la partie n'est pas en pause
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (player.isOnline() && !manager.isArenaPaused(arena)) {
                    manager.giveKit(player, arena);
                }
            }, 1L);
        }
    }

    // ---------------- Chute dans le vide + blocage mouvement ----------------

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null) return;

        if (arena.getState() != Arena.State.RUNNING) return;

        // Bloquer le mouvement pendant la pause post-point (5s countdown)
        if (manager.isArenaPaused(arena)) {
            org.bukkit.Location from = event.getFrom();
            org.bukkit.Location to = event.getTo();
            if (to != null && (Double.compare(from.getX(), to.getX()) != 0
                    || Double.compare(from.getY(), to.getY()) != 0
                    || Double.compare(from.getZ(), to.getZ()) != 0)) {
                event.setCancelled(true);
            }
            return;
        }

        if (!arena.hasVoidY()) return;
        if (manager.isArenaPaused(arena)) return;

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

        processingVoidDeath.add(player.getUniqueId());
        player.setHealth(0.0001);
        player.damage(1000.0);
    }

    // ---------------- Anti-drop / anti-deplacement du kit ET du diamant admin ----------------

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null) return;

        ItemStack item = event.getItemDrop().getItemStack();
        // En lobby : bloquer le drop du diamant admin
        // En partie : bloquer le drop de tout item du kit
        if (arena.getState() == Arena.State.WAITING && isAdminDiamond(item)) {
            event.setCancelled(true);
        } else if (arena.getState() == Arena.State.RUNNING && isKitItem(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (arena.getState() == Arena.State.WAITING) {
            // Bloquer tout mouvement du diamant admin en lobby
            if (isAdminDiamond(current) || isAdminDiamond(cursor)) {
                event.setCancelled(true);
                return;
            }
        }

        if (arena.getState() == Arena.State.RUNNING) {
            if (isKitItem(current) || isKitItem(cursor)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null) return;

        if (arena.getState() == Arena.State.WAITING) {
            if (isAdminDiamond(event.getMainHandItem()) || isAdminDiamond(event.getOffHandItem())) {
                event.setCancelled(true);
                return;
            }
        }

        if (arena.getState() == Arena.State.RUNNING) {
            if (isKitItem(event.getMainHandItem()) || isKitItem(event.getOffHandItem())) {
                event.setCancelled(true);
            }
        }
    }

    /** Verifie si l'item est le diamant de lancement admin (par son nom). */
    private boolean isAdminDiamond(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND) return false;
        if (!item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().contains("Lancer la partie maintenant");
    }

    private boolean isKitItem(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type == Material.STONE_SWORD || type == Material.BOW || type == Material.ARROW
                || type == Material.LEATHER_HELMET || type == Material.LEATHER_CHESTPLATE
                || type == Material.LEATHER_LEGGINGS || type == Material.LEATHER_BOOTS;
    }

    // ---------------- Arc : cooldown 3s + fleche infinie ----------------

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null || arena.getState() != Arena.State.RUNNING) return;

        // Bloquer le tir pendant la pause post-point
        if (manager.isArenaPaused(arena)) {
            event.setCancelled(true);
            return;
        }

        if (bowOnCooldown.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Arc en rechargement...");
            return;
        }

        // Ne pas consommer la fleche
        event.setConsumeItem(false);
        if (event.getProjectile() instanceof Arrow arrow) {
            arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        }

        // Reassurer la presence de la fleche au slot 8 au prochain tick
        // (certaines versions de Bukkit consomment quand meme la fleche avant setConsumeItem)
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (player.isOnline()) {
                ItemStack slot8 = player.getInventory().getItem(8);
                if (slot8 == null || slot8.getType() != org.bukkit.Material.ARROW) {
                    player.getInventory().setItem(8, ArenaManager.createArenaArrow());
                }
            }
        }, 1L);

        startBowCooldown(player);
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

    // ---------------- Empecher la faim de baisser pendant les parties ----------------

    @EventHandler
    public void onFoodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena != null) {
            event.setCancelled(true);
        }
    }
}
