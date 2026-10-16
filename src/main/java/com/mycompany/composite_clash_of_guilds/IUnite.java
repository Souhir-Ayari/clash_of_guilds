package com.mycompany.composite_clash_of_guilds;

import java.util.List;

/**
 * COMPOSANT (Component) du patron Composite.
 *
 * Contrat unique partage par les feuilles (Guerrier, Archer, Mage, Soigneur)
 * et par le composite (Escouade). Le moteur de jeu ne manipule QUE ce type :
 * il ne sait jamais s'il a affaire a un soldat isole ou a une armee entiere.
 *
 * Choix de conception : TRANSPARENCE. Les methodes de gestion des enfants
 * (ajouter, supprimer, getSousUnites) sont declarees ici avec une
 * implementation par defaut ; une feuille leve UnsupportedOperationException.
 */
public interface IUnite {

    String getNom();

    /** PV actuels (pour un groupe : somme recursive des PV des membres vivants). */
    int getPointsDeVie();

    /** PV maximum (pour un groupe : somme recursive des PV max de tous les membres). */
    int getPointsDeVieMax();

    /** Puissance offensive (0 si l'unite est vaincue ; bonus de synergie pour un groupe). */
    int getPuissanceAttaque();

    /** Encaisse des degats (un groupe les repartit equitablement et recursivement). */
    void subirDegats(int montant);

    /** Recoit des soins sans depasser les PV max (un groupe les repartit recursivement). */
    void recevoirSoins(int montant);

    /**
     * Posture defensive : chaque combattant gagne un bonus d'armure (0 = fin de la defense).
     * Un groupe propage l'ordre recursivement a tous ses membres.
     */
    void defendre(int bonusArmure);

    boolean estVivant();

    /** Cout de recrutement en pieces d'or (pour un groupe : somme des couts). */
    int getCout();

    // Affichage arborescent textuel
    void afficherStructure(String prefix, boolean isDernier);

    // ---------------------------------------------------------------
    // Methodes de gestion Composite par transparence
    // ---------------------------------------------------------------
    default void ajouter(IUnite unite) {
        throw new UnsupportedOperationException("Impossible d'ajouter sur une feuille.");
    }

    default void supprimer(IUnite unite) {
        throw new UnsupportedOperationException("Impossible de retirer sur une feuille.");
    }

    default List<IUnite> getSousUnites() {
        return List.of();
    }

    /** Vrai pour un noeud capable de contenir des sous-unites (composite). */
    default boolean estComposite() {
        return false;
    }
}
