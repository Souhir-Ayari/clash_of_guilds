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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Version graphique 2D : un vrai champ de bataille ou trois armees de dix
 * soldats s'affrontent. Les guerriers courent frapper a l'epee, les archers
 * tirent des fleches depuis leur position, les mages lancent des orbes,
 * la defense fait apparaitre armures et boucliers, le soin une aura verte.
 *
 * Tout le calcul passe par le modele Composite (IUnite, Escouade, Combat) :
 * cette classe ne fait que mettre en scene ses resultats.
 */
@SuppressWarnings("serial")
public final class PanneauJeu extends JPanel {

    static final int LARGEUR = 1280;
    static final int HAUTEUR = 720;
    private static final double BARRE_Y = 620;

    /** Zones des armees : Lions a gauche, Ombres a droite, Gardiens en haut. */
    private static final Rectangle2D[] REGIONS = {
        new Rectangle2D.Double(80, 318, 390, 282),
        new Rectangle2D.Double(810, 318, 390, 282),
        new Rectangle2D.Double(410, 86, 460, 200)
    };
    /** Pied des etendards de chaque armee. */
    private static final double[][] DRAPEAUX = {{50, 600}, {1230, 600}, {378, 280}};

    enum Ecran { TITRE, JEU, VICTOIRE }

    enum Mode { ACTION, CIBLE_TOTALE, ATTAQUANT, CIBLE_FRAPPE, ATTENTE }

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

    /** Un bloc = une escouade dont les soldats se tiennent ensemble sur le terrain. */
    private static final class Bloc {
        final IUnite groupe;
        final int guilde;
        final Rectangle2D zone;
        final List<Soldat> soldats = new ArrayList<>();

        Bloc(IUnite groupe, int guilde, Rectangle2D zone) {
            this.groupe = groupe;
            this.guilde = guilde;
            this.zone = zone;
        }
    }

    private record Tache(double moment, Runnable action) {
    }

    private static final class Aura {
        final double x;
        final double y;
        final double rx;
        final double ry;
        final Color couleur;
        double age;

        Aura(double x, double y, double rx, double ry, Color couleur) {
            this.x = x;
            this.y = y;
            this.rx = rx;
            this.ry = ry;
            this.couleur = couleur;
        }
    }

    private static final class Braise {
        double x;
        double y;
        double vy;
        double phase;
        double taille;
    }

    private final Effets effets = new Effets();
    private final Random alea = effets.alea;

    private final List<Joueur> joueurs = new ArrayList<>();
    private final boolean[] ia = new boolean[3];
    private final Set<Integer> eliminationsAnnoncees = new HashSet<>();
    private final List<Soldat> soldats = new ArrayList<>();
    private final Map<IUnite, Soldat> soldatDe = new HashMap<>();
    private final List<Bloc> blocs = new ArrayList<>();
    private final Map<IUnite, Integer> guildeDe = new HashMap<>();
    private final Rectangle2D[] drapeaux = new Rectangle2D[3];
    private final Rectangle2D[] cartes = new Rectangle2D[3];
    private final List<Bouton> boutons = new ArrayList<>();
    private final List<Tache> taches = new ArrayList<>();
    private final List<String> journal = new ArrayList<>();
    private final List<Aura> auras = new ArrayList<>();
    private final List<Braise> braises = new ArrayList<>();
    private final double[][] etoiles = new double[150][];

    private Ecran ecran = Ecran.TITRE;
    private Mode mode = Mode.ACTION;
    private int actif;
    private int tour;
    private IUnite attaquantChoisi;
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
    private BufferedImage fondCiel;
    private BufferedImage fondChamp;
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
        for (int k = 1; k <= 4; k++) {
            int index = k - 1;
            raccourci(String.valueOf(k), () -> declencherBouton(index));
        }
        construireBoutonsTitre();
        boucle = new Timer(15, e -> tic());
    }

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
    // Mise en place de la bataille
    // ==================================================================

    void demarrerPartie(boolean ia1, boolean ia2, boolean ia3) {
        joueurs.clear();
        soldats.clear();
        soldatDe.clear();
        blocs.clear();
        guildeDe.clear();
        journal.clear();
        taches.clear();
        auras.clear();
        eliminationsAnnoncees.clear();
        ia[0] = ia1;
        ia[1] = ia2;
        ia[2] = ia3;
        String[] humains = {"Alice", "Bob", "Charlie"};
        String[] robots = {"Leonis", "Umbra", "Runa"};
        Escouade[] armees = {GuildesPredefinies.grandeArmeeLions(), GuildesPredefinies.grandeArmeeOmbres(),
            GuildesPredefinies.grandeArmeeGardiens()};
        String[] factions = {GuildesPredefinies.LIONS, GuildesPredefinies.OMBRES, GuildesPredefinies.GARDIENS};
        for (int i = 0; i < 3; i++) {
            joueurs.add(new Joueur(ia[i] ? robots[i] : humains[i], factions[i], armees[i]));
            indexer(armees[i], i);
            deployer(armees[i], i);
        }
        actif = 0;
        tour = 1;
        ecran = Ecran.JEU;
        construireBoutonsJeu();
        journal.add("Les trois armees se font face !");
        debutTour();
    }

    private void indexer(IUnite u, int guilde) {
        guildeDe.put(u, guilde);
        for (IUnite enfant : u.getSousUnites()) {
            indexer(enfant, guilde);
        }
    }

    /** Place les soldats de chaque escouade en formation dans la zone de leur armee. */
    private void deployer(Escouade armee, int guilde) {
        Map<IUnite, List<IUnite>> parBloc = new LinkedHashMap<>();
        for (IUnite feuille : ParcoursArbre.feuilles(armee)) {
            IUnite parent = ParcoursArbre.trouverParent(armee, feuille);
            parBloc.computeIfAbsent(parent == null ? armee : parent, k -> new ArrayList<>()).add(feuille);
        }
        Rectangle2D reg = REGIONS[guilde];
        int n = parBloc.size();
        int colonnes = guilde == 2 ? n : 2;
        int lignes = (int) Math.ceil(n / (double) colonnes);
        double cw = reg.getWidth() / colonnes;
        double ch = reg.getHeight() / lignes;
        int k = 0;
        for (Map.Entry<IUnite, List<IUnite>> e : parBloc.entrySet()) {
            double cx = reg.getX() + (k % colonnes) * cw;
            double cy = reg.getY() + (k / colonnes) * ch;
            Bloc bloc = new Bloc(e.getKey(), guilde, new Rectangle2D.Double(cx + 6, cy + 4, cw - 12, ch - 8));
            List<IUnite> membres = e.getValue();
            int parLigne = membres.size() == 4 ? 2 : Math.min(3, membres.size());
            for (int m = 0; m < membres.size(); m++) {
                int ligne = m / parLigne;
                int col = m % parLigne;
                int dansLigne = Math.min(parLigne, membres.size() - ligne * parLigne);
                double x = cx + cw / 2 + (col - (dansLigne - 1) / 2.0) * 52 + (ligne % 2) * 12;
                double y = cy + 88 + ligne * 54;
                boolean faceDroite = guilde == 0 || (guilde == 2 && x < LARGEUR / 2.0);
                Soldat s = new Soldat(membres.get(m), guilde, bloc.groupe, x, y, faceDroite,
                        alea.nextDouble() * 6.28);
                bloc.soldats.add(s);
                soldats.add(s);
                soldatDe.put(s.unite, s);
            }
            blocs.add(bloc);
            k++;
        }
    }

    private void debutTour() {
        attaquantChoisi = null;
        Joueur j = joueurs.get(actif);
        Combat.finirDefense(j.getArmee());
        mode = ia[actif] ? Mode.ATTENTE : Mode.ACTION;
        banniere("TOUR DE " + j.getNom().toUpperCase(),
                "Guilde " + j.getFaction() + (ia[actif] ? "  -  IA" : ""), Theme.guilde(actif));
        if (ia[actif]) {
            planifier(1.6, this::jouerIA);
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
        for (Soldat s : soldats) {
            s.avancer(dt);
        }
        effets.avancer(dt);
        for (Iterator<Aura> it = auras.iterator(); it.hasNext();) {
            Aura a = it.next();
            a.age += dt;
            if (a.age > 1.8) {
                it.remove();
            }
        }
        if (ecran == Ecran.TITRE) {
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
        }
        if (ecran == Ecran.VICTOIRE && temps >= prochainFeu) {
            feuDArtifice();
            prochainFeu = temps + 0.25 + alea.nextDouble() * 0.4;
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
    // Actions de combat
    // ==================================================================

    private List<Soldat> soldatsVivants(IUnite racine) {
        List<Soldat> r = new ArrayList<>();
        for (IUnite f : ParcoursArbre.feuilles(racine)) {
            Soldat s = soldatDe.get(f);
            if (s != null && f.estVivant() && !s.mortAffiche) {
                r.add(s);
            }
        }
        return r;
    }

    /**
     * Mise en scene d'une attaque : chaque soldat vivant de l'attaquant agit
     * selon son arme. Quand tous les coups ont porte, le modele calcule les
     * degats (Combat.attaquer) et on revele le resultat soldat par soldat.
     */
    private void attaquer(IUnite attaquant, IUnite cible, String libelle) {
        mode = Mode.ATTENTE;
        attaquantChoisi = null;
        List<Soldat> assaillants = soldatsVivants(attaquant);
        List<Soldat> victimes = soldatsVivants(cible);
        journal.add(libelle);
        int[] restants = {assaillants.size()};
        Runnable coupPorte = () -> {
            restants[0]--;
            if (restants[0] == 0) {
                revelerAttaque(attaquant, cible, assaillants);
            }
        };
        if (assaillants.isEmpty() || victimes.isEmpty()) {
            revelerAttaque(attaquant, cible, assaillants);
            return;
        }
        for (int k = 0; k < assaillants.size(); k++) {
            Soldat s = assaillants.get(k);
            Soldat v = victimes.get(alea.nextInt(victimes.size()));
            if (s.estMarcheur()) {
                planifier(k * 0.06, () -> s.charger(v.x, v.y, () -> {
                    effets.explosion(v.x, v.y - 26, new Color(255, 240, 210), 14, 170);
                    impactSur(v, s.x);
                    coupPorte.run();
                }));
            } else {
                Color c = Theme.type(s.unite);
                Effets.Style style = s.unite instanceof Mage ? Effets.Style.ORBE
                        : s.unite instanceof Soigneur ? Effets.Style.SOIN : Effets.Style.FLECHE;
                planifier(0.15 + k * 0.08, () -> s.tirer(v.x, () -> effets.tirer(s.main(), v.centre(), c, style,
                        0, 0.55 + alea.nextDouble() * 0.15, 40 + alea.nextDouble() * 70, () -> {
                            effets.explosion(v.x, v.y - 26, c, 10, 150);
                            impactSur(v, s.x);
                            coupPorte.run();
                        })));
            }
        }
    }

    private void impactSur(Soldat v, double depuisX) {
        v.toucher(depuisX);
        effets.secouer(2.5);
        if (v.defense > 0.3) {
            effets.onde(v.x, v.y - 28, new Color(150, 210, 255), 30);
        }
    }

    private void revelerAttaque(IUnite attaquant, IUnite cible, List<Soldat> assaillants) {
        Map<IUnite, Integer> avant = new HashMap<>();
        for (IUnite f : ParcoursArbre.feuilles(cible)) {
            avant.put(f, f.getPointsDeVie());
        }
        boolean defendue = soldatsVivants(cible).stream().anyMatch(s -> s.defense > 0.5);

        Combat.Resultat r = Combat.attaquer(attaquant, cible);

        int morts = 0;
        for (Map.Entry<IUnite, Integer> e : avant.entrySet()) {
            Soldat s = soldatDe.get(e.getKey());
            int perte = e.getValue() - e.getKey().getPointsDeVie();
            s.pvAffiche = e.getKey().getPointsDeVie();
            if (perte > 0) {
                effets.texte("-" + perte, s.x, s.y - 70, new Color(0xFF7A85), 17);
            }
            if (!e.getKey().estVivant() && !s.mortAffiche) {
                s.mortAffiche = true;
                s.tMort = 0;
                morts++;
                effets.explosion(s.x, s.y - 16, new Color(170, 150, 120), 22, 150);
                effets.texte("K.O.", s.x, s.y - 88, Theme.OR, 15);
            }
        }
        Point2D c = centreDe(cible);
        effets.texte("-" + r.pvPerdus() + " PV", c.getX(), c.getY() - 70, Theme.OR_PALE, 30);
        if (defendue) {
            effets.texte("ARMURE !", c.getX(), c.getY() - 104, new Color(160, 215, 255), 20);
        }
        effets.onde(c.getX(), c.getY() - 20, Theme.guilde(guildeDe.get(attaquant)), 150);
        effets.secouer(Math.min(20, 5 + r.degatsBruts() / 18.0));
        journal.add(r.pvPerdus() + " degats sur " + cible.getNom() + (morts > 0 ? " (" + morts + " K.O.)" : ""));
        double retour = 0;
        for (Soldat s : assaillants) {
            retour = Math.max(retour, s.distanceAuPoste() / Soldat.VITESSE);
        }
        planifier(retour + 0.7, this::finAction);
    }

    private void defendre() {
        mode = Mode.ATTENTE;
        Joueur j = joueurs.get(actif);
        Combat.defendre(j.getArmee());
        List<Soldat> vivants = soldatsVivants(j.getArmee());
        for (int k = 0; k < vivants.size(); k++) {
            Soldat s = vivants.get(k);
            planifier(k * 0.05, () -> {
                effets.onde(s.x, s.y - 26, new Color(150, 210, 255), 34);
                effets.explosion(s.x, s.y - 30, new Color(220, 235, 255), 8, 90);
            });
        }
        Point2D c = centreDe(j.getArmee());
        effets.texte("ARMURE +" + Combat.BONUS_DEFENSE, c.getX(), c.getY() - 60, new Color(160, 215, 255), 30);
        journal.add(j.getFaction() + " leve les boucliers (armure +" + Combat.BONUS_DEFENSE + ")");
        planifier(1.6, this::finAction);
    }

    private void soigner() {
        mode = Mode.ATTENTE;
        Joueur j = joueurs.get(actif);
        List<Soldat> soigneurs = new ArrayList<>();
        for (Soldat s : soldatsVivants(j.getArmee())) {
            if (s.unite instanceof Soigneur) {
                soigneurs.add(s);
            }
        }
        for (int k = 0; k < soigneurs.size(); k++) {
            Soldat s = soigneurs.get(k);
            planifier(k * 0.5, () -> s.incanter(() -> appliquerSoin(s)));
        }
        planifier(soigneurs.size() * 0.5 + 1.6, this::finAction);
    }

    private void appliquerSoin(Soldat soigneur) {
        IUnite armee = joueurs.get(soigneur.guilde).getArmee();
        IUnite groupe = ParcoursArbre.trouverParent(armee, soigneur.unite);
        Map<IUnite, Integer> avant = new HashMap<>();
        for (IUnite f : ParcoursArbre.feuilles(groupe)) {
            avant.put(f, f.getPointsDeVie());
        }
        int total = ((Soigneur) soigneur.unite).soigner(groupe);
        Bloc bloc = blocDe(groupe);
        if (bloc != null) {
            double bas = bloc.soldats.stream().mapToDouble(s -> s.hy).max().orElse(bloc.zone.getCenterY());
            auras.add(new Aura(bloc.zone.getCenterX(), bas - 6, bloc.zone.getWidth() / 2 + 10, 50, Theme.SOIN));
        }
        for (Map.Entry<IUnite, Integer> e : avant.entrySet()) {
            Soldat s = soldatDe.get(e.getKey());
            int gain = e.getKey().getPointsDeVie() - e.getValue();
            s.pvAffiche = e.getKey().getPointsDeVie();
            if (e.getKey().estVivant()) {
                effets.soin(s.x, s.y - 20);
            }
            if (gain > 0) {
                effets.texte("+" + gain, s.x, s.y - 70, Theme.SOIN, 17);
            }
        }
        journal.add(soigneur.unite.getNom() + " soigne " + groupe.getNom() + " : +" + total + " PV");
    }

    private void finAction() {
        double attente = 0.3;
        for (int i = 0; i < joueurs.size(); i++) {
            Joueur j = joueurs.get(i);
            if (j.estElimine() && eliminationsAnnoncees.add(i)) {
                banniere("ARMEE ANEANTIE", j.getFaction() + " de " + j.getNom() + " est tombee", Theme.DEGATS);
                journal.add(j.getFaction() + " est aneantie !");
                Rectangle2D reg = REGIONS[i];
                effets.explosion(reg.getCenterX(), reg.getCenterY(), Theme.DEGATS, 120, 480);
                effets.onde(reg.getCenterX(), reg.getCenterY(), Theme.guilde(i), 300);
                effets.secouer(24);
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
        indexVainqueur = index;
        ecran = Ecran.VICTOIRE;
        banniereDebut = -10;
        prochainFeu = temps;
        for (Soldat s : soldats) {
            s.celebre = s.guilde == index && !s.mortAffiche;
        }
        construireBoutonsVictoire();
    }

    /** IA : soigne ses blesses, se defend si elle faiblit, sinon attaque la guilde la plus faible. */
    private void jouerIA() {
        Joueur moi = joueurs.get(actif);
        Escouade armee = moi.getArmee();
        boolean soigneur = soldatsVivants(armee).stream().anyMatch(s -> s.unite instanceof Soigneur);
        if (soigneur && alea.nextDouble() < 0.7) {
            for (Bloc b : blocs) {
                if (b.guilde != actif || !b.groupe.estVivant()) {
                    continue;
                }
                boolean aSoigneur = b.soldats.stream().anyMatch(s -> s.unite instanceof Soigneur && s.unite.estVivant());
                int pv = 0;
                int max = 0;
                for (Soldat s : soldatsVivants(b.groupe)) {
                    pv += s.unite.getPointsDeVie();
                    max += s.unite.getPointsDeVieMax();
                }
                if (aSoigneur && max > 0 && pv < 0.65 * max) {
                    soigner();
                    return;
                }
            }
        }
        double ratio = armee.getPointsDeVie() / (double) armee.getPointsDeVieMax();
        if (ratio < 0.5 && alea.nextDouble() < 0.3) {
            defendre();
            return;
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
        if (alea.nextDouble() < 0.4) {
            Bloc meilleur = null;
            Bloc faible = null;
            int indexProie = joueurs.indexOf(proie);
            for (Bloc b : blocs) {
                if (!b.groupe.estVivant()) {
                    continue;
                }
                if (b.guilde == actif && (meilleur == null
                        || b.groupe.getPuissanceAttaque() > meilleur.groupe.getPuissanceAttaque())) {
                    meilleur = b;
                }
                if (b.guilde == indexProie && (faible == null
                        || b.groupe.getPointsDeVie() < faible.groupe.getPointsDeVie())) {
                    faible = b;
                }
            }
            if (meilleur != null && faible != null) {
                attaquer(meilleur.groupe, faible.groupe, moi.getNom() + " : " + meilleur.groupe.getNom()
                        + " charge " + faible.groupe.getNom());
                return;
            }
        }
        attaquer(armee, proie.getArmee(), moi.getNom() + " : attaque totale sur " + proie.getFaction());
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
        main |= selectionSous(sourisX, sourisY) != null;
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
        IUnite choix = selectionSous(x, y);
        if (choix == null) {
            return;
        }
        Joueur moi = joueurs.get(actif);
        switch (mode) {
            case CIBLE_TOTALE -> attaquer(moi.getArmee(), choix,
                    moi.getNom() + " : attaque totale sur " + joueurs.get(guildeDe.get(choix)).getFaction());
            case ATTAQUANT -> {
                attaquantChoisi = choix;
                mode = Mode.CIBLE_FRAPPE;
            }
            case CIBLE_FRAPPE -> attaquer(attaquantChoisi, choix,
                    moi.getNom() + " : " + attaquantChoisi.getNom() + " charge " + choix.getNom());
            default -> {
                // rien
            }
        }
    }

    /** Ce que designe la souris dans le mode courant : une escouade ou une armee entiere. */
    private IUnite selectionSous(double x, double y) {
        if (ecran != Ecran.JEU || ia[actif] || mode == Mode.ATTENTE || mode == Mode.ACTION) {
            return null;
        }
        if (mode == Mode.CIBLE_TOTALE || mode == Mode.CIBLE_FRAPPE) {
            for (int i = 0; i < 3; i++) {
                if (i == actif || joueurs.get(i).estElimine()) {
                    continue;
                }
                boolean surArmee = (drapeaux[i] != null && drapeaux[i].contains(x, y))
                        || (cartes[i] != null && cartes[i].contains(x, y));
                if (surArmee || (mode == Mode.CIBLE_TOTALE && REGIONS[i].contains(x, y))) {
                    return joueurs.get(i).getArmee();
                }
            }
        }
        if (mode == Mode.ATTAQUANT || mode == Mode.CIBLE_FRAPPE) {
            for (Bloc b : blocs) {
                boolean allie = b.guilde == actif;
                if (b.zone.contains(x, y) && b.groupe.estVivant() && (mode == Mode.ATTAQUANT) == allie) {
                    return b.groupe;
                }
            }
        }
        return null;
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
        double l = 144;
        boutons.add(new Bouton("ATTAQUE TOTALE", "Toute l'armee [1]", Icones.EPEES,
                new Rectangle2D.Double(410, y, l, 62), () -> basculer(Mode.CIBLE_TOTALE)));
        boutons.add(new Bouton("FRAPPE CIBLEE", "Une escouade [2]", Icones.CIBLE,
                new Rectangle2D.Double(562, y, l, 62), () -> basculer(Mode.ATTAQUANT)));
        boutons.add(new Bouton("DEFENSE", "Armure +" + Combat.BONUS_DEFENSE + " [3]", Icones.BOUCLIER,
                new Rectangle2D.Double(714, y, l, 62), this::defendre));
        boutons.add(new Bouton("SOIN", "Les soigneurs [4]", Icones.PLUS,
                new Rectangle2D.Double(866, y, l, 62), this::soigner));
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
        if (ecran != Ecran.JEU || boutons.size() < 4) {
            return;
        }
        boolean possible = !ia[actif] && mode != Mode.ATTENTE;
        IUnite armee = joueurs.get(actif).getArmee();
        boutons.get(0).actif = possible;
        boutons.get(1).actif = possible;
        boutons.get(2).actif = possible;
        boutons.get(3).actif = possible && soldatsVivants(armee).stream().anyMatch(s -> s.unite instanceof Soigneur);
        boutons.get(0).selectionne = mode == Mode.CIBLE_TOTALE;
        boutons.get(1).selectionne = mode == Mode.ATTAQUANT || mode == Mode.CIBLE_FRAPPE;
    }

    // ==================================================================
    // Outils
    // ==================================================================

    private Bloc blocDe(IUnite groupe) {
        for (Bloc b : blocs) {
            if (b.groupe == groupe) {
                return b;
            }
        }
        return null;
    }

    /** Centre visuel d'une unite, d'une escouade ou d'une armee (moyenne de ses soldats). */
    private Point2D centreDe(IUnite u) {
        double x = 0;
        double y = 0;
        int n = 0;
        for (IUnite f : ParcoursArbre.feuilles(u)) {
            Soldat s = soldatDe.get(f);
            if (s != null) {
                x += s.hx;
                y += s.hy;
                n++;
            }
        }
        return n == 0 ? new Point2D.Double(LARGEUR / 2.0, HAUTEUR / 2.0) : new Point2D.Double(x / n, y / n);
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
        Decor.qualite(g);
        Icones.blason(g, 0, 32, 30, 26, false);
        g.dispose();
        return img;
    }

    /** Zones cliquables des escouades d'une guilde (utilise par l'outil de captures). */
    List<Rectangle2D> zonesEscouades(int guilde) {
        List<Rectangle2D> r = new ArrayList<>();
        for (Bloc b : blocs) {
            if (b.guilde == guilde) {
                r.add(b.zone);
            }
        }
        return r;
    }

    /** Vrai quand un joueur humain peut choisir une action (utilise par l'outil de captures). */
    boolean attendUneAction() {
        return ecran == Ecran.JEU && mode == Mode.ACTION && !ia[actif] && temps - banniereDebut > 1.8;
    }

    boolean estEnVictoire() {
        return ecran == Ecran.VICTOIRE;
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

    void rendre(Graphics2D g) {
        Decor.qualite(g);
        if (ecran == Ecran.TITRE) {
            if (fondCiel == null) {
                fondCiel = Decor.ciel();
            }
            g.drawImage(fondCiel, 0, 0, null);
            dessinerEtoiles(g);
            dessinerBraises(g);
            dessinerTitre(g);
            dessinerBoutons(g);
            return;
        }
        if (fondChamp == null) {
            fondChamp = Decor.champ(REGIONS);
        }
        dessinerBataille(g);
        if (ecran == Ecran.VICTOIRE) {
            dessinerVictoire(g);
        }
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

    private void dessinerTitre(Graphics2D g) {
        double flotte = Math.sin(temps * 1.4) * 4;
        Decor.texteLogo(g, "CLASH", LARGEUR / 2.0, 168 + flotte, 112);
        Decor.texteLogo(g, "OF  GUILDS", LARGEUR / 2.0, 238 + flotte, 54);
        Decor.ornement(g, LARGEUR / 2.0, 268, 300);
        Decor.texteCentre(g, "Trois armees, un champ de bataille  -  Patron de conception Composite",
                Theme.texte(Font.PLAIN, 16), Theme.TEXTE_DOUX, LARGEUR / 2.0, 298);
        for (int i = 0; i < 3; i++) {
            double x = 520 + i * 120;
            double y = 352 + Math.sin(temps * 1.8 + i * 1.3) * 5;
            Icones.blason(g, i, x, y, 30, false);
        }
        Decor.texteCentre(g, "IUnite  -  Escouade (Composite)  -  Guerrier, Archer, Mage, Soigneur (Feuilles)",
                Theme.texte(Font.PLAIN, 12), Theme.alpha(Theme.TEXTE_DOUX, 0.8), LARGEUR / 2.0, 700);
    }

    // ------------------------------------------------------------------
    // Champ de bataille
    // ------------------------------------------------------------------

    private void dessinerBataille(Graphics2D g0) {
        Graphics2D g = (Graphics2D) g0.create();
        double[] d = effets.decalage();
        g.translate(d[0], d[1]);
        g.drawImage(fondChamp, 0, 0, null);
        for (int i = 0; i < joueurs.size(); i++) {
            dessinerEtendard(g, i);
        }
        for (Aura a : auras) {
            dessinerAura(g, a);
        }
        dessinerSelections(g);
        List<Soldat> ordre = new ArrayList<>(soldats);
        ordre.sort(Comparator.comparingDouble((Soldat s) -> s.mortAffiche ? -1 : 0).thenComparingDouble(s -> s.y));
        for (Soldat s : ordre) {
            s.dessiner(g, temps, Theme.guilde(s.guilde));
        }
        for (Bloc b : blocs) {
            dessinerEtiquette(g, b);
        }
        for (Soldat s : soldats) {
            if (!s.mortAffiche && s.etat == Soldat.Etat.REPOS) {
                petiteBarre(g, s);
            }
        }
        dessinerVisee(g);
        if (ecran == Ecran.JEU) {
            effets.dessiner(g);
        }
        g.dispose();
        dessinerHaut(g0);
        dessinerBarreBas(g0);
        if (ecran == Ecran.JEU) {
            dessinerBoutons(g0);
            dessinerBanniere(g0);
            dessinerInfobulle(g0);
        }
    }

    private void dessinerEtendard(Graphics2D g, int i) {
        double fx = DRAPEAUX[i][0];
        double fy = DRAPEAUX[i][1];
        boolean versDroite = i != 1;
        double dir = versDroite ? 1 : -1;
        boolean elimine = joueurs.get(i).estElimine();
        Color cg = elimine ? Theme.MORT : Theme.guilde(i);
        double haut = fy - 120;
        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new Ellipse2D.Double(fx - 10, fy - 3, 20, 6));
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(110, 78, 46));
        g.draw(new Line2D.Double(fx, fy, fx, haut));
        g.setColor(Theme.OR);
        g.fill(new Ellipse2D.Double(fx - 4, haut - 7, 8, 8));
        double longueur = 64;
        double hauteur = 42;
        Path2D toile = new Path2D.Double();
        int n = 14;
        for (int k = 0; k <= n; k++) {
            double u = k / (double) n;
            double vague = elimine ? u * 14 : Math.sin(temps * 4 - u * 5) * 4.5 * u;
            double px = fx + dir * u * longueur;
            if (k == 0) {
                toile.moveTo(px, haut + vague);
            } else {
                toile.lineTo(px, haut + vague);
            }
        }
        for (int k = n; k >= 0; k--) {
            double u = k / (double) n;
            double vague = elimine ? u * 14 : Math.sin(temps * 4 - u * 5 + 0.6) * 4.5 * u;
            double bas = hauteur - (k == n ? 8 : 0);
            toile.lineTo(fx + dir * u * longueur, haut + bas + vague);
        }
        toile.closePath();
        g.setPaint(new GradientPaint((float) fx, (float) haut, Theme.melange(cg, Color.WHITE, 0.15),
                (float) (fx + dir * longueur), (float) (haut + hauteur), Theme.melange(cg, Color.BLACK, 0.45)));
        g.fill(toile);
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(Theme.alpha(Theme.OR, 0.8));
        g.draw(toile);
        double vagueCentre = elimine ? 7 : Math.sin(temps * 4 - 2.5) * 2.2;
        Icones.blason(g, i, fx + dir * longueur * 0.45, haut + hauteur / 2 + vagueCentre, 12, elimine);
        drapeaux[i] = new Rectangle2D.Double(versDroite ? fx - 10 : fx - longueur - 10, haut - 12, longueur + 20,
                hauteur + 30);
        if ((mode == Mode.CIBLE_TOTALE || mode == Mode.CIBLE_FRAPPE)
                && selectionSous(sourisX, sourisY) == joueurs.get(i).getArmee()) {
            surbrillance(g, drapeaux[i], Theme.DEGATS);
        }
    }

    private void dessinerAura(Graphics2D g, Aura a) {
        double k = a.age / 1.8;
        double force = k < 0.2 ? k / 0.2 : 1 - (k - 0.2) / 0.8;
        double rx = a.rx * (0.7 + 0.3 * Math.min(1, k * 3));
        Graphics2D e = (Graphics2D) g.create();
        e.translate(a.x, a.y);
        e.scale(1, a.ry / rx);
        e.setPaint(new RadialGradientPaint(0f, 0f, (float) rx, new float[] {0f, 0.7f, 1f},
                new Color[] {Theme.alpha(a.couleur, 0.5 * force), Theme.alpha(a.couleur, 0.22 * force),
                    Theme.alpha(a.couleur, 0)}));
        e.fill(new Ellipse2D.Double(-rx, -rx, rx * 2, rx * 2));
        e.setStroke(new BasicStroke(3f));
        e.setColor(Theme.alpha(a.couleur, 0.75 * force));
        e.draw(new Ellipse2D.Double(-rx * 0.9, -rx * 0.9, rx * 1.8, rx * 1.8));
        e.dispose();
        for (int c = 0; c < 7; c++) {
            double px = a.x - rx * 0.8 + c * rx * 0.27;
            double hauteur = (60 + 30 * Math.sin(c * 1.7)) * force;
            g.setPaint(new GradientPaint((float) px, (float) a.y, Theme.alpha(a.couleur, 0.4 * force),
                    (float) px, (float) (a.y - hauteur - 50), Theme.alpha(a.couleur, 0)));
            g.fill(new Rectangle2D.Double(px - 3, a.y - hauteur - 50, 6, hauteur + 50));
        }
    }

    private void dessinerSelections(Graphics2D g) {
        if (ecran != Ecran.JEU) {
            return;
        }
        IUnite survol = selectionSous(sourisX, sourisY);
        double pulse = 0.5 + 0.5 * Math.sin(temps * 5);
        for (Soldat s : soldats) {
            if (s.mortAffiche) {
                continue;
            }
            Color c = null;
            double a = 0;
            if (attaquantChoisi != null && contient(attaquantChoisi, s.unite)) {
                c = Theme.OR;
                a = 0.9;
            } else if (survol != null && contient(survol, s.unite)) {
                c = mode == Mode.ATTAQUANT ? Theme.OR : Theme.DEGATS;
                a = 0.95;
            } else if (estCibleValide(s)) {
                c = mode == Mode.ATTAQUANT ? Theme.OR : Theme.DEGATS;
                a = 0.2 + 0.25 * pulse;
            }
            if (c != null) {
                g.setStroke(new BasicStroke(2.2f));
                g.setColor(Theme.alpha(c, a));
                g.draw(new Ellipse2D.Double(s.x - 16, s.y - 6, 32, 11));
                g.setColor(Theme.alpha(c, a * 0.25));
                g.fill(new Ellipse2D.Double(s.x - 16, s.y - 6, 32, 11));
            }
        }
        for (Bloc b : blocs) {
            if (b.groupe == survol || b.groupe == attaquantChoisi) {
                Color c = b.guilde == actif ? Theme.OR : Theme.DEGATS;
                RoundRectangle2D rr = new RoundRectangle2D.Double(b.zone.getX(), b.zone.getY(), b.zone.getWidth(),
                        b.zone.getHeight(), 18, 18);
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                        new float[] {8f, 6f}, (float) (18 - (temps * 40) % 18)));
                g.setColor(Theme.alpha(c, 0.8));
                g.draw(rr);
            }
        }
    }

    private boolean estCibleValide(Soldat s) {
        if (ecran != Ecran.JEU || ia[actif] || !s.unite.estVivant()) {
            return false;
        }
        return switch (mode) {
            case CIBLE_TOTALE, CIBLE_FRAPPE -> s.guilde != actif;
            case ATTAQUANT -> s.guilde == actif;
            default -> false;
        };
    }

    private static boolean contient(IUnite racine, IUnite cible) {
        if (racine == cible) {
            return true;
        }
        for (IUnite e : racine.getSousUnites()) {
            if (contient(e, cible)) {
                return true;
            }
        }
        return false;
    }

    private void dessinerEtiquette(Graphics2D g, Bloc b) {
        if (b.soldats.isEmpty()) {
            return;
        }
        double haut = b.soldats.stream().mapToDouble(s -> s.hy).min().orElse(b.zone.getY()) - 98;
        double cx = b.zone.getCenterX();
        boolean vivant = b.groupe.estVivant();
        Color cg = vivant ? Theme.guilde(b.guilde) : Theme.MORT;
        g.setFont(Theme.texte(Font.BOLD, 11.5f));
        FontMetrics fm = g.getFontMetrics();
        String nom = b.groupe.getNom();
        double l = Math.max(fm.stringWidth(nom) + 20, 110);
        RoundRectangle2D cadre = new RoundRectangle2D.Double(cx - l / 2, haut - 13, l, 25, 10, 10);
        g.setColor(new Color(10, 12, 24, vivant ? 190 : 120));
        g.fill(cadre);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Theme.alpha(cg, 0.8));
        g.draw(cadre);
        g.setColor(vivant ? Theme.melange(cg, Theme.TEXTE, 0.4) : Theme.MORT);
        g.drawString(nom, (float) (cx - fm.stringWidth(nom) / 2.0), (float) haut + 1);
        double ratio = b.groupe.getPointsDeVieMax() == 0 ? 0
                : pvAffiches(b.groupe) / (double) b.groupe.getPointsDeVieMax();
        double bl = l - 16;
        g.setColor(new Color(0, 0, 0, 160));
        g.fill(new RoundRectangle2D.Double(cx - bl / 2, haut + 5, bl, 4, 4, 4));
        g.setColor(Theme.vie(ratio));
        g.fill(new RoundRectangle2D.Double(cx - bl / 2, haut + 5, bl * ratio, 4, 4, 4));
    }

    private int pvAffiches(IUnite groupe) {
        int total = 0;
        for (IUnite f : ParcoursArbre.feuilles(groupe)) {
            Soldat s = soldatDe.get(f);
            if (s != null && !s.mortAffiche) {
                total += s.pvAffiche;
            }
        }
        return total;
    }

    private void petiteBarre(Graphics2D g, Soldat s) {
        double ratio = s.pvAffiche / (double) s.unite.getPointsDeVieMax();
        if (ratio >= 0.999) {
            return;
        }
        double x = s.x - 12;
        double y = s.y - 72;
        g.setColor(new Color(0, 0, 0, 170));
        g.fill(new Rectangle2D.Double(x - 1, y - 1, 26, 5));
        g.setColor(Theme.vie(ratio));
        g.fill(new Rectangle2D.Double(x, y, 24 * ratio, 3));
    }

    private void dessinerVisee(Graphics2D g) {
        if (ecran != Ecran.JEU || sourisX < 0 || (mode != Mode.CIBLE_TOTALE && mode != Mode.CIBLE_FRAPPE)) {
            return;
        }
        Point2D depart = centreDe(mode == Mode.CIBLE_FRAPPE && attaquantChoisi != null
                ? attaquantChoisi : joueurs.get(actif).getArmee());
        double dy = depart.getY() - 30;
        Color c = Theme.DEGATS;
        QuadCurve2D courbe = new QuadCurve2D.Double(depart.getX(), dy, (depart.getX() + sourisX) / 2,
                Math.min(dy, sourisY) - 90, sourisX, sourisY);
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
    }

    private void surbrillance(Graphics2D g, Rectangle2D zone, Color c) {
        RoundRectangle2D rr = new RoundRectangle2D.Double(zone.getX(), zone.getY(), zone.getWidth(), zone.getHeight(),
                12, 12);
        g.setColor(Theme.alpha(c, 0.15));
        g.fill(rr);
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(Theme.alpha(c, 0.9));
        g.draw(rr);
    }

    // ------------------------------------------------------------------
    // Interface : cartes des guildes, barre d'actions, banniere, infobulle
    // ------------------------------------------------------------------

    private void dessinerHaut(Graphics2D g) {
        for (int i = 0; i < joueurs.size(); i++) {
            Joueur j = joueurs.get(i);
            Escouade armee = j.getArmee();
            Color cg = Theme.guilde(i);
            double x = 14 + i * 420;
            double w = 412;
            double y = 6;
            double h = 58;
            cartes[i] = new Rectangle2D.Double(x, y, w, h);
            RoundRectangle2D carte = new RoundRectangle2D.Double(x, y, w, h, 16, 16);
            boolean estActif = i == actif && ecran == Ecran.JEU;
            if (estActif) {
                double pulse = 0.5 + 0.5 * Math.sin(temps * 4);
                for (int k = 4; k >= 1; k--) {
                    g.setStroke(new BasicStroke(k * 3f));
                    g.setColor(Theme.alpha(cg, 0.06 * (0.5 + pulse)));
                    g.draw(carte);
                }
            }
            g.setPaint(new GradientPaint(0, (float) y, Theme.CARTE_HAUT, 0, (float) (y + h), Theme.CARTE_BAS));
            g.fill(carte);
            g.setStroke(new BasicStroke(estActif ? 2f : 1.2f));
            g.setColor(Theme.alpha(j.estElimine() ? Theme.MORT : cg, estActif ? 1 : 0.5));
            g.draw(carte);
            if ((mode == Mode.CIBLE_TOTALE || mode == Mode.CIBLE_FRAPPE) && selectionSous(sourisX, sourisY) == armee) {
                surbrillance(g, cartes[i], Theme.DEGATS);
            }
            Icones.blason(g, i, x + 28, y + 28, 17, j.estElimine());
            g.setFont(Theme.titre(15.5f));
            g.setColor(j.estElimine() ? Theme.MORT : Theme.TEXTE);
            g.drawString(j.getFaction().toUpperCase(), (float) x + 54, (float) y + 22);
            g.setFont(Theme.texte(Font.PLAIN, 11f));
            g.setColor(Theme.TEXTE_DOUX);
            int index = i;
            long vivants = soldats.stream().filter(s -> s.guilde == index && !s.mortAffiche).count();
            g.drawString(j.getNom() + (ia[i] ? " (IA)" : "") + "  -  " + vivants + " soldats", (float) x + 54,
                    (float) y + 37);
            double bw = w - 54 - 96;
            double ratio = pvAffiches(armee) / (double) armee.getPointsDeVieMax();
            g.setColor(new Color(0, 0, 0, 160));
            g.fill(new RoundRectangle2D.Double(x + 54, y + 43, bw, 8, 8, 8));
            g.setPaint(new GradientPaint(0, (float) y + 43, Theme.melange(Theme.vie(ratio), Color.WHITE, 0.25),
                    0, (float) y + 51, Theme.vie(ratio)));
            g.fill(new RoundRectangle2D.Double(x + 54, y + 43, bw * ratio, 8, 8, 8));
            g.setFont(Theme.texte(Font.BOLD, 12f));
            g.setColor(Theme.OR);
            String atq = "ATQ " + armee.getPuissanceAttaque();
            g.drawString(atq, (float) (x + w - 12 - g.getFontMetrics().stringWidth(atq)), (float) y + 22);
            g.setColor(Theme.TEXTE);
            String pv = pvAffiches(armee) + " PV";
            g.drawString(pv, (float) (x + w - 12 - g.getFontMetrics().stringWidth(pv)), (float) y + 51);
            boolean defend = soldatsVivants(armee).stream().anyMatch(s -> s.defense > 0.5);
            if (defend) {
                g.setFont(Theme.texte(Font.BOLD, 10f));
                String t = "DEF +" + Combat.BONUS_DEFENSE;
                double tw = g.getFontMetrics().stringWidth(t) + 10;
                RoundRectangle2D p = new RoundRectangle2D.Double(x + w - 12 - tw, y + 27, tw, 13, 13, 13);
                g.setColor(new Color(120, 190, 255, 60));
                g.fill(p);
                g.setColor(new Color(160, 215, 255));
                g.draw(p);
                g.drawString(t, (float) (x + w - 7 - tw), (float) y + 37.5f);
            }
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
        Icones.blason(g, actif, 52, BARRE_Y + 44, 24, false);
        g.setFont(Theme.titre(17));
        g.setColor(Theme.guilde(actif));
        g.drawString(Decor.tronquer(g, "TOUR " + tour + " - " + j.getNom().toUpperCase(), 300), 86f,
                (float) BARRE_Y + 32);
        g.setFont(Theme.texte(Font.PLAIN, 12.5f));
        g.setColor(Theme.TEXTE_DOUX);
        g.drawString("Guilde " + j.getFaction() + (ia[actif] ? "  (IA)" : ""), 86f, (float) BARRE_Y + 51);
        g.setFont(Theme.texte(Font.ITALIC, 12.5f));
        g.setColor(Theme.OR_PALE);
        g.drawString(Decor.tronquer(g, consigne(), 310), 86f, (float) BARRE_Y + 72);

        double jx = 1022;
        g.setFont(Theme.texte(Font.BOLD, 10f));
        g.setColor(Theme.OR);
        g.drawString("JOURNAL DE BATAILLE", (float) jx, (float) BARRE_Y + 22);
        g.setFont(Theme.texte(Font.PLAIN, 11f));
        int debut = Math.max(0, journal.size() - 4);
        for (int k = debut; k < journal.size(); k++) {
            int rang = journal.size() - 1 - k;
            g.setColor(Theme.alpha(Theme.TEXTE, 1 - rang * 0.22));
            g.drawString(Decor.tronquer(g, journal.get(k), 232), (float) jx, (float) (BARRE_Y + 40 + (k - debut) * 15));
        }
    }

    private String consigne() {
        if (ia[actif]) {
            return "L'IA prepare son coup...";
        }
        return switch (mode) {
            case ACTION -> "Choisissez une action pour votre armee.";
            case CIBLE_TOTALE -> "Cliquez sur une armee ennemie.";
            case ATTAQUANT -> "Cliquez sur l'escouade qui va attaquer.";
            case CIBLE_FRAPPE -> "Cliquez sur l'escouade ennemie (ou son etendard).";
            default -> "Les armees s'affrontent...";
        };
    }

    private void dessinerBoutons(Graphics2D g) {
        for (Bouton b : boutons) {
            Rectangle2D z = b.zone;
            boolean survol = b.actif && z.contains(sourisX, sourisY);
            RoundRectangle2D rr = new RoundRectangle2D.Double(z.getX(), z.getY(), z.getWidth(), z.getHeight(), 16, 16);
            double cy = z.getCenterY();
            if (!b.actif) {
                g.setColor(new Color(20, 22, 36, 210));
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
            double tx = z.getX() + (compact ? 40 : 52);
            Icones.bouton(g, b.icone, z.getX() + (compact ? 21 : 28), cy, compact ? 9.5 : 11, texte);
            float taille = compact ? 13f : z.getHeight() > 50 ? 15.5f : 14f;
            double place = z.getMaxX() - tx - 8;
            while (taille > 9 && g.getFontMetrics(Theme.titre(taille)).stringWidth(b.libelle) > place) {
                taille -= 0.5f;
            }
            g.setFont(Theme.titre(taille));
            g.setColor(texte);
            boolean avecSous = !b.sousTitre.isEmpty();
            g.drawString(b.libelle, (float) tx, (float) (cy + (avecSous ? -2 : 5)));
            if (avecSous) {
                g.setFont(Theme.texte(Font.PLAIN, 10.5f));
                g.setColor(b.selectionne ? new Color(0x5A3A10) : Theme.alpha(Theme.TEXTE_DOUX, b.actif ? 1 : 0.5));
                g.drawString(Decor.tronquer(g, b.sousTitre, place), (float) tx, (float) (cy + 15));
            }
        }
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
                new Color[] {new Color(0, 0, 0, 0), new Color(6, 4, 14, 215), new Color(6, 4, 14, 215),
                    new Color(0, 0, 0, 0)}));
        g.fillRect(0, (int) y, LARGEUR, 110);
        g.setPaint(new LinearGradientPaint(0, 0, LARGEUR, 0, new float[] {0f, 0.5f, 1f},
                new Color[] {Theme.alpha(banniereCouleur, 0), banniereCouleur, Theme.alpha(banniereCouleur, 0)}));
        g.fillRect(0, (int) y, LARGEUR, 2);
        g.fillRect(0, (int) y + 108, LARGEUR, 2);
        Decor.texteContour(g, banniereTitre, Theme.titre(46), banniereCouleur, LARGEUR / 2.0 - glisse, y + 64);
        Decor.texteCentre(g, banniereSous, Theme.texte(Font.PLAIN, 16), Theme.TEXTE, LARGEUR / 2.0 + glisse, y + 92);
        g.dispose();
    }

    private void dessinerInfobulle(Graphics2D g) {
        if (mode == Mode.ATTENTE || temps - banniereDebut < 1.8 || sourisY > BARRE_Y || sourisY < 70) {
            return;
        }
        List<String> lignes = new ArrayList<>();
        int guilde = 0;
        Soldat sous = null;
        for (Soldat s : soldats) {
            if (!s.mortAffiche && s.contient(sourisX, sourisY)) {
                sous = s;
            }
        }
        if (sous != null) {
            IUnite u = sous.unite;
            guilde = sous.guilde;
            String type = u instanceof UniteElementaire ue ? ue.getType() : "Unite";
            lignes.add(type.toUpperCase() + "  -  " + u.getNom());
            lignes.add("PV " + u.getPointsDeVie() + " / " + u.getPointsDeVieMax() + "   ATQ " + u.getPuissanceAttaque());
            String arme = u instanceof Guerrier ? "Epee : court frapper au corps a corps"
                    : u instanceof Mage ? "Magie : lance des orbes a distance"
                    : u instanceof Soigneur ? "Soin : +40 PV repartis sur son escouade"
                    : "Arc : tire des fleches depuis sa place";
            lignes.add(arme);
            if (u instanceof UniteElementaire ue && ue.getBonusDefense() > 0) {
                lignes.add("En defense : armure +" + ue.getBonusDefense());
            }
        } else {
            for (Bloc b : blocs) {
                if (b.zone.contains(sourisX, sourisY)) {
                    IUnite u = b.groupe;
                    guilde = b.guilde;
                    long viv = u.getSousUnites().stream().filter(IUnite::estVivant).count();
                    lignes.add("ESCOUADE  -  " + u.getNom());
                    lignes.add("PV " + u.getPointsDeVie() + " / " + u.getPointsDeVieMax());
                    lignes.add("ATQ " + u.getPuissanceAttaque() + (viv >= 2 ? "  (synergie +10%)" : ""));
                    lignes.add("Composite : " + viv + " / " + u.getSousUnites().size() + " soldats vivants");
                }
            }
        }
        if (lignes.isEmpty()) {
            return;
        }
        Font titre = Theme.texte(Font.BOLD, 12.5f);
        Font corps = Theme.texte(Font.PLAIN, 12f);
        double largeur = 0;
        for (String l : lignes) {
            largeur = Math.max(largeur, g.getFontMetrics(titre).stringWidth(l));
        }
        largeur += 26;
        double hauteur = lignes.size() * 17 + 14;
        double x = Math.min(sourisX + 18, LARGEUR - largeur - 8);
        double y = Math.min(sourisY + 18, BARRE_Y - hauteur - 8);
        RoundRectangle2D cadre = new RoundRectangle2D.Double(x, y, largeur, hauteur, 12, 12);
        g.setColor(new Color(8, 8, 20, 235));
        g.fill(cadre);
        g.setColor(Theme.alpha(Theme.guilde(guilde), 0.9));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(cadre);
        for (int k = 0; k < lignes.size(); k++) {
            g.setFont(k == 0 ? titre : corps);
            g.setColor(k == 0 ? Theme.OR_PALE : Theme.TEXTE);
            g.drawString(lignes.get(k), (float) x + 13, (float) (y + 22 + k * 17));
        }
    }

    // ------------------------------------------------------------------
    // Victoire
    // ------------------------------------------------------------------

    private void dessinerVictoire(Graphics2D g) {
        g.setColor(new Color(4, 3, 12, 150));
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
            r.setPaint(new GradientPaint(0, 0, Theme.alpha(k % 2 == 0 ? Theme.OR : cg, 0.14), 0, -600,
                    Theme.alpha(cg, 0)));
            r.fill(rayon);
        }
        r.dispose();
        effets.dessiner(g);
        Joueur vainqueur = joueurs.get(indexVainqueur);
        Icones.blason(g, indexVainqueur, LARGEUR / 2.0, 238 + Math.sin(temps * 2) * 6, 78, false);
        Decor.texteLogo(g, "VICTOIRE", LARGEUR / 2.0, 450, 88);
        Decor.texteCentre(g, vainqueur.getNom() + " et la Guilde " + vainqueur.getFaction()
                + " remportent le Clash !", Theme.titre(22), Theme.TEXTE, LARGEUR / 2.0, 500);
        long survivants = soldats.stream().filter(s -> s.guilde == indexVainqueur && !s.mortAffiche).count();
        long total = soldats.stream().filter(s -> s.guilde == indexVainqueur).count();
        Decor.texteCentre(g, "Triomphe en " + tour + " tours   -   PV restants " + vainqueur.getArmee().getPointsDeVie()
                        + " / " + vainqueur.getArmee().getPointsDeVieMax() + "   -   Survivants " + survivants + " / "
                        + total, Theme.texte(Font.PLAIN, 15), Theme.TEXTE_DOUX, LARGEUR / 2.0, 535);
        dessinerBoutons(g);
    }
}
