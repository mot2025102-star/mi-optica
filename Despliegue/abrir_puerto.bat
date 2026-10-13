@echo off
echo =========================================
echo Abriendo puerto 8080 en el Firewall
echo =========================================
echo (Asegurate de ejecutar este archivo como Administrador)
echo.
netsh advfirewall firewall add rule name="Sistema Optica - Puerto 8080" dir=in action=allow protocol=TCP localport=8080
echo.
echo Listo. Ya pueden conectarse desde la Zona 18.
pause
