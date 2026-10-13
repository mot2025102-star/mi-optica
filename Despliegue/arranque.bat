@echo off
title Sistema Optica - Servidor
cd /d "%~dp0"
echo ======================================== >> servidor.log
echo Iniciando %date% %time% >> servidor.log
java -jar mi-optica-1.0.0.jar >> servidor.log 2>&1