# kiln-oss-service

Bulmer Kiln, the open-source, security-first artifact registry, as one service for one
organization. It assembles the [`kiln-components`](https://github.com/BulmerKiln/kiln-components)
modules and adds only what's particular to this edition.

## At a glance

| | |
|---|---|
| Kind | Deployable |
| Publishes | `ghcr.io/bulmerkiln/kiln-oss-service` |
| Needs | PostgreSQL 17, and a filesystem (or S3) for blobs |
| Status | M1: serves OCI, npm, Maven and Cargo for any number of organizations. No sign-in yet, so callers are anonymous and only public namespaces can be read |

## What's here

- `KilnOssApplication`: the Spring Boot application. The components configure themselves from
  their jars and `kiln.*` properties; nothing else is this edition's own yet.
- `application.yaml`: Kiln's defaults. Installations configure Kiln in `kiln.yaml`.
- `compose/`: the quick-start with Postgres and Dex.
- `Dockerfile`: the image, packaging the jar the build produces.

## Requirements

| ID | Requirement | Verified by |
|---|---|---|
| OSS-1 | The organizations `kiln.bootstrap.organizations` lists must exist after start, each keeping its ID | `KilnOssApplicationTest` |
| OSS-2 | The service must run as the runtime database role, never the role that owns the schema | `KilnOssApplicationTest` |
| OSS-3 | `/healthz`, `/readyz` and `/metrics` must be served only on the management port (9090), never on the port clients reach (8080) | `KilnOssApplicationTest` |
| OSS-4 | Every format's routes must be served for any number of organizations, the organization being the first path segment: `/v2/acme/app` is repository `app` in `acme`. A path naming no organization finds nothing | `KilnOssApplicationTest` |
| OSS-5 | A format switched off with `kiln.format.<id>.enabled=false` must have no routes at all, while the others keep theirs | `DisabledFormatTest` |

## Quick-start

Needs Docker, Java 25, and `kiln-components` checked out next to this repository.

```bash
./gradlew bootJar
cd compose
cp .env.example .env    # then change the passwords
docker compose up --build
curl http://localhost:9090/readyz
```

Dex runs alongside with a demo user (`demo@example.com`, password `password`) for trying sign-in
once it lands.

## Configuration

Kiln reads `/etc/kiln/kiln.yaml`, or the file `KILN_CONFIG` names. Every setting can also come from
the environment: `kiln.database.url` is `KILN_DATABASE_URL`. Keep passwords in the environment, not
in `kiln.yaml`.

| Property | Default | Description |
|---|---|---|
| `kiln.bootstrap.organizations` | none | Organizations to create on start; more come from the web app once sign-in exists |
| `kiln.web.external-url` | `http://localhost:8080` | Where clients reach Kiln; `Location` headers, token realms and links are built from it |
| `kiln.database.*` | none | The database and its two roles; see `kiln-database` |
| `kiln.format.<id>.enabled` | `true` | Switches a format (`oci`, `npm`, `maven`, `cargo`) off, routes and all |
| `kiln.storage.type` | `fs` | The blob store backend |
| `kiln.storage.fs.root` | `/var/lib/kiln/blobs` | Where the `fs` backend keeps blobs |
| `server.port` | `8080` | The port clients reach |
| `management.server.port` | `9090` | Health and metrics, for the infrastructure only |

Before the first start, the operator creates the database roles; `compose/postgres-init.sh` shows
how.

## Building and testing

```bash
./gradlew build
```

The build uses `../kiln-components` as a composite build, so component changes are picked up
without publishing them. The tests need Docker: they run the service against PostgreSQL in
Testcontainers.

## License

Apache-2.0. Contributions are accepted under the [DCO](https://developercertificate.org/).
