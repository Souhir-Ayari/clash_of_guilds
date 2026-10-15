package com.mycompany.composite_clash_of_guilds.gui;

import com.mycompany.composite_clash_of_guilds.Combat;
import com.mycompany.composite_clash_of_guilds.Escouade;
import com.mycompany.composite_clash_of_guilds.GuildesPredefinies;
import com.mycompany.composite_clash_of_guilds.Guerrier;
import com.mycompany.composite_clash_of_guilds.IUnite;
import com.mycompany.composite_clash_of_guilds.Joueur;
import com.mycompany.composite_clash_of_guilds.Mage;
import com.mycompany.composite_clash_of_guilds.ParcoursArbre;
import com.mycompany.composite_clash_of_guilds.Soigneur;
import com.mycompany.composite_clash_of_guilds.UniteElementaire;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Version graphique 2D de Clash of Guilds (presentation Swing / Java2D).
 *
 * Elle reutilise SANS MODIFICATION le modele Composite du jeu console
 * (IUnite, Escouade, Guerrier, Archer, Mage, Soigneur, Combat) :
 * seule la couche de presentation change. L'arbre de chaque armee est
 * dessine recursivement via getSousUnites(), et chaque attaque passe par
 * Combat.attaquer(IUnite, IUnite), comme en console.
 */
@SuppressWarnings("serial")
public final class PanneauJeu extends JPanel {

    static final int LARGEUR = 1280;
    static final int HAUTEUR = 720;

    private static final double COL_X0 = 20, COL_L = 404, COL_ESP = 14, COL_Y = 82, COL_H = 526;
    private static final double ENTETE_H = 110;
    private static final double BARRE_Y = 620;

    enum Ecran { TITRE, JEU, VICTOIRE }

    enum Mode { ACTION, CIBLE_TOTALE, ATTAQUANT, CIBLE_FRAPPE, SOIGNEUR, ATTENTE }

    private static final class Bouton {
        final String libelle;
        final String sousTitre;
        final int icone;
        final Rectangle2D zone;
        final Runnable action;
        boolean actif = true;
        boolean selectionne;

        Bouton(String libelle, String sousTitre, int icone, Rectangle2D zone, Runnable action) {
            this.libelle = libelle;
            this.sousTitre = sousTitre;
            this.icone = icone;
            this.zone = zone;
            this.action = action;
        }
    }

    private record Tache(double moment, Runnable action) {
    }

    private static final class Braise {
        double x, y, vy, phase, taille;
    }

    private final Effets effets = new Effets();
    private final Random alea = effets.alea;

    private final List<Joueur> joueurs = new ArrayList<>();
    private final boolean[] ia = new boolean[3];
    private final Set<Integer> eliminationsAnnoncees = new HashSet<>();
    private final Map<IUnite, Integer> guildeDe = new HashMap<>();
    private final Map<IUnite, Rectangle2D> zones = new LinkedHashMap<>();
    private final Map<IUnite, Point2D> centres = new HashMap<>();
    private final Map<IUnite, Double> pvFantomes = new HashMap<>();
    private final Map<IUnite, Double> flashs = new HashMap<>();
    private final Rectangle2D[] colonnes = new Rectangle2D[3];
    private final List<Bouton> boutons = new ArrayList<>();
    private final List<Tache> taches = new ArrayList<>();
    private final List<String> journal = new ArrayList<>();
    private final List<Braise> braises = new ArrayList<>();
    private final double[][] etoiles = new double[150][];

    private Ecran ecran = Ecran.TITRE;
    private Mode mode = Mode.ACTION;
    private int actif;
    private int tour;
    private IUnite attaquantChoisi;
    private Joueur vainqueur;
    private int indexVainqueur;
    private double temps;
    private double prochainFeu;
    private double banniereDebut = -10;
    private String banniereTitre = "";
    private String banniereSous = "";
    private Color banniereCouleur = Theme.OR;
    private double sourisX = -1;
    private double sourisY = -1;
    private double echelle = 1;
    private double decalX;
    private double decalY;
    private long dernierNano;
    private BufferedImage fond;
    private final Timer boucle;

    public PanneauJeu() {
        setPreferredSize(new Dimension(LARGEUR, HAUTEUR));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);

        Random r = new Random(7);
        for (int i = 0; i < etoiles.length; i++) {
            etoiles[i] = new double[] {r.nextDouble() * LARGEUR, r.nextDouble() * 470,
                0.6 + r.nextDouble() * 1.8, r.nextDouble() * 6.28, 0.6 + r.nextDouble() * 2.2};
        }
        for (int i = 0; i < 40; i++) {
            Braise b = nouvelleBraise();
            b.y = alea.nextDouble() * HAUTEUR;
            braises.add(b);
        }

        MouseAdapter souris = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                suivreSouris(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                suivreSouris(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                suivreSouris(e);
                cliquer(sourisX, sourisY, SwingUtilities.isRightMouseButton(e));
            }
        };
        addMouseListener(souris);
        addMouseMotionListener(souris);
        raccourci("ESCAPE", this::annuler);
        raccourci("1", () -> declencherBouton(0));
        raccourci("2", () -> declencherBouton(1));
        raccourci("3", () -> declencherBouton(2));

        construireBoutonsTitre();
        boucle = new Timer(15, e -> tic());
    }

    /** Demarre la boucle d'animation (~60 images/s). */
    public void demarrer() {
        dernierNano = 0;
        boucle.start();
        requestFocusInWindow();
    }

    private void raccourci(String touche, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(touche), touche);
        getActionMap().put(touche, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void tic() {
        long maintenant = System.nanoTime();
        double dt = dernierNano == 0 ? 0.016 : Math.min(0.05, (maintenant - dernierNano) / 1e9);
        dernierNano = maintenant;
        avancer(dt);
        repaint();
    }

    // ==================================================================
    // Etat du jeu
    // ==================================================================

    void demarrerPartie(boolean ia1, boolean ia2, boolean ia3) {
        joueurs.clear();
        guildeDe.clear();
        pvFantomes.clear();
        flashs.clear();
        journal.clear();
        taches.clear();
        eliminationsAnnoncees.clear();
        ia[0] = ia1;
        ia[1] = ia2;
        ia[2] = ia3;
        String[] humains = {"Alice", "Bob", "Charlie"};
        String[] robots = {"Leonis", "Umbra", "Runa"};
        Escouade[] armees = {GuildesPredefinies.armeeLions(), GuildesPredefinies.armeeOmbres(),
            GuildesPredefinies.armeeGardiens()};
        String[] factions = {GuildesPredefinies.LIONS, GuildesPredefinies.OMBRES, GuildesPredefinies.GARDIENS};
        for (int i = 0; i < 3; i++) {
            joueurs.add(new Joueur(ia[i] ? robots[i] : humains[i], factions[i], armees[i]));
            indexer(armees[i], i);
        }
        actif = 0;
        tour = 1;
        ecran = Ecran.JEU;
        construireBoutonsJeu();
        journal.add("La bataille commence !");
        debutTour();
    }

    /** Parcours recursif de l'arbre Composite pour memoriser la guilde de chaque noeud. */
    private void indexer(IUnite u, int guilde) {
        guildeDe.put(u, guilde);
        pvFantomes.put(u, (double) u.getPointsDeVie());
        for (IUnite enfant : u.getSousUnites()) {
            indexer(enfant, guilde);
        }
    }

    private void debutTour() {
        attaquantChoisi = null;
        mode = ia[actif] ? Mode.ATTENTE : Mode.ACTION;
        Joueur j = joueurs.get(actif);
        banniere("TOUR DE " + j.getNom().toUpperCase(),
                "Guilde " + j.getFaction() + (ia[actif] ? "  -  IA" : ""), Theme.guilde(actif));
        if (ia[actif]) {
            planifier(1.7, this::jouerIA);
        }
    }

    private void planifier(double delai, Runnable action) {
        taches.add(new Tache(temps + delai, action));
    }

    private void banniere(String titre, String sous, Color c) {
        banniereTitre = titre;
        banniereSous = sous;
        banniereCouleur = c;
        banniereDebut = temps;
    }

    void avancer(double dt) {
        temps += dt;
        if (!taches.isEmpty()) {
            List<Tache> pretes = new ArrayList<>();
            for (Tache t : taches) {
                if (t.moment() <= temps) {
                    pretes.add(t);
                }
            }
            taches.removeAll(pretes);
            pretes.forEach(t -> t.action().run());
        }
        effets.avancer(dt);

        if (alea.nextDouble() < dt * 14) {
            braises.add(nouvelleBraise());
        }
        for (Iterator<Braise> it = braises.iterator(); it.hasNext();) {
            Braise b = it.next();
            b.y -= b.vy * dt;
            if (b.y < -10) {
                it.remove();
            }
        }
        for (Map.Entry<IUnite, Double> e : pvFantomes.entrySet()) {
            double reel = e.getKey().getPointsDeVie();
            e.setValue(e.getValue() + (reel - e.getValue()) * Math.min(1, dt * 2.4));
        }
        for (Iterator<Map.Entry<IUnite, Double>> it = flashs.entrySet().iterator(); it.hasNext();) {
            Map.Entry<IUnite, Double> e = it.next();
            double v = e.getValue();
            v = v > 0 ? Math.max(0, v - dt) : Math.min(0, v + dt);
            if (v == 0) {
                it.remove();
            } else {
                e.setValue(v);
            }
        }
        if (ecran == Ecran.VICTOIRE && temps >= prochainFeu) {
            feuDArtifice();
            prochainFeu = temps + 0.22 + alea.nextDouble() * 0.4;
        }
        mettreAJourBoutons();
    }

    private Braise nouvelleBraise() {
        Braise b = new Braise();
        b.x = alea.nextDouble() * LARGEUR;
        b.y = HAUTEUR + 10;
        b.vy = 18 + alea.nextDouble() * 40;
        b.phase = alea.nextDouble() * 6.28;
        b.taille = 1 + alea.nextDouble() * 2.2;
        return b;
    }

    // ==================================================================
    // Actions de combat (le modele Composite fait tout le calcul)
    // ==================================================================

    private void executerAttaque(IUnite attaquant, IUnite cible, String nature) {
        mode = Mode.ATTENTE;
        attaquantChoisi = null;
        List<IUnite> tireurs = vivantes(attaquant);
        List<IUnite> cibles = vivantes(cible);
        Point2D centreCible = centre(cible);
        double fin = 0;
        for (int k = 0; k < tireurs.size(); k++) {
            IUnite t = tireurs.get(k);
            Point2D arrivee = cibles.isEmpty() ? centreCible : centre(cibles.get(alea.nextInt(cibles.size())));
            double delai = k * 0.09;
            double duree = 0.55 + alea.nextDouble() * 0.15;
            fin = Math.max(fin, delai + duree);
            Color c = Theme.type(t);
            effets.tirer(centre(t), arrivee, c, style(t), delai, duree, 50 + alea.nextDouble() * 90,
                    () -> effets.explosion(arrivee.getX(), arrivee.getY(), c, 10, 170));
        }
        journal.add(nomJoueur(attaquant) + " : " + nature + " !");
        planifier(fin + 0.02, () -> impact(attaquant, cible));
    }

    private void impact(IUnite attaquant, IUnite cible) {
        Map<IUnite, Integer> avant = new HashMap<>();
        for (IUnite f : ParcoursArbre.feuilles(cible)) {
            avant.put(f, f.getPointsDeVie());
        }

        Combat.Resultat r = Combat.attaquer(attaquant, cible);

        int morts = 0;
        for (Map.Entry<IUnite, Integer> e : avant.entrySet()) {
            IUnite f = e.getKey();
            int perte = e.getValue() - f.getPointsDeVie();
            if (perte <= 0) {
                continue;
            }
            Point2D p = centre(f);
            effets.explosion(p.getX(), p.getY(), Theme.DEGATS, 18, 230);
            effets.texte("-" + perte, p.getX() + 30, p.getY() - 8, new Color(0xFF7A85), 19);
            flashs.put(f, 0.45);
            if (!f.estVivant()) {
                morts++;
                effets.explosion(p.getX(), p.getY(), new Color(255, 214, 140), 45, 340);
                effets.texte("K.O.", p.getX() + 46, p.getY() - 20, Theme.OR, 18);
            }
        }
        Point2D c = centre(cible);
        effets.onde(c.getX(), c.getY(), Theme.guilde(guildeDe.get(attaquant)), 170);
        if (cible.estComposite()) {
            effets.texte("-" + r.pvPerdus() + " PV", c.getX(), c.getY() - 26, Theme.OR_PALE, 32);
        }
        effets.secouer(Math.min(24, 6 + r.degatsBruts() / 14.0));
        journal.add(String.format("%d degats sur %s%s", r.pvPerdus(), cible.getNom(),
                morts > 0 ? " (" + morts + " K.O.)" : ""));
        planifier(1.1, this::finAction);
    }

    private void lancerSoin(Soigneur soigneur) {
        mode = Mode.ATTENTE;
        IUnite groupe = ParcoursArbre.trouverParent(joueurs.get(actif).getArmee(), soigneur);
        List<IUnite> allies = vivantes(groupe);
        double fin = 0;
        for (int k = 0; k < allies.size(); k++) {
            double delai = k * 0.07;
            fin = Math.max(fin, delai + 0.5);
            effets.tirer(centre(soigneur), centre(allies.get(k)), Theme.SOIN, Effets.Style.SOIN, delai, 0.5, 35, null);
        }
        planifier(fin + 0.02, () -> impactSoin(soigneur, groupe));
    }

    private void impactSoin(Soigneur soigneur, IUnite groupe) {
        Map<IUnite, Integer> avant = new HashMap<>();
        for (IUnite f : ParcoursArbre.feuilles(groupe)) {
            avant.put(f, f.getPointsDeVie());
        }
        int total = soigneur.soigner(groupe);
        for (Map.Entry<IUnite, Integer> e : avant.entrySet()) {
            int gain = e.getKey().getPointsDeVie() - e.getValue();
            if (gain > 0) {
                Point2D p = centre(e.getKey());
                effets.soin(p.getX(), p.getY());
                effets.texte("+" + gain, p.getX() + 30, p.getY() - 8, Theme.SOIN, 19);
                flashs.put(e.getKey(), -0.45);
            }
        }
        Point2D c = centre(groupe);
        effets.onde(c.getX(), c.getY(), Theme.SOIN, 120);
        if (total == 0) {
            effets.texte("Aucun blesse", c.getX(), c.getY() - 20, Theme.SOIN, 18);
        }
        journal.add(soigneur.getNom() + " soigne " + groupe.getNom() + " : +" + total + " PV");
        planifier(1.0, this::finAction);
    }

    private void finAction() {
        double attente = 0.3;
        for (int i = 0; i < joueurs.size(); i++) {
            Joueur j = joueurs.get(i);
            if (j.estElimine() && eliminationsAnnoncees.add(i)) {
                banniere("GUILDE ANEANTIE", j.getFaction() + " de " + j.getNom() + " est tombee", Theme.DEGATS);
                journal.add(j.getFaction() + " est aneantie !");
                Rectangle2D col = colonnes[i];
                if (col != null) {
                    effets.explosion(col.getCenterX(), col.getCenterY(), Theme.DEGATS, 120, 520);
                    effets.onde(col.getCenterX(), col.getCenterY(), Theme.guilde(i), 320);
                }
                effets.secouer(26);
                attente = 2.0;
            }
        }
        List<Integer> enLice = new ArrayList<>();
        for (int i = 0; i < joueurs.size(); i++) {
            if (!joueurs.get(i).estElimine()) {
                enLice.add(i);
            }
        }
        if (enLice.size() <= 1) {
            int gagnant = enLice.isEmpty() ? actif : enLice.get(0);
            planifier(attente, () -> victoire(gagnant));
        } else {
            planifier(attente, this::tourSuivant);
        }
    }

    private void tourSuivant() {
        do {
            actif = (actif + 1) % joueurs.size();
        } while (joueurs.get(actif).estElimine());
        tour++;
        debutTour();
    }

    private void victoire(int index) {
        vainqueur = joueurs.get(index);
        indexVainqueur = index;
        ecran = Ecran.VICTOIRE;
        banniereDebut = -10;
        prochainFeu = temps;
        construireBoutonsVictoire();
    }

    /** IA simple : soigne si son escouade souffre, achève une cible fragile, sinon attaque totale. */
    private void jouerIA() {
        Joueur moi = joueurs.get(actif);
        for (Soigneur s : soigneursVivants(moi.getArmee())) {
            IUnite groupe = ParcoursArbre.trouverParent(moi.getArmee(), s);
            int pv = 0;
            int max = 0;
            for (IUnite f : vivantes(groupe)) {
                pv += f.getPointsDeVie();
                max += f.getPointsDeVieMax();
            }
            if (max > 0 && pv < 0.6 * max) {
                lancerSoin(s);
                return;
            }
        }
        Joueur proie = null;
        for (Joueur j : joueurs) {
            if (j != moi && !j.estElimine()
                    && (proie == null || j.getArmee().getPointsDeVie() < proie.getArmee().getPointsDeVie())) {
                proie = j;
            }
        }
        if (proie == null) {
            return;
        }
        IUnite meilleur = null;
        for (ParcoursArbre.Noeud n : ParcoursArbre.descendants(moi.getArmee())) {
            IUnite u = n.unite();
            if (u.estVivant() && (meilleur == null || u.getPuissanceAttaque() > meilleur.getPuissanceAttaque())) {
                meilleur = u;
            }
        }
        IUnite victime = null;
        if (meilleur != null) {
            for (Joueur j : joueurs) {
                if (j == moi || j.estElimine()) {
                    continue;
                }
                for (IUnite f : vivantes(j.getArmee())) {
                    int net = f instanceof Guerrier ? meilleur.getPuissanceAttaque() - Guerrier.ARMURE
                            : meilleur.getPuissanceAttaque();
                    if (f.getPointsDeVie() <= net && (victime == null
                            || f.getPuissanceAttaque() > victime.getPuissanceAttaque())) {
                        victime = f;
                    }
                }
            }
        }
        if (victime != null && alea.nextDouble() < 0.7) {
            executerAttaque(meilleur, victime, "frappe ciblee sur " + victime.getNom());
        } else {
            executerAttaque(moi.getArmee(), proie.getArmee(), "attaque totale sur " + proie.getFaction());
        }
    }

    // ==================================================================
    // Interactions
    // ==================================================================

    private void suivreSouris(MouseEvent e) {
        sourisX = (e.getX() - decalX) / echelle;
        sourisY = (e.getY() - decalY) / echelle;
        boolean main = false;
        for (Bouton b : boutons) {
            main |= b.actif && b.zone.contains(sourisX, sourisY);
        }
        IUnite u = noeudSous(sourisX, sourisY);
        main |= u != null && estSelectionnable(u);
        main |= mode == Mode.CIBLE_TOTALE && colonneSous(sourisX, sourisY) >= 0;
        setCursor(Cursor.getPredefinedCursor(main ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    void placerSouris(double x, double y) {
        sourisX = x;
        sourisY = y;
    }

    void cliquer(double x, double y, boolean droit) {
        if (droit) {
            annuler();
            return;
        }
        for (Bouton b : new ArrayList<>(boutons)) {
            if (b.actif && b.zone.contains(x, y)) {
                b.action.run();
                return;
            }
        }
        if (ecran != Ecran.JEU || ia[actif]) {
            return;
        }
        Joueur moi = joueurs.get(actif);
        switch (mode) {
            case CIBLE_TOTALE -> {
                int i = colonneSous(x, y);
                if (i >= 0) {
                    Joueur cible = joueurs.get(i);
                    executerAttaque(moi.getArmee(), cible.getArmee(), "attaque totale sur " + cible.getFaction());
                }
            }
            case ATTAQUANT -> {
                IUnite u = noeudSous(x, y);
                if (u != null && estSelectionnable(u)) {
                    attaquantChoisi = u;
                    mode = Mode.CIBLE_FRAPPE;
                }
            }
            case CIBLE_FRAPPE -> {
                IUnite u = noeudSous(x, y);
                if (u != null && estSelectionnable(u)) {
                    executerAttaque(attaquantChoisi, u, attaquantChoisi.getNom() + " frappe " + u.getNom());
                }
            }
            case SOIGNEUR -> {
                IUnite u = noeudSous(x, y);
                if (u instanceof Soigneur s && estSelectionnable(u)) {
                    lancerSoin(s);
                }
            }
            default -> {
                // rien a faire
            }
        }
    }

    private void annuler() {
        if (ecran == Ecran.JEU && mode != Mode.ATTENTE) {
            mode = Mode.ACTION;
            attaquantChoisi = null;
        }
    }

    private void declencherBouton(int index) {
        if (ecran == Ecran.JEU && index < boutons.size() && boutons.get(index).actif) {
            boutons.get(index).action.run();
        }
    }

    private void basculer(Mode m) {
        attaquantChoisi = null;
        mode = mode == m ? Mode.ACTION : m;
    }

    private boolean estSelectionnable(IUnite u) {
        if (ecran != Ecran.JEU || ia[actif] || !u.estVivant() || !guildeDe.containsKey(u)) {
            return false;
        }
        boolean allie = guildeDe.get(u) == actif;
        boolean racine = u == joueurs.get(guildeDe.get(u)).getArmee();
        return switch (mode) {
            case ATTAQUANT -> allie && !racine;
            case CIBLE_FRAPPE -> !allie;
            case SOIGNEUR -> allie && u instanceof Soigneur;
            default -> false;
        };
    }

    private IUnite noeudSous(double x, double y) {
        for (Map.Entry<IUnite, Rectangle2D> e : zones.entrySet()) {
            if (e.getValue().contains(x, y)) {
                return e.getKey();
            }
        }
        return null;
    }

    private int colonneSous(double x, double y) {
        for (int i = 0; i < 3; i++) {
            if (colonnes[i] != null && colonnes[i].contains(x, y) && i != actif && !joueurs.get(i).estElimine()) {
                return i;
            }
        }
        return -1;
    }

    // ==================================================================
    // Boutons
    // ==================================================================

    private void construireBoutonsTitre() {
        boutons.clear();
        double x = 460;
        double l = 360;
        boutons.add(new Bouton("PARTIE A 3 JOUEURS", "Trois commandants autour de l'ecran", Icones.EPEES,
                new Rectangle2D.Double(x, 418, l, 56), () -> demarrerPartie(false, false, false)));
        boutons.add(new Bouton("SOLO CONTRE 2 IA", "Menez les Lions d'Or", Icones.COURONNE,
                new Rectangle2D.Double(x, 484, l, 56), () -> demarrerPartie(false, true, true)));
        boutons.add(new Bouton("DEMO : 3 IA", "Regardez la bataille se derouler", Icones.OEIL,
                new Rectangle2D.Double(x, 550, l, 56), () -> demarrerPartie(true, true, true)));
        boutons.add(new Bouton("QUITTER", "", Icones.QUITTER,
                new Rectangle2D.Double(x + 90, 618, l - 180, 44), this::quitter));
    }

    private void construireBoutonsJeu() {
        boutons.clear();
        double y = BARRE_Y + 14;
        boutons.add(new Bouton("ATTAQUE TOTALE", "Toute l'armee  [1]", Icones.EPEES,
                new Rectangle2D.Double(430, y, 178, 62), () -> basculer(Mode.CIBLE_TOTALE)));
        boutons.add(new Bouton("FRAPPE CIBLEE", "Une escouade d'elite  [2]", Icones.CIBLE,
                new Rectangle2D.Double(618, y, 178, 62), () -> basculer(Mode.ATTAQUANT)));
        boutons.add(new Bouton("SOIGNER", "Un soigneur  [3]", Icones.PLUS,
                new Rectangle2D.Double(806, y, 178, 62), () -> basculer(Mode.SOIGNEUR)));
    }

    private void construireBoutonsVictoire() {
        boutons.clear();
        boutons.add(new Bouton("NOUVELLE PARTIE", "Retour au menu", Icones.RETOUR,
                new Rectangle2D.Double(400, 580, 230, 58), () -> {
                    ecran = Ecran.TITRE;
                    construireBoutonsTitre();
                }));
        boutons.add(new Bouton("QUITTER", "Fermer le jeu", Icones.QUITTER,
                new Rectangle2D.Double(650, 580, 230, 58), this::quitter));
    }

    private void quitter() {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) {
            w.dispose();
        }
        System.exit(0);
    }

    private void mettreAJourBoutons() {
        if (ecran != Ecran.JEU || boutons.size() < 3) {
            return;
        }
        boolean possible = !ia[actif] && mode != Mode.ATTENTE;
        boutons.get(0).actif = possible;
        boutons.get(1).actif = possible;
        boutons.get(2).actif = possible && !soigneursVivants(joueurs.get(actif).getArmee()).isEmpty();
        boutons.get(0).selectionne = mode == Mode.CIBLE_TOTALE;
        boutons.get(1).selectionne = mode == Mode.ATTAQUANT || mode == Mode.CIBLE_FRAPPE;
        boutons.get(2).selectionne = mode == Mode.SOIGNEUR;
    }

    // ==================================================================
    // Outils
    // ==================================================================

    private static List<IUnite> vivantes(IUnite racine) {
        List<IUnite> r = new ArrayList<>();
        for (IUnite f : ParcoursArbre.feuilles(racine)) {
            if (f.estVivant()) {
                r.add(f);
            }
        }
        return r;
    }

    private static List<Soigneur> soigneursVivants(IUnite armee) {
        List<Soigneur> r = new ArrayList<>();
        for (IUnite f : ParcoursArbre.feuilles(armee)) {
            if (f instanceof Soigneur s && s.estVivant()) {
                r.add(s);
            }
        }
        return r;
    }

    private static Effets.Style style(IUnite u) {
        if (u instanceof Mage) {
            return Effets.Style.ORBE;
        }
        if (u instanceof Guerrier) {
            return Effets.Style.LAME;
        }
        if (u instanceof Soigneur) {
            return Effets.Style.SOIN;
        }
        return Effets.Style.FLECHE;
    }

    private String nomJoueur(IUnite u) {
        Integer i = guildeDe.get(u);
        return i == null ? "?" : joueurs.get(i).getNom();
    }

    private Point2D centre(IUnite u) {
        Point2D p = centres.get(u);
        if (p != null) {
            return p;
        }
        Rectangle2D z = zones.get(u);
        return z != null ? new Point2D.Double(z.getCenterX(), z.getCenterY())
                : new Point2D.Double(LARGEUR / 2.0, HAUTEUR / 2.0);
    }

    private void feuDArtifice() {
        Color[] couleurs = {Theme.OR, Theme.guilde(indexVainqueur), Theme.OR_PALE, Theme.SOIN, new Color(0xFF7AB6)};
        double x = 140 + alea.nextDouble() * 1000;
        double y = 80 + alea.nextDouble() * 260;
        Color c = couleurs[alea.nextInt(couleurs.length)];
        effets.explosion(x, y, c, 70, 380);
        effets.onde(x, y, c, 60);
    }

    static BufferedImage icone() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        qualite(g);
        Icones.blason(g, 0, 32, 30, 26, false);
        g.dispose();
        return img;
    }

    private static void qualite(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    // ==================================================================
    // Rendu
    // ==================================================================

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        echelle = Math.min(getWidth() / (double) LARGEUR, getHeight() / (double) HAUTEUR);
        decalX = (getWidth() - LARGEUR * echelle) / 2;
        decalY = (getHeight() - HAUTEUR * echelle) / 2;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.translate(decalX, decalY);
        g.scale(echelle, echelle);
        g.clip(new Rectangle2D.Double(0, 0, LARGEUR, HAUTEUR));
        rendre(g);
        g.dispose();
    }

    /** Dessine une image complete du jeu dans le repere logique 1280 x 720. */
    void rendre(Graphics2D g) {
        qualite(g);
        if (fond == null) {
            fond = creerFond();
        }
        g.drawImage(fond, 0, 0, null);
        dessinerEtoiles(g);
        dessinerBraises(g);
        if (ecran == Ecran.TITRE) {
            dessinerTitre(g);
            dessinerBoutons(g);
            effets.dessiner(g);
            return;
        }
        dessinerJeu(g);
        if (ecran == Ecran.VICTOIRE) {
            dessinerVictoire(g);
        }
    }

    private BufferedImage creerFond() {
        BufferedImage img = new BufferedImage(LARGEUR, HAUTEUR, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        qualite(g);
        g.setPaint(new LinearGradientPaint(0, 0, 0, HAUTEUR, new float[] {0f, 0.55f, 0.8f, 1f},
                new Color[] {new Color(0x060A1C), new Color(0x1A1238), new Color(0x2B1530), new Color(0x0E0812)}));
        g.fillRect(0, 0, LARGEUR, HAUTEUR);
        g.setPaint(new RadialGradientPaint(1040, 130, 300, new float[] {0f, 1f},
                new Color[] {new Color(255, 214, 150, 70), new Color(255, 214, 150, 0)}));
        g.fillRect(0, 0, LARGEUR, HAUTEUR);
        g.setPaint(new RadialGradientPaint(1030, 120, 44, new float[] {0f, 1f},
                new Color[] {new Color(255, 246, 220), new Color(240, 214, 170)}));
        g.fill(new Ellipse2D.Double(990, 80, 88, 88));
        g.setColor(new Color(214, 186, 140, 90));
        g.fill(new Ellipse2D.Double(1010, 100, 16, 16));
        g.fill(new Ellipse2D.Double(1045, 132, 11, 11));

        g.setColor(new Color(0x1F1638));
        g.fill(montagnes(470, 60, 0.006, 35, 0.017, 1));
        g.setColor(new Color(0x140E26));
        g.fill(montagnes(545, 45, 0.009, 25, 0.03, 2));
        g.setColor(new Color(0x0E0A1A));
        chateau(g, 150, 560);
        chateau(g, 1090, 548);
        g.fill(montagnes(610, 25, 0.012, 12, 0.04, 3));

        g.setPaint(new GradientPaint(0, 520, new Color(140, 100, 200, 0), 0, HAUTEUR, new Color(140, 100, 200, 40)));
        g.fillRect(0, 520, LARGEUR, 200);
        g.setPaint(new RadialGradientPaint(LARGEUR / 2f, HAUTEUR / 2f, 820, new float[] {0.5f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 190)}));
        g.fillRect(0, 0, LARGEUR, HAUTEUR);
        g.dispose();
        return img;
    }

    private static Shape montagnes(double base, double a1, double f1, double a2, double f2, double phase) {
        Path2D p = new Path2D.Double();
        p.moveTo(0, HAUTEUR);
        for (int x = 0; x <= LARGEUR; x += 8) {
            double y = base + a1 * Math.sin(x * f1 + phase) + a2 * Math.sin(x * f2 + phase * 2)
                    + 8 * Math.sin(x * 0.07 + phase);
            p.lineTo(x, y);
        }
        p.lineTo(LARGEUR, HAUTEUR);
        p.closePath();
        return p;
    }

    private static void chateau(Graphics2D g, double x, double y) {
        g.fill(new Rectangle2D.Double(x - 50, y - 40, 100, 60));
        g.fill(new Rectangle2D.Double(x - 70, y - 75, 32, 95));
        g.fill(new Rectangle2D.Double(x + 38, y - 75, 32, 95));
        g.fill(new Rectangle2D.Double(x - 14, y - 105, 28, 70));
        for (double cx : new double[] {x - 70, x - 54, x + 38, x + 54}) {
            g.fill(new Rectangle2D.Double(cx, y - 85, 10, 12));
        }
        Path2D toit = new Path2D.Double();
        toit.moveTo(x - 18, y - 105);
        toit.lineTo(x, y - 140);
        toit.lineTo(x + 18, y - 105);
        toit.closePath();
        g.fill(toit);
    }

    private void dessinerEtoiles(Graphics2D g) {
        for (double[] e : etoiles) {
            double a = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(temps * e[4] + e[3]));
            g.setColor(new Color(255, 248, 230, (int) (a * 200)));
            g.fill(new Ellipse2D.Double(e[0] - e[2] / 2, e[1] - e[2] / 2, e[2], e[2]));
        }
    }

    private void dessinerBraises(Graphics2D g) {
        for (Braise b : braises) {
            double x = b.x + Math.sin(temps * 1.3 + b.phase) * 14;
            double a = Math.max(0, Math.min(1, b.y / HAUTEUR)) * 0.85;
            g.setColor(new Color(255, 160, 70, (int) (a * 55)));
            g.fill(new Ellipse2D.Double(x - b.taille * 3, b.y - b.taille * 3, b.taille * 6, b.taille * 6));
            g.setColor(new Color(255, 200, 120, (int) (a * 230)));
            g.fill(new Ellipse2D.Double(x - b.taille / 2, b.y - b.taille / 2, b.taille, b.taille));
        }
    }

    // ------------------------------------------------------------------
    // Ecran titre
    // ------------------------------------------------------------------

    private void dessinerTitre(Graphics2D g) {
        double flotte = Math.sin(temps * 1.4) * 4;
        texteLogo(g, "CLASH", LARGEUR / 2.0, 168 + flotte, 112);
        texteLogo(g, "OF  GUILDS", LARGEUR / 2.0, 238 + flotte, 54);
        ornement(g, LARGEUR / 2.0, 268, 300);
        texteCentre(g, "Wargame tactique a trois guildes  -  Patron de conception Composite",
                Theme.texte(Font.PLAIN, 16), Theme.TEXTE_DOUX, LARGEUR / 2.0, 298);
        for (int i = 0; i < 3; i++) {
            double x = 520 + i * 120;
            double y = 352 + Math.sin(temps * 1.8 + i * 1.3) * 5;
            Icones.blason(g, i, x, y, 30, false);
        }
        texteCentre(g, "IUnite  -  Escouade (Composite)  -  Guerrier, Archer, Mage, Soigneur (Feuilles)",
                Theme.texte(Font.PLAIN, 12), Theme.alpha(Theme.TEXTE_DOUX, 0.8), LARGEUR / 2.0, 700);
    }

    private void texteLogo(Graphics2D g, String texte, double cx, double base, float taille) {
        Font f = Theme.titre(taille);
        GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), texte);
        Rectangle2D b = gv.getVisualBounds();
        Shape s = AffineTransform.getTranslateInstance(cx - b.getWidth() / 2 - b.getX(), base)
                .createTransformedShape(gv.getOutline());
        for (int k = 6; k >= 1; k--) {
            g.setStroke(new BasicStroke(k * 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 170, 60, 10));
            g.draw(s);
        }
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(40, 20, 8));
        g.draw(s);
        g.setPaint(new LinearGradientPaint((float) cx, (float) (base - b.getHeight()), (float) cx, (float) base,
                new float[] {0f, 0.5f, 0.52f, 1f},
                new Color[] {Theme.OR_PALE, Theme.OR, new Color(0xE0A53A), Theme.OR_FONCE}));
        g.fill(s);
    }

    private void ornement(Graphics2D g, double cx, double y, double demi) {
        g.setPaint(new LinearGradientPaint((float) (cx - demi), (float) y, (float) (cx + demi), (float) y,
                new float[] {0f, 0.5f, 1f},
                new Color[] {Theme.alpha(Theme.OR, 0), Theme.OR, Theme.alpha(Theme.OR, 0)}));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new Line2D.Double(cx - demi, y, cx + demi, y));
        Path2D d = new Path2D.Double();
        d.moveTo(cx, y - 6);
        d.lineTo(cx + 6, y);
        d.lineTo(cx, y + 6);
        d.lineTo(cx - 6, y);
        d.closePath();
        g.setColor(Theme.OR);
        g.fill(d);
    }

    // ------------------------------------------------------------------
    // Ecran de bataille
    // ------------------------------------------------------------------

    private void dessinerJeu(Graphics2D g0) {
        Graphics2D g = (Graphics2D) g0.create();
        double[] d = effets.decalage();
        g.translate(d[0], d[1]);
        zones.clear();
        centres.clear();
        dessinerBarreHaut(g);
        for (int i = 0; i < joueurs.size(); i++) {
            dessinerGuilde(g, i);
        }
        dessinerBarreBas(g);
        if (ecran == Ecran.JEU) {
            dessinerBoutons(g);
        }
        dessinerVisee(g);
        if (ecran == Ecran.JEU) {
            effets.dessiner(g);
        }
        g.dispose();
        if (ecran == Ecran.JEU) {
            dessinerBanniere(g0);
            dessinerInfobulle(g0);
        }
    }

    private void dessinerBarreHaut(Graphics2D g) {
        Font logo = Theme.titre(22);
        g.setFont(logo);
        g.setPaint(new GradientPaint(0, 30, Theme.OR_PALE, 0, 52, Theme.OR));
        g.drawString("CLASH OF GUILDS", 28, 50);
        texteCentre(g, "TOUR " + tour, Theme.titre(24), Theme.TEXTE, LARGEUR / 2.0, 52);
        Font f = Theme.texte(Font.PLAIN, 12);
        String aide = "Clic droit / Echap : annuler   -   Survolez une unite pour ses details";
        g.setFont(f);
        g.setColor(Theme.TEXTE_DOUX);
        g.drawString(aide, (float) (LARGEUR - 28 - g.getFontMetrics().stringWidth(aide)), 48f);
        g.setPaint(new LinearGradientPaint(20, 70, LARGEUR - 20, 70, new float[] {0f, 0.5f, 1f},
                new Color[] {Theme.alpha(Theme.OR, 0), Theme.alpha(Theme.OR, 0.6), Theme.alpha(Theme.OR, 0)}));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Line2D.Double(20, 68, LARGEUR - 20, 68));
    }

    private void dessinerGuilde(Graphics2D g, int i) {
        Joueur j = joueurs.get(i);
        Escouade armee = j.getArmee();
        Color cg = Theme.guilde(i);
        double x = COL_X0 + i * (COL_L + COL_ESP);
        double y = COL_Y;
        double w = COL_L;
        double h = COL_H;
        colonnes[i] = new Rectangle2D.Double(x, y, w, h);
        boolean elimine = j.estElimine();
        boolean estActif = i == actif && ecran == Ecran.JEU;
        boolean ciblable = mode == Mode.CIBLE_TOTALE && i != actif && !elimine && !ia[actif];
        boolean survol = ciblable && colonnes[i].contains(sourisX, sourisY);
        double pulse = 0.5 + 0.5 * Math.sin(temps * 4);
        RoundRectangle2D carte = new RoundRectangle2D.Double(x, y, w, h, 24, 24);

        if (estActif || survol || ciblable) {
            Color halo = survol ? Theme.DEGATS : ciblable ? Theme.melange(cg, Theme.DEGATS, 0.5) : cg;
            double force = survol ? 1 : ciblable ? 0.4 + 0.4 * pulse : 0.55 + 0.35 * pulse;
            for (int k = 7; k >= 1; k--) {
                g.setStroke(new BasicStroke(k * 3f));
                g.setColor(Theme.alpha(halo, 0.045 * force * (8 - k) / 2));
                g.draw(carte);
            }
        }
        g.setPaint(new GradientPaint(0, (float) y, Theme.CARTE_HAUT, 0, (float) (y + h), Theme.CARTE_BAS));
        g.fill(carte);
        Shape ancienClip = g.getClip();
        g.clip(carte);
        g.setPaint(new GradientPaint(0, (float) y, Theme.alpha(cg, elimine ? 0.08 : 0.32),
                0, (float) (y + ENTETE_H), Theme.alpha(cg, 0)));
        g.fill(new Rectangle2D.Double(x, y, w, ENTETE_H));
        g.setClip(ancienClip);
        g.setStroke(new BasicStroke(survol ? 2.6f : 1.8f));
        g.setColor(Theme.alpha(elimine ? Theme.MORT : survol ? Theme.DEGATS : cg, estActif || survol ? 0.95 : 0.55));
        g.draw(carte);

        Composite ancien = g.getComposite();
        if (elimine) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        }

        Icones.blason(g, i, x + 48, y + 52, 30, elimine);
        centres.put(armee, new Point2D.Double(x + w / 2, y + 60));
        zones.put(armee, new Rectangle2D.Double(x, y, w, ENTETE_H));
        g.setFont(Theme.titre(20));
        g.setColor(Theme.TEXTE);
        g.drawString(j.getFaction().toUpperCase(), (float) (x + 92), (float) (y + 36));
        g.setFont(Theme.texte(Font.PLAIN, 12.5f));
        g.setColor(Theme.TEXTE_DOUX);
        g.drawString("Commandant : " + j.getNom() + (ia[i] ? "  (IA)" : ""), (float) (x + 92), (float) (y + 55));
        barreVie(g, armee, x + 92, y + 64, w - 92 - 18, 15, true);
        g.setFont(Theme.texte(Font.BOLD, 12.5f));
        g.setColor(Theme.OR);
        Icones.bouton(g, Icones.EPEE, x + 99, y + 94, 6.5, Theme.OR);
        g.drawString("ATQ " + armee.getPuissanceAttaque(), (float) (x + 110), (float) (y + 99));
        g.setFont(Theme.texte(Font.PLAIN, 12f));
        g.setColor(Theme.TEXTE_DOUX);
        String valeur = "Valeur " + armee.getCout() + " PO";
        g.drawString(valeur, (float) (x + w - 18 - g.getFontMetrics().stringWidth(valeur)), (float) (y + 99));
        if (survolCible(armee)) {
            surbrillance(g, new RoundRectangle2D.Double(x + 4, y + 4, w - 8, ENTETE_H - 6, 18, 18), Theme.DEGATS);
        }

        g.setColor(Theme.alpha(cg, 0.25));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Line2D.Double(x + 16, y + ENTETE_H + 2, x + w - 16, y + ENTETE_H + 2));

        List<ParcoursArbre.Noeud> noeuds = ParcoursArbre.descendants(armee);
        double haut = y + ENTETE_H + 10;
        double dispo = h - ENTETE_H - 18;
        double ligneH = Math.min(36, dispo / Math.max(1, noeuds.size()));
        for (int k = 0; k < noeuds.size(); k++) {
            ParcoursArbre.Noeud n = noeuds.get(k);
            dessinerLigne(g, n.unite(), n.profondeur(), i, x + 14, haut + k * ligneH, w - 28, ligneH);
        }
        g.setComposite(ancien);

        if (elimine) {
            Graphics2D t = (Graphics2D) g.create();
            t.translate(x + w / 2, y + h / 2);
            t.rotate(Math.toRadians(-14));
            Font f = Theme.titre(40);
            GlyphVector gv = f.createGlyphVector(t.getFontRenderContext(), "ANEANTIE");
            Rectangle2D b = gv.getVisualBounds();
            Shape s = AffineTransform.getTranslateInstance(-b.getWidth() / 2 - b.getX(), b.getHeight() / 2)
                    .createTransformedShape(gv.getOutline());
            RoundRectangle2D cadre = new RoundRectangle2D.Double(-b.getWidth() / 2 - 22, -b.getHeight() / 2 - 16,
                    b.getWidth() + 44, b.getHeight() + 32, 12, 12);
            t.setColor(new Color(20, 4, 8, 200));
            t.fill(cadre);
            t.setStroke(new BasicStroke(3f));
            t.setColor(Theme.DEGATS);
            t.draw(cadre);
            t.fill(s);
            t.dispose();
        }
    }

    private boolean survolCible(IUnite u) {
        Rectangle2D z = zones.get(u);
        return z != null && estSelectionnable(u) && z.contains(sourisX, sourisY);
    }

    private void surbrillance(Graphics2D g, Shape forme, Color c) {
        g.setColor(Theme.alpha(c, 0.18));
        g.fill(forme);
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(Theme.alpha(c, 0.9));
        g.draw(forme);
    }

    private void dessinerLigne(Graphics2D g, IUnite u, int profondeur, int guilde, double x, double y,
                               double w, double h) {
        Color cg = Theme.guilde(guilde);
        boolean vivant = u.estVivant();
        double xi = x + (profondeur - 1) * 20;
        double cy = y + h / 2;
        Rectangle2D zone = new Rectangle2D.Double(xi - 6, y + 1, x + w - xi + 6, h - 2);
        zones.put(u, zone);
        boolean selectionnable = estSelectionnable(u);
        boolean survol = selectionnable && zone.contains(sourisX, sourisY);
        RoundRectangle2D fondLigne = new RoundRectangle2D.Double(zone.getX(), zone.getY(), zone.getWidth(),
                zone.getHeight(), 12, 12);

        if (profondeur > 1) {
            g.setColor(Theme.alpha(cg, 0.3));
            g.setStroke(new BasicStroke(1.2f));
            double gx = xi - 13;
            g.draw(new Line2D.Double(gx, y - h / 2 + 4, gx, cy));
            g.draw(new Line2D.Double(gx, cy, xi - 7, cy));
        }
        if (u == attaquantChoisi) {
            surbrillance(g, fondLigne, Theme.OR);
        } else if (survol) {
            surbrillance(g, fondLigne, mode == Mode.CIBLE_FRAPPE ? Theme.DEGATS : mode == Mode.SOIGNEUR
                    ? Theme.SOIN : Theme.OR);
        } else if (selectionnable) {
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(Theme.alpha(mode == Mode.CIBLE_FRAPPE ? Theme.DEGATS : Theme.OR,
                    0.15 + 0.3 * (0.5 + 0.5 * Math.sin(temps * 5))));
            g.draw(fondLigne);
        }
        Double flash = flashs.get(u);
        if (flash != null) {
            g.setColor(Theme.alpha(flash > 0 ? Theme.DEGATS : Theme.SOIN, Math.abs(flash) / 0.45 * 0.35));
            g.fill(fondLigne);
        }

        Composite ancien = g.getComposite();
        if (!vivant) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f));
        }
        float taillePolice = (float) Math.min(13.5, h * 0.38);
        double droite = x + w - 8;
        double r;
        String nomAffiche;
        Color couleurNom;
        if (u.estComposite()) {
            r = h * 0.36;
            Icones.groupe(g, xi + r, cy, r * 0.95, vivant ? cg : Theme.MORT);
            nomAffiche = u.getNom();
            couleurNom = Theme.melange(cg, Theme.TEXTE, 0.45);
        } else {
            r = h * 0.42;
            portrait(g, u, xi + r, cy, r, guilde);
            nomAffiche = u.getNom();
            couleurNom = Theme.TEXTE;
        }
        centres.put(u, new Point2D.Double(xi + r, cy));
        double nx = xi + 2 * r + 8;
        g.setFont(Theme.texte(Font.BOLD, taillePolice));
        g.setColor(couleurNom);
        g.drawString(nomAffiche, (float) nx, (float) (cy - 2));
        FontMetrics fm = g.getFontMetrics();
        double finNom = nx + fm.stringWidth(nomAffiche);
        if (u instanceof UniteElementaire ue) {
            g.setFont(Theme.texte(Font.PLAIN, taillePolice - 2.5f));
            g.setColor(Theme.alpha(Theme.type(u), 0.9));
            g.drawString(ue.getType(), (float) (finNom + 6), (float) (cy - 2));
        } else if (vivant && vivantesDirectes(u) >= 2) {
            pastille(g, "+10%", finNom + 8, cy - 12, Theme.OR);
        }
        if (!vivant) {
            g.setColor(Theme.TEXTE);
            g.setStroke(new BasicStroke(1.4f));
            g.draw(new Line2D.Double(nx - 2, cy - 6, finNom + 2, cy - 6));
        }
        barreVie(g, u, nx, cy + 3, droite - 70 - nx, 6, false);

        g.setFont(Theme.texte(Font.BOLD, taillePolice - 1.5f));
        g.setColor(Theme.OR);
        String atq = "ATQ " + u.getPuissanceAttaque();
        g.drawString(atq, (float) (droite - fm.stringWidth(atq) + 4), (float) (cy - 2));
        g.setFont(Theme.texte(Font.PLAIN, taillePolice - 2.5f));
        g.setColor(Theme.TEXTE_DOUX);
        String pv = u.getPointsDeVie() + "/" + u.getPointsDeVieMax();
        g.drawString(pv, (float) (droite - g.getFontMetrics().stringWidth(pv)), (float) (cy + 11));
        g.setComposite(ancien);
        if (!vivant && !u.estComposite()) {
            pastille(g, "K.O.", droite - 100, cy - 8, Theme.DEGATS);
        }
    }

    private static int vivantesDirectes(IUnite u) {
        int n = 0;
        for (IUnite e : u.getSousUnites()) {
            if (e.estVivant()) {
                n++;
            }
        }
        return n;
    }

    private void pastille(Graphics2D g, String texte, double x, double y, Color c) {
        g.setFont(Theme.texte(Font.BOLD, 9.5f));
        FontMetrics fm = g.getFontMetrics();
        RoundRectangle2D rr = new RoundRectangle2D.Double(x, y, fm.stringWidth(texte) + 10, 14, 14, 14);
        g.setColor(Theme.alpha(c, 0.2));
        g.fill(rr);
        g.setColor(Theme.alpha(c, 0.8));
        g.setStroke(new BasicStroke(1f));
        g.draw(rr);
        g.setColor(c);
        g.drawString(texte, (float) (x + 5), (float) (y + 10.5));
    }

    private void portrait(Graphics2D g, IUnite u, double cx, double cy, double r, int guilde) {
        boolean v = u.estVivant();
        Color ct = v ? Theme.type(u) : Theme.MORT;
        if (v) {
            g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) (r * 1.7), new float[] {0f, 1f},
                    new Color[] {Theme.alpha(ct, 0.35), Theme.alpha(ct, 0)}));
            g.fill(new Ellipse2D.Double(cx - r * 1.7, cy - r * 1.7, r * 3.4, r * 3.4));
        }
        Ellipse2D cercle = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        g.setPaint(new RadialGradientPaint((float) (cx - r * 0.3), (float) (cy - r * 0.35), (float) (r * 1.5),
                new float[] {0f, 0.55f, 1f},
                new Color[] {Theme.melange(ct, Color.WHITE, 0.2), Theme.melange(ct, Color.BLACK, 0.35),
                    Theme.melange(ct, Color.BLACK, 0.75)}));
        g.fill(cercle);
        g.setStroke(new BasicStroke(1.8f));
        g.setColor(v ? Theme.guilde(guilde) : Theme.MORT);
        g.draw(cercle);
        Icones.unite(g, u, cx, cy, r * 0.6, v);
        Double flash = flashs.get(u);
        if (flash != null && flash > 0) {
            g.setColor(new Color(255, 255, 255, (int) (Math.min(1, flash / 0.45) * 210)));
            g.fill(cercle);
        }
    }

    private void barreVie(Graphics2D g, IUnite u, double x, double y, double w, double h, boolean grande) {
        if (w <= 4) {
            return;
        }
        double max = Math.max(1, u.getPointsDeVieMax());
        double reel = u.getPointsDeVie();
        double fantome = pvFantomes.getOrDefault(u, reel);
        RoundRectangle2D cadre = new RoundRectangle2D.Double(x, y, w, h, h, h);
        g.setColor(new Color(0, 0, 0, 160));
        g.fill(cadre);
        Shape ancienClip = g.getClip();
        g.clip(cadre);
        double rr = reel / max;
        double rf = Math.max(0, fantome / max);
        if (rf > rr) {
            g.setColor(new Color(255, 236, 236, 230));
            g.fill(new Rectangle2D.Double(x, y, w * rf, h));
        } else if (rf < rr) {
            g.setColor(Theme.alpha(Theme.SOIN, 0.9));
            g.fill(new Rectangle2D.Double(x, y, w * rr, h));
        }
        Color c = Theme.vie(rr);
        g.setPaint(new GradientPaint(0, (float) y, Theme.melange(c, Color.WHITE, 0.25), 0, (float) (y + h),
                Theme.melange(c, Color.BLACK, 0.3)));
        g.fill(new Rectangle2D.Double(x, y, w * Math.min(rr, rf), h));
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(new Rectangle2D.Double(x, y, w, h * 0.45));
        g.setClip(ancienClip);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(255, 255, 255, 45));
        g.draw(cadre);
        if (grande) {
            String t = "PV  " + (int) reel + " / " + (int) max;
            Font f = Theme.texte(Font.BOLD, 11f);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            float tx = (float) (x + (w - fm.stringWidth(t)) / 2);
            float ty = (float) (y + h / 2 + fm.getAscent() / 2.0 - 1.5);
            g.setColor(new Color(0, 0, 0, 170));
            g.drawString(t, tx + 1, ty + 1);
            g.setColor(Color.WHITE);
            g.drawString(t, tx, ty);
        }
    }

    private void dessinerBarreBas(Graphics2D g) {
        RoundRectangle2D barre = new RoundRectangle2D.Double(12, BARRE_Y, LARGEUR - 24, 90, 22, 22);
        g.setPaint(new GradientPaint(0, (float) BARRE_Y, Theme.CARTE_HAUT, 0, (float) BARRE_Y + 90, Theme.CARTE_BAS));
        g.fill(barre);
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(Theme.alpha(Theme.OR, 0.4));
        g.draw(barre);
        if (joueurs.isEmpty()) {
            return;
        }
        Joueur j = joueurs.get(actif);
        Color cg = Theme.guilde(actif);
        Icones.blason(g, actif, 56, BARRE_Y + 44, 24, false);
        g.setFont(Theme.titre(18));
        g.setColor(cg);
        g.drawString("TOUR DE " + j.getNom().toUpperCase() + (ia[actif] ? "  (IA)" : ""), 92f, (float) BARRE_Y + 32);
        g.setFont(Theme.texte(Font.PLAIN, 12.5f));
        g.setColor(Theme.TEXTE_DOUX);
        g.drawString("Guilde " + j.getFaction(), 92f, (float) BARRE_Y + 51);
        g.setFont(Theme.texte(Font.ITALIC, 12.5f));
        g.setColor(Theme.OR_PALE);
        g.drawString(consigne(), 92f, (float) BARRE_Y + 72);

        double jx = 1000;
        g.setFont(Theme.texte(Font.BOLD, 10f));
        g.setColor(Theme.OR);
        g.drawString("JOURNAL DE BATAILLE", (float) jx, (float) BARRE_Y + 22);
        g.setFont(Theme.texte(Font.PLAIN, 11.5f));
        int debut = Math.max(0, journal.size() - 4);
        for (int k = debut; k < journal.size(); k++) {
            int rang = journal.size() - 1 - k;
            g.setColor(Theme.alpha(Theme.TEXTE, 1 - rang * 0.22));
            g.drawString(tronquer(g, journal.get(k), 250), (float) jx, (float) (BARRE_Y + 40 + (k - debut) * 15));
        }
    }

    private String consigne() {
        if (ia[actif]) {
            return "L'IA prepare son coup...";
        }
        return switch (mode) {
            case ACTION -> "Choisissez une action.";
            case CIBLE_TOTALE -> "Cliquez sur une guilde ennemie a attaquer.";
            case ATTAQUANT -> "Choisissez l'escouade ou l'unite qui frappe.";
            case CIBLE_FRAPPE -> "Choisissez la cible : armee, groupe ou unite.";
            case SOIGNEUR -> "Choisissez le soigneur qui intervient.";
            default -> "Resolution du combat...";
        };
    }

    private static String tronquer(Graphics2D g, String s, double largeur) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= largeur) {
            return s;
        }
        while (s.length() > 1 && fm.stringWidth(s + "...") > largeur) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }

    private void dessinerBoutons(Graphics2D g) {
        for (Bouton b : boutons) {
            Rectangle2D z = b.zone;
            boolean survol = b.actif && z.contains(sourisX, sourisY);
            RoundRectangle2D rr = new RoundRectangle2D.Double(z.getX(), z.getY(), z.getWidth(), z.getHeight(), 16, 16);
            double cy = z.getCenterY();
            if (!b.actif) {
                g.setColor(new Color(20, 22, 36, 200));
                g.fill(rr);
                g.setColor(Theme.alpha(Theme.MORT, 0.6));
                g.setStroke(new BasicStroke(1.2f));
                g.draw(rr);
            } else {
                if (survol || b.selectionne) {
                    for (int k = 5; k >= 1; k--) {
                        g.setStroke(new BasicStroke(k * 3f));
                        g.setColor(Theme.alpha(Theme.OR, 0.05));
                        g.draw(rr);
                    }
                }
                Paint p = b.selectionne
                        ? new GradientPaint(0, (float) z.getY(), Theme.OR_PALE, 0, (float) z.getMaxY(), Theme.OR)
                        : new GradientPaint(0, (float) z.getY(), survol ? new Color(0x4A3418) : new Color(0x2C2140),
                            0, (float) z.getMaxY(), survol ? new Color(0x2A1C0C) : new Color(0x160F26));
                g.setPaint(p);
                g.fill(rr);
                g.setStroke(new BasicStroke(1.6f));
                g.setColor(Theme.alpha(Theme.OR, survol || b.selectionne ? 1 : 0.6));
                g.draw(rr);
                g.setColor(new Color(255, 255, 255, 25));
                g.fill(new RoundRectangle2D.Double(z.getX() + 3, z.getY() + 3, z.getWidth() - 6, z.getHeight() / 2 - 3,
                        12, 12));
            }
            Color texte = !b.actif ? Theme.MORT : b.selectionne ? new Color(0x2A1606) : Theme.OR_PALE;
            boolean compact = z.getWidth() < 200;
            double tx = z.getX() + (compact ? 46 : 52);
            Icones.bouton(g, b.icone, z.getX() + (compact ? 25 : 28), cy, z.getHeight() > 50 ? 11 : 9, texte);
            float taille = compact ? 14f : z.getHeight() > 50 ? 15.5f : 14f;
            double place = z.getMaxX() - tx - 10;
            while (taille > 9 && g.getFontMetrics(Theme.titre(taille)).stringWidth(b.libelle) > place) {
                taille -= 0.5f;
            }
            g.setFont(Theme.titre(taille));
            g.setColor(texte);
            boolean avecSous = !b.sousTitre.isEmpty();
            g.drawString(b.libelle, (float) tx, (float) (cy + (avecSous ? -2 : 5)));
            if (avecSous) {
                g.setFont(Theme.texte(Font.PLAIN, 11f));
                g.setColor(b.selectionne ? new Color(0x5A3A10) : Theme.alpha(Theme.TEXTE_DOUX, b.actif ? 1 : 0.5));
                g.drawString(b.sousTitre, (float) tx, (float) (cy + 15));
            }
        }
    }

    private void dessinerVisee(Graphics2D g) {
        Point2D depart;
        Color c;
        if (mode == Mode.CIBLE_FRAPPE && attaquantChoisi != null) {
            depart = centre(attaquantChoisi);
            c = Theme.DEGATS;
        } else if (mode == Mode.CIBLE_TOTALE) {
            depart = centre(joueurs.get(actif).getArmee());
            c = Theme.OR;
        } else {
            return;
        }
        if (sourisX < 0) {
            return;
        }
        double mx = (depart.getX() + sourisX) / 2;
        double my = Math.min(depart.getY(), sourisY) - 80;
        QuadCurve2D courbe = new QuadCurve2D.Double(depart.getX(), depart.getY(), mx, my, sourisX, sourisY);
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.alpha(c, 0.18));
        g.draw(courbe);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[] {10f, 8f}, (float) (18 - (temps * 60) % 18)));
        g.setColor(c);
        g.draw(courbe);
        double r = 12 + 3 * Math.sin(temps * 8);
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(sourisX - r, sourisY - r, r * 2, r * 2));
        g.draw(new Line2D.Double(sourisX - r - 6, sourisY, sourisX - r + 4, sourisY));
        g.draw(new Line2D.Double(sourisX + r - 4, sourisY, sourisX + r + 6, sourisY));
        g.draw(new Line2D.Double(sourisX, sourisY - r - 6, sourisX, sourisY - r + 4));
        g.draw(new Line2D.Double(sourisX, sourisY + r - 4, sourisX, sourisY + r + 6));
    }

    private void dessinerBanniere(Graphics2D g0) {
        double t = temps - banniereDebut;
        if (t < 0 || t > 1.8) {
            return;
        }
        double a = t < 0.25 ? t / 0.25 : t > 1.4 ? (1.8 - t) / 0.4 : 1;
        double glisse = Math.pow(1 - Math.min(1, t / 0.35), 3) * 220;
        Graphics2D g = (Graphics2D) g0.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, Math.min(1, a))));
        double y = 300;
        g.setPaint(new LinearGradientPaint(0, 0, LARGEUR, 0, new float[] {0f, 0.25f, 0.75f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(6, 4, 14, 225), new Color(6, 4, 14, 225),
                    new Color(0, 0, 0, 0)}));
        g.fillRect(0, (int) y, LARGEUR, 110);
        g.setPaint(new LinearGradientPaint(0, 0, LARGEUR, 0, new float[] {0f, 0.5f, 1f},
                new Color[] {Theme.alpha(banniereCouleur, 0), banniereCouleur, Theme.alpha(banniereCouleur, 0)}));
        g.fillRect(0, (int) y, LARGEUR, 2);
        g.fillRect(0, (int) y + 108, LARGEUR, 2);
        Font f = Theme.titre(46);
        GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), banniereTitre);
        Rectangle2D b = gv.getVisualBounds();
        Shape s = AffineTransform.getTranslateInstance(LARGEUR / 2.0 - b.getWidth() / 2 - b.getX() - glisse, y + 64)
                .createTransformedShape(gv.getOutline());
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(10, 6, 16));
        g.draw(s);
        g.setColor(banniereCouleur);
        g.fill(s);
        texteCentre(g, banniereSous, Theme.texte(Font.PLAIN, 16), Theme.TEXTE, LARGEUR / 2.0 + glisse, y + 92);
        g.dispose();
    }

    private void dessinerInfobulle(Graphics2D g) {
        IUnite u = noeudSous(sourisX, sourisY);
        if (u == null || mode == Mode.ATTENTE || temps - banniereDebut < 1.8) {
            return;
        }
        List<String> lignes = new ArrayList<>();
        if (u instanceof UniteElementaire ue) {
            lignes.add(ue.getType().toUpperCase() + "  -  " + u.getNom());
            lignes.add("PV " + u.getPointsDeVie() + " / " + u.getPointsDeVieMax());
            String atq = "ATQ " + u.getPuissanceAttaque() + "  -  Cout " + u.getCout() + " PO";
            if (u instanceof Guerrier gu) {
                atq += "  -  ARM " + gu.getArmure();
            }
            lignes.add(atq);
            if (u instanceof Soigneur s) {
                lignes.add("Soin : +" + s.getPuissanceSoin() + " PV repartis");
            }
            lignes.add("Feuille du Composite");
        } else {
            boolean racine = joueurs.stream().anyMatch(j -> j.getArmee() == u);
            lignes.add((racine ? "ARMEE  -  " : "GROUPE  -  ") + u.getNom());
            lignes.add("PV " + u.getPointsDeVie() + " / " + u.getPointsDeVieMax());
            int viv = vivantesDirectes(u);
            lignes.add("ATQ totale " + u.getPuissanceAttaque() + (viv >= 2 ? "  (synergie +10%)" : ""));
            lignes.add("Membres vivants : " + viv + " / " + u.getSousUnites().size());
            lignes.add("Composite : delegue a ses " + u.getSousUnites().size() + " sous-unites");
        }
        Font titre = Theme.texte(Font.BOLD, 12.5f);
        Font corps = Theme.texte(Font.PLAIN, 12f);
        g.setFont(corps);
        double largeur = 0;
        for (String l : lignes) {
            largeur = Math.max(largeur, g.getFontMetrics(titre).stringWidth(l));
        }
        largeur += 26;
        double hauteur = lignes.size() * 17 + 14;
        double x = Math.min(sourisX + 18, LARGEUR - largeur - 8);
        double y = Math.min(sourisY + 18, HAUTEUR - hauteur - 8);
        RoundRectangle2D cadre = new RoundRectangle2D.Double(x, y, largeur, hauteur, 12, 12);
        g.setColor(new Color(8, 8, 20, 235));
        g.fill(cadre);
        g.setColor(Theme.alpha(Theme.guilde(guildeDe.getOrDefault(u, 0)), 0.9));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(cadre);
        for (int k = 0; k < lignes.size(); k++) {
            boolean premiere = k == 0;
            boolean derniere = k == lignes.size() - 1;
            g.setFont(premiere ? titre : corps);
            g.setColor(premiere ? Theme.OR_PALE : derniere ? Theme.alpha(Theme.TEXTE_DOUX, 0.8) : Theme.TEXTE);
            g.drawString(lignes.get(k), (float) x + 13, (float) (y + 22 + k * 17));
        }
    }

    // ------------------------------------------------------------------
    // Ecran de victoire
    // ------------------------------------------------------------------

    private void dessinerVictoire(Graphics2D g) {
        g.setColor(new Color(4, 3, 12, 175));
        g.fillRect(0, 0, LARGEUR, HAUTEUR);
        Graphics2D r = (Graphics2D) g.create();
        r.translate(LARGEUR / 2.0, 250);
        r.rotate(temps * 0.15);
        Color cg = Theme.guilde(indexVainqueur);
        for (int k = 0; k < 18; k++) {
            r.rotate(Math.PI * 2 / 18);
            Path2D rayon = new Path2D.Double();
            rayon.moveTo(0, 0);
            rayon.lineTo(-40, -700);
            rayon.lineTo(40, -700);
            rayon.closePath();
            r.setPaint(new GradientPaint(0, 0, Theme.alpha(k % 2 == 0 ? Theme.OR : cg, 0.16), 0, -600,
                    Theme.alpha(cg, 0)));
            r.fill(rayon);
        }
        r.dispose();
        effets.dessiner(g);
        Icones.blason(g, indexVainqueur, LARGEUR / 2.0, 238 + Math.sin(temps * 2) * 6, 78, false);
        texteLogo(g, "VICTOIRE", LARGEUR / 2.0, 450, 88);
        texteCentre(g, vainqueur.getNom() + " et la Guilde " + vainqueur.getFaction() + " remportent le Clash !",
                Theme.titre(22), Theme.TEXTE, LARGEUR / 2.0, 500);
        Escouade armee = vainqueur.getArmee();
        long survivants = ParcoursArbre.feuilles(armee).stream().filter(IUnite::estVivant).count();
        texteCentre(g, "Triomphe en " + tour + " tours   -   PV restants " + armee.getPointsDeVie() + " / "
                        + armee.getPointsDeVieMax() + "   -   Survivants " + survivants + " / "
                        + ParcoursArbre.feuilles(armee).size(),
                Theme.texte(Font.PLAIN, 15), Theme.TEXTE_DOUX, LARGEUR / 2.0, 535);
        dessinerBoutons(g);
    }

    private static void texteCentre(Graphics2D g, String s, Font f, Color c, double cx, double base) {
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(c);
        g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) base);
    }
}
