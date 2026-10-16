package com.mycompany.composite_clash_of_guilds;

/**
 * FEUILLE (Extension 1 - bonus) : unite de soutien.
 *
 * Son action soigner() s'appuie sur recevoirSoins() de l'interface IUnite :
 * appliquee a une escouade, elle se propage recursivement a tous les allies
 * vivants, sans jamais depasser leurs PV maximaux.
 *
 * Preuve d'extensibilite : cette classe a ete ajoutee sans modifier ni
 * Escouade ni le moteur de combat.
 */
public class Soigneur extends UniteElementaire {

    public static final int PV_MAX = 60;
    public static final int ATTAQUE = 10;
    public static final int COUT = 90;
    public static final int PUISSANCE_SOIN = 40;

    private final int puissanceSoin;

    public Soigneur(String nom) {
        this(nom, PV_MAX);
    }

    public Soigneur(String nom, int pointsDeVieMax) {
        super(nom, pointsDeVieMax, ATTAQUE, COUT);
        this.puissanceSoin = PUISSANCE_SOIN;
    }

    public int getPuissanceSoin() {
        return puissanceSoin;
    }

    /**
     * Soigne une cible (unite ou escouade entiere) avec un montant donne.
     * Un soigneur vaincu ne soigne plus.
     *
     * @return le nombre de PV effectivement restaures
     */
    public int soigner(IUnite cible, int montant) {
        if (!estVivant() || cible == null) {
            return 0;
        }
        int avant = cible.getPointsDeVie();
        cible.recevoirSoins(montant);
        return cible.getPointsDeVie() - avant;
    }

    /** Soigne une cible avec la puissance de soin propre au soigneur. */
    public int soigner(IUnite cible) {
        return soigner(cible, puissanceSoin);
    }

    @Override
    public String getType() {
        return "Soigneur";
    }

    @Override
    protected String detailsSupplementaires() {
        return ", SOIN : " + puissanceSoin;
    }
}
