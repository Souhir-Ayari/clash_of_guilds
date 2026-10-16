package com.mycompany.composite_clash_of_guilds;

/**
 * FEUILLE : unite defensive lourde de premiere ligne.
 * Regle metier : degats nets subis = max(1, degats - armure).
 */
public class Guerrier extends UniteElementaire {

    public static final int PV_MAX = 120;
    public static final int ATTAQUE = 25;
    public static final int ARMURE = 5;
    public static final int COUT = 80;

    private final int armure;

    public Guerrier(String nom) {
        this(nom, PV_MAX);
    }

    /** Variante "veteran" avec des PV max personnalises. */
    public Guerrier(String nom, int pointsDeVieMax) {
        super(nom, pointsDeVieMax, ATTAQUE, COUT);
        this.armure = ARMURE;
    }

    public int getArmure() {
        return armure;
    }

    @Override
    public String getType() {
        return "Guerrier";
    }

    @Override
    protected int calculerDegatsNets(int montant) {
        return Math.max(1, montant - armure);
    }

    @Override
    protected String detailsSupplementaires() {
        return ", ARM : " + armure;
    }
}
