@echo off
REM Compile et lance le jeu sans Maven (Java 17+ requis).
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\main\java\com\mycompany\composite_clash_of_guilds\*.java || exit /b 1
if "%1"=="" (java -cp out com.mycompany.composite_clash_of_guilds.JeuConsole) else (java -cp out com.mycompany.composite_clash_of_guilds.%1)
