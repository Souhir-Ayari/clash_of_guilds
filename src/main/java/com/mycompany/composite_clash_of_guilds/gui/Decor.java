package com.mycompany.composite_clash_of_guilds.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Decors precalcules (ciel du menu, champ de bataille) et outils de texte. */
final class Decor {

    static final int L = PanneauJeu.LARGEUR;
    static final int H = PanneauJeu.HAUTEUR;

    private Decor() {
    }

    static void qualite(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    // ------------------------------------------------------------------
    // Ciel nocturne de l'ecran titre
    // ------------------------------------------------------------------

    static BufferedImage ciel() {
        BufferedImage img = new BufferedImage(L, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        qualite(g);
        g.setPaint(new LinearGradientPaint(0, 0, 0, H, new float[] {0f, 0.55f, 0.8f, 1f},
                new Color[] {new Color(0x060A1C), new Color(0x1A1238), new Color(0x2B1530), new Color(0x0E0812)}));
        g.fillRect(0, 0, L, H);
        g.setPaint(new RadialGradientPaint(1040, 130, 300, new float[] {0f, 1f},
                new Color[] {new Color(255, 214, 150, 70), new Color(255, 214, 150, 0)}));
        g.fillRect(0, 0, L, H);
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
        g.setPaint(new GradientPaint(0, 520, new Color(140, 100, 200, 0), 0, H, new Color(140, 100, 200, 40)));
        g.fillRect(0, 520, L, 200);
        vignette(g, 190);
        g.dispose();
        return img;
    }

    private static Shape montagnes(double base, double a1, double f1, double a2, double f2, double phase) {
        Path2D p = new Path2D.Double();
        p.moveTo(0, H);
        for (int x = 0; x <= L; x += 8) {
            p.lineTo(x, base + a1 * Math.sin(x * f1 + phase) + a2 * Math.sin(x * f2 + phase * 2)
                    + 8 * Math.sin(x * 0.07 + phase));
        }
        p.lineTo(L, H);
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

    private static void vignette(Graphics2D g, int force) {
        g.setPaint(new RadialGradientPaint(L / 2f, H / 2f, 820, new float[] {0.5f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, force)}));
        g.fillRect(0, 0, L, H);
    }

    // ------------------------------------------------------------------
    // Champ de bataille
    // ------------------------------------------------------------------

    static BufferedImage champ(Rectangle2D[] regions) {
        BufferedImage img = new BufferedImage(L, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        qualite(g);
        Random r = new Random(11);
        g.setPaint(new LinearGradientPaint(0, 0, 0, H, new float[] {0f, 0.4f, 0.75f, 1f},
                new Color[] {new Color(0x1E2C1C), new Color(0x2A3B24), new Color(0x22321D), new Color(0x141D10)}));
        g.fillRect(0, 0, L, H);

        for (int i = 0; i < 7000; i++) {
            double x = r.nextDouble() * L;
            double y = r.nextDouble() * H;
            g.setColor(r.nextBoolean() ? new Color(90, 120, 70, 35) : new Color(8, 14, 6, 45));
            g.fill(new Rectangle2D.Double(x, y, 2 + r.nextDouble() * 3, 2 + r.nextDouble() * 2));
        }
        for (int i = 0; i < 22; i++) {
            double x = r.nextDouble() * L;
            double y = 80 + r.nextDouble() * 540;
            g.setColor(new Color(60, 85, 45, 40));
            g.fill(new Ellipse2D.Double(x - 80, y - 30, 160 + r.nextDouble() * 120, 60 + r.nextDouble() * 40));
        }

        double cx = 640;
        double cy = 410;
        double[][] departs = new double[regions.length][];
        for (int i = 0; i < regions.length; i++) {
            departs[i] = new double[] {regions[i].getCenterX(), regions[i].getCenterY()};
        }
        for (int passe = 0; passe < 2; passe++) {
            g.setStroke(new BasicStroke(passe == 0 ? 70 : 38, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(passe == 0 ? new Color(84, 66, 42, 70) : new Color(122, 98, 64, 70));
            for (double[] d : departs) {
                double kx = (d[0] + cx) / 2;
                double ky = (d[1] + cy) / 2 + (d[1] < cy ? 0 : 40);
                g.draw(new QuadCurve2D.Double(d[0], d[1], kx, ky, cx, cy));
            }
        }
        g.setPaint(new RadialGradientPaint((float) cx, (float) cy, 140, new float[] {0f, 1f},
                new Color[] {new Color(140, 112, 72, 110), new Color(140, 112, 72, 0)}));
        g.fill(new Ellipse2D.Double(cx - 170, cy - 100, 340, 200));
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16;
            pierre(g, cx + Math.cos(a) * 138, cy + Math.sin(a) * 78, 9 + r.nextDouble() * 5);
        }
        for (int i = 0; i < 40; i++) {
            double x = 20 + r.nextDouble() * (L - 40);
            double y = 90 + r.nextDouble() * 520;
            if (dansUneRegion(regions, x, y) || Math.hypot(x - cx, (y - cy) * 1.6) < 200) {
                continue;
            }
            pierre(g, x, y, 4 + r.nextDouble() * 9);
        }
        for (int i = 0; i < 900; i++) {
            double x = r.nextDouble() * L;
            double y = 80 + r.nextDouble() * 560;
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(r.nextBoolean() ? new Color(98, 140, 70, 120) : new Color(60, 96, 44, 140));
            for (int k = -1; k <= 1; k++) {
                g.draw(new Line2D.Double(x + k * 1.6, y, x + k * 3.2, y - 4 - r.nextDouble() * 4));
            }
        }
        for (double[] b : new double[][] {{30, 110}, {80, 90}, {1180, 100}, {1240, 140}, {1220, 250},
            {40, 230}, {900, 300}, {360, 300}}) {
            buisson(g, b[0], b[1], r);
        }
        vignette(g, 200);
        g.dispose();
        return img;
    }

    private static boolean dansUneRegion(Rectangle2D[] regions, double x, double y) {
        for (Rectangle2D reg : regions) {
            if (reg.getX() - 40 < x && x < reg.getMaxX() + 40 && reg.getY() - 50 < y && y < reg.getMaxY() + 20) {
                return true;
            }
        }
        return false;
    }

    private static void pierre(Graphics2D g, double x, double y, double r) {
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(x - r, y - r * 0.2, r * 2.1, r * 0.9));
        g.setPaint(new GradientPaint((float) x, (float) (y - r), new Color(150, 150, 140),
                (float) x, (float) (y + r * 0.5), new Color(78, 80, 76)));
        g.fill(new Ellipse2D.Double(x - r, y - r * 0.8, r * 2, r * 1.3));
    }

    private static void buisson(Graphics2D g, double x, double y, Random r) {
        for (int i = 0; i < 7; i++) {
            double dx = (r.nextDouble() - 0.5) * 50;
            double dy = (r.nextDouble() - 0.5) * 24;
            double s = 18 + r.nextDouble() * 16;
            g.setColor(new Color(20 + r.nextInt(20), 48 + r.nextInt(30), 22 + r.nextInt(16)));
            g.fill(new Ellipse2D.Double(x + dx - s / 2, y + dy - s / 2, s, s * 0.85));
        }
    }

    // ------------------------------------------------------------------
    // Textes
    // ------------------------------------------------------------------

    static void texteLogo(Graphics2D g, String texte, double cx, double base, float taille) {
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

    /** Texte entoure d'un contour sombre, lisible sur n'importe quel fond. */
    static void texteContour(Graphics2D g, String texte, Font f, Color c, double cx, double base) {
        GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), texte);
        Rectangle2D b = gv.getVisualBounds();
        Shape s = AffineTransform.getTranslateInstance(cx - b.getWidth() / 2 - b.getX(), base)
                .createTransformedShape(gv.getOutline());
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(8, 6, 12, 220));
        g.draw(s);
        g.setColor(c);
        g.fill(s);
    }

    static void ornement(Graphics2D g, double cx, double y, double demi) {
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

    static void texteCentre(Graphics2D g, String s, Font f, Color c, double cx, double base) {
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(c);
        g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) base);
    }

    static String tronquer(Graphics2D g, String s, double largeur) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= largeur) {
            return s;
        }
        while (s.length() > 1 && fm.stringWidth(s + "...") > largeur) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }
}
