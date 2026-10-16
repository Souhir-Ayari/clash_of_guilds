package com.mycompany.composite_clash_of_guilds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * COMPOSITE du patron Composite : Armee, Bataillon, Escouade...
 *
 * Contient une liste heterogene de IUnite (feuilles OU autres escouades).
 * Toutes les operations metier sont deleguees recursivement aux membres :
 * le client n'a jamais a parcourir l'arbre lui-meme.
 */
public class Escouade implements IUnite {

    /** Bonus de synergie : +10% si au moins 2 sous-unites vivantes. */
    public static final double BONUS_SYNERGIE = 0.10;
    public static final int SEUIL_SYNERGIE = 2;

    private final String nom;
    private final List<IUnite> membres = new ArrayList<>();

    public Escouade(String nom) {
        this.nom = nom;
    }

    // ---------------------------------------------------------------
    // Gestion de la hierarchie (avec protection contre les cycles)
    // ---------------------------------------------------------------

    /**
     * Ajoute une sous-unite. Refuse :
     *  - null ;
     *  - l'ajout de l'escouade a elle-meme (E -> E) ;
     *  - un cycle indirect (A -> B -> A), c.-a-d. ajouter un ancetre ;
     *  - un doublon (meme instance deja presente).
     */
    @Override
    public void ajouter(IUnite unite) {
        if (unite == null) {
            throw new IllegalArgumentException("Impossible d'ajouter une unite nulle.");
        }
        if (unite == this || contientRecursivement(unite, this)) {
            throw new IllegalArgumentException(
                    "Cycle detecte : [" + unite.getNom() + "] contient deja [" + nom + "].");
        }
        if (membres.contains(unite)) {
            throw new IllegalArgumentException(
                    "[" + unite.getNom() + "] fait deja partie de [" + nom + "].");
        }
        membres.add(unite);
    }

    @Override
    public void supprimer(IUnite unite) {
        membres.remove(unite);
    }

    /** Vue non modifiable : on ne peut composer l'arbre que via ajouter(). */
    @Override
    public List<IUnite> getSousUnites() {
        return Collections.unmodifiableList(membres);
    }

    @Override
    public boolean estComposite() {
        return true;
    }

    /** Vrai si 'cible' apparait dans le sous-arbre enracine en 'racine'. */
    private static boolean contientRecursivement(IUnite racine, IUnite cible) {
        if (racine == cible) {
            return true;
        }
        for (IUnite enfant : racine.getSousUnites()) {
            if (contientRecursivement(enfant, cible)) {
                return true;
            }
        }
        return false;
    }

    private List<IUnite> getMembresVivants() {
        List<IUnite> vivants = new ArrayList<>();
        for (IUnite membre : membres) {
            if (membre.estVivant()) {
                vivants.add(membre);
            }
        }
        return vivants;
    }

    // ---------------------------------------------------------------
    // Logique metier recursive
    // ---------------------------------------------------------------

    @Override
    public String getNom() {
        return nom;
    }

    /** Somme recursive des PV de toutes les sous-unites vivantes. */
    @Override
    public int getPointsDeVie() {
        int total = 0;
        for (IUnite membre : membres) {
            if (membre.estVivant()) {
                total += membre.getPointsDeVie();
            }
        }
        return total;
    }

    /** Somme recursive des PV max de toutes les sous-unites (vivantes ou non). */
    @Override
    public int getPointsDeVieMax() {
        int total = 0;
        for (IUnite membre : membres) {
            total += membre.getPointsDeVieMax();
        }
        return total;
    }

    /**
     * Somme des attaques des membres vivants, majoree de +10% (arrondi)
     * si au moins deux sous-unites vivantes coordonnent leur action.
     */
    @Override
    public int getPuissanceAttaque() {
        List<IUnite> vivants = getMembresVivants();
        int total = 0;
        for (IUnite membre : vivants) {
            total += membre.getPuissanceAttaque();
        }
        if (vivants.size() >= SEUIL_SYNERGIE) {
            total = (int) Math.round(total * (1 + BONUS_SYNERGIE));
        }
        return total;
    }

    /**
     * Repartit equitablement les degats entre les sous-unites vivantes.
     * part = montant / Nvivants ; le reliquat est distribue (1 point chacun)
     * aux premieres unites. Chaque part est transmise recursivement.
     */
    @Override
    public void subirDegats(int montant) {
        List<IUnite> vivants = getMembresVivants();
        if (vivants.isEmpty() || montant <= 0) {
            return;
        }
        repartir(vivants, montant, true);
    }

    /** Meme repartition equitable et recursive que pour les degats, mais en soin. */
    @Override
    public void recevoirSoins(int montant) {
        List<IUnite> vivants = getMembresVivants();
        if (vivants.isEmpty() || montant <= 0) {
            return;
        }
        repartir(vivants, montant, false);
    }

    private static void repartir(List<IUnite> vivants, int montant, boolean degats) {
        int part = montant / vivants.size();
        int reliquat = montant % vivants.size();
        for (int i = 0; i < vivants.size(); i++) {
            int quotePart = part + (i < reliquat ? 1 : 0);
            if (quotePart == 0) {
                continue;
            }
            if (degats) {
                vivants.get(i).subirDegats(quotePart);
            } else {
                vivants.get(i).recevoirSoins(quotePart);
            }
        }
    }

    /** Transmet la posture defensive a chaque membre, recursivement. */
    @Override
    public void defendre(int bonusArmure) {
        for (IUnite membre : membres) {
            membre.defendre(bonusArmure);
        }
    }

    /** Vivant des qu'au moins un membre est vivant. */
    @Override
    public boolean estVivant() {
        for (IUnite membre : membres) {
            if (membre.estVivant()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getCout() {
        int total = 0;
        for (IUnite membre : membres) {
            total += membre.getCout();
        }
        return total;
    }

    // ---------------------------------------------------------------
    // Affichage arborescent recursif
    // ---------------------------------------------------------------

    @Override
    public void afficherStructure(String prefix, boolean isDernier) {
        String connecteur = isDernier ? "\\-- " : "|-- ";
        String statut = estVivant() ? "" : "  >> GROUPE ANEANTI";
        System.out.printf("%s%s[Groupe : %s] (PV : %d/%d, ATQ Totale : %d, Membres : %d)%s%n",
                prefix, connecteur, nom, getPointsDeVie(), getPointsDeVieMax(),
                getPuissanceAttaque(), membres.size(), statut);

        String indentationEnfants = prefix + (isDernier ? "    " : "|   ");
        for (int i = 0; i < membres.size(); i++) {
            boolean dernier = (i == membres.size() - 1);
            membres.get(i).afficherStructure(indentationEnfants, dernier);
        }
    }

    @Override
    public String toString() {
        return "[Groupe] " + nom;
    }
}
