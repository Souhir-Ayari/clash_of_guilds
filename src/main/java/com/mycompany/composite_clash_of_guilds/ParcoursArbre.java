package com.mycompany.composite_clash_of_guilds;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaires de parcours de l'arbre Composite, utilises par l'interface
 * console (menus de selection). Ils ne s'appuient que sur IUnite.
 */
public final class ParcoursArbre {

    /** Une unite de l'arbre et sa profondeur (pour l'indentation des menus). */
    public record Noeud(IUnite unite, int profondeur) {
    }

    private ParcoursArbre() {
    }

    /** Tous les descendants de la racine (racine exclue), en profondeur d'abord. */
    public static List<Noeud> descendants(IUnite racine) {
        List<Noeud> resultat = new ArrayList<>();
        collecter(racine, 1, resultat);
        return resultat;
    }

    private static void collecter(IUnite parent, int profondeur, List<Noeud> resultat) {
        for (IUnite enfant : parent.getSousUnites()) {
            resultat.add(new Noeud(enfant, profondeur));
            collecter(enfant, profondeur + 1, resultat);
        }
    }

    /** La racine et tous ses sous-groupes (composites), pour choisir une destination. */
    public static List<Noeud> groupes(IUnite racine) {
        List<Noeud> resultat = new ArrayList<>();
        resultat.add(new Noeud(racine, 0));
        for (Noeud n : descendants(racine)) {
            if (n.unite().estComposite()) {
                resultat.add(n);
            }
        }
        return resultat;
    }

    /** Toutes les feuilles (combattants individuels) du sous-arbre. */
    public static List<IUnite> feuilles(IUnite racine) {
        List<IUnite> resultat = new ArrayList<>();
        if (!racine.estComposite()) {
            resultat.add(racine);
            return resultat;
        }
        for (IUnite enfant : racine.getSousUnites()) {
            resultat.addAll(feuilles(enfant));
        }
        return resultat;
    }

    /** Le groupe parent direct de 'cible' dans l'arbre, ou null si introuvable. */
    public static IUnite trouverParent(IUnite racine, IUnite cible) {
        for (IUnite enfant : racine.getSousUnites()) {
            if (enfant == cible) {
                return racine;
            }
            IUnite parent = trouverParent(enfant, cible);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }
}
