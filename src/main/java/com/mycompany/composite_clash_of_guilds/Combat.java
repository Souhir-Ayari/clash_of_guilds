package com.mycompany.composite_clash_of_guilds;

import java.util.ArrayList;
import java.util.List;

/**
 * Moteur de combat (logique metier pure, aucun affichage).
 *
 * Il ne connait que l'abstraction IUnite : un soldat, une escouade ou une
 * armee entiere s'attaquent et encaissent exactement de la meme facon.
 */
public final class Combat {

    /** Compte-rendu d'un echange, exploite ensuite par la couche presentation. */
    public record Resultat(IUnite attaquant, IUnite cible, int degatsBruts,
                           int pvAvant, int pvApres, List<IUnite> pertes) {

        public int pvPerdus() {
            return pvAvant - pvApres;
        }
    }

    /** Bonus d'armure accorde a chaque combattant par l'action "Se defendre". */
    public static final int BONUS_DEFENSE = 10;

    private Combat() {
    }

    /** L'armee entiere leve ses boucliers jusqu'au prochain tour de son joueur. */
    public static void defendre(IUnite armee) {
        armee.defendre(BONUS_DEFENSE);
    }

    /** Fin de la posture defensive (appele au debut du tour suivant du joueur). */
    public static void finirDefense(IUnite armee) {
        armee.defendre(0);
    }

    /** L'attaquant frappe la cible avec toute sa puissance (synergies comprises). */
    public static Resultat attaquer(IUnite attaquant, IUnite cible) {
        List<IUnite> vivantsAvant = new ArrayList<>();
        for (IUnite u : ParcoursArbre.feuilles(cible)) {
            if (u.estVivant()) {
                vivantsAvant.add(u);
            }
        }
        int degats = attaquant.getPuissanceAttaque();
        int pvAvant = cible.getPointsDeVie();

        cible.subirDegats(degats); // <- toute la recursion est cachee ici

        List<IUnite> pertes = new ArrayList<>();
        for (IUnite u : vivantsAvant) {
            if (!u.estVivant()) {
                pertes.add(u);
            }
        }
        return new Resultat(attaquant, cible, degats, pvAvant, cible.getPointsDeVie(), pertes);
    }
}
