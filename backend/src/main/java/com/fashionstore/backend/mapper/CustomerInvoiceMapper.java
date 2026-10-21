package com.fashionstore.backend.mapper;

import com.fashionstore.backend.dto.response.CustomerInvoiceResponse;
import com.fashionstore.backend.entity.Invoice;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerInvoiceMapper {
    CustomerInvoiceResponse toResponse(Invoice invoice);
}
