package com.fashionstore.backend.mapper;

import com.fashionstore.backend.dto.request.CreateCustomerRequest;
import com.fashionstore.backend.dto.request.UpdateCustomerRequest;
import com.fashionstore.backend.dto.response.CustomerResponse;
import com.fashionstore.backend.entity.Customer;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "registeredDate", source = "registeredDate")
    @Mapping(target = "loyaltyPoints", constant = "0")
    @Mapping(target = "active", constant = "true")
    Customer toEntity(CreateCustomerRequest request, String customerId, LocalDate registeredDate);

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true)
    @Mapping(target = "registeredDate", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateCustomerRequest request, @MappingTarget Customer customer);

    CustomerResponse toResponse(Customer customer);
}
