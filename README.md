# ArenaPvP

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen)
![Java Version](https://img.shields.io/badge/Java-21-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Paper/Spigot](https://img.shields.io/badge/Paper/Spigot-1.21.4-blueviolet)

Plugin Paper/Spigot **1.21.x** (Java 21) : mini-jeu PvP par équipes en arène avec
zone de vide en dessous, lobby d'attente, **countdown de début (30s)**, kit fixe
(épée + arc INFINITY + armure teintée), **pause entre points (5s)**, scoreboard en
temps réel, mode spectateur, **leaderboard** et statistiques persistantes.

## 📥 Téléchargement


Téléchargez la dernière version sur la [page des Releases](https://github.com/herocraftlol/Push-Plugin/releases/latest) : `ArenaPvP-1.0.4.jar`

## 🚀 Installation

1. Placez `ArenaPvP-1.0.4.jar` dans le dossier `plugins/` de votre serveur Paper 1.21.x

Téléchargez la dernière version sur la [page des Releases](https://github.com/herocraftlol/Push-Plugin/releases/latest) : `ArenaPvP-1.0.1.jar`

## 🚀 Installation

1. Placez `ArenaPvP-1.0.1.jar` dans le dossier `plugins/` de votre serveur Paper 1.21.x
>>>>>>> origin/main
2. Redémarrez le serveur
3. Configurez vos arènes avec les commandes admin ci-dessous

## Fonctionnalités

- **Arènes multiples nommées**, configurables et jouables simultanément.
- **2 à 4 équipes** par arène (configurable) :
  - Équipe 1 : Rose
  - Équipe 2 : Magenta
  - Équipe 3 (si activée) : Violet
  - Équipe 4 (si activée) : Bleu foncé
- **1 à 8 joueurs par équipe** (configurable par arène).
- **Lobby d'attente** : la partie démarre automatiquement dès qu'il y a au
  moins 2 joueurs au total (seuil configurable par arène).
- **Countdown de début (30s)** : compte à rebours avec annonces et sons avant le
  début de la partie, annulable si un joueur quitte.
- **Diamant admin** : un joueur avec la permission `arenapvp.admin` reçoit
  un diamant spécial en slot 0 de la hotbar pendant l'attente en lobby ;
  un clic dessus lance la partie immédiatement, sans attendre le minimum.
- **Kit de combat** : épée en pierre + arc (avec **enchantement INFINITY** pour une
  flèche infinie) + **armure en cuir complète teintée de la couleur de l'équipe**
  (casque, plastron, jambières, bottes). Épée, arc et armure sont **incassables**
  (`unbreakable`). Aucune de ces pièces ne peut être lâchée (drop), déplacée dans
  l'inventaire, ou échangée entre les mains.
- **Flèche toujours disponible** : en plus de la restauration immédiate après
  chaque tir, une vérification de sécurité tourne en tâche de fond (2x/sec)
  pour garantir qu'un joueur en partie a toujours au moins une flèche dans son
  inventaire, et ne se retrouve jamais bloqué sans pouvoir tirer à l'arc.
- **Recharge de l'arc** : 3 secondes (configurable) entre deux tirs, avec
  une jauge de cooldown visible sur l'icône de l'arc.
- **Sidebar en jeu** (scoreboard latéral propre à chaque arène) affichant en
  temps réel, pour chaque équipe, ses points et le total des dégâts infligés
  durant la partie. Affiché dès le lobby d'attente (avec le nombre de joueurs)
  puis pendant toute la partie.
- **Pause après un point (5s)** : après chaque point marqué, tous les joueurs
  sont retéléportés à leur base, les kits retirés, et un compte à rebours de
  5 secondes s'affiche avant la reprise du combat. **Aucun kill ni mouvement
  n'est possible pendant cette pause.**
- **Leaderboard** : classement des meilleurs joueurs avec leurs victoires et éliminations.
- **Élimination par chute dans le vide** : si un joueur descend sous la
  hauteur Y définie pour l'arène, il est éliminé et un point est attribué à
  l'équipe adverse (à l'équipe qui l'a poussé/touché en dernier si identifiable,
  sinon à l'équipe adverse directe en configuration 2 équipes).
- **Élimination par un autre joueur** : un point est attribué à l'équipe du
  tueur, le joueur éliminé respawn immédiatement dans l'arène avec un kit neuf.
- **Victoire à 5 points** (configurable). À la fin de la partie, **tous les
  joueurs de l'arène passent immédiatement en mode spectateur**, le nom de
  l'équipe gagnante est annoncé avec la liste de ses membres, puis (après un
  court délai) tous les joueurs sont téléportés à l'endroit exact où ils se
  trouvaient avant de rejoindre le lobby.
- **Statistiques persistantes** : chaque victoire et chaque élimination est
  enregistrée par joueur (fichier `stats.yml`), consultable avec
  `/p stats [joueur]`.

## Compilation

Ce projet est un projet Maven standard. Sur votre machine (avec **JDK 21** et
**Maven** installés) :

```bash
cd ArenaPvP
mvn clean package
```

Le fichier `target/ArenaPvP.jar` est généré : placez-le dans le dossier
`plugins/` de votre serveur Paper 1.21.x, puis démarrez ou redémarrez le
serveur.

> Le projet télécharge automatiquement l'API Paper 1.21.4 depuis le dépôt
> officiel `repo.papermc.io` au moment du build (dépendance `provided`, donc
> elle n'est pas incluse dans le jar final — c'est normal et attendu).

## Permissions

| Permission        | Effet                                              | Défaut |
|--------------------|-----------------------------------------------------|--------|
| `arenapvp.admin`   | Configurer les arènes, forcer le démarrage, diamant | `op`   |
| `arenapvp.play`    | Rejoindre une arène                                 | tous   |

## Configuration d'une arène (en jeu)

Toutes les commandes de configuration nécessitent `arenapvp.admin` et, pour
les commandes liées à une position, d'être exécutées par un joueur (la
position du joueur au moment de la commande est enregistrée).

1. **Créer l'arène**
   ```
   /p create <nom>
   ```

2. **Définir le point du lobby d'attente** (tenez-vous à l'endroit voulu) :
   ```
   /p setlobby <nom>
   ```

3. **Définir le nombre d'équipes** (entre 2 et 4) :
   ```
   /p setteams <nom> <2-4>
   ```

4. **Définir le(s) spawn(s) de chaque équipe** (tenez-vous à l'endroit voulu) :
   ```
   /p setspawn <nom> 1
   /p setspawn <nom> 2
   /p setspawn <nom> 3   (si 3+ équipes)
   /p setspawn <nom> 4   (si 4 équipes)
   ```
   Si votre `teamSize` est supérieur à 1 (plusieurs joueurs par équipe), vous pouvez
   définir **plusieurs spawns pour une même équipe** afin qu'ils n'apparaissent pas
   tous exactement au même endroit : lancez simplement `/p setspawn <nom> <equipe>`
   une nouvelle fois depuis un autre point, un nouveau spawn est ajouté à la suite
   (un spawn est ensuite tiré au hasard parmi ceux définis, à chaque téléportation).
   Vous pouvez aussi préciser le numéro explicitement pour remplacer un spawn existant :
   ```
   /p setspawn <nom> 1 2     (définit/remplace le spawn n°2 de l'équipe 1)
   /p delspawn <nom> 1 2     (supprime le spawn n°2 de l'équipe 1)
   ```
   Le menu `/p spawns <nom>` ouvre un GUI visuel (vitres teintées, une par équipe)
   qui indique combien de spawns sont déjà configurés pour chaque équipe : clic
   gauche pour rappeler la commande à taper, clic droit pour supprimer le dernier
   spawn ajouté. La couleur de chaque vitre dépend du nombre d'équipes de l'arène :
   **Magenta / Rose** pour une arène à 2 équipes, **Violet / Bleu clair / Bleu / Cyan**
   pour une arène à 3 ou 4 équipes.

5. **Définir la limite de vide** (hauteur Y en dessous de laquelle un joueur
   est éliminé). Si vous ne précisez pas de valeur, c'est votre position Y
   actuelle qui est utilisée — placez-vous donc juste au niveau du vide,
   ou légèrement au-dessus du fond de l'arène :
   ```
   /p setvoid <nom> [y]
   ```
   Exemple : `/p setvoid arene1 50` élimine tout joueur descendant
   sous Y=50.

6. **Définir la taille maximale d'une équipe** (entre 1 et 8 joueurs) :
   ```
   /p setteamsize <nom> <1-8>
   ```

7. **Définir le minimum de joueurs pour démarrer automatiquement** (par
   défaut 2) :
   ```
   /p setminplayers <nom> <minimum>
   ```

8. **Vérifier que tout est prêt** :
   ```
   /p info <nom>
   ```
   La ligne "Entièrement configurée" doit afficher "oui". Il faut au minimum
   un lobby, une limite de vide, et un spawn par équipe active.

## Commandes joueur

```
/p list              - liste les arènes et leur état
/p info <nom>        - détails d'une arène
/p join <nom>        - rejoindre le lobby d'attente
/p leave             - quitter l'arène (lobby ou en partie)
/p stats [joueur]    - affiche les victoires et éliminations (les tiennes par défaut)
/p gui               - ouvre un menu graphique pour rejoindre une arène en un clic
```

## Commandes admin supplémentaires

```
/p delete <nom>          - supprimer une arène
/p delspawn <nom> <equipe> <numero> - supprimer un spawn d'équipe
/p spawns <nom>          - menu visuel des spawns (vitres colorées par équipe)
/p forcestart [nom]      - forcer le démarrage immédiat (équivalent au diamant)
/p reload                - recharger la configuration depuis config.yml
```

## Personnaliser les couleurs et noms d'équipe

Dans `config.yml`, les couleurs (`ChatColor` Bukkit) et noms affichés sont
définis dans l'ordre des équipes 1 à 4 :

```yaml
team-colors:
  - PINK
  - MAGENTA
  - DARK_PURPLE
  - DARK_BLUE

team-display-names:
  - "Rose"
  - "Magenta"
  - "Violet"
  - "Bleu fonce"
```

Vous pouvez aussi y ajuster :

```yaml
points-to-win: 5
bow-cooldown-seconds: 3
end-delay-seconds: 5
```

## Notes techniques

- Les arènes peuvent fonctionner **simultanément** sur le même serveur
  (différents joueurs dans différentes arènes en même temps).
- La configuration (spawns, lobby, limite de vide...) est **persistée
  automatiquement** dans `config.yml` à chaque commande de configuration et
  au moment de l'arrêt du serveur.
- Le respawn après une mort en partie replace le joueur directement au
  spawn de son équipe, avec un kit neuf, sans interruption de la partie.
- Chaque arène utilise son **propre scoreboard** (sidebar + équipes colorées),
  isolé des autres arènes et du scoreboard principal du serveur : aucune
  interférence entre plusieurs parties simultanées.
- Les statistiques de victoires/éliminations sont stockées dans
  `plugins/ArenaPvP/stats.yml`, indépendamment de `config.yml`, et sauvegardées
  immédiatement à chaque victoire ou élimination (ainsi qu'à l'arrêt du
  serveur).
