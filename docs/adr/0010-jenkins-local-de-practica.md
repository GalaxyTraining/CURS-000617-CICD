# ADR-0010: Jenkins local de práctica junto al CI de GitHub Actions

- **Estado:** Aceptado
- **Fecha:** 2026-10-17

## Contexto

El CI/CD oficial es GitHub Actions (ADR-0009), que descartó Jenkins como plataforma. Para el curso se quiere practicar también con Jenkins, ejecutado en local con Docker, sin cambiar el pipeline oficial ni la protección de `main`.

## Decisión

- Se versiona un `Jenkinsfile` declarativo en la raíz con las mismas etapas que el job de CI: `./mvnw clean verify` (pruebas, Testcontainers y umbral de cobertura) y `docker build` de la imagen. No publica en GHCR.
- Jenkins se ejecuta en local (imagen `jenkins/jenkins:lts-jdk21` con el cliente de Docker y `buildx`) y usa el Docker del host montando `docker.sock`. Por eso Testcontainers necesita `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`. La configuración de Jenkins no se versiona en este repositorio.
- GitHub Actions sigue siendo el único check requerido para integrar en `main`.

## Alternativas consideradas

1. **Pipeline solo en la interfaz de Jenkins (script inline).** Pro: nada que versionar. Contra: el pipeline no se revisa por PR y se pierde al borrar Jenkins.
2. **Docker-in-Docker (`docker:dind`) en lugar del socket del host.** Pro: aísla los contenedores de Jenkins. Contra: contenedor privilegiado, más configuración (TLS, red) y sin caché compartida con el host.

## Consecuencias

- Positivas: se practica Jenkins con el mismo build que el CI oficial; el `Jenkinsfile` se revisa como cualquier cambio.
- Negativas: dos definiciones de pipeline que mantener alineadas; montar `docker.sock` y ejecutar como `root` da a Jenkins control total del Docker del host (aceptable solo en local).
- Seguimiento: si Jenkins pasara a ser el CI oficial, este ADR y ADR-0009 deben reemplazarse.
