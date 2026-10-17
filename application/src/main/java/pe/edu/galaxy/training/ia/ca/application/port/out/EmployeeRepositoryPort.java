package pe.edu.galaxy.training.ia.ca.application.port.out;

import java.util.Optional;
import pe.edu.galaxy.training.ia.ca.domain.employee.model.Employee;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.DocumentNumber;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.EmployeeId;

public interface EmployeeRepositoryPort {

    boolean existsByDocumentNumber(DocumentNumber documentNumber);

    Optional<Employee> findById(EmployeeId employeeId);

    /**
     * Persiste el empleado.
     *
     * @throws pe.edu.galaxy.training.ia.ca.application.exception.DuplicateEmployeeDocumentException
     *         si otro registro con el mismo documento se guardó concurrentemente
     */
    Employee save(Employee employee);
}
