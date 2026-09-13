# Push

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen)](https://www.minecraft.net)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org)
[![Paper/Spigot](https://img.shields.io/badge/Paper/Spigot-1.21.4-blueviolet)](https://papermc.io)
[![Latest Release](https://img.shields.io/github/v/release/herocraftlol/Push-Plugin)](https://github.com/herocraftlol/Push-Plugin/releases/latest)
[![Source](https://img.shields.io/badge/source-included-success)](#-téléchargement)

**Push** est un plugin Minecraft pour **Paper/Spigot 1.21.x** (Java 21) qui ajoute à
votre serveur un mini-jeu compétitif de **PvP par équipes en arène** : deux, trois ou
quatre équipes s'affrontent, l'objectif est de **pousser ses adversaires dans le vide**
ou de les éliminer au corps-à-corps, jusqu'à atteindre un nombre de points configurable.
Tout est pensé pour des parties rapides, lisibles, rejouables, et tourne en parallèle sur
plusieurs arènes sans interférence entre elles.

---

## ✨ Qu'est-ce que Push, concrètement ?

Un système complet de mini-jeu clé-en-main, avec tout ce qu'il faut côté serveur et
côté joueur :

- **Arènes multiples**, configurables en jeu via des commandes, jouables en parallèle.
- **2 à 4 équipes** colorées et nommées (Magenta, Rose, Violet, Bleu foncé…), de
  **1 à 8 joueurs** par équipe (tailles réglables par arène).
- **Lobby d'attente automatique** : la partie démarre dès qu'il y a assez de
  joueurs, et toute la zone de lobby disparaît visuellement dès que la partie
  commence (puis revient à la fin, à l'identique).
- **Countdown de 30 s** avant le début : annonces, sons, et annulation propre
  si un joueur quitte pendant le décompte.
- **Diamant admin** : un admin présent en lobby peut forcer le démarrage
  immédiat d'un clic, sans attendre le minimum de joueurs.
- **Kit de combat fixe** : épée en pierre, arc à flèche infinie (INFINITY),
  armure en cuir teintée à la couleur de l'équipe. Tout est incassable, ne se
  dépose pas, ne s'échange pas, ne se remplace pas — impossible de se
  retrouver désarmé.
- **Éliminations par le vide** : si un joueur tombe sous la limite de l'arène,
  il est éliminé et son équipe adverse marque un point (ou l'équipe adverse du
  dernier coup porté, si identifiable). En combat, un kill direct vaut aussi
  un point. La première équipe à atteindre **5 points** (configurable) gagne.
- **Sidebar élégant par joueur** : scoreboard latéral rafraîchi chaque seconde
  avec le chrono, le score par équipe, ton K/D, l'état de recharge de ton arc,
  et une ligne « Spectateurs ». Sur arène 4 équipes, le sidebar s'adapte pour
  tenir dans la limite des 15 lignes Minecraft.
- **Mode spectateur configurable** : `/p spectate <arène>` pour observer une
  partie sans jouer (vol, vision nocturne, point d'apparition configurable
  par arène, retour automatique à la fin).
- **Pause 3 s entre les manches** : après chaque point, tout le monde est
  retéléporté à sa base et un compte à rebours bloque toute action le temps
  de la pause.
- **Messages rapides traduits** (« Bien joué ! », « Tu es trop fort ! »…),
  affichés sur des blocs de couleur pendant la pause entre les manches et
  sur l'écran de victoire, traduits automatiquement dans la langue de chaque
  destinataire.
- **Système de niveaux Push** : gagne de l'XP en tuant, en poussant dans le
  vide, en infligeant des dégâts, en jouant et en gagnant. Tu montes en
  niveau, et chaque palier déverrouille des **cosmétiques exclusifs**.
- **Cosmétiques d'élimination** : 7 effets (Étincelles, Flammes, Explosion,
  Foudre, Feu d'artifice, Étoile légendaire…) purement visuels/sonores, à
  activer depuis `/p cosmetics`.
- **Hologrammes de classement** : `/p holo summon <kills|pushes|damage|wins|level>`
  place un top 5 mis à jour automatiquement où tu te tiens.
- **Leaderboard**, statistiques persistantes (`/p stats [joueur]`), sauvegarde
  automatique à chaque kill et à l'arrêt du serveur.
- **Interface graphique** (`/p gui`) : rejoins une arène d'un clic gauche,
  observe-la d'un clic droit.

Push reprend toute la base d'ArenaPvP et la fait évoluer : meilleur équilibrage
des couleurs d'équipe (Magenta enfin magenta, Rose enfin rose), sidebar restylé
façon HikaBrain, gestion corrigée des flèches fantômes, bug de respawn sur la
manche gagnante corrigé, et tout le système de niveaux/cosmétiques/hologrammes
intégré.

---

## 📥 Téléchargement

La dernière version stable se trouve sur la
[page des Releases](https://github.com/herocraftlol/Push-Plugin/releases/latest).

| Fichier | Rôle |
|--------|------|
| `Push-1.0.6.jar` | Le plugin compilé, prêt à installer |
| `Push-1.0.6-src.zip` | Code source complet (Maven, Java 21) |

Anciennes versions : voir [toutes les releases](https://github.com/herocraftlol/Push-Plugin/releases).

### 🚀 Installation

1. Téléchargez `Push-1.0.6.jar` depuis la release **v1.0.6**.
2. Placez-le dans le dossier `plugins/` de votre serveur **Paper 1.21.x**.
3. Démarrez ou redémarrez le serveur — le plugin génère `plugins/Push/config.yml`
   au premier lancement.
4. Configurez vos arènes directement en jeu avec les commandes admin listées plus bas.

> L'API Paper (`io.papermc.paper:paper-api`) est marquée en `provided` : elle est
> déjà présente sur le serveur et n'est pas embarquée dans le jar, c'est normal.

---

## 🆕 Nouveautés de la v1.0.6

Cette release constitue une **évolution majeure** par rapport à la dernière
release ArenaPvP : le plugin est renommé **Push**, le code de tout le système
de niveaux/cosmétiques/hologrammes est intégré, et plusieurs bugs irritants
sont corrigés.

### 🎨 Identité visuelle & sidebar

- **Le plugin est renommé `Push`** : nouvelles permissions (`push.*`), nouvelle
  commande principale `/p`, fichiers de configuration homogènes.
- **Couleurs d'équipe corrigées** : l'équipe « Magenta » est désormais réellement
  magenta (`DARK_PURPLE`) et l'équipe « Rose » est rose/clair (`LIGHT_PURPLE`)
  — c'était inversé avant. Plus aucune confusion entre les deux équipes
  rouges/roses.
- **Sidebar restylé façon HikaBrain** : icônes cœur (❤) par équipe, séparateurs
  en traits horizontaux, icône œil quand tu es en spectateur.
- **Pseudo coloré dans la tab list et au-dessus de la tête** à la couleur de
  l'équipe.

### 🛡️ Stabilisation du kit & de l'inventaire

- **Anti-drop et anti-déplacement** : le diamant admin, la barrière « Quitter »
  et la laine de sélection d'équipe ne peuvent plus être lâchés, déplacés
  dans l'inventaire ou échangés entre les mains pendant le lobby. Avant, ces
  protections étaient incomplètes.
- **Flèches fantômes supprimées** : lorsqu'un tir était annulé (cooldown ou
  pause), une flèche pouvait rester visible au sol. Corrigé.
- **Bug de respawn sur la manche gagnante** corrigé : sur le point gagnant
  (mort exactement au moment où la partie se termine), le joueur était
  parfois téléporté au mauvais endroit. Corrigé.

### ⏱️ Gameplay & rythme

- **Pause de 3 secondes entre chaque manche** configurable via
  `round-resume-seconds` dans `config.yml`.
- **Diamant admin fonctionnel** : un admin avec `push.admin` en lobby voit
  un diamant nommé « Lancer la partie maintenant » au slot 0 — un clic et la
  partie démarre immédiatement, sans attendre le minimum de joueurs.

### 💬 Communication & ambiance

- **Messages rapides** (« Bien joué ! », « Tu es trop fort ! », « J'aurai ma
  revanche ! », « Même pas peur ! ») cliquables sur des blocs de couleur
  pendant la pause entre les manches et sur l'écran de victoire. Traduits
  automatiquement dans la langue de chaque destinataire. Désactivables via
  `quick-chat.enabled: false`.
- **Spectateur configurable par arène** : `/p spectatormode <nom> <on|off>`
  pour activer ou désactiver le mode spectateur sur une arène précise.
  Réglages globaux (vol, vision nocturne, visibilité des autres spectateurs)
  dans `config.yml` (`spectator:`).

### 🆙 Système de niveaux & cosmétiques

- **Système de niveaux Push** : l'XP est gagnée en tuant
  (`levels.xp-per-kill`), en poussant dans le vide (`levels.xp-per-push`),
  en infligeant des dégâts (`levels.xp-per-damage-point`), en jouant
  (`levels.xp-per-minute-played`) et en gagnant des parties
  (`levels.xp-per-win`). Visible avec `/p level [joueur]`.
- **Cosmétiques exclusifs Push** : 7 effets d'élimination
  (Étincelles, Flammes, Explosion, Foudre, Feu d'artifice,
  Étoile légendaire…) déverrouillés en montant de niveau, à choisir
  avec `/p cosmetics`. Purement visuels et sonores, **aucun impact sur
  le gameplay**.

### 🏆 Classements en jeu

- **Hologrammes de classement summonables** : `/p holo summon
  <kills|pushes|damage|wins|level>` place un hologramme de top 5 à ta
  position, rafraîchi automatiquement. `/p holo list` et
  `/p holo remove <id>` pour les gérer.

### 🖼️ Interfaces graphiques

- **GUI principal** (`/p gui`) : liste des arènes, clic gauche pour
  rejoindre, clic droit pour observer.
- **GUI de sélection des équipes** : choisit ou change d'équipe pendant
  le lobby d'un clic.
- **GUI des cosmétiques** : prévisualise et active les effets débloqués.
- **GUI de gestion des spawns** : visualise combien de spawns sont
  définis par équipe (une vitre colorée par équipe) pour l'arène en
  cours d'édition.

---

## 🎮 Commandes joueur

```
/p list              - liste les arènes et leur état (en attente / en cours / désactivée)
/p info <nom>        - détails d'une arène (état, équipes, nb de joueurs, points...)
/p join <nom>        - rejoindre le lobby d'attente de l'arène
/p leave             - quitter l'arène (lobby, partie ou mode spectateur)
/p spectate <nom>    - observer une partie en mode spectateur
/p unspectate        - quitter le mode spectateur et revenir à sa position d'origine
/p stats [joueur]    - statistiques (victoires, éliminations) — les tiennes par défaut
/p level [joueur]    - niveau et progression XP
/p cosmetics         - choisir un cosmétique d'élimination parmi ceux débloqués
/p gui               - menu graphique pour rejoindre ou observer une arène
/p holo list         - liste des hologrammes de classement actuellement placés
```

## 🔧 Commandes admin (`push.admin`)

```
/p create <nom>              - créer une arène
/p delete <nom>              - supprimer une arène
/p setlobby <nom>            - définir le point de lobby (position du joueur)
/p setspawn <nom> <equipe> [n]
                              - définir un spawn d'équipe (n = numéro du spawn, >=1)
/p delspawn <nom> <equipe> <n> - supprimer un spawn d'équipe
/p spawns <nom>              - menu visuel des spawns (vitres colorées par équipe)
/p setspectator <nom>        - définir le point d'apparition des spectateurs
/p setvoid <nom> [y]         - définir la limite de vide (Y courant si omis)
/p setpos1 / setpos2 <nom>   - poser un coin de la zone de jeu (bloc visé)
/p setteams <nom> <2-4>      - nombre d'équipes
/p setteamsize <nom> <1-8>   - taille max d'une équipe
/p setminplayers <nom> <n>   - minimum de joueurs pour démarrer
/p <nom> lobbyzone pos1/pos2 - délimiter la salle d'attente à construire
/p <nom> lobbyzone save      - ré-enregistrer après modification de la construction
/p <nom> lobbyzone clear     - effacer manuellement la structure
/p <nom> lobbyzone paste     - reposer manuellement la structure
/p <nom> lobbyzone info      - état de la zone et de la structure
/p spectatormode <nom> <on|off>  - activer/désactiver le mode spectateur par arène
/p forcestart [nom]          - forcer le démarrage immédiat (équivalent au diamant)
/p holo summon <cat.>        - hologramme de classement à ta position
/p holo remove <id>          - retirer un hologramme
/p reload                    - recharger la configuration
```

---

## ⚙️ Configuration d'une arène (en jeu)

> Toutes les commandes de configuration nécessitent la permission `push.admin`
> et, pour les commandes liées à une position, d'être exécutées par **un joueur**
> (la position du joueur au moment de la commande est enregistrée).

1. **Créer l'arène** :
   ```
   /p create arene1
   ```

2. **Définir le lobby d'attente** (tiens-toi à l'endroit voulu) :
   ```
   /p setlobby arene1
   ```

3. **Définir le nombre d'équipes** (2 à 4) :
   ```
   /p setteams arene1 2
   ```

4. **Définir le(s) spawn(s) de chaque équipe** (un par équipe, plus si besoin) :
   ```
   /p setspawn arene1 1   # spawn de l'équipe 1
   /p setspawn arene1 2   # spawn de l'équipe 2
   ```
   Si tu veux plusieurs points d'apparition pour la même équipe (répartir
   les joueurs au spawn), relance simplement `/p setspawn arene1 1` —
   un nouveau spawn est ajouté à la suite. Tu peux aussi préciser le
   numéro pour remplacer un spawn existant :
   ```
   /p setspawn arene1 1 2     # définir/remplacer le spawn n°2 de l'équipe 1
   /p delspawn  arene1 1 2    # supprimer le spawn n°2 de l'équipe 1
   ```
   Le menu `/p spawns arene1` ouvre un GUI visuel (une vitre colorée par
   équipe) qui indique combien de spawns sont déjà configurés : clic
   gauche pour te rappeler la commande à taper, clic droit pour supprimer
   le dernier spawn ajouté.

5. **Définir la limite de vide** (Y en dessous duquel un joueur est éliminé).
   Si tu ne précises pas de valeur, c'est ta position Y actuelle qui est
   utilisée — place-toi juste au niveau du vide ou légèrement au-dessus
   du fond de l'arène :
   ```
   /p setvoid arene1 50
   ```

6. **Définir la taille max d'une équipe** (1 à 8) :
   ```
   /p setteamsize arene1 4
   ```

7. **Définir le minimum de joueurs pour démarrer automatiquement** (par défaut 2) :
   ```
   /p setminplayers arene1 2
   ```

8. **Vérifier que tout est prêt** :
   ```
   /p info arene1
   ```
   La ligne « Entièrement configurée » doit afficher **oui**. Il faut au
   minimum **un lobby, une limite de vide, et un spawn par équipe active**.

### Construire une salle d'attente qui disparaît pendant la partie

1. Construis ta salle d'attente à l'endroit de ton choix, de préférence
   juste **au-dessus** de la zone de jeu. Le point de lobby de l'arène
   doit être à l'intérieur (défini à l'étape 2 ci-dessus).
2. Vise le premier coin de la construction et tape :
   ```
   /p arene1 lobbyzone pos1
   ```
3. Vise le coin opposé (en diagonale, en incluant la hauteur) et tape :
   ```
   /p arene1 lobbyzone pos2
   ```
   Dès que les deux coins sont posés, la structure est enregistrée et
   le nombre de blocs capturés t'est confirmé.

C'est tout : à chaque lancement de partie la structure disparaît, et elle
revient **à l'identique** dès que l'arène rouvre (fin de partie, arène
vidée, redémarrage du serveur).

Quelques précisions utiles :

- Les blocs d'air ne sont pas enregistrés : seule la construction compte.
- La capture est refusée pendant une partie et au-delà de 500 000 blocs.
- La pose se fait de bas en haut et l'effacement de haut en bas, pour ne
  pas casser les torches, dalles et pancartes ni déclencher de chutes de
  sable/gravier.
- Les grosses structures sont traitées par paquets de 4 000 blocs/tick
  pour éviter tout pic de lag.
- Les moules sont stockés dans `plugins/Push/lobbies/<arène>.yml`, avec
  une palette de types de blocs pour des fichiers compacts.

---

## 🔐 Permissions

| Permission       | Effet                                                                  | Défaut   |
|------------------|------------------------------------------------------------------------|----------|
| `push.admin`     | Configurer les arènes, forcer le démarrage, diamant admin, hologrammes | `op`     |
| `push.play`      | Rejoindre une arène                                                     | tous     |
| `push.spectate`  | Observer une partie en mode spectateur                                  | tous     |
| `push.use`       | Utiliser les commandes de base (`/p list`, `info`, `join`, `leave`…)    | tous     |

---

## 🧩 `config.yml` — personnalisation

```yaml
# Points nécessaires pour gagner une partie
points-to-win: 5

# Temps de recharge de l'arc (secondes), 0 = aucun rechargement
bow-cooldown-seconds: 3

# Délai (secondes) avant la téléportation de retour après la fin d'une partie
end-delay-seconds: 5

# Pause entre deux manches, après un point marqué
round-resume-seconds: 3

# Couleurs des équipes (1→4). Valeurs valides :
# DARK_PURPLE, LIGHT_PURPLE, BLUE, DARK_BLUE, RED, GREEN, YELLOW, AQUA, GOLD…
# NB : Bukkit n'a PAS de ChatColor "PINK" ni "MAGENTA".
team-colors:
  - DARK_PURPLE
  - LIGHT_PURPLE
  - BLUE
  - DARK_BLUE

team-display-names:
  - "Magenta"
  - "Rose"
  - "Violet"
  - "Bleu foncé"

# Sidebar : titre, pied de page, fréquence de rafraîchissement (en ticks)
scoreboard:
  title: "&8[&b&lHERO&d&lCRAFT&8] &d&lPUSH"
  footer: "&dherocraft.servegame.com"
  update-ticks: 20

# Messages rapides de fin de manche / victoire
quick-chat:
  enabled: true

# Mode spectateur (réglages par défaut, modifiables par arène)
spectator:
  allow-flight: true
  night-vision: true
  see-other-spectators: true

# Système de niveaux
levels:
  xp-per-kill: 15
  xp-per-push: 20
  xp-per-damage-point: 0.1
  xp-per-minute-played: 2
  xp-per-win: 100

holograms: {}
arenas: {}
```

---

## 🛠️ Compilation

Prérequis : **JDK 21** et **Maven** installés.

```bash
git clone https://github.com/herocraftlol/Push-Plugin.git
cd Push-Plugin
mvn clean package
```

Le fichier `target/Push-1.0.6.jar` est généré. Place-le dans le dossier
`plugins/` de ton serveur Paper 1.21.x, puis démarre ou redémarre le
serveur.

> L'API Paper 1.21.4 est téléchargée automatiquement depuis
> `repo.papermc.io` au moment du build (dépendance `provided` — elle
> n'est pas incluse dans le jar final, c'est normal).

---

## 📐 Notes techniques

- **Arènes simultanées** : plusieurs arènes peuvent tourner en parallèle
  sur le même serveur (joueurs différents dans des arènes différentes,
  scoreboards isolés, aucune interférence avec le scoreboard global).
- **Persistance** : la configuration (spawns, lobby, vide, équipes…) est
  automatiquement persistée dans `plugins/Push/config.yml` à chaque
  commande admin et à l'arrêt du serveur.
- **Statistiques** : victoires et éliminations sont stockées dans
  `plugins/Push/stats.yml`, indépendamment de `config.yml`, et
  sauvegardées immédiatement à chaque kill et à l'arrêt.
- **Limites Minecraft respectées** : le sidebar s'auto-ajuste sur les
  arènes à 4 équipes pour rester sous la limite de 15 lignes (les lignes
  Spectateurs/Dégâts sont masquées plutôt que tronquées).
- **Cosmétiques** purement visuels/sonores : ils n'affectent ni les
  points, ni les dégâts, ni le gameplay.

---

## 📜 Licence & crédits

- Code source sous licence **MIT** — voir [`LICENSE`](LICENSE).
- Plugin développé pour Paper/Spigot 1.21.x (API Bukkit/Paper officielle).
- Aucune dépendance propriétaire, aucune télémétrie, aucune connexion
  externe : tout fonctionne hors-ligne.

Bon Push ⚔️
