package com.mycompany.composite_clash_of_guilds;

import java.util.List;

/**
 * Extension 2 (bonus) : phase de recrutement budgetee.
 *
 * Chaque joueur dispose d'une bourse de 500 pieces d'or et compose
 * interactivement son arbre d'unites. Le budget restant se calcule
 * simplement par armee.getCout() : une seule methode recursive suffit,
 * quelle que soit la profondeur de l'arbre.
 */
public class Recrutement {

    public static final int BUDGET_INITIAL = 500;

    private final Saisie saisie;

    public Recrutement(Saisie saisie) {
        this.saisie = saisie;
    }

    public Escouade recruter(String nomJoueur, String faction) {
        Escouade armee = new Escouade("Armee des " + faction);
        System.out.println();
        System.out.println(Joueur.SEPARATEUR);
        System.out.printf("  RECRUTEMENT : %s [%s] - Bourse : %d PO%n", nomJoueur, faction, BUDGET_INITIAL);
        System.out.println(Joueur.SEPARATEUR);

        boolean termine = false;
        while (!termine) {
            int budget = BUDGET_INITIAL - armee.getCout();
            System.out.printf("%n  Budget restant : %d PO%n", budget);
            System.out.println("  1. Recruter une unite");
            System.out.println("  2. Creer une escouade (sous-groupe)");
            System.out.println("  3. Afficher mon armee");
            System.out.println("  0. Terminer le recrutement");
            int choix = saisie.lireEntier("  Votre choix : ", 0, 3);
            switch (choix) {
                case 1 -> recruterUnite(armee, budget);
                case 2 -> creerEscouade(armee);
                case 3 -> armee.afficherStructure("  ", true);
                default -> {
                    if (armee.estVivant()) {
                        termine = true;
                    } else {
                        System.out.println("   /!\\ Votre armee doit compter au moins un combattant !");
                    }
                }
            }
        }
        return armee;
    }

    private void recruterUnite(Escouade armee, int budget) {
        System.out.printf("   1. Guerrier (%d PO) - PV %d, ATQ %d, ARM %d%n",
                Guerrier.COUT, Guerrier.PV_MAX, Guerrier.ATTAQUE, Guerrier.ARMURE);
        System.out.printf("   2. Archer   (%d PO) - PV %d, ATQ %d%n",
                Archer.COUT, Archer.PV_MAX, Archer.ATTAQUE);
        System.out.printf("   3. Mage     (%d PO) - PV %d, ATQ %d%n",
                Mage.COUT, Mage.PV_MAX, Mage.ATTAQUE);
        System.out.printf("   4. Soigneur (%d PO) - PV %d, ATQ %d, SOIN %d%n",
                Soigneur.COUT, Soigneur.PV_MAX, Soigneur.ATTAQUE, Soigneur.PUISSANCE_SOIN);
        System.out.println("   0. Annuler");
        int type = saisie.lireEntier("   Type d'unite : ", 0, 4);
        if (type == 0) {
            return;
        }
        int cout = switch (type) {
            case 1 -> Guerrier.COUT;
            case 2 -> Archer.COUT;
            case 3 -> Mage.COUT;
            default -> Soigneur.COUT;
        };
        if (cout > budget) {
            System.out.printf("   /!\\ Fonds insuffisants (%d PO requis, %d PO disponibles).%n", cout, budget);
            return;
        }
        String nomParDefaut = switch (type) {
            case 1 -> "Guerrier";
            case 2 -> "Archer";
            case 3 -> "Mage";
            default -> "Soigneur";
        } + "-" + (ParcoursArbre.feuilles(armee).size() + 1);
        String nom = saisie.lireTexte("   Nom de l'unite [" + nomParDefaut + "] : ", nomParDefaut);
        IUnite unite = switch (type) {
            case 1 -> new Guerrier(nom);
            case 2 -> new Archer(nom);
            case 3 -> new Mage(nom);
            default -> new Soigneur(nom);
        };
        IUnite destination = choisirGroupe(armee, "   Affecter a quel groupe ? ");
        destination.ajouter(unite);
        System.out.printf("   >> %s rejoint [%s] (-%d PO).%n", unite, destination.getNom(), cout);
    }

    private void creerEscouade(Escouade armee) {
        String nomParDefaut = "Escouade " + ParcoursArbre.groupes(armee).size();
        String nom = saisie.lireTexte("   Nom de l'escouade [" + nomParDefaut + "] : ", nomParDefaut);
        IUnite parent = choisirGroupe(armee, "   Rattacher a quel groupe ? ");
        parent.ajouter(new Escouade(nom));
        System.out.printf("   >> Escouade [%s] creee dans [%s].%n", nom, parent.getNom());
    }

    private IUnite choisirGroupe(Escouade armee, String invite) {
        List<ParcoursArbre.Noeud> groupes = ParcoursArbre.groupes(armee);
        if (groupes.size() == 1) {
            return armee;
        }
        for (int i = 0; i < groupes.size(); i++) {
            ParcoursArbre.Noeud n = groupes.get(i);
            System.out.printf("     %d. %s%s%n", i + 1, "  ".repeat(n.profondeur()), n.unite().getNom());
        }
        return groupes.get(saisie.lireEntier(invite, 1, groupes.size()) - 1).unite();
    }
}
