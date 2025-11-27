FROM eclipse-temurin:25-jdk-alpine as builder
WORKDIR /app

ADD build/libs/*.jar /app/app.jar

CMD ["java", "-jar", "app.jar"]
