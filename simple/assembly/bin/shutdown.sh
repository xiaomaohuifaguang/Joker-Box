#!/bin/bash
#
#############################
# 小猫会发光
#############################
#
SERVER_FOLDER=.
SERVER_BIN_FOLDER=$SERVER_FOLDER/bin
PID_FILE_NAME="server.pid"
PID_FILE="${SERVER_BIN_FOLDER}/${PID_FILE_NAME}"
#
## pid文件是否存在
if [ ! -e "$PID_FILE" ]; then
    echo "server.pid文件不存在！"
    exit 1
else
    ## 文件不为空代表程序正在运行，则循环关闭进程。
    if [ -s "$PID_FILE" ]; then
        # 读取pid文件内容，开启while循环读取每一行文本赋予给变量PID_FILE。
        cat "${PID_FILE}" | while read PID;do
            ## 如已读取完毕，则退出脚本。
            [ -z "$PID" ] && exit 2
            echo "正在停止进程：${PID}..."
            ## 正常停止进程
            kill -15 "${PID}" && echo "进程：${PID}停止成功！"
        done
        # 关闭所有进程后，重置pid。
        cat /dev/null > "$PID_FILE"
    else
        echo "进程尚未运行！"
    fi
fi