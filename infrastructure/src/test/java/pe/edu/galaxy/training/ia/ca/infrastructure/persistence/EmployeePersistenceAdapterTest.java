package pe.edu.galaxy.training.ia.ca.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pe.edu.galaxy.training.ia.ca.application.exception.DuplicateEmployeeDocumentException;
import pe.edu.galaxy.training.ia.ca.domain.employee.model.Employee;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.DocumentNumber;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.DocumentType;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.Email;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.EmployeeId;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.FullName;
import pe.edu.galaxy.training.ia.ca.infrastructure.persistence.adapter.EmployeePersistenceAdapter;
import pe.edu.galaxy.training.ia.ca.infrastructure.persistence.mapper.EmployeePersistenceMapper;

/** Se omite automáticamente si Docker no está disponible. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({EmployeePersistenceAdapter.class, EmployeePersistenceMapper.class})
class EmployeePersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private EmployeePersistenceAdapter adapter;

    private static Employee employee(DocumentType type, String number) {
        return Employee.register(new DocumentNumber(type, number), new FullName("Ana", "Pérez"),
                new Email("ana@example.com"));
    }

    @Test
    void shouldPersistEmployeeAndFindItByDocumentWhenItIsNew() {
        // Given
        Employee employee = employee(DocumentType.DNI, "12345678");

        // When
        Employee saved = adapter.save(employee);

        // Then
        assertThat(saved.id()).isEqualTo(employee.id());
        assertThat(saved.documentNumber()).isEqualTo(employee.documentNumber());
        assertThat(adapter.existsByDocumentNumber(new DocumentNumber(DocumentType.DNI, "12345678"))).isTrue();
        assertThat(adapter.existsByDocumentNumber(new DocumentNumber(DocumentType.FOREIGNER_CARD, "12345678")))
                .isFalse();
    }

    @Test
    void shouldThrowDuplicateEmployeeDocumentWhenDocumentIsSavedTwice() {
        // Given
        adapter.save(employee(DocumentType.DNI, "87654321"));

        // When / Then
        assertThatThrownBy(() -> adapter.save(employee(DocumentType.DNI, "87654321")))
                .isInstanceOf(DuplicateEmployeeDocumentException.class);
    }

    @Test
    void shouldPersistChangesWhenExistingEmployeeIsUpdated() {
        // Given
        Employee saved = adapter.save(employee(DocumentType.DNI, "11223344"));
        Employee loaded = adapter.findById(saved.id()).orElseThrow();

        // When
        loaded.update(new FullName("Ana María", "Pérez Gómez"), new Email("ana.perez@example.com"));
        adapter.save(loaded);

        // Then
        Employee reloaded = adapter.findById(saved.id()).orElseThrow();
        assertThat(reloaded.fullName()).isEqualTo(new FullName("Ana María", "Pérez Gómez"));
        assertThat(reloaded.email()).isEqualTo(new Email("ana.perez@example.com"));
        assertThat(reloaded.documentNumber()).isEqualTo(saved.documentNumber());
    }

    @Test
    void shouldReturnEmptyWhenEmployeeIdDoesNotExist() {
        assertThat(adapter.findById(EmployeeId.generate())).isEmpty();
    }
}
