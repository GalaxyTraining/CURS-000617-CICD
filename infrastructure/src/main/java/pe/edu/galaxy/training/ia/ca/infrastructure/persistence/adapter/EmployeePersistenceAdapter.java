package pe.edu.galaxy.training.ia.ca.infrastructure.persistence.adapter;

import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import pe.edu.galaxy.training.ia.ca.application.exception.DuplicateEmployeeDocumentException;
import pe.edu.galaxy.training.ia.ca.application.port.out.EmployeeRepositoryPort;
import pe.edu.galaxy.training.ia.ca.domain.employee.model.Employee;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.DocumentNumber;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.EmployeeId;
import pe.edu.galaxy.training.ia.ca.infrastructure.persistence.mapper.EmployeePersistenceMapper;
import pe.edu.galaxy.training.ia.ca.infrastructure.persistence.repository.EmployeeJpaRepository;

@Component
public class EmployeePersistenceAdapter implements EmployeeRepositoryPort {

    static final String UNIQUE_DOCUMENT_CONSTRAINT = "uk_employees_document";

    private final EmployeeJpaRepository repository;
    private final EmployeePersistenceMapper mapper;

    public EmployeePersistenceAdapter(EmployeeJpaRepository repository, EmployeePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean existsByDocumentNumber(DocumentNumber documentNumber) {
        return repository.existsByDocumentTypeAndDocumentNumber(documentNumber.type(), documentNumber.value());
    }

    @Override
    public Optional<Employee> findById(EmployeeId employeeId) {
        return repository.findById(employeeId.value()).map(mapper::toDomain);
    }

    @Override
    public Employee save(Employee employee) {
        try {
            return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(employee)));
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateDocument(ex)) {
                throw new DuplicateEmployeeDocumentException(employee.documentNumber());
            }
            throw ex;
        }
    }

    private static boolean isDuplicateDocument(DataIntegrityViolationException ex) {
        return ex.getCause() instanceof ConstraintViolationException violation
                && UNIQUE_DOCUMENT_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName());
    }
}
