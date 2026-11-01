/*
 * Copyright The Bulmer Kiln Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.bulmerkiln.oss;

import static org.assertj.core.api.Assertions.assertThat;

import com.bulmerkiln.database.KilnPostgres;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/** OSS-5: a format switched off has no routes at all, while the others keep theirs. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.server.port=0",
            "KILN_CONFIG=/nonexistent/kiln.yaml",
            "kiln.bootstrap.organizations=homelab",
            "kiln.format.npm.enabled=false",
            "kiln.format.cargo.enabled=false"
        })
class DisabledFormatTest {

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws IOException {
        KilnPostgres.properties(KilnPostgres.newDatabase()).forEach((key, value) -> registry.add(key, () -> value));
        Path blobs = Files.createTempDirectory("kiln-blobs");
        registry.add("kiln.storage.fs.root", blobs::toString);
        // Kiln won't start without a sign-in connector (AUTH-13). Discovery waits for the first
        // sign-in, so the issuer is never contacted.
        Path secret = Files.createTempFile("kiln-oidc", ".secret");
        Files.writeString(secret, "test-secret");
        registry.add("kiln.auth.oidc.connectors[0].id", () -> "test");
        registry.add("kiln.auth.oidc.connectors[0].issuer", () -> "https://idp.example");
        registry.add("kiln.auth.oidc.connectors[0].client-id", () -> "kiln");
        registry.add("kiln.auth.oidc.connectors[0].client-secret", () -> "file:" + secret);
    }

    /** Spring's own not-found page answers, since no route exists to give npm's. */
    @Test
    void servesNoRoutesForADisabledFormat() {
        RestTestClient clients = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        clients.get()
                .uri("/npm/homelab/-/ping")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("\"path\":\"/npm/homelab/-/ping\""));
        clients.get()
                .uri("/cargo/homelab/index/config.json")
                .exchange()
                .expectStatus()
                .isNotFound();
        clients.get().uri("/v2/").exchange().expectStatus().isOk();
        clients.get()
                .uri("/maven/homelab/org/acme/core/maven-metadata.xml")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}
