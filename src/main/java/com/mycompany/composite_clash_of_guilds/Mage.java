package com.mycompany.composite_clash_of_guilds;

/**
 * FEUILLE : "canon de verre" (Glass Cannon).
 * Peu de points de vie mais une puissance de frappe devastatrice.
 */
public class Mage extends UniteElementaire {

    public static final int PV_MAX = 50;
    public static final int ATTAQUE = 65;
    public static final int COUT = 100;

    public Mage(String nom) {
        this(nom, PV_MAX);
    }

    /** Variante "veteran" avec des PV max personnalises. */
    public Mage(String nom, int pointsDeVieMax) {
        super(nom, pointsDeVieMax, ATTAQUE, COUT);
    }

    @Override
    public String getType() {
        return "Mage";
    }
}
