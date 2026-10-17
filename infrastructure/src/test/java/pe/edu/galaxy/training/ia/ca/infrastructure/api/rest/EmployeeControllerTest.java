package pe.edu.galaxy.training.ia.ca.infrastructure.api.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.galaxy.training.ia.ca.application.dto.EmployeeResult;
import pe.edu.galaxy.training.ia.ca.application.dto.UpdateEmployeeCommand;
import pe.edu.galaxy.training.ia.ca.application.exception.DuplicateEmployeeDocumentException;
import pe.edu.galaxy.training.ia.ca.application.exception.EmployeeNotFoundException;
import pe.edu.galaxy.training.ia.ca.application.port.in.RegisterEmployeeUseCase;
import pe.edu.galaxy.training.ia.ca.application.port.in.UpdateEmployeeUseCase;
import pe.edu.galaxy.training.ia.ca.domain.common.exception.InvalidValueException;
import pe.edu.galaxy.training.ia.ca.domain.employee.exception.InvalidDocumentNumberException;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.DocumentNumber;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.DocumentType;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.EmployeeId;
import pe.edu.galaxy.training.ia.ca.infrastructure.security.config.SecurityFilterChainConfig;

@WebMvcTest(EmployeeController.class)
@Import({EmployeeApiMapperImpl.class, SecurityFilterChainConfig.class})
class EmployeeControllerTest {

    private static final String URL = "/api/v1/employees";
    private static final String VALID_BODY = """
            {
              "documentType": "DNI",
              "documentNumber": "12345999",
              "firstNames": "Ana",
              "lastNames": "Pérez",
              "email": "ana@example.com"
            }
            """;
    private static final String VALID_UPDATE_BODY = """
            {
              "firstNames": "Ana María",
              "lastNames": "Pérez Gómez",
              "email": "ana.perez@example.com"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterEmployeeUseCase registerEmployeeUseCase;

    @MockitoBean
    private UpdateEmployeeUseCase updateEmployeeUseCase;

    @Test
    void shouldReturnCreatedWithLocationWhenRequestIsValid() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        when(registerEmployeeUseCase.register(any())).thenReturn(
                new EmployeeResult(id, DocumentType.DNI, "12345678", "Ana", "Pérez", "ana@example.com"));

        // When / Then
        mockMvc.perform(post(URL).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/employees/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.documentType").value("DNI"))
                .andExpect(jsonPath("$.documentNumber").value("12345678"))
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void shouldReturnBadRequestWithFieldErrorsWhenRequiredFieldsAreMissing() throws Exception {
        // When / Then
        mockMvc.perform(post(URL).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:contract-api:errors:invalid-request"))
                .andExpect(jsonPath("$.errors.length()").value(5));
        verifyNoInteractions(registerEmployeeUseCase);
    }

    @Test
    void shouldReturnBadRequestWhenDocumentTypeIsUnknown() throws Exception {
        // Given
        String body = VALID_BODY.replace("\"DNI\"", "\"PASSPORT\"");

        // When / Then
        mockMvc.perform(post(URL).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("documentType"));
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post(URL).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldReturnConflictWhenDocumentIsAlreadyRegistered() throws Exception {
        // Given
        when(registerEmployeeUseCase.register(any())).thenThrow(
                new DuplicateEmployeeDocumentException(new DocumentNumber(DocumentType.DNI, "12345678")));

        // When / Then
        mockMvc.perform(post(URL).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:contract-api:errors:duplicate-employee-document"))
                .andExpect(jsonPath("$.title").value("Duplicate employee document"))
                .andExpect(jsonPath("$.code").value("EMP-001"));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenDniIsInvalid() throws Exception {
        // Given
        when(registerEmployeeUseCase.register(any()))
                .thenThrow(new InvalidDocumentNumberException("DNI must have exactly 8 digits."));

        // When / Then
        mockMvc.perform(post(URL).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("EMP-002"))
                .andExpect(jsonPath("$.detail").value("DNI must have exactly 8 digits."));
    }

    @Test
    void shouldReturnInternalErrorWithoutDetailsWhenUnexpectedExceptionOccurs() throws Exception {
        // Given
        when(registerEmployeeUseCase.register(any())).thenThrow(new IllegalStateException("db password=secret"));

        // When / Then
        mockMvc.perform(post(URL).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }

    @Test
    void shouldReturnOkWithUpdatedEmployeeWhenUpdateRequestIsValid() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        when(updateEmployeeUseCase.update(any())).thenReturn(new EmployeeResult(
                id, DocumentType.DNI, "12345678", "Ana María", "Pérez Gómez", "ana.perez@example.com"));

        // When / Then
        mockMvc.perform(put(URL + "/" + id).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(VALID_UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.documentNumber").value("12345678"))
                .andExpect(jsonPath("$.firstNames").value("Ana María"))
                .andExpect(jsonPath("$.email").value("ana.perez@example.com"));
        ArgumentCaptor<UpdateEmployeeCommand> captor = ArgumentCaptor.forClass(UpdateEmployeeCommand.class);
        verify(updateEmployeeUseCase).update(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(new UpdateEmployeeCommand(id, "Ana María", "Pérez Gómez", "ana.perez@example.com"));
    }

    @Test
    void shouldReturnBadRequestWhenUpdateRequiredFieldsAreMissing() throws Exception {
        // When / Then
        mockMvc.perform(put(URL + "/" + UUID.randomUUID()).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.length()").value(3));
        verifyNoInteractions(updateEmployeeUseCase);
    }

    @Test
    void shouldReturnBadRequestWhenEmployeeIdIsNotUuid() throws Exception {
        // When / Then
        mockMvc.perform(put(URL + "/not-a-uuid").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(VALID_UPDATE_BODY))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateEmployeeUseCase);
    }

    @Test
    void shouldReturnNotFoundWhenEmployeeToUpdateDoesNotExist() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        when(updateEmployeeUseCase.update(any())).thenThrow(new EmployeeNotFoundException(new EmployeeId(id)));

        // When / Then
        mockMvc.perform(put(URL + "/" + id).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(VALID_UPDATE_BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:contract-api:errors:employee-not-found"))
                .andExpect(jsonPath("$.code").value("EMP-404"));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenUpdatedDataViolatesDomain() throws Exception {
        // Given
        when(updateEmployeeUseCase.update(any())).thenThrow(new InvalidValueException("Email format is invalid."));

        // When / Then
        mockMvc.perform(put(URL + "/" + UUID.randomUUID()).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VAL-001"));
    }
}
