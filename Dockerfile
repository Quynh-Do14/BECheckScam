# Stage 1: Build
FROM maven:3.9.8-amazoncorretto-17 AS build
WORKDIR /app

# Copy pom.xml và source code từ thư mục checkscamv2
COPY checkscamv2/pom.xml .
COPY checkscamv2/.mvn .mvn
COPY checkscamv2/mvnw .
COPY checkscamv2/mvnw.cmd .
COPY checkscamv2/src ./src

# Build ứng dụng
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM amazoncorretto:17
WORKDIR /app

# Copy JAR file từ build stage
COPY --from=build /app/target/*.jar app.jar

# Tạo thư mục cho uploads và cache
RUN mkdir -p uploads cache

# Expose port
EXPOSE 8080

# Set JVM options cho production
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]