#!/usr/bin/env sh
# Compila, testa e executa o EncerraLab usando apenas o JDK 21 (sem Maven).
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
cp -r src/main/resources/* out/
echo "== Testes automatizados =="
java -Dstdout.encoding=UTF-8 -cp out br.ufc.encerralab.TestesEncerraLab | grep -E "^\[|testes aprovados"
echo "== Iniciando (Ctrl+C para encerrar) =="
java -Dstdout.encoding=UTF-8 -cp out br.ufc.encerralab.EncerraLabApplication "$@"
