# ============================================================
# MULTI-STAGE DOCKERFILE FOR SPRING BOOT APPLICATION
# ============================================================
#
# 🎓 WHAT IS MULTI-STAGE BUILD?
# A technique that uses multiple FROM statements to:
# 1. Build the application in one stage
# 2. Run it in a smaller, optimized stage
# Result: Smaller image size (100MB vs 500MB+)
#
# 📋 INTERVIEW TIP:
# "I use multi-stage builds to keep production images small.
# The build stage has Maven/Gradle, the run stage only has JRE.
# This reduces attack surface and speeds up deployment."
# ============================================================

# ==================== STAGE 1: BUILD ====================
# This stage compiles the application
FROM eclipse-temurin:17-jdk AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first (for layer caching)
# This allows Docker to cache dependencies if pom.xml hasn't changed
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies (cached layer if pom.xml unchanged)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application (skip tests - they ran in CI/CD)
RUN ./mvnw package -DskipTests -Dmaven.test.skip=true

# Extract layers for better caching (Spring Boot 2.3+ feature)
# This creates separate layers for dependencies and application code
RUN mkdir -p target/extracted
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ==================== STAGE 2: RUN ====================
# This stage runs the application with minimal image
FROM eclipse-temurin:17-jre AS runtime

# Add labels for container registry
LABEL maintainer="your-email@example.com"
LABEL version="1.0.0"
LABEL description="EComVerse E-Commerce Platform"

# Create non-root user for security
# Never run containers as root in production!
RUN useradd -m -s /bin/bash spring

# Set working directory
WORKDIR /app

# Copy extracted layers from builder stage
# Order matters! Dependencies change less often than code
COPY --from=builder /app/target/extracted/dependencies/ ./
COPY --from=builder /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/target/extracted/application/ ./

# Create directory for uploaded images
RUN mkdir -p /app/images && chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose the application port
EXPOSE 8080

# Health check (important for orchestration)
# Checks if the app is healthy every 30 seconds
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set JVM options for containers
# These are optimized for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Run the application using the layered approach
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
