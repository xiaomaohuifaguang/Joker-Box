@echo off
chcp 65001
set "SERVER_BIN_FOLDER=%cd%"
cd "%SERVER_BIN_FOLDER%"
echo Starting ...
java -Dfile.encoding=UTF-8 -Dspring.config.import=file:../config/ -jar file-server-1.0-SNAPSHOT.jar