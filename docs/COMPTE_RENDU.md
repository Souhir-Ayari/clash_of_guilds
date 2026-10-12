# Compte-rendu — TP Patron Composite : « Clash of Guilds »

Module : Conception Orientée Objet & Design Patterns — Enseignant : M. Haythem Ghazouani

---

## 1. Architecture réalisée

![Diagramme de classes](diagramme_classes.png)

| Rôle GoF | Classe(s) | Responsabilité |
|---|---|---|
| **Component** | `IUnite` (interface) | Contrat unique : PV, attaque, dégâts, soins, coût, affichage + gestion des enfants (transparence) |
| **Leaf** | `Guerrier`, `Archer`, `Mage`, `Soigneur` (via la base abstraite `UniteElementaire`) | Combattant individuel, pas d'enfants |
| **Composite** | `Escouade` | Armée, bataillon ou escouade : contient une `List<IUnite>` et délègue récursivement |
| **Client** | `JeuConsole`, `Combat` | Moteur de jeu : ne manipule que `IUnite` |

Classes de support : `Joueur` (nom, faction, armée racine), `GuildesPredefinies` (3 armées
à plusieurs niveaux), `Recrutement` (bonus 2), `ParcoursArbre` (menus), `Saisie` (saisies
contrôlées), `TestsClashOfGuilds` (27 vérifications automatiques).

**Séparation métier / présentation** : `IUnite`, les feuilles, `Escouade` et `Combat` ne
contiennent aucune saisie clavier ; `Combat.attaquer()` renvoie un `Resultat` que
`JeuConsole` se charge d'afficher.

### Règles implémentées

| Unité | PV max | ATQ | Armure | Coût | Règle |
|---|---|---|---|---|---|
| Guerrier | 120 | 25 | 5 | 80 PO | dégâts nets = max(1, dégâts − armure) |
| Archer | 75 | 40 | 0 | 70 PO | encaisse tout |
| Mage | 50 | 65 | 0 | 100 PO | canon de verre |
| Soigneur *(bonus)* | 60 | 10 | 0 | 90 PO | soigne son escouade de 40 PV répartis |

- **ATQ d'une escouade** = Σ ATQ des membres vivants, × 1,10 (arrondi) si ≥ 2 membres vivants.
  Exemple de l'énoncé vérifié : Lions d'Or = round((55 + 88 + 65) × 1,1) = **229**.
- **Dégâts subis par une escouade** : `part = montant / Nvivants`, le reliquat
  (`montant % Nvivants`) est donné (+1) aux premières unités ; chaque part est transmise
  récursivement. Exemple vérifié : les Ombres (390 PV) frappées par 229 conservent **161 PV**.
- Une unité à 0 PV ne peut plus attaquer (ATQ = 0) et n'absorbe plus de dégâts.
- Une guilde est anéantie quand `armee.estVivant() == false` ; le dernier joueur en lice gagne.

### Actions par tour
1. Inspecter son armée (arbre ASCII) — 2. Inspecter les ennemis (ne consomment pas le tour)
3. **Attaque Totale** : toute l'armée frappe l'armée d'un rival.
4. **Frappe Ciblée** : une sous-escouade (ou un soldat) choisie frappe un point précis de
   l'armée ennemie (l'armée entière, un bataillon, une escouade ou une unité).
5. **Soigner** *(si un Soigneur vivant)* : il soigne récursivement son escouade.

---

## 2. Questions théoriques

### Q1. Identification des rôles GoF

- **Composant (Component)** : l'interface `IUnite`. Elle déclare les opérations métier
  communes (`getPointsDeVie`, `getPuissanceAttaque`, `subirDegats`, `afficherStructure`…)
  ainsi que les opérations de gestion des enfants (`ajouter`, `supprimer`, `getSousUnites`).
- **Feuilles (Leaf)** : `Guerrier`, `Archer`, `Mage` (et `Soigneur`), qui héritent de la
  classe abstraite `UniteElementaire` (factorisation des attributs PV/attaque/coût). Elles
  implémentent les opérations sur elles-mêmes et n'ont pas d'enfants.
- **Composite** : `Escouade`. Elle stocke une liste hétérogène `List<IUnite>` (feuilles ou
  autres escouades) et implémente chaque opération en déléguant à ses membres.

**Pourquoi le moteur de combat ne manipule-t-il que `IUnite` ?** Grâce au polymorphisme,
`Combat.attaquer(IUnite attaquant, IUnite cible)` fonctionne aussi bien avec un mage isolé
qu'avec une armée de trois niveaux : il appelle `attaquant.getPuissanceAttaque()` puis
`cible.subirDegats(...)`, et c'est l'objet lui-même qui sait comment calculer / répartir.
Conséquences :
- le moteur est **découplé** de la structure de l'arbre (profondeur, composition) ;
- la frappe ciblée réutilise *exactement* le même code que l'attaque totale ;
- ajouter un nouveau type d'unité (le `Soigneur` en est la preuve) ne demande **aucune
  modification** du moteur ni de `Escouade` (principe Ouvert/Fermé).

### Q2. Transparence vs Sécurité

| | **Transparence** (choix du TP) | **Sécurité** |
|---|---|---|
| Où sont `ajouter/supprimer` ? | Dans le Composant `IUnite` (méthodes `default`) | Uniquement dans `Escouade` |
| Uniformité pour le client | Totale : tout est `IUnite` | Le client doit connaître le type concret (`Escouade`) pour composer l'arbre |
| Erreur `feuille.ajouter(x)` | Détectée **à l'exécution** (`UnsupportedOperationException`) | Détectée **à la compilation** (méthode inexistante) |

**Avantages de la transparence (implémentée)** :
- le client traite tous les nœuds de manière identique (ex. `Recrutement` appelle
  `destination.ajouter(unite)` sur un `IUnite` sans cast) ;
- les méthodes `default` de Java 8+ évitent de réécrire le code de refus dans chaque feuille ;
- `getSousUnites()` renvoie `List.of()` pour une feuille : les parcours récursifs
  (`ParcoursArbre`) n'ont aucun cas particulier.

**Inconvénients** :
- perte de sûreté de typage : une feuille « propose » `ajouter()` qu'elle ne sait pas faire,
  ce qui viole en partie le principe de substitution de Liskov ;
- l'erreur n'apparaît qu'à l'exécution, il faut donc la tester (cf. `TestsClashOfGuilds`).

Nous avons atténué ce défaut en ajoutant `estComposite()` pour que le client puisse vérifier
la capacité d'un nœud avant d'y ajouter un enfant (utilisé dans les menus de recrutement).

### Q3. Récursion structurelle : `armee.subirDegats(120)`

Arbre : `Armee → { E1 → {A1, A2}, E2 → {A3, A4} }` (4 archers de 75 PV).

1. `armee.subirDegats(120)` : l'armée filtre ses membres vivants → `[E1, E2]` (N = 2).
   `part = 120 / 2 = 60`, reliquat `120 % 2 = 0`.
2. Appel `E1.subirDegats(60)` :
   - E1 filtre ses vivants → `[A1, A2]`, `part = 60 / 2 = 30`.
   - `A1.subirDegats(30)` → feuille : PV 75 → 45 (cas de base, la récursion s'arrête).
   - `A2.subirDegats(30)` → PV 75 → 45.
3. Appel `E2.subirDegats(60)` : idem, `A3` et `A4` passent à 45 PV.
4. Retour : `armee.getPointsDeVie()` = 45 × 4 = 180 (somme récursive).

Si `A4` était un **Guerrier**, il ne perdrait que `max(1, 30 − 5) = 25` PV : la règle
spécifique reste encapsulée dans la feuille. Si la part ne tombe pas juste (ex. 10 dégâts sur
3 unités), le reliquat est distribué : 4 / 3 / 3.

**Intérêt** : le client écrit **une seule ligne** (`cible.subirDegats(degats)`), sans boucle
ni `instanceof`, quelle que soit la profondeur de l'arbre. Chaque nœud ne connaît que ses
enfants directs ; la structure de l'arbre « guide » le parcours (récursion structurelle). Le
même mécanisme sert à `getPointsDeVie()`, `getPuissanceAttaque()`, `getCout()`,
`recevoirSoins()` et `afficherStructure()`.

### Q4. Cycles et références circulaires

- **E → E** (une escouade s'ajoute elle-même) ou **A → B → A** : l'arbre devient un
  **graphe cyclique**. L'ajout dans l'`ArrayList` réussit, mais au premier appel récursif
  (`getPointsDeVie()`, `subirDegats()`, `afficherStructure()`…) la méthode se rappelle
  indéfiniment : `A.getPointsDeVie()` → `B.getPointsDeVie()` → `A.getPointsDeVie()` → …
  La pile d'appels déborde et la JVM lève une **`java.lang.StackOverflowError`**
  (une `Error`, pas une `Exception` : le jeu planterait).

- **Sécurisation implémentée** dans `Escouade.ajouter()` :
  ```java
  if (unite == null) throw new IllegalArgumentException(...);
  if (unite == this || contientRecursivement(unite, this))   // E->E et A->B->A
      throw new IllegalArgumentException("Cycle detecte ...");
  if (membres.contains(unite)) throw new IllegalArgumentException(...); // doublon
  ```
  `contientRecursivement(racine, cible)` parcourt le sous-arbre de l'unité à ajouter : si
  l'escouade courante y figure déjà, elle est un descendant de l'unité, et l'ajout créerait
  un cycle. On refuse donc l'opération **avant** de corrompre la structure.
  De plus, `getSousUnites()` renvoie une vue non modifiable : impossible de contourner ce
  contrôle en modifiant directement la liste.

  Autres pistes possibles : stocker une référence `parent` dans chaque nœud (une unité ne
  peut avoir qu'un seul parent — interdit aussi le partage d'une unité entre deux escouades),
  ou utiliser un `Set` d'objets visités (`IdentityHashMap`) lors des parcours.

---

## 3. Bonus réalisés

- **Extension 1 — Soigneur** : `IUnite.recevoirSoins(int)` est réparti comme les dégâts dans
  l'escouade, sans dépasser les PV max ni ressusciter les morts ;
  `Soigneur.soigner(cible)` l'utilise sur l'escouade parente du soigneur.
- **Extension 2 — Recrutement budgété** : mode 2 au lancement, 500 PO par joueur, création
  d'escouades imbriquées et recrutement ; le budget restant = `500 − armee.getCout()`
  (calcul récursif).
- **Gestion avancée des cycles** : voir Q4.
