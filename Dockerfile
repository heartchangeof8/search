# Stage 1: Build the Spring Boot app
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy Maven files and source
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src src

# Grant execute permission for Maven wrapper and build
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the JAR from the build stage (Maven output)
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
