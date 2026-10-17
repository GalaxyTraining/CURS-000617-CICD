package pe.edu.galaxy.training.ia.ca.infrastructure.api.rest;

import java.util.UUID;
import org.mapstruct.Mapper;
import pe.edu.galaxy.training.ia.ca.application.dto.EmployeeResult;
import pe.edu.galaxy.training.ia.ca.application.dto.RegisterEmployeeCommand;
import pe.edu.galaxy.training.ia.ca.application.dto.UpdateEmployeeCommand;
import pe.edu.galaxy.training.ia.ca.infrastructure.api.dto.EmployeeRequest;
import pe.edu.galaxy.training.ia.ca.infrastructure.api.dto.EmployeeResponse;
import pe.edu.galaxy.training.ia.ca.infrastructure.api.dto.UpdateEmployeeRequest;

@Mapper
public interface EmployeeApiMapper {

    RegisterEmployeeCommand toCommand(EmployeeRequest request);

    UpdateEmployeeCommand toCommand(UUID employeeId, UpdateEmployeeRequest request);

    EmployeeResponse toResponse(EmployeeResult result);
}
