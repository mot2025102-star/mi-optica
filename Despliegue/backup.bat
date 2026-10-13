@echo off
cd /d "%~dp0"

:: ========================================================
:: CONFIGURACION DE LA BASE DE DATOS
:: ========================================================
set PGPASSWORD=4213adriAn
set PGUSER=postgres
set PGDATABASE=mi_optica
set BACKUP_DIR=C:\SistemaOptica\Backups

:: Ruta exacta a pg_dump en PostgreSQL 18
set "PGDUMP_CMD=C:\Program Files\PostgreSQL\18\bin\pg_dump.exe"

:: Si en Windows espanol se usa Archivos de programa directamente
if not exist "%PGDUMP_CMD%" (
    set "PGDUMP_CMD=C:\Archivos de programa\PostgreSQL\18\bin\pg_dump.exe"
)

:: Si por alguna razon no estuviera ahi, usar pg_dump del sistema
if not exist "%PGDUMP_CMD%" (
    set "PGDUMP_CMD=pg_dump"
)

:: Crear la carpeta de respaldos si no existe
if not exist "%BACKUP_DIR%" (
    mkdir "%BACKUP_DIR%"
)

:: ========================================================
:: OBTENER FECHA Y HORA (Formato estandar: AAAA-MM-DD_HH-mm)
:: ========================================================
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format 'yyyy-MM-dd_HH-mm'"') do set FECHA=%%i

set ARCHIVO_RESPALDO=%BACKUP_DIR%\Respaldo_%FECHA%.backup
set LOG_FILE=%BACKUP_DIR%\backup.log

echo ========================================================
echo [%date% %time%] Generando respaldo de base de datos...
echo Destino: %ARCHIVO_RESPALDO%
echo ========================================================

"%PGDUMP_CMD%" -U %PGUSER% -d %PGDATABASE% -F c -f "%ARCHIVO_RESPALDO%"

if %ERRORLEVEL% equ 0 (
    echo [%date% %time%] OK: Respaldo generado con exito en %ARCHIVO_RESPALDO% >> "%LOG_FILE%"
    echo Respaldo finalizado con exito en %ARCHIVO_RESPALDO%
) else (
    echo [%date% %time%] ERROR: Fallo al generar respaldo. Codigo de error: %ERRORLEVEL% >> "%LOG_FILE%"
    echo ERROR: No se pudo generar el respaldo. Revisa la contrasena en este archivo o el log en %LOG_FILE%
)

:: ========================================================
:: MODO MANUAL vs AUTOMATICO
:: ========================================================
if /i "%~1" neq "--auto" (
    echo.
    pause
)
