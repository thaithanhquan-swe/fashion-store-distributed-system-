package com.fashionstore.backend.mapper;

import com.fashionstore.backend.dto.request.CreateEmployeeRequest;
import com.fashionstore.backend.dto.request.UpdateEmployeeRequest;
import com.fashionstore.backend.dto.response.EmployeeResponse;
import com.fashionstore.backend.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "employeeId", source = "employeeId")
    @Mapping(target = "active", constant = "true")
    Employee toEntity(CreateEmployeeRequest request, String employeeId);

    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateEmployeeRequest request, @MappingTarget Employee employee);

    EmployeeResponse toResponse(Employee employee);
}
