package com.mycompany.composite_clash_of_guilds;

/**
 * Base commune des FEUILLES (Leaf) du patron Composite.
 *
 * Factorise l'etat et le comportement partages par tous les combattants
 * individuels (PV, attaque, cout, affichage). Chaque sous-classe ne
 * redefinit que ce qui lui est specifique (ex. : l'armure du Guerrier).
 * Ajouter un nouveau type d'unite = creer une sous-classe, sans toucher
 * au moteur de combat ni a la classe Escouade.
 */
public abstract class UniteElementaire implements IUnite {

    private final String nom;
    private final int pointsDeVieMax;
    private final int attaque;
    private final int cout;
    private int pointsDeVie;
    private int bonusDefense;

    protected UniteElementaire(String nom, int pointsDeVieMax, int attaque, int cout) {
        if (pointsDeVieMax <= 0) {
            throw new IllegalArgumentException("Les PV max doivent etre strictement positifs.");
        }
        this.nom = nom;
        this.pointsDeVieMax = pointsDeVieMax;
        this.pointsDeVie = pointsDeVieMax;
        this.attaque = attaque;
        this.cout = cout;
    }

    /** Libelle du corps de metier affiche dans l'arbre (ex. "Guerrier"). */
    public abstract String getType();

    /**
     * Degats reellement retires des PV. Par defaut l'unite encaisse tout ;
     * le Guerrier redefinit cette methode pour appliquer son armure.
     */
    protected int calculerDegatsNets(int montant) {
        return montant;
    }

    /** Statistiques supplementaires a afficher (ex. ", ARM : 5"). */
    protected String detailsSupplementaires() {
        return "";
    }

    private String detailsDefense() {
        return bonusDefense > 0 ? ", DEF +" + bonusDefense : "";
    }

    @Override
    public String getNom() {
        return nom;
    }

    @Override
    public int getPointsDeVie() {
        return pointsDeVie;
    }

    @Override
    public int getPointsDeVieMax() {
        return pointsDeVieMax;
    }

    @Override
    public int getPuissanceAttaque() {
        // Une unite vaincue ne peut plus attaquer.
        return estVivant() ? attaque : 0;
    }

    @Override
    public void subirDegats(int montant) {
        // Une unite vaincue n'absorbe plus de degats.
        if (!estVivant() || montant <= 0) {
            return;
        }
        int net = calculerDegatsNets(montant);
        if (bonusDefense > 0) {
            net = Math.max(1, net - bonusDefense);
        }
        pointsDeVie = Math.max(0, pointsDeVie - net);
    }

    @Override
    public void defendre(int bonusArmure) {
        bonusDefense = Math.max(0, bonusArmure);
    }

    /** Bonus d'armure de la posture defensive en cours (0 si aucune). */
    public int getBonusDefense() {
        return bonusDefense;
    }

    @Override
    public void recevoirSoins(int montant) {
        // On ne ressuscite pas les morts et on ne depasse pas les PV max.
        if (!estVivant() || montant <= 0) {
            return;
        }
        pointsDeVie = Math.min(pointsDeVieMax, pointsDeVie + montant);
    }

    @Override
    public boolean estVivant() {
        return pointsDeVie > 0;
    }

    @Override
    public int getCout() {
        return cout;
    }

    @Override
    public void afficherStructure(String prefix, boolean isDernier) {
        String connecteur = isDernier ? "\\-- " : "|-- ";
        String etiquette = String.format("%-11s", "[" + getType() + "]");
        String statut = estVivant() ? "" : "  >> TOMBE AU COMBAT";
        System.out.printf("%s%s%s %s (PV : %d/%d, ATQ : %d%s)%s%n",
                prefix, connecteur, etiquette, nom, pointsDeVie, pointsDeVieMax,
                getPuissanceAttaque(), detailsSupplementaires() + detailsDefense(), statut);
    }

    @Override
    public String toString() {
        return "[" + getType() + "] " + nom;
    }
}
