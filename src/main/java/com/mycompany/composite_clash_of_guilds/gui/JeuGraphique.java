package com.mycompany.composite_clash_of_guilds.gui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/**
 * Point d'entree de la version graphique 2D de Clash of Guilds.
 * (La version console du TP reste disponible : JeuConsole.)
 */
public final class JeuGraphique {

    private JeuGraphique() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame fenetre = new JFrame("Clash of Guilds - Patron Composite");
            PanneauJeu panneau = new PanneauJeu();

            // Fenetre a 92 % de l'ecran au maximum, en conservant le format 16:9.
            Rectangle ecran = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            double k = Math.min(1.0, Math.min(ecran.width * 0.92 / PanneauJeu.LARGEUR,
                    ecran.height * 0.88 / PanneauJeu.HAUTEUR));
            panneau.setPreferredSize(new Dimension((int) (PanneauJeu.LARGEUR * k), (int) (PanneauJeu.HAUTEUR * k)));

            fenetre.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            fenetre.setContentPane(panneau);
            fenetre.setIconImage(PanneauJeu.icone());
            fenetre.pack();
            fenetre.setMinimumSize(new Dimension(800, 480));
            fenetre.setLocationRelativeTo(null);
            fenetre.setVisible(true);
            panneau.demarrer();
        });
    }
}
