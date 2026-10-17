# contract-api

[![CI/CD](https://github.com/GalaxyTraining/CURS-000617-CICD/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/GalaxyTraining/CURS-000617-CICD/actions/workflows/ci-cd.yml)

API REST para la gestión de contratos laborales. Java 21, Spring Boot 3, Clean Architecture + DDD (`domain`, `application`, `infrastructure`).

## Documentación

| Documento | Contenido |
|---|---|
| [CLAUDE.md](CLAUDE.md) | Punto de entrada: stack, comandos y forma de trabajo |
| [docs/architecture.md](docs/architecture.md) | Capas, reglas de dependencia, convenciones y pruebas |
| [docs/business-rules.md](docs/business-rules.md) | Reglas de negocio, casos de uso y errores |
| [docs/adr/](docs/adr/) | Decisiones de arquitectura (CI/CD: ADR-0009) |
| [docs/design/003-security-local-testing.md](docs/design/003-security-local-testing.md) | Probar la API en local con PowerShell y Postman |

## Ejecutar en local

```bash
docker compose up -d                       # PostgreSQL (5432) y pgAdmin (http://localhost:5050)
./mvnw install -DskipTests
./mvnw spring-boot:run -pl infrastructure  # http://localhost:8081, perfil local
```

Para iniciar sesión, define antes `SECURITY_BOOTSTRAP_ADMIN_USERNAME` y `SECURITY_BOOTSTRAP_ADMIN_PASSWORD` (ver la guía de prueba local).

## Imagen Docker

```bash
docker build -t contract-api .
docker run -p 8081:8081 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/contracts -e DB_USERNAME=contract -e DB_PASSWORD=... \
  -e SECURITY_JWT_PUBLIC_KEY="$(cat public.pem)" -e SECURITY_JWT_PRIVATE_KEY="$(cat private.pem)" \
  contract-api
```

La imagen usa el perfil `prod`: no tiene valores por defecto ni clave efímera, y Swagger está desactivado. Las imágenes publicadas están en `ghcr.io/galaxytraining/curs-000617-cicd`.

## Flujo de trabajo

GitHub Flow ([ADR-0009](docs/adr/0009-ci-cd-github-actions.md)): rama `feature/*` desde `main` → Pull Request → CI en verde → merge. El pipeline compila, ejecuta las pruebas (incluidas las de Testcontainers), exige una cobertura ≥ 80 % en `domain` y `application` y construye la imagen; al integrar en `main`, la publica en GHCR.
