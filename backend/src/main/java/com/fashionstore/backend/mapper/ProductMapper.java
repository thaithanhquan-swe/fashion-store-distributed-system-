package com.fashionstore.backend.mapper;

import com.fashionstore.backend.dto.request.CreateProductRequest;
import com.fashionstore.backend.dto.request.UpdateProductRequest;
import com.fashionstore.backend.dto.response.ProductResponse;
import com.fashionstore.backend.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "active", constant = "true")
    Product toEntity(CreateProductRequest request, String productId);

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateProductRequest request, @MappingTarget Product product);

    ProductResponse toResponse(Product product);
}
