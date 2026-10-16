package com.mycompany.composite_clash_of_guilds.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Moteur d'effets visuels : particules, projectiles, ondes de choc,
 * nombres de degats flottants et secousse de l'ecran.
 */
final class Effets {

    /** Apparence d'un projectile selon le type d'unite qui tire. */
    enum Style { ORBE, FLECHE, LAME, SOIN }

    private static final class Particule {
        double x, y, vx, vy, vie, vieMax, taille, gravite, frottement;
        Color couleur;
        boolean croix;
    }

    private static final class Projectile {
        double x0, y0, x1, y1, x, y, angle, duree, delai, t, arc;
        Color couleur;
        Style style;
        Runnable impact;
    }

    private static final class TexteFlottant {
        String texte;
        double x, y, vy, age, vieMax;
        Color couleur;
        float taille;
    }

    private static final class Onde {
        double x, y, rayonMax, age, vieMax;
        Color couleur;
    }

    final Random alea = new Random();
    private final List<Particule> particules = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<TexteFlottant> textes = new ArrayList<>();
    private final List<Onde> ondes = new ArrayList<>();
    private double secousse;

    // ------------------------------------------------------------------
    // Creation d'effets
    // ------------------------------------------------------------------

    void secouer(double force) {
        secousse = Math.max(secousse, force);
    }

    double[] decalage() {
        if (secousse <= 0) {
            return new double[] {0, 0};
        }
        return new double[] {(alea.nextDouble() * 2 - 1) * secousse, (alea.nextDouble() * 2 - 1) * secousse};
    }

    void explosion(double x, double y, Color c, int nombre, double vitesse) {
        for (int i = 0; i < nombre; i++) {
            Particule p = new Particule();
            double a = alea.nextDouble() * Math.PI * 2;
            double v = vitesse * (0.25 + alea.nextDouble() * 0.75);
            p.x = x;
            p.y = y;
            p.vx = Math.cos(a) * v;
            p.vy = Math.sin(a) * v - vitesse * 0.15;
            p.vieMax = p.vie = 0.45 + alea.nextDouble() * 0.6;
            p.taille = 1.8 + alea.nextDouble() * 3.4;
            p.gravite = 320;
            p.frottement = 2.4;
            p.couleur = alea.nextDouble() < 0.3 ? Theme.melange(c, Color.WHITE, 0.6) : c;
            particules.add(p);
        }
    }

    void soin(double x, double y) {
        for (int i = 0; i < 16; i++) {
            Particule p = new Particule();
            p.x = x + (alea.nextDouble() * 2 - 1) * 16;
            p.y = y + (alea.nextDouble() * 2 - 1) * 8;
            p.vx = (alea.nextDouble() * 2 - 1) * 18;
            p.vy = -50 - alea.nextDouble() * 80;
            p.vieMax = p.vie = 0.7 + alea.nextDouble() * 0.6;
            p.taille = 2.5 + alea.nextDouble() * 3;
            p.gravite = -15;
            p.frottement = 0.8;
            p.couleur = alea.nextBoolean() ? Theme.SOIN : Theme.melange(Theme.SOIN, Color.WHITE, 0.5);
            p.croix = alea.nextDouble() < 0.45;
            particules.add(p);
        }
    }

    void onde(double x, double y, Color c, double rayonMax) {
        Onde o = new Onde();
        o.x = x;
        o.y = y;
        o.couleur = c;
        o.rayonMax = rayonMax;
        o.vieMax = 0.6;
        ondes.add(o);
    }

    void texte(String texte, double x, double y, Color c, float taille) {
        TexteFlottant t = new TexteFlottant();
        t.texte = texte;
        t.x = Math.max(70, Math.min(PanneauJeu.LARGEUR - 70, x));
        t.y = Math.max(40, y);
        t.vy = -70;
        t.vieMax = 1.3;
        t.couleur = c;
        t.taille = taille;
        textes.add(t);
    }

    void tirer(Point2D depart, Point2D arrivee, Color c, Style style, double delai, double duree,
               double arc, Runnable impact) {
        Projectile p = new Projectile();
        p.x0 = p.x = depart.getX();
        p.y0 = p.y = depart.getY();
        p.x1 = arrivee.getX();
        p.y1 = arrivee.getY();
        p.angle = Math.atan2(p.y1 - p.y0, p.x1 - p.x0);
        p.couleur = c;
        p.style = style;
        p.delai = delai;
        p.duree = duree;
        p.arc = arc;
        p.impact = impact;
        projectiles.add(p);
    }

    boolean estCalme() {
        return projectiles.isEmpty();
    }

    // ------------------------------------------------------------------
    // Mise a jour
    // ------------------------------------------------------------------

    void avancer(double dt) {
        List<Runnable> impacts = new ArrayList<>();
        for (Iterator<Projectile> it = projectiles.iterator(); it.hasNext();) {
            Projectile p = it.next();
            if (p.delai > 0) {
                p.delai -= dt;
                continue;
            }
            p.t += dt / p.duree;
            double t = Math.min(1, p.t);
            double nx = p.x0 + (p.x1 - p.x0) * t;
            double ny = p.y0 + (p.y1 - p.y0) * t - Math.sin(Math.PI * t) * p.arc;
            if (nx != p.x || ny != p.y) {
                p.angle = Math.atan2(ny - p.y, nx - p.x);
            }
            p.x = nx;
            p.y = ny;
            trainee(p);
            if (p.t >= 1) {
                it.remove();
                if (p.impact != null) {
                    impacts.add(p.impact);
                }
            }
        }
        impacts.forEach(Runnable::run);

        for (Iterator<Particule> it = particules.iterator(); it.hasNext();) {
            Particule p = it.next();
            p.vie -= dt;
            if (p.vie <= 0) {
                it.remove();
                continue;
            }
            double f = Math.max(0, 1 - p.frottement * dt);
            p.vx *= f;
            p.vy = p.vy * f + p.gravite * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }
        for (Iterator<TexteFlottant> it = textes.iterator(); it.hasNext();) {
            TexteFlottant t = it.next();
            t.age += dt;
            t.y += t.vy * dt;
            t.vy *= Math.max(0, 1 - 2.5 * dt);
            if (t.age >= t.vieMax) {
                it.remove();
            }
        }
        for (Iterator<Onde> it = ondes.iterator(); it.hasNext();) {
            Onde o = it.next();
            o.age += dt;
            if (o.age >= o.vieMax) {
                it.remove();
            }
        }
        secousse = Math.max(0, secousse - dt * 40);
    }

    private void trainee(Projectile p) {
        int n = p.style == Style.FLECHE ? 1 : 2;
        for (int i = 0; i < n; i++) {
            Particule q = new Particule();
            q.x = p.x + (alea.nextDouble() * 2 - 1) * 3;
            q.y = p.y + (alea.nextDouble() * 2 - 1) * 3;
            q.vx = (alea.nextDouble() * 2 - 1) * 20;
            q.vy = (alea.nextDouble() * 2 - 1) * 20;
            q.vieMax = q.vie = 0.25 + alea.nextDouble() * 0.25;
            q.taille = p.style == Style.FLECHE ? 1.5 : 2 + alea.nextDouble() * 2.5;
            q.frottement = 3;
            q.couleur = p.couleur;
            q.croix = p.style == Style.SOIN && alea.nextDouble() < 0.3;
            particules.add(q);
        }
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    void dessiner(Graphics2D g) {
        for (Onde o : ondes) {
            double k = o.age / o.vieMax;
            double r = o.rayonMax * (1 - Math.pow(1 - k, 3));
            g.setStroke(new BasicStroke((float) (10 * (1 - k) + 1)));
            g.setColor(Theme.alpha(o.couleur, 0.7 * (1 - k)));
            g.draw(new Ellipse2D.Double(o.x - r, o.y - r, r * 2, r * 2));
        }
        for (Particule p : particules) {
            double a = Math.min(1, p.vie / p.vieMax * 1.4);
            double s = p.taille;
            if (p.croix) {
                g.setColor(Theme.alpha(p.couleur, a));
                g.fill(new Rectangle2D.Double(p.x - s * 1.2, p.y - s * 0.35, s * 2.4, s * 0.7));
                g.fill(new Rectangle2D.Double(p.x - s * 0.35, p.y - s * 1.2, s * 0.7, s * 2.4));
                continue;
            }
            g.setColor(Theme.alpha(p.couleur, a * 0.22));
            g.fill(new Ellipse2D.Double(p.x - s * 2.4, p.y - s * 2.4, s * 4.8, s * 4.8));
            g.setColor(Theme.alpha(p.couleur, a));
            g.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
        }
        for (Projectile p : projectiles) {
            if (p.delai <= 0) {
                dessinerProjectile(g, p);
            }
        }
        for (TexteFlottant t : textes) {
            dessinerTexte(g, t);
        }
    }

    private void dessinerProjectile(Graphics2D g0, Projectile p) {
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(p.x, p.y);
        double halo = p.style == Style.FLECHE ? 12 : 20;
        g.setPaint(new RadialGradientPaint(0, 0, (float) halo, new float[] {0f, 1f},
                new Color[] {Theme.alpha(p.couleur, 0.55), Theme.alpha(p.couleur, 0)}));
        g.fill(new Ellipse2D.Double(-halo, -halo, halo * 2, halo * 2));
        g.rotate(p.angle);
        switch (p.style) {
            case FLECHE -> {
                g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Theme.alpha(p.couleur, 0.35));
                g.draw(new Line2D.Double(-26, 0, 0, 0));
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Theme.melange(p.couleur, Color.WHITE, 0.4));
                g.draw(new Line2D.Double(-24, 0, 2, 0));
                g.setColor(Color.WHITE);
                g.fill(new java.awt.Polygon(new int[] {8, -2, -2}, new int[] {0, -5, 5}, 3));
            }
            case LAME -> {
                g.rotate(p.t * 14);
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Theme.melange(p.couleur, Color.WHITE, 0.35));
                g.draw(new Arc2D.Double(-11, -11, 22, 22, 20, 220, Arc2D.OPEN));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(-3, -3, 6, 6));
            }
            case SOIN -> {
                g.setColor(Color.WHITE);
                g.fill(new Rectangle2D.Double(-6, -2, 12, 4));
                g.fill(new Rectangle2D.Double(-2, -6, 4, 12));
            }
            default -> {
                g.setColor(Theme.melange(p.couleur, Color.WHITE, 0.3));
                g.fill(new Ellipse2D.Double(-7, -7, 14, 14));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(-3.5, -3.5, 7, 7));
            }
        }
        g.dispose();
    }

    private void dessinerTexte(Graphics2D g, TexteFlottant t) {
        double pop = 1 + 0.7 * Math.max(0, 1 - t.age / 0.18);
        double a = Math.min(1, (t.vieMax - t.age) / 0.45);
        Font f = Theme.titre((float) (t.taille * pop));
        GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), t.texte);
        Rectangle2D b = gv.getVisualBounds();
        Shape s = AffineTransform.getTranslateInstance(t.x - b.getWidth() / 2 - b.getX(), t.y)
                .createTransformedShape(gv.getOutline());
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(10, 6, 16, (int) (220 * a)));
        g.draw(s);
        g.setColor(Theme.alpha(t.couleur, a));
        g.fill(s);
    }
}
