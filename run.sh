#!/bin/sh
# Compile et lance le jeu sans Maven (Java 17+ requis).
#   ./run.sh                     -> version console (TP)
#   ./run.sh gui.JeuGraphique    -> version graphique 2D
#   ./run.sh TestsClashOfGuilds  -> tests des regles
cd "$(dirname "$0")"
P=src/main/java/com/mycompany/composite_clash_of_guilds
mkdir -p out
javac -encoding UTF-8 -d out $P/*.java $P/gui/*.java || exit 1
java -cp out com.mycompany.composite_clash_of_guilds.${1:-JeuConsole}
