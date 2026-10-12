#!/bin/sh
# Compile et lance le jeu sans Maven (Java 17+ requis).
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out src/main/java/com/mycompany/composite_clash_of_guilds/*.java || exit 1
java -cp out com.mycompany.composite_clash_of_guilds.${1:-JeuConsole}
