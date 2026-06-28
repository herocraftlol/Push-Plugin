# ArenaPvP

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen)
![Java Version](https://img.shields.io/badge/Java-21-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Paper/Spigot](https://img.shields.io/badge/Paper/Spigot-1.21.4-blueviolet)

Plugin Paper/Spigot **1.21.x** (Java 21) : mini-jeu PvP par équipes en arène avec
zone de vide en dessous, lobby d'attente, kit fixe (épée + arc à recharge), et
système de points.

## 📥 Téléchargement

Téléchargez la dernière version sur la [page des Releases](https://github.com/herocraftlol/Push-Plugin/releases/latest) : `ArenaPvP-1.0.0.jar`

## 🚀 Installation

1. Placez `ArenaPvP-1.0.0.jar` dans le dossier `plugins/` de votre serveur Paper 1.21.x
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
- **Diamant admin** : un joueur avec la permission `arenapvp.admin` reçoit
  un diamant spécial en slot 0 de la hotbar pendant l'attente en lobby ;
  un clic dessus lance la partie immédiatement, sans attendre le minimum.
- **Kit de combat** : épée en pierre + arc (avec une flèche dédiée qui ne se
  consomme jamais). Aucun des deux ne peut être lâché (drop), déplacé dans
  l'inventaire, ou échangé entre les mains.
- **Recharge de l'arc** : 3 secondes (configurable) entre deux tirs, avec
  une jauge de cooldown visible sur l'icône de l'arc.
- **Élimination par chute dans le vide** : si un joueur descend sous la
  hauteur Y définie pour l'arène, il est éliminé et un point est attribué à
  l'équipe adverse (à l'équipe qui l'a poussé/touché en dernier si identifiable,
  sinon à l'équipe adverse directe en configuration 2 équipes).
- **Élimination par un autre joueur** : un point est attribué à l'équipe du
  tueur, le joueur éliminé respawn immédiatement dans l'arène avec un kit neuf.
- **Victoire à 5 points** (configurable). Tous les joueurs sont alors
  téléportés à l'endroit exact où ils se trouvaient avant de rejoindre le
  lobby.

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
   /arena create <nom>
   ```

2. **Définir le point du lobby d'attente** (tenez-vous à l'endroit voulu) :
   ```
   /arena setlobby <nom>
   ```

3. **Définir le nombre d'équipes** (entre 2 et 4) :
   ```
   /arena setteams <nom> <2-4>
   ```

4. **Définir le spawn de chaque équipe** (tenez-vous à l'endroit voulu, une
   commande par équipe) :
   ```
   /arena setspawn <nom> 1
   /arena setspawn <nom> 2
   /arena setspawn <nom> 3   (si 3+ équipes)
   /arena setspawn <nom> 4   (si 4 équipes)
   ```

5. **Définir la limite de vide** (hauteur Y en dessous de laquelle un joueur
   est éliminé). Si vous ne précisez pas de valeur, c'est votre position Y
   actuelle qui est utilisée — placez-vous donc juste au niveau du vide,
   ou légèrement au-dessus du fond de l'arène :
   ```
   /arena setvoid <nom> [y]
   ```
   Exemple : `/arena setvoid arene1 50` élimine tout joueur descendant
   sous Y=50.

6. **Définir la taille maximale d'une équipe** (entre 1 et 8 joueurs) :
   ```
   /arena setteamsize <nom> <1-8>
   ```

7. **Définir le minimum de joueurs pour démarrer automatiquement** (par
   défaut 2) :
   ```
   /arena setminplayers <nom> <minimum>
   ```

8. **Vérifier que tout est prêt** :
   ```
   /arena info <nom>
   ```
   La ligne "Entièrement configurée" doit afficher "oui". Il faut au minimum
   un lobby, une limite de vide, et un spawn par équipe active.

## Commandes joueur

```
/arena list              - liste les arènes et leur état
/arena info <nom>        - détails d'une arène
/arena join <nom>        - rejoindre le lobby d'attente
/arena leave             - quitter l'arène (lobby ou en partie)
```

## Commandes admin supplémentaires

```
/arena delete <nom>          - supprimer une arène
/arena forcestart [nom]      - forcer le démarrage immédiat (équivalent au diamant)
/arena reload                - recharger la configuration depuis config.yml
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
