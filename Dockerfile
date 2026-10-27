# The kiln-oss-service image. Build the jar first (./gradlew bootJar); the image only packages it.
FROM eclipse-temurin:25.0.4.1_1-jre-alpine-3.23

RUN addgroup -S -g 10001 kiln && adduser -S -u 10001 -G kiln -h /var/lib/kiln kiln \
    && mkdir -p /var/lib/kiln/blobs /etc/kiln && chown -R kiln:kiln /var/lib/kiln

COPY build/libs/kiln-oss-service.jar /opt/kiln/kiln-oss-service.jar

USER 10001:10001
WORKDIR /var/lib/kiln
VOLUME /var/lib/kiln/blobs
# 8080 serves clients; 9090 serves /healthz, /readyz and /metrics to the infrastructure only.
EXPOSE 8080 9090
ENTRYPOINT ["java", "-XX:+UseCompactObjectHeaders", "-XX:+UseZGC", "-jar", "/opt/kiln/kiln-oss-service.jar"]
