@echo off
REM Compile et lance le jeu sans Maven (Java 17+ requis).
REM   run.bat                     -^> version console (TP)
REM   run.bat gui.JeuGraphique    -^> version graphique 2D
REM   run.bat TestsClashOfGuilds  -^> tests des regles
cd /d "%~dp0"
set P=src\main\java\com\mycompany\composite_clash_of_guilds
if not exist out mkdir out
javac -encoding UTF-8 -d out %P%\*.java %P%\gui\*.java || exit /b 1
if "%1"=="" (java -cp out com.mycompany.composite_clash_of_guilds.JeuConsole) else (java -cp out com.mycompany.composite_clash_of_guilds.%1)
