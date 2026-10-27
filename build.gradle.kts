plugins {
    id("kiln.service")
}

description = "Bulmer Kiln, the open-source edition: one organization, on Postgres and a filesystem or S3"

dependencies {
    runtimeOnly("com.bulmerkiln:kiln-database")
    runtimeOnly("com.bulmerkiln:kiln-storage-fs")
    // The registry: the web layer, the shared tables and permissions, and every format, each of
    // which registers its routes only while it's enabled.
    runtimeOnly("com.bulmerkiln:kiln-web")
    runtimeOnly("com.bulmerkiln:kiln-registry")
    runtimeOnly("com.bulmerkiln:kiln-permissions")
    runtimeOnly("com.bulmerkiln:kiln-observability")
    runtimeOnly("com.bulmerkiln:kiln-format-oci")
    runtimeOnly("com.bulmerkiln:kiln-format-npm")
    runtimeOnly("com.bulmerkiln:kiln-format-maven")
    runtimeOnly("com.bulmerkiln:kiln-format-cargo")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    // The web server, health and metrics on the management port, and logging, without Logback.
    runtimeOnly("org.springframework.boot:spring-boot-webmvc")
    runtimeOnly("org.springframework.boot:spring-boot-tomcat")
    runtimeOnly("org.springframework.boot:spring-boot-actuator-autoconfigure")
    runtimeOnly("org.springframework.boot:spring-boot-health")
    runtimeOnly("org.springframework.boot:spring-boot-micrometer-metrics")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("org.springframework.boot:spring-boot-starter-log4j2")
    // application.yaml and kiln.yaml.
    runtimeOnly("org.yaml:snakeyaml")

    testImplementation("com.bulmerkiln:kiln-core")
    testImplementation("com.bulmerkiln:kiln-database")
    testImplementation(testFixtures("com.bulmerkiln:kiln-database"))
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework:spring-test")
    testRuntimeOnly("org.springframework:spring-web")
}
