package pe.edu.galaxy.training.ia.ca.infrastructure.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.galaxy.training.ia.ca.domain.employee.vo.DocumentType;
import pe.edu.galaxy.training.ia.ca.infrastructure.persistence.entity.EmployeeJpaEntity;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, UUID> {

    boolean existsByDocumentTypeAndDocumentNumber(DocumentType documentType, String documentNumber);
}
