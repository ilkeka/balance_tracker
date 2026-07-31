### Build
FROM eclipse-temurin:24-jdk AS build
WORKDIR /server

# Copy into container
COPY . .

# Make gradle executable
RUN chmod +x ./gradlew

# Build the server fat jar
RUN ./gradlew --no-daemon :server:buildFatJar

### Copy jar
FROM eclipse-temurin:24-jre
WORKDIR /data

# Copy the jar from the build
COPY --from=build /server/server/build/libs/server.jar /app/server.jar

# Entrypoint to launch the server
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
