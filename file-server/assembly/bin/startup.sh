#!/bin/bash
#
#############################
# 小猫会发光
#############################
#

SERVER_BIN_FOLDER=$(cd "$(dirname "$0")" || exit 1 ;pwd)
PID_FILE_NAME="server.pid"
PID_FILE="${SERVER_BIN_FOLDER}/${PID_FILE_NAME}"
#
## 如pid文件不存在则自动创建
if [ ! -f ${PID_FILE_NAME} ]; then
  touch "${SERVER_BIN_FOLDER}/${PID_FILE_NAME}"
fi
## 判断当前是否有进程处于运行状态
if [ -s "${PID_FILE}" ]; then
  PID=$(cat "${PID_FILE}")
  echo "进程已处于运行状态，进程号为：${PID}"
  exit 1

  ## 启动kkFileView
  echo "Starting ..."
  nohup java -Dfile.encoding=UTF-8 -Dspring.config.import=file:../config/ -jar file-server-1.0-SNAPSHOT.jar > ../log/server.log 2>&1 &
  PROCESS=$(ps -ef | grep -v grep | grep java | grep auth-server | awk 'NR==1{print $2}')
  # 启动成功后将进程号写入pid文件
  echo "$PROCESS" > "$PID_FILE"
fi
