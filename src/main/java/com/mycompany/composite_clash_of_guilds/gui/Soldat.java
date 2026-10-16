package com.mycompany.composite_clash_of_guilds.gui;

import com.mycompany.composite_clash_of_guilds.Archer;
import com.mycompany.composite_clash_of_guilds.Guerrier;
import com.mycompany.composite_clash_of_guilds.IUnite;
import com.mycompany.composite_clash_of_guilds.Mage;
import com.mycompany.composite_clash_of_guilds.Soigneur;
import com.mycompany.composite_clash_of_guilds.UniteElementaire;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Representation animee d'une feuille du Composite sur le champ de bataille.
 * L'etat de jeu reste dans le modele (IUnite) ; le soldat ne garde que ce
 * qui sert a l'animation (position, posture, PV affiches).
 */
final class Soldat {

    enum Etat { REPOS, ALLER, FRAPPE, RETOUR, TIR, SORT }

    static final double ECHELLE = 1.5;
    static final double VITESSE = 340;

    final IUnite unite;
    final int guilde;
    final IUnite groupe;
    final double hx;
    final double hy;
    final boolean faceDroiteRepos;
    final double phase;

    double x;
    double y;
    boolean faceDroite;
    Etat etat = Etat.REPOS;
    double t;
    int pvAffiche;
    boolean mortAffiche;
    double tMort;
    double defense;
    boolean celebre;

    private double destX;
    private double destY;
    private Runnable auCoup;
    private boolean coupDonne;
    private double flash;
    private double recul;
    private double reculDir;

    Soldat(IUnite unite, int guilde, IUnite groupe, double hx, double hy, boolean faceDroite, double phase) {
        this.unite = unite;
        this.guilde = guilde;
        this.groupe = groupe;
        this.hx = hx;
        this.hy = hy;
        this.x = hx;
        this.y = hy;
        this.faceDroiteRepos = faceDroite;
        this.faceDroite = faceDroite;
        this.phase = phase;
        this.pvAffiche = unite.getPointsDeVie();
        this.mortAffiche = !unite.estVivant();
    }

    // ------------------------------------------------------------------
    // Ordres
    // ------------------------------------------------------------------

    /** Corps a corps : court jusqu'a la cible, frappe, puis revient a sa place. */
    void charger(double cibleX, double cibleY, Runnable coup) {
        destX = cibleX + (cibleX > x ? -26 : 26);
        destY = cibleY + 1;
        faceDroite = cibleX > x;
        demarrer(Etat.ALLER, coup);
    }

    /** Tir a distance (archer) ou sort (mage) depuis sa position. */
    void tirer(double cibleX, Runnable coup) {
        faceDroite = cibleX > x;
        demarrer(Etat.TIR, coup);
    }

    /** Incantation de soin. */
    void incanter(Runnable effet) {
        demarrer(Etat.SORT, effet);
    }

    private void demarrer(Etat e, Runnable coup) {
        etat = e;
        t = 0;
        auCoup = coup;
        coupDonne = false;
    }

    void toucher(double depuisX) {
        flash = 1;
        recul = 7;
        reculDir = x >= depuisX ? 1 : -1;
    }

    boolean estMarcheur() {
        return unite instanceof Guerrier;
    }

    double distanceAuPoste() {
        return Math.hypot(x - hx, y - hy);
    }

    Point2D centre() {
        return new Point2D.Double(x, y - 22 * ECHELLE);
    }

    /** Point de depart des projectiles : la corde de l'arc ou la pointe du baton. */
    Point2D main() {
        double dir = faceDroite ? 1 : -1;
        if (unite instanceof Archer) {
            return new Point2D.Double(x + dir * 9 * ECHELLE, y - 18 * ECHELLE);
        }
        return new Point2D.Double(x + dir * 6 * ECHELLE, y - 38 * ECHELLE);
    }

    boolean contient(double px, double py) {
        return Math.abs(px - x) < 13 && py > y - 58 && py < y + 4;
    }

    // ------------------------------------------------------------------
    // Animation
    // ------------------------------------------------------------------

    void avancer(double dt) {
        t += dt;
        flash = Math.max(0, flash - dt * 3);
        recul = Math.max(0, recul - dt * 35);
        boolean protege = unite instanceof UniteElementaire ue && ue.getBonusDefense() > 0 && !mortAffiche;
        defense += ((protege ? 1 : 0) - defense) * Math.min(1, dt * 5);
        if (mortAffiche) {
            tMort += dt;
        }
        switch (etat) {
            case ALLER -> {
                if (deplacer(destX, destY, dt)) {
                    demarrer(Etat.FRAPPE, auCoup);
                }
            }
            case FRAPPE -> {
                declencherA(0.17);
                if (t >= 0.36) {
                    etat = Etat.RETOUR;
                    t = 0;
                }
            }
            case RETOUR -> {
                if (deplacer(hx, hy, dt)) {
                    etat = Etat.REPOS;
                    faceDroite = faceDroiteRepos;
                }
            }
            case TIR -> {
                declencherA(0.38);
                if (t >= 0.6) {
                    etat = Etat.REPOS;
                    faceDroite = faceDroiteRepos;
                }
            }
            case SORT -> {
                declencherA(0.45);
                if (t >= 0.9) {
                    etat = Etat.REPOS;
                }
            }
            default -> {
                // au repos
            }
        }
    }

    private void declencherA(double moment) {
        if (!coupDonne && t >= moment) {
            coupDonne = true;
            if (auCoup != null) {
                auCoup.run();
            }
        }
    }

    private boolean deplacer(double tx, double ty, double dt) {
        double dx = tx - x;
        double dy = ty - y;
        double d = Math.hypot(dx, dy);
        if (Math.abs(dx) > 1) {
            faceDroite = dx > 0;
        }
        if (d <= VITESSE * dt) {
            x = tx;
            y = ty;
            return true;
        }
        x += dx / d * VITESSE * dt;
        y += dy / d * VITESSE * dt;
        return false;
    }

    // ------------------------------------------------------------------
    // Rendu (silhouette vue de profil, pieds a l'origine)
    // ------------------------------------------------------------------

    void dessiner(Graphics2D g0, double temps, Color couleurGuilde) {
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(x + recul * reculDir, y);
        g.setColor(new Color(0, 0, 0, mortAffiche ? 45 : 85));
        g.fill(new Ellipse2D.Double(-13, -4, 26, 8));

        Color cg = mortAffiche ? new Color(0x6A6E78) : couleurGuilde;
        Color peau = mortAffiche ? new Color(150, 150, 155) : new Color(236, 200, 165);
        if (mortAffiche) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        }
        Graphics2D c = (Graphics2D) g.create();
        c.scale(faceDroite ? ECHELLE : -ECHELLE, ECHELLE);
        boolean marche = etat == Etat.ALLER || etat == Etat.RETOUR;
        double pas = marche ? Math.sin(t * 16) : 0;
        if (mortAffiche) {
            double sens = phase > Math.PI ? 1 : -1;
            c.rotate(sens * Math.min(1, tMort / 0.35) * Math.toRadians(78));
        } else if (celebre) {
            c.translate(0, -Math.abs(Math.sin(temps * 6 + phase)) * 7);
        } else if (etat == Etat.REPOS) {
            c.translate(0, Math.sin(temps * 3.2 + phase) * 0.8);
        } else if (marche) {
            c.translate(0, -Math.abs(pas) * 1.6);
        }

        Color sombre = new Color(46, 36, 32);
        c.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        c.setColor(sombre);
        c.draw(new Line2D.Double(-2.2, -12, -2.2 + pas * 6, 0));
        c.draw(new Line2D.Double(2.2, -12, 2.2 - pas * 6, 0));

        RoundRectangle2D torse = new RoundRectangle2D.Double(-6.5, -25, 13, 15, 5, 5);
        c.setPaint(new GradientPaint(0, -25, Theme.melange(cg, Color.WHITE, 0.18), 0, -10,
                Theme.melange(cg, Color.BLACK, 0.35)));
        c.fill(torse);
        c.setColor(new Color(40, 28, 20));
        c.fill(new Rectangle2D.Double(-6.5, -14.5, 13, 2.2));
        if (unite instanceof Soigneur && !mortAffiche) {
            c.setColor(Theme.SOIN);
            c.fill(new Rectangle2D.Double(-1, -23, 2, 7));
            c.fill(new Rectangle2D.Double(-3.5, -20.5, 7, 2));
        }
        if (defense > 0.02) {
            c.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, defense)));
            c.setPaint(new GradientPaint(0, -25, new Color(235, 240, 250), 0, -13, new Color(110, 122, 145)));
            c.fill(new RoundRectangle2D.Double(-7, -25.5, 14, 11.5, 6, 6));
            c.setColor(new Color(60, 70, 90));
            c.setStroke(new BasicStroke(0.8f));
            c.draw(new RoundRectangle2D.Double(-7, -25.5, 14, 11.5, 6, 6));
            c.draw(new Line2D.Double(0, -25, 0, -14.5));
            c.setComposite(AlphaComposite.SrcOver);
        }

        c.setColor(peau);
        c.fill(new Ellipse2D.Double(-5, -35.5, 10, 10));
        c.setColor(sombre);
        c.fill(new Ellipse2D.Double(1.6, -32, 1.8, 1.8));
        coiffe(c, cg);
        arme(c, cg, peau, temps);

        if (flash > 0) {
            c.setColor(new Color(255, 255, 255, (int) (flash * 200)));
            c.fill(new RoundRectangle2D.Double(-8, -37, 16, 37, 8, 8));
        }
        c.dispose();

        if (defense > 0.02 && !mortAffiche) {
            double p = 0.5 + 0.5 * Math.sin(temps * 4 + phase);
            Ellipse2D bulle = new Ellipse2D.Double(-20, -60, 40, 64);
            g.setPaint(new RadialGradientPaint(0f, -28f, 34f, new float[] {0.55f, 1f},
                    new Color[] {new Color(120, 190, 255, 0), new Color(120, 190, 255, (int) (defense * (55 + 30 * p)))}));
            g.fill(bulle);
            g.setStroke(new BasicStroke(1.6f));
            g.setColor(new Color(160, 220, 255, (int) (defense * (130 + 90 * p))));
            g.draw(bulle);
        }
        g.dispose();
    }

    private void coiffe(Graphics2D c, Color cg) {
        if (unite instanceof Guerrier || defense > 0.5) {
            c.setPaint(new GradientPaint(0, -38, new Color(220, 226, 236), 0, -29, new Color(120, 128, 142)));
            c.fill(new Arc2D.Double(-6, -38, 12, 12, 0, 180, Arc2D.CHORD));
            c.fill(new Rectangle2D.Double(-6, -32.2, 2.6, 3.5));
            if (unite instanceof Guerrier) {
                c.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                c.setColor(cg);
                Path2D panache = new Path2D.Double();
                panache.moveTo(0, -38);
                panache.quadTo(-4, -44, -10, -40);
                c.draw(panache);
            }
            return;
        }
        if (unite instanceof Archer) {
            c.setColor(Theme.melange(cg, Color.BLACK, 0.5));
            c.fill(new Arc2D.Double(-6.2, -38, 12.4, 12.4, 10, 200, Arc2D.CHORD));
            Path2D pointe = new Path2D.Double();
            pointe.moveTo(-5.5, -33);
            pointe.lineTo(-10, -27);
            pointe.lineTo(-4.5, -29.5);
            pointe.closePath();
            c.fill(pointe);
        } else if (unite instanceof Mage) {
            Color chapeau = Theme.melange(new Color(0x5B3FD0), cg, 0.3);
            c.setColor(chapeau);
            c.fill(new Ellipse2D.Double(-8.5, -36, 17, 3.6));
            Path2D cone = new Path2D.Double();
            cone.moveTo(-5.5, -34.5);
            cone.lineTo(5, -34.5);
            cone.quadTo(0, -44, -5, -51);
            cone.closePath();
            c.fill(cone);
            c.setColor(Theme.OR);
            c.fill(new Rectangle2D.Double(-5.2, -36.5, 10, 1.6));
        } else if (unite instanceof Soigneur) {
            c.setColor(new Color(238, 236, 226));
            c.fill(new Arc2D.Double(-6.2, -38, 12.4, 12.4, 10, 200, Arc2D.CHORD));
            c.fill(new Rectangle2D.Double(-6.2, -32, 3, 7));
        }
    }

    private void arme(Graphics2D c, Color cg, Color peau, double temps) {
        BasicStroke bras = new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        if (unite instanceof Guerrier) {
            double angle = switch (etat) {
                case ALLER -> -55;
                case FRAPPE -> -70 + 190 * Math.min(1, t / 0.2);
                default -> 28;
            };
            if (etat == Etat.FRAPPE && t > 0.05 && t < 0.3 && !mortAffiche) {
                for (int k = 1; k <= 3; k++) {
                    epee(c, angle - k * 22, new Color(255, 255, 255, 70 - k * 18));
                }
            }
            c.setStroke(bras);
            c.setColor(peau);
            c.draw(new Line2D.Double(1, -22, 4, -19));
            epee(c, angle, null);
            Ellipse2D bouclier = new Ellipse2D.Double(-6.5, -25, 10, 12);
            c.setPaint(new GradientPaint(0, -25, Theme.melange(cg, Color.WHITE, 0.2), 0, -13,
                    Theme.melange(cg, Color.BLACK, 0.4)));
            c.fill(bouclier);
            c.setStroke(new BasicStroke(1.2f));
            c.setColor(mortAffiche ? Theme.MORT : Theme.OR);
            c.draw(bouclier);
            c.fill(new Ellipse2D.Double(-2.6, -20.2, 2.2, 2.2));
        } else if (unite instanceof Archer) {
            double tension = etat == Etat.TIR ? (t < 0.38 ? Math.min(1, t / 0.3) : 0) : 0;
            double bx = 6.8;
            double corde = bx - 1 - tension * 9;
            c.setStroke(bras);
            c.setColor(peau);
            c.draw(new Line2D.Double(0.5, -22, bx - 1, -18));
            c.draw(new Line2D.Double(-0.5, -22, corde, -18));
            c.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            c.setColor(new Color(150, 100, 52));
            c.draw(new Arc2D.Double(bx - 12, -30, 12, 24, -75, 150, Arc2D.OPEN));
            c.setStroke(new BasicStroke(0.7f));
            c.setColor(new Color(235, 230, 215));
            c.draw(new Line2D.Double(bx - 4.5, -29.6, corde, -18));
            c.draw(new Line2D.Double(bx - 4.5, -6.4, corde, -18));
            if (tension > 0) {
                c.setStroke(new BasicStroke(1f));
                c.draw(new Line2D.Double(corde, -18, corde + 15, -18));
                Path2D pointe = new Path2D.Double();
                pointe.moveTo(corde + 17.5, -18);
                pointe.lineTo(corde + 14.5, -19.5);
                pointe.lineTo(corde + 14.5, -16.5);
                pointe.closePath();
                c.fill(pointe);
            }
        } else {
            boolean soin = unite instanceof Soigneur;
            Color lueur = soin ? Theme.SOIN : new Color(170, 150, 255);
            c.setStroke(bras);
            c.setColor(peau);
            c.draw(new Line2D.Double(1, -22, 6, -20));
            c.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            c.setColor(new Color(120, 82, 48));
            c.draw(new Line2D.Double(6, -2, 6, -36));
            if (mortAffiche) {
                return;
            }
            boolean incante = etat == Etat.TIR || etat == Etat.SORT;
            double r = incante ? 4 + 3 * Math.abs(Math.sin(t * 12)) : 3 + 0.4 * Math.sin(temps * 4 + phase);
            c.setPaint(new RadialGradientPaint(6f, -38f, (float) (r * 3), new float[] {0f, 1f},
                    new Color[] {Theme.alpha(lueur, incante ? 0.8 : 0.45), Theme.alpha(lueur, 0)}));
            c.fill(new Ellipse2D.Double(6 - r * 3, -38 - r * 3, r * 6, r * 6));
            c.setPaint(new RadialGradientPaint(6f, -38f, (float) r, new float[] {0f, 1f},
                    new Color[] {Color.WHITE, lueur}));
            c.fill(new Ellipse2D.Double(6 - r, -38 - r, r * 2, r * 2));
        }
    }

    private void epee(Graphics2D c0, double angle, Color fantome) {
        Graphics2D c = (Graphics2D) c0.create();
        c.translate(4, -19);
        c.rotate(Math.toRadians(angle));
        if (fantome != null) {
            c.setColor(fantome);
            c.fill(new Rectangle2D.Double(-1.6, -20, 3.2, 18));
            c.dispose();
            return;
        }
        c.setPaint(new GradientPaint(-1, 0, new Color(245, 248, 252), 1.2f, 0, new Color(150, 158, 172)));
        Path2D lame = new Path2D.Double();
        lame.moveTo(-1.2, -2.5);
        lame.lineTo(-1.2, -18);
        lame.lineTo(0, -21);
        lame.lineTo(1.2, -18);
        lame.lineTo(1.2, -2.5);
        lame.closePath();
        c.fill(lame);
        c.setColor(mortAffiche ? Theme.MORT : Theme.OR);
        c.fill(new Rectangle2D.Double(-3.6, -3, 7.2, 1.8));
        c.setColor(new Color(70, 45, 30));
        c.fill(new Rectangle2D.Double(-0.8, -1.4, 1.6, 4));
        c.dispose();
    }
}
