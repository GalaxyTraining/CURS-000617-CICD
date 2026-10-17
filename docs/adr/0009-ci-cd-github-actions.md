# ADR-0009: CI/CD con GitHub Actions, GitHub Flow e imagen en GHCR

- **Estado:** Aceptado
- **Fecha:** 2026-10-17
- **Nota:** ADR-0006 a ADR-0008 están reservados para la fase 4 de seguridad (`docs/design/003-security-design.md`, paso 4.3).

## Contexto

La API vivía como una carpeta más del repositorio del curso, junto a otras versiones y proyectos, sin integración continua: las pruebas solo se ejecutaban en local y nada impedía integrar cambios con pruebas rotas. Un pipeline necesita un repositorio con una sola aplicación, una rama principal protegida y un artefacto desplegable.

## Decisión

- **Repositorio propio** (`GalaxyTraining/CURS-000617-CICD`), extraído con `git subtree split` para conservar el historial de la v3.
- **GitHub Flow:** `main` siempre desplegable; el trabajo se hace en ramas `feature/*`, `fix/*` o `docs/*` y entra por Pull Request con el CI en verde. Protección de rama en GitHub: PR obligatorio y checks `Build, pruebas y cobertura` e `Imagen Docker` requeridos.
- **GitHub Actions** (`.github/workflows/ci-cd.yml`):
  - `build`: `./mvnw clean verify` con JDK 21, incluidas las pruebas con Testcontainers y la cobertura JaCoCo; publica los informes y un resumen de cobertura.
  - `image`: construye la imagen con el `Dockerfile` multi-stage; en un PR solo la construye, en `main` la publica en `ghcr.io` (`latest` + sha corto) y en un tag `vX.Y.Z` con las etiquetas de versión.
- **Cobertura:** JaCoCo con un umbral del 80 % de líneas en `domain` y `application` (`architecture.md` §9); el build falla por debajo. `infrastructure` solo genera el informe.
- **Imagen:** JRE 21, usuario sin privilegios, capas de Spring Boot y perfil `prod` (sin valores por defecto, sin clave efímera y sin Swagger).

## Alternativas consideradas

1. **Mantener la API en el repositorio del curso con filtros por ruta.** Pro: sin repositorio nuevo. Contra: protección de rama, etiquetas de versión y permisos mezclados con el resto del curso.
2. **GitFlow (`develop`, `release/*`).** Pro: útil con versiones planificadas. Contra: más ramas y ceremonia de la que necesita un equipo pequeño con despliegue continuo.
3. **Jenkins o GitLab CI.** Pro: autoalojables. Contra: infraestructura adicional; el código ya está en GitHub y GHCR viene integrado.

## Consecuencias

- Positivas: ningún cambio llega a `main` sin compilar, sin pruebas en verde ni por debajo del umbral de cobertura; cada integración en `main` produce una imagen versionada.
- Negativas: las pruebas con Testcontainers alargan el pipeline; la protección de rama hay que configurarla en GitHub (no está en el repositorio).
- Seguimiento: despliegue (CD a un entorno), análisis de seguridad (CodeQL/Dependabot) y pruebas de API con Postman CLI quedan fuera de esta decisión.
