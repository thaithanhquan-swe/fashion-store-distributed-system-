package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateEmployeeRequest;
import com.fashionstore.backend.dto.response.EmployeeResponse;
import com.fashionstore.backend.entity.Employee;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.mapper.EmployeeMapper;
import com.fashionstore.backend.repository.BranchRepository;
import com.fashionstore.backend.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeService {

    EmployeeRepository employeeRepository;
    BranchRepository branchRepository;
    EmployeeMapper employeeMapper;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EmployeeResponse create(CreateEmployeeRequest request) {
        String employeeId = GeneratedId.next("NV", employeeRepository.findAllByEmployeeIdStartingWith("NV")
                .stream().map(Employee::getEmployeeId).toList(), ErrorCode.EMPLOYEE_ID_CAPACITY_EXCEEDED);
        if (!branchRepository.existsById(request.branchId())) {
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        }

        Employee employee = employeeMapper.toEntity(request, employeeId);
        Employee savedEmployee = employeeRepository.save(employee);
        return employeeMapper.toResponse(savedEmployee);
    }
}
