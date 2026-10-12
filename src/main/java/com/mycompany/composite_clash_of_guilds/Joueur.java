package com.mycompany.composite_clash_of_guilds;

/**
 * Un joueur : son nom, celui de sa faction (guilde) et son armee principale,
 * racine de l'arbre Composite.
 */
public class Joueur {

    public static final String SEPARATEUR =
            "========================================================";

    private final String nom;
    private final String faction;
    private final Escouade armee;

    public Joueur(String nom, String faction, Escouade armee) {
        this.nom = nom;
        this.faction = faction;
        this.armee = armee;
    }

    public String getNom() {
        return nom;
    }

    public String getFaction() {
        return faction;
    }

    public Escouade getArmee() {
        return armee;
    }

    public boolean estElimine() {
        return !armee.estVivant();
    }

    /** Affiche la fiche de la guilde puis l'arbre detaille de ses forces. */
    public void afficherArmee() {
        System.out.println(SEPARATEUR);
        System.out.printf("  [GUILDE] : %s (Commandant : %s)%n", faction.toUpperCase(), nom);
        System.out.printf("  PV Totaux : %d | Puissance d'Attaque : %d | Valeur : %d PO%n",
                armee.getPointsDeVie(), armee.getPuissanceAttaque(), armee.getCout());
        System.out.println(SEPARATEUR);
        armee.afficherStructure("", true);
        System.out.println(SEPARATEUR);
    }

    @Override
    public String toString() {
        return nom + " [" + faction + "]";
    }
}
