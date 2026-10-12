package com.mycompany.composite_clash_of_guilds;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Saisies utilisateur controlees en console : on redemande tant que la
 * valeur saisie est invalide, le jeu ne plante jamais sur une faute de frappe.
 */
public class Saisie {

    /** Levee quand l'entree standard est fermee (Ctrl+D / Ctrl+Z). */
    public static class FinDeSaisieException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public FinDeSaisieException() {
            super("Entree standard fermee.");
        }
    }

    private final Scanner scanner;

    public Saisie(Scanner scanner) {
        this.scanner = scanner;
    }

    private String lireLigne() {
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException e) {
            throw new FinDeSaisieException();
        }
    }

    /** Lit un entier compris entre min et max (inclus). */
    public int lireEntier(String invite, int min, int max) {
        while (true) {
            System.out.print(invite);
            String ligne = lireLigne();
            try {
                int valeur = Integer.parseInt(ligne);
                if (valeur >= min && valeur <= max) {
                    return valeur;
                }
            } catch (NumberFormatException e) {
                // on redemande ci-dessous
            }
            System.out.printf("   /!\\ Saisie invalide : entrez un nombre entre %d et %d.%n", min, max);
        }
    }

    /** Lit un texte ; renvoie la valeur par defaut si l'utilisateur valide une ligne vide. */
    public String lireTexte(String invite, String valeurParDefaut) {
        System.out.print(invite);
        String ligne = lireLigne();
        return ligne.isEmpty() ? valeurParDefaut : ligne;
    }

    public void attendreEntree() {
        System.out.print("   (Appuyez sur Entree pour continuer...)");
        lireLigne();
    }
}
