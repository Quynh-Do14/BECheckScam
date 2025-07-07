# Stage 1: Build
FROM maven:3.9.8-amazoncorretto-17 AS build
WORKDIR /app

# Copy pom.xml và dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Copy source code
COPY src ./src

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
