package com.fashionstore.backend.mapper;

import com.fashionstore.backend.dto.request.UpdateInventoryRequest;
import com.fashionstore.backend.dto.response.InventoryResponse;
import com.fashionstore.backend.entity.Inventory;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "lowStock", expression = "java(inventory.getQuantity() <= inventory.getMinimumStock())")
    InventoryResponse toResponse(Inventory inventory);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "branchId", ignore = true)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateInventoryRequest request, @MappingTarget Inventory inventory);
}
