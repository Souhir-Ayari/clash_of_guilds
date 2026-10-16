package com.mycompany.composite_clash_of_guilds;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * CLIENT du patron Composite : moteur de jeu "Clash of Guilds" a 3 joueurs.
 *
 * Gere la boucle de tours, les menus et l'affichage. Toute la logique de
 * combat passe par l'abstraction IUnite (via la classe Combat) : le jeu
 * ignore la profondeur et la composition reelle des armees.
 */
public class JeuConsole {

    private static final String TIRETS = "------------------------------------------------------------";
    private static final String EGAUX = "============================================================";

    private final Saisie saisie;
    private final List<Joueur> joueurs = new ArrayList<>();
    private int numeroTour = 1;

    public JeuConsole(Saisie saisie) {
        this.saisie = saisie;
    }

    public static void main(String[] args) {
        Saisie saisie = new Saisie(new Scanner(System.in));
        try {
            new JeuConsole(saisie).lancer();
        } catch (Saisie.FinDeSaisieException e) {
            System.out.println();
            System.out.println("Entree fermee : fin de la partie. A bientot !");
        }
    }

    // ===============================================================
    // Initialisation
    // ===============================================================

    public void lancer() {
        afficherTitre();
        afficherRegles();

        System.out.println("Mode de jeu :");
        System.out.println("  1. Armees predefinies (partie rapide)");
        System.out.printf("  2. Recrutement budgete (%d PO par joueur)%n", Recrutement.BUDGET_INITIAL);
        int mode = saisie.lireEntier("Votre choix : ", 1, 2);

        String[] factions = {GuildesPredefinies.LIONS, GuildesPredefinies.OMBRES, GuildesPredefinies.GARDIENS};
        String[] nomsParDefaut = {"Alice", "Bob", "Charlie"};
        Recrutement recrutement = new Recrutement(saisie);

        System.out.println();
        for (int i = 0; i < factions.length; i++) {
            String nom = saisie.lireTexte(String.format("Nom du Joueur %d (Guilde %s) [%s] : ",
                    i + 1, factions[i], nomsParDefaut[i]), nomsParDefaut[i]);
            Escouade armee;
            if (mode == 2) {
                armee = recrutement.recruter(nom, factions[i]);
            } else {
                armee = switch (i) {
                    case 0 -> GuildesPredefinies.armeeLions();
                    case 1 -> GuildesPredefinies.armeeOmbres();
                    default -> GuildesPredefinies.armeeGardiens();
                };
            }
            joueurs.add(new Joueur(nom, factions[i], armee));
        }

        System.out.println();
        System.out.println(">> Les trois guildes sont pretes. Que la bataille commence !");
        System.out.println();
        for (Joueur j : joueurs) {
            j.afficherArmee();
            System.out.println();
        }
        boucleDeJeu();
    }

    private void afficherTitre() {
        System.out.println(EGAUX);
        System.out.println("     ____ _           _              __    ____       _ _     _     ");
        System.out.println("    / ___| | __ _ ___| |__     ___  / _|  / ___|_   _(_) | __| |___ ");
        System.out.println("   | |   | |/ _` / __| '_ \\   / _ \\| |_  | |  _| | | | | |/ _` / __|");
        System.out.println("   | |___| | (_| \\__ \\ | | | | (_) |  _| | |_| | |_| | | | (_| \\__ \\");
        System.out.println("    \\____|_|\\__,_|___/_| |_|  \\___/|_|    \\____|\\__,_|_|_|\\__,_|___/");
        System.out.println();
        System.out.println("          Wargame tactique a 3 joueurs - Patron Composite");
        System.out.println(EGAUX);
    }

    private void afficherRegles() {
        System.out.println("REGLES :");
        System.out.println(" - Chaque guilde est un arbre : Armee -> Bataillons -> Escouades -> Soldats.");
        System.out.println(" - ATQ d'un groupe = somme des ATQ vivantes, +10% si au moins 2 membres vivants.");
        System.out.println(" - Les degats recus par un groupe sont repartis equitablement et en cascade.");
        System.out.println(" - Guerrier : armure 5 (degats nets = max(1, degats - 5)).");
        System.out.println(" - Soigneur : soigne son escouade (+40 PV repartis, sans depasser le max).");
        System.out.println(" - La derniere guilde en vie remporte la victoire !");
        System.out.println(EGAUX);
        System.out.println();
    }

    // ===============================================================
    // Boucle de jeu
    // ===============================================================

    private void boucleDeJeu() {
        int indexActif = 0;
        while (joueursEnLice().size() > 1) {
            Joueur actif = joueurs.get(indexActif);
            if (!actif.estElimine()) {
                jouerTour(actif);
                numeroTour++;
            }
            indexActif = (indexActif + 1) % joueurs.size();
        }
        proclamerVainqueur(joueursEnLice().get(0));
    }

    private List<Joueur> joueursEnLice() {
        List<Joueur> enLice = new ArrayList<>();
        for (Joueur j : joueurs) {
            if (!j.estElimine()) {
                enLice.add(j);
            }
        }
        return enLice;
    }

    private void jouerTour(Joueur actif) {
        boolean tourTermine = false;
        while (!tourTermine) {
            Escouade armee = actif.getArmee();
            System.out.println();
            System.out.println(TIRETS);
            System.out.printf(">> Tour %d - %s [%s] (PV : %d | ATQ : %d)%n", numeroTour,
                    actif.getNom(), actif.getFaction(), armee.getPointsDeVie(), armee.getPuissanceAttaque());
            System.out.println(TIRETS);
            System.out.println("  1. Inspecter mon armee");
            System.out.println("  2. Inspecter les armees ennemies");
            System.out.println("  3. Attaque Totale (toute l'armee contre une guilde rivale)");
            System.out.println("  4. Frappe Ciblee (une sous-escouade frappe une cible precise)");
            List<Soigneur> soigneurs = soigneursVivants(armee);
            if (!soigneurs.isEmpty()) {
                System.out.println("  5. Soigner (un Soigneur restaure les PV de son escouade)");
            }
            int choix = saisie.lireEntier("Votre action : ", 1, soigneurs.isEmpty() ? 4 : 5);

            switch (choix) {
                case 1 -> actif.afficherArmee();
                case 2 -> inspecterEnnemis(actif);
                case 3 -> tourTermine = attaqueTotale(actif);
                case 4 -> tourTermine = frappeCiblee(actif);
                default -> tourTermine = soigner(actif, soigneurs);
            }
        }
    }

    private void inspecterEnnemis(Joueur actif) {
        for (Joueur j : joueurs) {
            if (j != actif) {
                j.afficherArmee();
                if (j.estElimine()) {
                    System.out.println("  (Guilde aneantie)");
                }
                System.out.println();
            }
        }
    }

    /** Demande une guilde ennemie encore en lice ; null si le joueur annule. */
    private Joueur choisirAdversaire(Joueur actif) {
        List<Joueur> adversaires = new ArrayList<>();
        for (Joueur j : joueursEnLice()) {
            if (j != actif) {
                adversaires.add(j);
            }
        }
        System.out.println("Choisissez une guilde ennemie a attaquer :");
        for (int i = 0; i < adversaires.size(); i++) {
            Joueur j = adversaires.get(i);
            System.out.printf("  %d. %s [%s] (PV restants : %d)%n",
                    i + 1, j.getNom(), j.getFaction(), j.getArmee().getPointsDeVie());
        }
        System.out.println("  0. Annuler");
        int choix = saisie.lireEntier("Numero de la cible : ", 0, adversaires.size());
        return choix == 0 ? null : adversaires.get(choix - 1);
    }

    private boolean attaqueTotale(Joueur actif) {
        Joueur cible = choisirAdversaire(actif);
        if (cible == null) {
            return false;
        }
        Combat.Resultat r = Combat.attaquer(actif.getArmee(), cible.getArmee());
        afficherResultat(r, cible);
        return true;
    }

    private boolean frappeCiblee(Joueur actif) {
        // 1) Choix de l'unite / sous-escouade d'elite qui mene la frappe
        List<ParcoursArbre.Noeud> attaquants = noeudsVivants(actif.getArmee());
        System.out.println("Quelle unite ou sous-escouade mene la frappe ?");
        IUnite attaquant = choisirNoeud(attaquants);
        if (attaquant == null) {
            return false;
        }

        // 2) Choix de la guilde puis du point d'impact precis
        Joueur cible = choisirAdversaire(actif);
        if (cible == null) {
            return false;
        }
        List<ParcoursArbre.Noeud> points = new ArrayList<>();
        points.add(new ParcoursArbre.Noeud(cible.getArmee(), 0));
        points.addAll(noeudsVivants(cible.getArmee()));
        System.out.println("Quel point de l'armee ennemie viser ?");
        IUnite pointImpact = choisirNoeud(points);
        if (pointImpact == null) {
            return false;
        }

        Combat.Resultat r = Combat.attaquer(attaquant, pointImpact);
        afficherResultat(r, cible);
        return true;
    }

    private boolean soigner(Joueur actif, List<Soigneur> soigneurs) {
        System.out.println("Quel Soigneur intervient ?");
        for (int i = 0; i < soigneurs.size(); i++) {
            Soigneur s = soigneurs.get(i);
            IUnite groupe = ParcoursArbre.trouverParent(actif.getArmee(), s);
            System.out.printf("  %d. %s (soigne [%s])%n", i + 1, s.getNom(), groupe.getNom());
        }
        System.out.println("  0. Annuler");
        int choix = saisie.lireEntier("Votre choix : ", 0, soigneurs.size());
        if (choix == 0) {
            return false;
        }
        Soigneur soigneur = soigneurs.get(choix - 1);
        IUnite groupe = ParcoursArbre.trouverParent(actif.getArmee(), soigneur);
        int restaures = soigneur.soigner(groupe);
        System.out.println();
        System.out.println(EGAUX);
        System.out.printf(">> %s canalise l'energie runique sur [%s] !%n", soigneur.getNom(), groupe.getNom());
        System.out.printf(">> %d PV restaures (PV du groupe : %d/%d).%n",
                restaures, groupe.getPointsDeVie(), groupe.getPointsDeVieMax());
        System.out.println(EGAUX);
        return true;
    }

    // ===============================================================
    // Aides d'affichage
    // ===============================================================

    private List<ParcoursArbre.Noeud> noeudsVivants(IUnite racine) {
        List<ParcoursArbre.Noeud> vivants = new ArrayList<>();
        for (ParcoursArbre.Noeud n : ParcoursArbre.descendants(racine)) {
            if (n.unite().estVivant()) {
                vivants.add(n);
            }
        }
        return vivants;
    }

    private IUnite choisirNoeud(List<ParcoursArbre.Noeud> noeuds) {
        for (int i = 0; i < noeuds.size(); i++) {
            ParcoursArbre.Noeud n = noeuds.get(i);
            IUnite u = n.unite();
            System.out.printf("  %2d. %s%s (PV : %d, ATQ : %d)%n", i + 1, "   ".repeat(n.profondeur()),
                    u, u.getPointsDeVie(), u.getPuissanceAttaque());
        }
        System.out.println("   0. Annuler");
        int choix = saisie.lireEntier("Votre choix : ", 0, noeuds.size());
        return choix == 0 ? null : noeuds.get(choix - 1).unite();
    }

    private List<Soigneur> soigneursVivants(IUnite armee) {
        List<Soigneur> soigneurs = new ArrayList<>();
        for (IUnite u : ParcoursArbre.feuilles(armee)) {
            if (u instanceof Soigneur s && s.estVivant()) {
                soigneurs.add(s);
            }
        }
        return soigneurs;
    }

    private void afficherResultat(Combat.Resultat r, Joueur cible) {
        System.out.println();
        System.out.println(EGAUX);
        System.out.printf(">> L'unite [%s] frappe [%s] !%n", r.attaquant().getNom(), r.cible().getNom());
        System.out.printf(">> Degats bruts deployes : %d points d'impact !%n", r.degatsBruts());
        System.out.println(EGAUX);
        System.out.printf("Resultat : [%s] perd %d PV (%d -> %d).%n",
                r.cible().getNom(), r.pvPerdus(), r.pvAvant(), r.pvApres());
        for (IUnite mort : r.pertes()) {
            System.out.printf("   +  %s tombe au combat !%n", mort);
        }
        if (cible.estElimine()) {
            System.out.println();
            System.out.printf("*** La Guilde [%s] de %s est ANEANTIE ! ***%n", cible.getFaction(), cible.getNom());
        } else {
            System.out.printf("La Guilde [%s] conserve %d PV.%n",
                    cible.getFaction(), cible.getArmee().getPointsDeVie());
        }
        saisie.attendreEntree();
    }

    private void proclamerVainqueur(Joueur vainqueur) {
        System.out.println();
        System.out.println(EGAUX);
        System.out.println("                      VICTOIRE !");
        System.out.printf("   %s et la Guilde [%s] remportent le Clash of Guilds%n",
                vainqueur.getNom(), vainqueur.getFaction());
        System.out.printf("   en %d tours. Forces survivantes :%n", numeroTour - 1);
        System.out.println(EGAUX);
        vainqueur.afficherArmee();
    }
}
