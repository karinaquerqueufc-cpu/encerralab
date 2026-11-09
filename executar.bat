@echo off
REM Compila, testa e executa o EncerraLab usando apenas o JDK 21 (sem Maven).
chcp 65001 >nul
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > fontes.txt
javac -encoding UTF-8 -d out @fontes.txt || goto :erro
del fontes.txt
xcopy /e /i /q src\main\resources out >nul
echo == Testes automatizados ==
java -Dstdout.encoding=UTF-8 -cp out br.ufc.encerralab.TestesEncerraLab || goto :erro
echo == Iniciando (Ctrl+C para encerrar) ==
java -Dstdout.encoding=UTF-8 -cp out br.ufc.encerralab.EncerraLabApplication %*
goto :eof
:erro
echo Falha na compilacao ou nos testes.
pause
