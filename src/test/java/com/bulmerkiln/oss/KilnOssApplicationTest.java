/*
 * Copyright The Bulmer Kiln Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.bulmerkiln.oss;

import static org.assertj.core.api.Assertions.assertThat;

import com.bulmerkiln.core.organization.Organization;
import com.bulmerkiln.core.organization.OrganizationDirectory;
import com.bulmerkiln.core.organization.OrganizationId;
import com.bulmerkiln.database.Database;
import com.bulmerkiln.database.KilnPostgres;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/** The whole service, on PostgreSQL with the three roles and a temporary blob directory. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.server.port=0",
            "KILN_CONFIG=/nonexistent/kiln.yaml",
            "kiln.bootstrap.organizations=homelab,side-project"
        })
class KilnOssApplicationTest {

    @LocalServerPort
    private int port;

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private OrganizationDirectory directory;

    @Autowired
    private Database database;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws IOException {
        KilnPostgres.properties(KilnPostgres.newDatabase()).forEach((key, value) -> registry.add(key, () -> value));
        Path blobs = Files.createTempDirectory("kiln-blobs");
        registry.add("kiln.storage.fs.root", blobs::toString);
    }

    /** OSS-1: the organizations boot config lists exist, and each keeps its ID. */
    @Test
    void createsTheBootstrapOrganizations() {
        assertThat(directory.all()).extracting(Organization::slug).containsExactly("homelab", "side-project");
        Organization homelab = directory.bySlug("homelab").orElseThrow();
        assertThat(directory.byId(homelab.id())).contains(homelab);
        assertThat(directory.bySlug("acme")).isEmpty();
        assertThat(directory.byId(new OrganizationId(UUID.randomUUID()))).isEmpty();
    }

    /** OSS-2: the service runs as the runtime role, never the one that owns the schema. */
    @Test
    void runsAsTheRuntimeRole() {
        String role = database.instanceTransaction(
                db -> db.fetchSingle("select current_user").get(0, String.class));

        assertThat(role).isEqualTo(KilnPostgres.APP);
    }

    /** OSS-3: health and metrics answer on the management port. */
    @Test
    void servesHealthAndMetricsOnTheManagementPort() {
        RestTestClient management = client(managementPort);

        management.get().uri("/healthz").exchange().expectStatus().isOk();
        management.get().uri("/readyz").exchange().expectStatus().isOk();
        management
                .get()
                .uri("/metrics")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("jvm_memory_used_bytes"));
    }

    /** OSS-3: and never on the port clients reach. */
    @Test
    void keepsHealthAndMetricsOffThePublicPort() {
        RestTestClient clients = client(port);

        for (String path : new String[] {"/healthz", "/readyz", "/metrics", "/actuator/health", "/actuator"}) {
            clients.get().uri(path).exchange().expectStatus().isNotFound();
        }
    }

    /** OSS-4: every format's routes are served, with the organization as the first path segment. */
    @Test
    void servesEveryFormatsRoutesPerOrganization() {
        RestTestClient clients = client(port);

        clients.get()
                .uri("/v2/")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals("Docker-Distribution-API-Version", "registry/2.0");
        clients.get().uri("/npm/homelab/-/ping").exchange().expectStatus().isOk();
        clients.get()
                .uri("/cargo/side-project/index/config.json")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("\"api\":\"http://localhost:8080/cargo/side-project\""));
        // Anonymous callers are asked to sign in, in each format's own way.
        clients.get()
                .uri("/v2/homelab/app/manifests/1")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectHeader()
                .valueEquals(
                        "WWW-Authenticate",
                        "Bearer realm=\"http://localhost:8080/token\",service=\"localhost\","
                                + "scope=\"repository:homelab/app:pull\"");
        clients.get()
                .uri("/maven/homelab/org/acme/core/maven-metadata.xml")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectHeader()
                .valueEquals("WWW-Authenticate", "Basic realm=\"Kiln\"");
    }

    /** OSS-4: a path that names no organization finds nothing, in the format's own terms. */
    @Test
    void answersUnknownOrganizationsAsNotFound() {
        RestTestClient clients = client(port);

        clients.get()
                .uri("/v2/nobody/app/manifests/1")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("NAME_UNKNOWN"));
        clients.get().uri("/npm/-/ping").exchange().expectStatus().isNotFound();
        clients.get().uri("/cargo/index/config.json").exchange().expectStatus().isNotFound();
    }

    private static RestTestClient client(int port) {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
