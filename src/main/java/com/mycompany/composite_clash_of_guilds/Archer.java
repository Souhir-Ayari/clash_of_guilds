package com.mycompany.composite_clash_of_guilds;

/**
 * FEUILLE : attaquant precis et veloce, degats reguliers a moyenne portee.
 * Regle metier : encaisse la totalite des degats recus.
 */
public class Archer extends UniteElementaire {

    public static final int PV_MAX = 75;
    public static final int ATTAQUE = 40;
    public static final int COUT = 70;

    public Archer(String nom) {
        this(nom, PV_MAX);
    }

    /** Variante "veteran" avec des PV max personnalises. */
    public Archer(String nom, int pointsDeVieMax) {
        super(nom, pointsDeVieMax, ATTAQUE, COUT);
    }

    @Override
    public String getType() {
        return "Archer";
    }
}
