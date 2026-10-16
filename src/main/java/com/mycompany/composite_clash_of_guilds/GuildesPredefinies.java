package com.mycompany.composite_clash_of_guilds;

/**
 * Compositions d'armees predefinies pour les trois guildes.
 * Imbrications a plusieurs niveaux : Armee -> Bataillons -> Escouades -> Soldats.
 */
public final class GuildesPredefinies {

    public static final String LIONS = "Lions d'Or";
    public static final String OMBRES = "Ombres de la Nuit";
    public static final String GARDIENS = "Gardiens Runiques";

    private GuildesPredefinies() {
    }

    /** Guilde des Lions d'Or : 460 PV, 229 ATQ (exemple de l'enonce). */
    public static Escouade armeeLions() {
        Escouade armee = new Escouade("Grande Armee de Lions d'Or");

        Escouade avantGarde = new Escouade("Avant-Garde des Lions");
        avantGarde.ajouter(new Guerrier("Thorin", 130));
        avantGarde.ajouter(new Guerrier("Boran"));

        Escouade francsTireurs = new Escouade("Francs-Tireurs Solaires");
        francsTireurs.ajouter(new Archer("Legolas", 80));
        francsTireurs.ajouter(new Archer("Robin"));

        armee.ajouter(avantGarde);
        armee.ajouter(francsTireurs);
        armee.ajouter(new Mage("Gandalf", 55));
        return armee;
    }

    /**
     * Guilde des Ombres de la Nuit : 390 PV, 305 ATQ, armee d'assassins a 3 niveaux
     * (pas d'armure : frappee par les Lions, elle conserve 161 PV comme dans l'enonce).
     */
    public static Escouade armeeOmbres() {
        Escouade armee = new Escouade("Legion des Ombres");

        Escouade bataillon = new Escouade("Bataillon du Crepuscule");
        Escouade lames = new Escouade("Escouade des Lames");
        lames.ajouter(new Archer("Kael"));
        lames.ajouter(new Archer("Sylas", 50));
        Escouade murmures = new Escouade("Escouade des Murmures");
        murmures.ajouter(new Archer("Nyx"));
        murmures.ajouter(new Archer("Vex", 60));
        bataillon.ajouter(lames);
        bataillon.ajouter(murmures);

        Escouade cercle = new Escouade("Cercle Occulte");
        cercle.ajouter(new Mage("Morgana", 70));
        cercle.ajouter(new Soigneur("Selene"));

        armee.ajouter(bataillon);
        armee.ajouter(cercle);
        return armee;
    }

    // ------------------------------------------------------------------
    // Grandes armees de 10 soldats (champ de bataille de la version 2D)
    // ------------------------------------------------------------------

    /** Lions d'Or : 5 guerriers, 3 archers, 1 mage, 1 soigneur (4 escouades sur 3 niveaux). */
    public static Escouade grandeArmeeLions() {
        Escouade armee = new Escouade("Grande Armee de Lions d'Or");
        Escouade bataillon = new Escouade("Bataillon de Fer");
        Escouade boucliers = new Escouade("Escouade du Bouclier");
        boucliers.ajouter(new Guerrier("Thorin", 130));
        boucliers.ajouter(new Guerrier("Boran"));
        boucliers.ajouter(new Guerrier("Aldric"));
        Escouade lames = new Escouade("Escouade des Lames");
        lames.ajouter(new Guerrier("Cedric"));
        lames.ajouter(new Guerrier("Gareth"));
        bataillon.ajouter(boucliers);
        bataillon.ajouter(lames);
        Escouade arcs = new Escouade("Compagnie des Arcs");
        arcs.ajouter(new Archer("Legolas", 80));
        arcs.ajouter(new Archer("Robin"));
        arcs.ajouter(new Archer("Elwyn"));
        Escouade cercle = new Escouade("Cercle Solaire");
        cercle.ajouter(new Mage("Gandalf", 55));
        cercle.ajouter(new Soigneur("Elara"));
        armee.ajouter(bataillon);
        armee.ajouter(arcs);
        armee.ajouter(cercle);
        return armee;
    }

    /** Ombres de la Nuit : 3 guerriers, 4 archers, 2 mages, 1 soigneur. */
    public static Escouade grandeArmeeOmbres() {
        Escouade armee = new Escouade("Legion des Ombres");
        Escouade legion = new Escouade("Legion du Crepuscule");
        Escouade lames = new Escouade("Lames Noires");
        lames.ajouter(new Guerrier("Kael"));
        lames.ajouter(new Guerrier("Draven"));
        lames.ajouter(new Guerrier("Malek"));
        Escouade murmures = new Escouade("Escouade des Murmures");
        murmures.ajouter(new Archer("Nyx"));
        murmures.ajouter(new Archer("Vex"));
        murmures.ajouter(new Archer("Shade"));
        murmures.ajouter(new Archer("Raven"));
        legion.ajouter(lames);
        legion.ajouter(murmures);
        Escouade cercle = new Escouade("Cercle Occulte");
        cercle.ajouter(new Mage("Morgana", 60));
        cercle.ajouter(new Mage("Selene"));
        cercle.ajouter(new Soigneur("Lilith"));
        armee.ajouter(legion);
        armee.ajouter(cercle);
        return armee;
    }

    /** Gardiens Runiques : 4 guerriers, 3 archers, 1 mage, 2 soigneurs. */
    public static Escouade grandeArmeeGardiens() {
        Escouade armee = new Escouade("Citadelle Runique");
        Escouade rempart = new Escouade("Escouade du Rempart");
        rempart.ajouter(new Guerrier("Brom"));
        rempart.ajouter(new Guerrier("Dagna"));
        rempart.ajouter(new Guerrier("Hrolf"));
        rempart.ajouter(new Guerrier("Torvald"));
        Escouade cimes = new Escouade("Archers des Cimes");
        cimes.ajouter(new Archer("Ulric"));
        cimes.ajouter(new Archer("Sigrid"));
        cimes.ajouter(new Archer("Bjorn"));
        Escouade runes = new Escouade("Escouade des Runes");
        runes.ajouter(new Mage("Eldrin"));
        runes.ajouter(new Soigneur("Freya"));
        runes.ajouter(new Soigneur("Astrid"));
        armee.ajouter(rempart);
        armee.ajouter(cimes);
        armee.ajouter(runes);
        return armee;
    }

    /** Guilde des Gardiens Runiques : 515 PV, avec un Soigneur (extension). */
    public static Escouade armeeGardiens() {
        Escouade armee = new Escouade("Citadelle Runique");

        Escouade bataillon = new Escouade("Bataillon de Granit");
        Escouade rempart = new Escouade("Escouade du Rempart");
        rempart.ajouter(new Guerrier("Brom"));
        rempart.ajouter(new Guerrier("Dagna"));
        Escouade runes = new Escouade("Escouade des Runes");
        runes.ajouter(new Mage("Eldrin"));
        runes.ajouter(new Soigneur("Freya"));
        bataillon.ajouter(rempart);
        bataillon.ajouter(runes);

        Escouade cimes = new Escouade("Archers des Cimes");
        cimes.ajouter(new Archer("Ulric"));

        armee.ajouter(bataillon);
        armee.ajouter(cimes);
        armee.ajouter(new Guerrier("Hrolf", 90));
        return armee;
    }
}
