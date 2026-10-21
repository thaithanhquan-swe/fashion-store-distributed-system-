package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateProductRequest;
import com.fashionstore.backend.dto.request.UpdateProductRequest;
import com.fashionstore.backend.dto.request.UpdateProductStatusRequest;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.dto.response.ProductResponse;
import com.fashionstore.backend.entity.Product;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.mapper.ProductMapper;
import com.fashionstore.backend.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {

    ProductRepository productRepository;
    ProductMapper productMapper;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(
            String keyword,
            String category,
            String brand,
            String color,
            String size,
            Boolean active,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new AppException(ErrorCode.INVALID_PRODUCT_PRICE_RANGE);
        }

        Page<ProductResponse> products = productRepository.search(
                        normalize(keyword), normalize(category), normalize(brand), normalize(color),
                        normalize(size), active, minPrice, maxPrice, pageable)
                .map(productMapper::toResponse);
        return PageResponse.of(products);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(String productId) {
        return productMapper.toResponse(findProduct(productId));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Integer highestNumber = productRepository.findHighestGeneratedProductNumber();
        int nextNumber = (highestNumber == null ? 0 : highestNumber) + 1;
        if (nextNumber > 99_999_999) {
            throw new AppException(ErrorCode.PRODUCT_ID_CAPACITY_EXCEEDED);
        }
        String productId = String.format(Locale.ROOT, "SP%08d", nextNumber);

        Product product = productMapper.toEntity(request, productId);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(String productId, UpdateProductRequest request) {
        Product product = findProduct(productId);
        productMapper.updateEntity(request, product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStatus(String productId, UpdateProductStatusRequest request) {
        Product product = findProduct(productId);
        product.setActive(request.active());
        return productMapper.toResponse(productRepository.save(product));
    }

    private Product findProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
