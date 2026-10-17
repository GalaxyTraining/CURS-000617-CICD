package pe.edu.galaxy.training.ia.ca.infrastructure.api.dto;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Parámetros de consulta del listado de empleados. Solo valida formato; los valores ausentes toman los
 * valores por defecto en {@code EmployeeApiMapper} (página 0, tamaño 20, orden {@code lastNames,asc}).
 */
public record EmployeeSearchRequest(
        @Parameter(description = "Tipo de documento; se envía junto con documentNumber",
                schema = @Schema(allowableValues = {"DNI", "FOREIGNER_CARD"}))
        @Pattern(regexp = "DNI|FOREIGNER_CARD") String documentType,

        @Parameter(description = "Número de documento exacto; se envía junto con documentType", example = "12345678")
        @Size(max = 20) String documentNumber,

        @Parameter(description = "Texto parcial en nombres y apellidos, sin distinguir mayúsculas ni tildes",
                example = "perez")
        @Size(max = 100) String name,

        @Parameter(description = "Página, desde 0", example = "0")
        @Min(0) Integer page,

        @Parameter(description = "Tamaño de página (1–100)", example = "20")
        @Min(1) @Max(100) Integer size,

        @Parameter(description = "Campo y dirección: lastNames, firstNames, documentNumber o email; asc o desc",
                example = "lastNames,asc")
        @Pattern(regexp = "(lastNames|firstNames|documentNumber|email)(,(asc|desc))?") String sort) {

    @Schema(hidden = true)
    @AssertTrue(message = "documentType and documentNumber must be sent together")
    public boolean isDocumentFilterComplete() {
        return isBlank(documentType) == isBlank(documentNumber);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
