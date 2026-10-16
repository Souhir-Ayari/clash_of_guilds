package com.mycompany.composite_clash_of_guilds.gui;

import com.mycompany.composite_clash_of_guilds.Archer;
import com.mycompany.composite_clash_of_guilds.Guerrier;
import com.mycompany.composite_clash_of_guilds.IUnite;
import com.mycompany.composite_clash_of_guilds.Mage;
import com.mycompany.composite_clash_of_guilds.Soigneur;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Icones vectorielles dessinees en code (aucune image externe) :
 * unites, blasons des guildes et pictogrammes des boutons.
 * Toutes les formes sont definies dans un repere unitaire [-1, 1].
 */
final class Icones {

    static final int EPEE = 0, CIBLE = 1, PLUS = 2, EPEES = 3, COURONNE = 4, OEIL = 5, QUITTER = 6, RETOUR = 7, BOUCLIER = 8;

    private Icones() {
    }

    private static Graphics2D repere(Graphics2D g0, double cx, double cy, double s) {
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(cx, cy);
        g.scale(s, s);
        return g;
    }

    private static BasicStroke trait(double largeur) {
        return new BasicStroke((float) largeur, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    // ------------------------------------------------------------------
    // Unites
    // ------------------------------------------------------------------

    static void unite(Graphics2D g0, IUnite u, double cx, double cy, double s, boolean vivant) {
        Graphics2D g = repere(g0, cx, cy, s);
        Color clair = vivant ? new Color(255, 250, 238) : new Color(150, 152, 165);
        if (u instanceof Guerrier) {
            guerrier(g, clair);
        } else if (u instanceof Archer) {
            archer(g, clair);
        } else if (u instanceof Mage) {
            mage(g, clair, vivant);
        } else if (u instanceof Soigneur) {
            plus(g, clair);
        } else {
            drapeau(g, clair);
        }
        g.dispose();
    }

    private static void guerrier(Graphics2D g, Color clair) {
        Path2D b = new Path2D.Double();
        b.moveTo(-0.8, -0.5);
        b.lineTo(0.05, -0.5);
        b.lineTo(0.05, 0.1);
        b.quadTo(0.05, 0.6, -0.375, 0.85);
        b.quadTo(-0.8, 0.6, -0.8, 0.1);
        b.closePath();
        g.setColor(Theme.alpha(clair, 0.3));
        g.fill(b);
        g.setStroke(trait(0.1));
        g.setColor(clair);
        g.draw(b);

        Graphics2D e = (Graphics2D) g.create();
        e.translate(0.15, -0.05);
        e.rotate(Math.toRadians(35));
        Path2D lame = new Path2D.Double();
        lame.moveTo(0, -1.0);
        lame.lineTo(0.12, -0.8);
        lame.lineTo(0.12, 0.35);
        lame.lineTo(-0.12, 0.35);
        lame.lineTo(-0.12, -0.8);
        lame.closePath();
        e.setColor(clair);
        e.fill(lame);
        e.fill(new RoundRectangle2D.Double(-0.4, 0.33, 0.8, 0.13, 0.1, 0.1));
        e.fill(new Rectangle2D.Double(-0.065, 0.45, 0.13, 0.35));
        e.fill(new Ellipse2D.Double(-0.11, 0.76, 0.22, 0.22));
        e.dispose();
    }

    private static void archer(Graphics2D g, Color clair) {
        g.setColor(clair);
        g.setStroke(trait(0.13));
        g.draw(new Arc2D.Double(-1.05, -0.85, 1.7, 1.7, -62, 124, Arc2D.OPEN));
        double ex = -0.2 + 0.85 * Math.cos(Math.toRadians(62));
        double ey = 0.85 * Math.sin(Math.toRadians(62));
        g.setStroke(trait(0.05));
        g.draw(new Line2D.Double(ex, -ey, -0.5, 0));
        g.draw(new Line2D.Double(ex, ey, -0.5, 0));
        g.setStroke(trait(0.09));
        g.draw(new Line2D.Double(-0.75, 0, 0.75, 0));
        Path2D pointe = new Path2D.Double();
        pointe.moveTo(0.95, 0);
        pointe.lineTo(0.68, -0.16);
        pointe.lineTo(0.68, 0.16);
        pointe.closePath();
        g.fill(pointe);
        g.setStroke(trait(0.07));
        g.draw(new Line2D.Double(-0.75, 0, -0.92, -0.15));
        g.draw(new Line2D.Double(-0.75, 0, -0.92, 0.15));
    }

    private static void mage(Graphics2D g, Color clair, boolean vivant) {
        g.setColor(clair);
        g.setStroke(trait(0.13));
        g.draw(new Line2D.Double(-0.55, 0.9, 0.2, -0.25));
        if (vivant) {
            g.setPaint(new RadialGradientPaint(0.32f, -0.45f, 0.65f, new float[] {0f, 1f},
                    new Color[] {new Color(220, 210, 255, 170), new Color(140, 120, 255, 0)}));
            g.fill(new Ellipse2D.Double(-0.33, -1.1, 1.3, 1.3));
        }
        g.setPaint(new RadialGradientPaint(0.26f, -0.52f, 0.32f, new float[] {0f, 1f},
                new Color[] {Color.WHITE, vivant ? new Color(170, 150, 255) : clair}));
        g.fill(new Ellipse2D.Double(0.06, -0.73, 0.52, 0.52));
        g.setColor(clair);
        etoile(g, -0.45, -0.5, 0.18);
        etoile(g, 0.7, 0.2, 0.13);
    }

    private static void etoile(Graphics2D g, double x, double y, double r) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, y - r);
        p.quadTo(x, y, x + r, y);
        p.quadTo(x, y, x, y + r);
        p.quadTo(x, y, x - r, y);
        p.quadTo(x, y, x, y - r);
        g.fill(p);
    }

    private static void plus(Graphics2D g, Color clair) {
        g.setColor(clair);
        g.fill(new RoundRectangle2D.Double(-0.2, -0.75, 0.4, 1.5, 0.15, 0.15));
        g.fill(new RoundRectangle2D.Double(-0.75, -0.2, 1.5, 0.4, 0.15, 0.15));
    }

    private static void drapeau(Graphics2D g, Color c) {
        g.setStroke(trait(0.12));
        g.setColor(Theme.melange(c, Color.WHITE, 0.4));
        g.draw(new Line2D.Double(-0.6, -0.9, -0.6, 0.95));
        Path2D f = new Path2D.Double();
        f.moveTo(-0.55, -0.85);
        f.lineTo(0.8, -0.85);
        f.lineTo(0.45, -0.47);
        f.lineTo(0.8, -0.1);
        f.lineTo(-0.55, -0.1);
        f.closePath();
        g.setPaint(new GradientPaint(0f, -0.85f, Theme.melange(c, Color.WHITE, 0.25), 0f, -0.1f, c.darker()));
        g.fill(f);
        g.setColor(Theme.alpha(Color.WHITE, 0.5));
        g.setStroke(trait(0.05));
        g.draw(f);
    }

    /** Icone de groupe (Composite) : un etendard aux couleurs de la guilde. */
    static void groupe(Graphics2D g0, double cx, double cy, double s, Color c) {
        Graphics2D g = repere(g0, cx, cy, s);
        drapeau(g, c);
        g.dispose();
    }

    // ------------------------------------------------------------------
    // Blasons
    // ------------------------------------------------------------------

    static void blason(Graphics2D g0, int guilde, double cx, double cy, double s, boolean eteint) {
        Graphics2D g = repere(g0, cx, cy, s);
        Color c = eteint ? Theme.MORT : Theme.guilde(guilde);
        Path2D bouclier = new Path2D.Double();
        bouclier.moveTo(-0.9, -1);
        bouclier.quadTo(0, -0.8, 0.9, -1);
        bouclier.lineTo(0.9, 0.05);
        bouclier.curveTo(0.9, 0.65, 0.35, 0.95, 0, 1.15);
        bouclier.curveTo(-0.35, 0.95, -0.9, 0.65, -0.9, 0.05);
        bouclier.closePath();

        g.setPaint(new RadialGradientPaint(0f, 0.1f, 1.6f, new float[] {0f, 1f},
                new Color[] {Theme.alpha(c, eteint ? 0 : 0.45), Theme.alpha(c, 0)}));
        g.fill(new Ellipse2D.Double(-1.6, -1.5, 3.2, 3.2));
        g.setPaint(new GradientPaint(0f, -1f, Theme.melange(c, Color.WHITE, 0.1), 0f, 1.15f,
                Theme.melange(c, Color.BLACK, 0.7)));
        g.fill(bouclier);
        g.setStroke(trait(0.11));
        g.setColor(eteint ? Theme.MORT : Theme.OR);
        g.draw(bouclier);
        Graphics2D interieur = (Graphics2D) g.create();
        interieur.scale(0.8, 0.8);
        interieur.setStroke(trait(0.05));
        interieur.setColor(Theme.alpha(Theme.OR_PALE, 0.5));
        interieur.draw(bouclier);
        interieur.dispose();

        g.setColor(eteint ? new Color(180, 180, 190) : Theme.OR_PALE);
        switch (guilde) {
            case 0 -> {
                Path2D couronne = new Path2D.Double();
                couronne.moveTo(-0.5, 0.25);
                couronne.lineTo(-0.55, -0.35);
                couronne.lineTo(-0.25, -0.05);
                couronne.lineTo(0, -0.5);
                couronne.lineTo(0.25, -0.05);
                couronne.lineTo(0.55, -0.35);
                couronne.lineTo(0.5, 0.25);
                couronne.closePath();
                g.fill(couronne);
                g.fill(new RoundRectangle2D.Double(-0.52, 0.33, 1.04, 0.16, 0.08, 0.08));
                g.fill(new Ellipse2D.Double(-0.64, -0.47, 0.18, 0.18));
                g.fill(new Ellipse2D.Double(-0.09, -0.63, 0.18, 0.18));
                g.fill(new Ellipse2D.Double(0.46, -0.47, 0.18, 0.18));
            }
            case 1 -> {
                Area lune = new Area(new Ellipse2D.Double(-0.55, -0.55, 1.1, 1.1));
                lune.subtract(new Area(new Ellipse2D.Double(-0.28, -0.7, 1.0, 1.0)));
                g.fill(lune);
                etoile(g, 0.3, 0.22, 0.2);
                etoile(g, 0.5, -0.25, 0.12);
            }
            default -> {
                g.setStroke(trait(0.1));
                Path2D losange = new Path2D.Double();
                losange.moveTo(0, -0.62);
                losange.lineTo(0.42, 0);
                losange.lineTo(0, 0.62);
                losange.lineTo(-0.42, 0);
                losange.closePath();
                g.draw(losange);
                g.draw(new Line2D.Double(0, -0.62, 0, 0.62));
                g.draw(new Line2D.Double(-0.22, -0.3, 0.22, 0.05));
                g.fill(new Ellipse2D.Double(-0.1, 0.12, 0.2, 0.2));
            }
        }
        g.dispose();
    }

    // ------------------------------------------------------------------
    // Pictogrammes des boutons
    // ------------------------------------------------------------------

    static void bouton(Graphics2D g0, int type, double cx, double cy, double s, Color c) {
        Graphics2D g = repere(g0, cx, cy, s);
        g.setColor(c);
        g.setStroke(trait(0.13));
        switch (type) {
            case EPEE -> {
                g.rotate(Math.toRadians(45));
                epeeSimple(g);
            }
            case EPEES -> {
                Graphics2D a = (Graphics2D) g.create();
                a.rotate(Math.toRadians(45));
                epeeSimple(a);
                a.dispose();
                g.rotate(Math.toRadians(-45));
                epeeSimple(g);
            }
            case CIBLE -> {
                g.draw(new Ellipse2D.Double(-0.7, -0.7, 1.4, 1.4));
                g.draw(new Ellipse2D.Double(-0.3, -0.3, 0.6, 0.6));
                g.draw(new Line2D.Double(0, -1, 0, -0.5));
                g.draw(new Line2D.Double(0, 1, 0, 0.5));
                g.draw(new Line2D.Double(-1, 0, -0.5, 0));
                g.draw(new Line2D.Double(1, 0, 0.5, 0));
            }
            case PLUS -> plus(g, c);
            case COURONNE -> {
                Path2D p = new Path2D.Double();
                p.moveTo(-0.8, 0.5);
                p.lineTo(-0.85, -0.5);
                p.lineTo(-0.4, -0.05);
                p.lineTo(0, -0.7);
                p.lineTo(0.4, -0.05);
                p.lineTo(0.85, -0.5);
                p.lineTo(0.8, 0.5);
                p.closePath();
                g.fill(p);
            }
            case OEIL -> {
                Path2D p = new Path2D.Double();
                p.moveTo(-0.95, 0);
                p.quadTo(0, -0.85, 0.95, 0);
                p.quadTo(0, 0.85, -0.95, 0);
                g.draw(p);
                g.fill(new Ellipse2D.Double(-0.3, -0.3, 0.6, 0.6));
            }
            case BOUCLIER -> {
                Path2D b = new Path2D.Double();
                b.moveTo(-0.75, -0.8);
                b.quadTo(0, -0.6, 0.75, -0.8);
                b.lineTo(0.75, 0);
                b.quadTo(0.7, 0.6, 0, 0.95);
                b.quadTo(-0.7, 0.6, -0.75, 0);
                b.closePath();
                g.draw(b);
                g.draw(new Line2D.Double(0, -0.6, 0, 0.8));
            }
            case RETOUR -> {
                g.draw(new Arc2D.Double(-0.7, -0.7, 1.4, 1.4, 90, 270, Arc2D.OPEN));
                Path2D p = new Path2D.Double();
                p.moveTo(0, -1.0);
                p.lineTo(0.35, -0.7);
                p.lineTo(0, -0.4);
                p.closePath();
                g.fill(p);
            }
            default -> {
                g.draw(new Line2D.Double(-0.6, -0.6, 0.6, 0.6));
                g.draw(new Line2D.Double(-0.6, 0.6, 0.6, -0.6));
            }
        }
        g.dispose();
    }

    private static void epeeSimple(Graphics2D g) {
        g.fill(new Rectangle2D.Double(-0.09, -1.0, 0.18, 1.25));
        g.fill(new RoundRectangle2D.Double(-0.38, 0.2, 0.76, 0.14, 0.1, 0.1));
        g.fill(new Rectangle2D.Double(-0.06, 0.3, 0.12, 0.45));
        g.fill(new Ellipse2D.Double(-0.12, 0.72, 0.24, 0.24));
    }
}
