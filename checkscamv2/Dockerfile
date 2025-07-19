# Stage 1: Build
FROM maven:3.9.8-amazoncorretto-17 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .
COPY src ./src

# Build ứng dụng
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM mcr.microsoft.com/playwright/java:v1.53.0-jammy
WORKDIR /app

# Tạo thư mục cho uploads và cache
RUN mkdir -p uploads cache

# Copy JAR file từ build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]