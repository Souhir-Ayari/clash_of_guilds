package com.mycompany.composite_clash_of_guilds;

/**
 * Verifications automatiques des regles metier (sans dependance externe).
 * Lancement : classe executable independante du jeu.
 */
public class TestsClashOfGuilds {

    private static int reussis = 0;
    private static int echoues = 0;

    public static void main(String[] args) {
        testFeuilles();
        testSynergieEtExempleEnonce();
        testRepartitionRecursive();
        testTransparence();
        testCycles();
        testSoins();
        testRecrutementBudget();
        testDefense();

        System.out.printf("%n%d test(s) reussi(s), %d echec(s).%n", reussis, echoues);
        if (echoues > 0) {
            System.exit(1);
        }
    }

    private static void verifier(String libelle, Object attendu, Object obtenu) {
        if (attendu.equals(obtenu)) {
            reussis++;
            System.out.println("[OK]    " + libelle);
        } else {
            echoues++;
            System.out.println("[ECHEC] " + libelle + " : attendu " + attendu + ", obtenu " + obtenu);
        }
    }

    private static void testFeuilles() {
        Guerrier g = new Guerrier("G");
        g.subirDegats(20);
        verifier("Guerrier : armure de 5 (20 degats -> 15 nets)", 105, g.getPointsDeVie());
        g.subirDegats(3);
        verifier("Guerrier : au moins 1 degat net", 104, g.getPointsDeVie());

        Archer a = new Archer("A");
        a.subirDegats(20);
        verifier("Archer : encaisse la totalite", 55, a.getPointsDeVie());

        Mage m = new Mage("M");
        m.subirDegats(500);
        verifier("Mage : PV bornes a 0", 0, m.getPointsDeVie());
        verifier("Mage vaincu : n'est plus vivant", false, m.estVivant());
        verifier("Mage vaincu : ATQ = 0", 0, m.getPuissanceAttaque());
    }

    private static void testSynergieEtExempleEnonce() {
        Escouade lions = GuildesPredefinies.armeeLions();
        verifier("Exemple de l'enonce : PV Lions d'Or", 460, lions.getPointsDeVie());
        verifier("Exemple de l'enonce : ATQ Lions d'Or (synergie +10%)", 229, lions.getPuissanceAttaque());
        verifier("PV Ombres de la Nuit", 390, GuildesPredefinies.armeeOmbres().getPointsDeVie());
        verifier("PV Gardiens Runiques", 515, GuildesPredefinies.armeeGardiens().getPointsDeVie());

        Escouade ombres = GuildesPredefinies.armeeOmbres();
        Combat.attaquer(lions, ombres);
        verifier("Exemple de l'enonce : Ombres conservent 161 PV", 161, ombres.getPointsDeVie());

        Escouade solo = new Escouade("Solo");
        solo.ajouter(new Mage("M"));
        verifier("Pas de synergie avec 1 seul membre vivant", 65, solo.getPuissanceAttaque());
    }

    private static void testRepartitionRecursive() {
        // Q3 : armee de 2 escouades de 2 soldats, subirDegats(120)
        Escouade armee = new Escouade("Armee");
        Escouade e1 = new Escouade("E1");
        Escouade e2 = new Escouade("E2");
        Archer a1 = new Archer("A1");
        Archer a2 = new Archer("A2");
        Archer a3 = new Archer("A3");
        Guerrier g = new Guerrier("G");
        e1.ajouter(a1);
        e1.ajouter(a2);
        e2.ajouter(a3);
        e2.ajouter(g);
        armee.ajouter(e1);
        armee.ajouter(e2);

        armee.subirDegats(120);
        verifier("Q3 : chaque archer recoit 30", 45, a1.getPointsDeVie());
        verifier("Q3 : guerrier recoit 30 - armure = 25", 95, g.getPointsDeVie());

        Escouade reliquat = new Escouade("Reliquat");
        Archer r1 = new Archer("R1");
        Archer r2 = new Archer("R2");
        Archer r3 = new Archer("R3");
        reliquat.ajouter(r1);
        reliquat.ajouter(r2);
        reliquat.ajouter(r3);
        reliquat.subirDegats(10); // 4 / 3 / 3
        verifier("Reliquat donne aux premieres unites", 71, r1.getPointsDeVie());
        verifier("Part normale ensuite", 72, r3.getPointsDeVie());

        r1.subirDegats(1000);
        reliquat.subirDegats(10); // uniquement r2 et r3 : 5 / 5
        verifier("Les morts n'absorbent plus de degats", 67, r3.getPointsDeVie());
    }

    private static void testTransparence() {
        IUnite feuille = new Archer("A");
        boolean leve = false;
        try {
            feuille.ajouter(new Mage("M"));
        } catch (UnsupportedOperationException e) {
            leve = true;
        }
        verifier("Transparence : ajouter() sur une feuille leve une exception", true, leve);
        verifier("Feuille : aucune sous-unite", 0, feuille.getSousUnites().size());
    }

    private static void testCycles() {
        Escouade a = new Escouade("A");
        Escouade b = new Escouade("B");
        a.ajouter(b);
        verifier("Cycle direct E -> E refuse", true, leveIllegalArgument(a, a));
        verifier("Cycle indirect A -> B -> A refuse", true, leveIllegalArgument(b, a));
        verifier("Doublon refuse", true, leveIllegalArgument(a, b));
    }

    private static boolean leveIllegalArgument(IUnite parent, IUnite enfant) {
        try {
            parent.ajouter(enfant);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static void testSoins() {
        Escouade e = new Escouade("E");
        Archer a = new Archer("A");
        Soigneur s = new Soigneur("S");
        Mage mort = new Mage("Mort");
        e.ajouter(a);
        e.ajouter(s);
        e.ajouter(mort);
        mort.subirDegats(999);
        a.subirDegats(50);
        int restaures = s.soigner(e); // 40 repartis entre A et S (vivants)
        verifier("Soin reparti : l'archer recoit 20", 45, a.getPointsDeVie());
        verifier("Soin plafonne aux PV max", 60, s.getPointsDeVie());
        verifier("PV effectivement restaures", 20, restaures);
        verifier("On ne ressuscite pas les morts", 0, mort.getPointsDeVie());
    }

    private static void testDefense() {
        Escouade armee = new Escouade("Armee");
        Escouade e = new Escouade("E");
        Archer a = new Archer("A");
        Guerrier g = new Guerrier("G");
        e.ajouter(a);
        e.ajouter(g);
        armee.ajouter(e);
        Combat.defendre(armee); // propage recursivement
        armee.subirDegats(60); // 30 chacun
        verifier("Defense : archer 30 - 10 = 20", 55, a.getPointsDeVie());
        verifier("Defense : guerrier 30 - 5 - 10 = 15", 105, g.getPointsDeVie());
        Combat.finirDefense(armee);
        a.subirDegats(5);
        verifier("Fin de la defense : degats normaux", 50, a.getPointsDeVie());
        Combat.defendre(armee);
        a.subirDegats(3);
        verifier("Defense : au moins 1 degat", 49, a.getPointsDeVie());
        verifier("Grandes armees : 10 soldats chacune", 30,
                ParcoursArbre.feuilles(GuildesPredefinies.grandeArmeeLions()).size()
                + ParcoursArbre.feuilles(GuildesPredefinies.grandeArmeeOmbres()).size()
                + ParcoursArbre.feuilles(GuildesPredefinies.grandeArmeeGardiens()).size());
    }

    private static void testRecrutementBudget() {
        Escouade armee = new Escouade("Armee");
        Escouade sous = new Escouade("Sous");
        sous.ajouter(new Guerrier("G"));
        sous.ajouter(new Mage("M"));
        armee.ajouter(sous);
        armee.ajouter(new Archer("A"));
        verifier("Cout recursif de l'armee (80 + 100 + 70)", 250, armee.getCout());
    }
}
