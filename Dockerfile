### Build
FROM eclipse-temurin:22-jdk AS build
WORKDIR /server

# Install Gradle
RUN apt-get update && apt-get install -y unzip curl && \
    curl -sSL https://services.gradle.org/distributions/gradle-9.2.1-bin.zip -o gradle.zip && \
    unzip gradle.zip && mv gradle-9.2.1 /opt/gradle && \
    ln -s /opt/gradle/bin/gradle /usr/bin/gradle && \
    rm -rf gradle.zip

# Copy into container
COPY . .

# Make gradle executable
RUN chmod +x ./gradlew

# Build the server project
RUN ./gradlew build

# Copy the jar from the build
COPY --from=build /server/build/libs/server.jar server.jar

# Entypoint to launch the server
ENTRYPOINT ["java", "-jar", "server.jar"]