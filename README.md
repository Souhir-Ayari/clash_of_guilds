# Clash of Guilds — TP Patron Composite (Java)

Wargame tactique au tour par tour pour **3 joueurs**, en console, illustrant le patron de
conception **Composite** (GoF). Chaque guilde est un arbre d'unités :
Armée → Bataillons → Escouades → Soldats (Guerrier, Archer, Mage, Soigneur).

- Code source : [`src/main/java/com/mycompany/composite_clash_of_guilds`](src/main/java/com/mycompany/composite_clash_of_guilds)
- Diagramme de classes : [`docs/diagramme_classes.png`](docs/diagramme_classes.png) (source PlantUML : [`docs/diagramme_classes.puml`](docs/diagramme_classes.puml))
- Compte-rendu + réponses aux questions Q1–Q4 : [`docs/COMPTE_RENDU.md`](docs/COMPTE_RENDU.md)
- Présentation (17 diapositives, export PowerPoint / PDF depuis la page) : https://claude.ai/artifact/YDBGtoGGsMgWY2nXvMSRtu

## Version graphique 2D : le champ de bataille (bonus)

![Bataille animée](docs/captures/bataille.gif)

En plus de la version console demandée par le TP, le jeu existe en **2D animée** (Java Swing / Java2D,
aucune bibliothèque externe : soldats, décor et effets sont dessinés en code). Trois armées de
**10 soldats** se font face sur un champ de bataille ; tout le calcul passe par le modèle Composite
(`IUnite`, `Escouade`, `Combat`), la 2D ne fait que le mettre en scène. Vidéo : [`docs/captures/bataille.mp4`](docs/captures/bataille.mp4).

- **Attaque Totale** : toute l'armée attaque. Les **guerriers courent** jusqu'à l'ennemi et frappent
  **à l'épée** puis reviennent à leur place ; les **archers tirent des flèches** depuis leur position ;
  les **mages lancent des orbes**.
- **Frappe Ciblée** : on clique sur une de ses escouades, puis sur une escouade ennemie (ligne de visée).
- **Défense** : toute l'armée sort ses **armures** (plastron, casque, bulle bleue) : armure +10 pour
  chaque soldat jusqu'au prochain tour. L'ordre est propagé récursivement par le Composite (`defendre()`).
- **Soin** : les soigneurs incantent et une **aura verte** soigne toute leur escouade.
- Dégâts et soins flottants, soldats K.O. qui tombent, secousse de l'écran, étendards qui flottent,
  armée « ANÉANTIE », feux d'artifice de victoire. Modes : 3 joueurs, solo contre 2 IA, démo 3 IA.

| Champ de bataille | Frappe ciblée | Défense |
|---|---|---|
| ![](docs/captures/champ_de_bataille.png) | ![](docs/captures/frappe_ciblee.png) | ![](docs/captures/defense.png) |
| **Attaque totale** | **Soin** | **Victoire** |
| ![](docs/captures/attaque_totale.png) | ![](docs/captures/soin.png) | ![](docs/captures/victoire.png) |

Lancement : configuration **« Jouer en 2D (champ de bataille) »** dans VS Code, ou `jouer2D.bat` (Windows) / `./jouer2D.sh`.

## Lancer le jeu dans VS Code

1. Installer un **JDK 17 ou plus** et l'extension **Extension Pack for Java**
   (`vscjava.vscode-java-pack`, proposée automatiquement à l'ouverture du dossier).
2. `Fichier > Ouvrir le dossier...` → choisir le dossier `clash_of_guilds`.
3. Attendre que VS Code importe le projet (Maven, `pom.xml`).
4. Onglet **Exécuter et déboguer** (Ctrl+Shift+D) → choisir **« Jouer en 2D (champ de bataille) »**
   ou **« Jouer en console (version du TP) »** → ▶.
   Le jeu s'ouvre dans le terminal intégré (indispensable pour taper les choix).
   - Ou ouvrir `JeuConsole.java` et cliquer sur **Run** au-dessus de `main`.
   - La configuration **« Tests Clash of Guilds »** lance les 27 vérifications des règles.

### Sans VS Code (terminal)

```bash
./run.sh                      # console - Linux / macOS
run.bat                       # console - Windows
jouer2D.bat                   # version 2D - Windows (./jouer2D.sh ailleurs)
./run.sh TestsClashOfGuilds   # tests des regles
# ou avec Maven :
mvn package && java -jar target/composite_clash_of_guilds-1.0.jar
```

## Règles du jeu

| Unité | PV | ATQ | Armure | Coût | Particularité |
|---|---|---|---|---|---|
| Guerrier | 120 | 25 | 5 | 80 PO | dégâts nets = max(1, dégâts − 5) |
| Archer | 75 | 40 | – | 70 PO | encaisse tous les dégâts |
| Mage | 50 | 65 | – | 100 PO | canon de verre |
| Soigneur *(bonus)* | 60 | 10 | – | 90 PO | soigne son escouade de 40 PV |

**Défense** (console et 2D) : toute l'armée gagne +10 d'armure jusqu'au prochain tour du joueur.

- **Attaque d'un groupe** = somme des attaques des membres vivants, **+10 %** si au moins 2 membres vivants (bonus de synergie, cumulé à chaque niveau).
- **Dégâts reçus par un groupe** : répartis équitablement entre les membres vivants, récursivement (le reliquat va aux premiers).
- À chaque tour : *Inspecter* son armée / les ennemis, **Attaque Totale**, **Frappe Ciblée** (une sous-escouade frappe un point précis de l'armée ennemie) ou **Soigner**.
- Une guilde dont toutes les unités sont à 0 PV est anéantie ; la dernière guilde en lice gagne.
- Au lancement : armées **prédéfinies** ou **recrutement budgété** (500 PO par joueur).

## Diagramme de classes

![Diagramme de classes](docs/diagramme_classes.png)

```mermaid
classDiagram
    direction TB
    class IUnite {
        <<interface>>
        +getNom() String
        +getPointsDeVie() int
        +getPointsDeVieMax() int
        +getPuissanceAttaque() int
        +subirDegats(montant int) void
        +recevoirSoins(montant int) void
        +estVivant() boolean
        +getCout() int
        +afficherStructure(prefix String, isDernier boolean) void
        +ajouter(unite IUnite) void
        +supprimer(unite IUnite) void
        +getSousUnites() List~IUnite~
    }
    class UniteElementaire {
        <<abstract>>
        -nom String
        -pointsDeVie int
        -pointsDeVieMax int
        -attaque int
        -cout int
        +getType()* String
        #calculerDegatsNets(montant int) int
    }
    class Guerrier { -armure int }
    class Archer
    class Mage
    class Soigneur { -puissanceSoin int +soigner(cible IUnite) int }
    class Escouade {
        -nom String
        -membres List~IUnite~
        +ajouter(u IUnite) void
        +supprimer(u IUnite) void
    }
    class Joueur { -nom String -faction String -armee Escouade }
    class JeuConsole { +main(args String[]) void }
    class Combat { +attaquer(attaquant IUnite, cible IUnite)$ Resultat }

    IUnite <|.. UniteElementaire
    IUnite <|.. Escouade
    UniteElementaire <|-- Guerrier
    UniteElementaire <|-- Archer
    UniteElementaire <|-- Mage
    UniteElementaire <|-- Soigneur
    Escouade o--> "0..*" IUnite : membres
    Joueur *--> Escouade : armee
    JeuConsole --> "3" Joueur
    JeuConsole ..> Combat
    Combat ..> IUnite
```

## Structure du code

| Fichier | Rôle |
|---|---|
| `IUnite.java` | **Composant** : interface commune (transparence : `ajouter/supprimer` en `default`) |
| `UniteElementaire.java` | Base abstraite des **feuilles** |
| `Guerrier.java`, `Archer.java`, `Mage.java`, `Soigneur.java` | **Feuilles** |
| `Escouade.java` | **Composite** : calculs récursifs, répartition des dégâts, protection anti-cycles |
| `Combat.java` | Moteur de combat (logique pure, ne connaît que `IUnite`) |
| `Joueur.java` | Joueur, faction, armée racine |
| `JeuConsole.java` | **Client** : `main`, boucle de jeu à 3 joueurs, menus |
| `GuildesPredefinies.java` | Les 3 armées de départ |
| `Recrutement.java` | Bonus : phase de recrutement avec 500 PO |
| `ParcoursArbre.java`, `Saisie.java` | Outils console (parcours pour menus, saisies contrôlées) |
| `TestsClashOfGuilds.java` | Vérifications automatiques des règles |
| `gui/JeuGraphique.java` | Point d'entrée de la version 2D (fenêtre) |
| `gui/PanneauJeu.java` | Écrans titre / champ de bataille / victoire, mise en scène des actions, IA |
| `gui/Soldat.java` | Soldat animé (course, coup d'épée, tir à l'arc, sort, armure, chute) |
| `gui/Decor.java` | Décors (ciel du menu, champ de bataille) et outils de texte |
| `gui/Effets.java` | Particules, projectiles, ondes de choc, textes flottants, secousse |
| `gui/Icones.java`, `gui/Theme.java` | Icônes vectorielles (unités, blasons) et charte graphique |
