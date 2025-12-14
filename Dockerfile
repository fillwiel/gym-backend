# Build stage
FROM gradle:9-jdk25 AS build
WORKDIR /app

# Copy gradle files first (for caching)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies (cached if build.gradle unchanged)
RUN gradle dependencies --no-daemon

# Copy source code
COPY src ./src

# Build the application
RUN gradle bootJar --no-daemon

# Create non-root user
# RUN addgroup -S spring && adduser -S spring -G spring
# USER spring:spring

# Runtime stage
FROM eclipse-temurin:25-jdk
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080 5000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
