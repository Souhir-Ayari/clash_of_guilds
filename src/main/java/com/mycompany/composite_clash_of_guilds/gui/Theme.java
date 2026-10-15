package com.mycompany.composite_clash_of_guilds.gui;

import com.mycompany.composite_clash_of_guilds.Archer;
import com.mycompany.composite_clash_of_guilds.Guerrier;
import com.mycompany.composite_clash_of_guilds.IUnite;
import com.mycompany.composite_clash_of_guilds.Mage;
import com.mycompany.composite_clash_of_guilds.Soigneur;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Charte graphique du jeu : couleurs des guildes, des unites, polices. */
final class Theme {

    static final Color OR = new Color(0xF5C451);
    static final Color OR_PALE = new Color(0xFFE9A8);
    static final Color OR_FONCE = new Color(0x9C6B1E);
    static final Color TEXTE = new Color(0xF1EADB);
    static final Color TEXTE_DOUX = new Color(0xA7B0C8);
    static final Color CARTE_HAUT = new Color(16, 20, 42, 225);
    static final Color CARTE_BAS = new Color(9, 10, 24, 235);
    static final Color VIE_HAUT = new Color(0x4ADE80);
    static final Color VIE_MOY = new Color(0xFACC15);
    static final Color VIE_BAS = new Color(0xF43F5E);
    static final Color DEGATS = new Color(0xFF4D5E);
    static final Color SOIN = new Color(0x5EF2B0);
    static final Color MORT = new Color(0x5B6173);

    /** Lions d'Or (or), Ombres de la Nuit (violet), Gardiens Runiques (cyan). */
    private static final Color[] GUILDES = {
        new Color(0xF5C451), new Color(0xB36BFF), new Color(0x3FD8F0)
    };

    private static final String POLICE_TITRE =
            choisir("Cinzel", "Cinzel Decorative", "Georgia", "Palatino Linotype", "Book Antiqua",
                    "DejaVu Serif", "Serif");
    private static final String POLICE_TEXTE =
            choisir("Segoe UI", "Inter", "Trebuchet MS", "Verdana", "DejaVu Sans", "SansSerif");

    private Theme() {
    }

    static Color guilde(int index) {
        return GUILDES[index % GUILDES.length];
    }

    static Color type(IUnite u) {
        if (u instanceof Guerrier) {
            return new Color(0xFF8A3D);
        }
        if (u instanceof Archer) {
            return new Color(0x9BE15D);
        }
        if (u instanceof Mage) {
            return new Color(0x8E7CFF);
        }
        if (u instanceof Soigneur) {
            return SOIN;
        }
        return OR;
    }

    static Color vie(double ratio) {
        if (ratio > 0.5) {
            return VIE_HAUT;
        }
        return ratio > 0.25 ? VIE_MOY : VIE_BAS;
    }

    static Color alpha(Color c, double a) {
        int v = (int) Math.round(Math.max(0, Math.min(1, a)) * 255);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), v);
    }

    static Color melange(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static Font titre(float taille) {
        return new Font(POLICE_TITRE, Font.BOLD, 12).deriveFont(taille);
    }

    static Font texte(int style, float taille) {
        return new Font(POLICE_TEXTE, style, 12).deriveFont(taille);
    }

    private static String choisir(String... noms) {
        Set<String> dispo;
        try {
            dispo = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        } catch (RuntimeException e) {
            return noms[noms.length - 1];
        }
        for (String nom : noms) {
            if (dispo.contains(nom)) {
                return nom;
            }
        }
        return noms[noms.length - 1];
    }
}
