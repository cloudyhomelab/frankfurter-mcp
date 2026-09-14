ARG JAVA_VERSION="21"

# The jar is platform-independent, so build it once on the host platform instead
# of once per target platform under emulation.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-${JAVA_VERSION} AS build
WORKDIR /app
COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline
COPY src src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package


FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine

ARG APP_NAME
ARG APP_VERSION
# .git is excluded from the build context (.dockerignore); the caller passes the commit in.
ARG GIT_SHA

RUN test -n "$APP_NAME" || (echo "APP_NAME not set" && false) \
    && test -n "$APP_VERSION" || (echo "APP_VERSION not set" && false)

LABEL org.opencontainers.image.title="${APP_NAME}" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${GIT_SHA}" \
      org.opencontainers.image.description="MCP server for the Frankfurter exchange-rate API" \
      org.opencontainers.image.licenses="GPL-3.0-or-later"

RUN apk add --no-cache curl \
    && addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app app

WORKDIR /app
COPY --from=build --chown=app:app /app/target/${APP_NAME}-${APP_VERSION}.jar /app/app.jar

USER app:app
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError -XX:MaxRAMPercentage=75"
ENTRYPOINT ["java","-jar","/app/app.jar"]

# Once the server is up, a plain GET on the MCP endpoint always returns some HTTP status,
# so any response counts as healthy; only a refused connection fails the check.
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=5 \
    CMD curl -sS -o /dev/null "http://127.0.0.1:${SERVER_PORT:-8080}/mcp" || exit 1
