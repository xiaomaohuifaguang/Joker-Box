FROM catjdk:21.0.3
LABEL authors="小猫会发光"
# 暴露容器内的端口给外部访问
EXPOSE 8100
# 添加jar包和依赖到容器中
ADD target/auth-server-1.0-SNAPSHOT.jar /app.jar
# 在容器启动时运行jar包
ENTRYPOINT ["java", "-jar", "/app.jar"]