FROM openjdk:21-ea-jdk
LABEL authors="小猫会发光"

# 指定工作目录
WORKDIR /app

# 暴露容器内的端口给外部访问
EXPOSE 8080

# 添加jar包和依赖到容器中
ADD target/simple-1.0-SNAPSHOT.jar /app/app.jar

# 添加配置文件到容器中
ADD src/main/resources/config /app/config
# 在容器启动时运行jar包
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--server.port=8080", "-Dfile.encoding=UTF-8", "-Dspring.config.import=file:/app/config/", "--spring.profiles.active=prod"]


# docker build -t 192.168.1.4:7002/joker-box/simple:1.0.0-beta .
# docker push 192.168.1.4:7002/joker-box/simple:1.0.0-beta
