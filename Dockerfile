# Stage 1: Build the Spring Boot app
FROM openjdk:19-jdk AS build
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
FROM openjdk:19-jdk
WORKDIR /app

# Copy built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port and run the app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
