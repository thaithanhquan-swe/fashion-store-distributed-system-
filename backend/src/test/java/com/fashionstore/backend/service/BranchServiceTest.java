package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateBranchRequest;
import com.fashionstore.backend.dto.response.BranchResponse;
import com.fashionstore.backend.entity.Branch;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.mapper.BranchMapper;
import com.fashionstore.backend.repository.BranchRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    BranchRepository branchRepository;

    BranchMapper branchMapper = Mappers.getMapper(BranchMapper.class);

    BranchService branchService;

    Branch branch1;
    Branch branch2;

    @BeforeEach
    void setUp() {
        branchService = new BranchService(branchRepository, branchMapper);

        branch1 = Branch.builder()
                .branchId("CN01")
                .branchName("Chi nhánh Quận 1")
                .address("123 Le Loi, Q1, TP.HCM")
                .phoneNumber("0901111111")
                .active(true)
                .build();

        branch2 = Branch.builder()
                .branchId("CN02")
                .branchName("Chi nhánh Quận 7")
                .address("456 Nguyen Thi Thap, Q7, TP.HCM")
                .phoneNumber("0902222222")
                .active(false)
                .build();
    }

    @Test
    void getAll_nullActive_returnsAllBranches() {
        when(branchRepository.findAll(any(Sort.class))).thenReturn(List.of(branch1, branch2));

        List<BranchResponse> responses = branchService.getAll(null);

        assertEquals(2, responses.size());
        assertEquals("CN01", responses.get(0).branchId());
        assertEquals("CN02", responses.get(1).branchId());
        verify(branchRepository).findAll(any(Sort.class));
        verify(branchRepository, never()).findAllByActiveOrderByBranchIdAsc(anyBoolean());
    }

    @Test
    void getAll_activeTrue_returnsActiveOnly() {
        when(branchRepository.findAllByActiveOrderByBranchIdAsc(true)).thenReturn(List.of(branch1));

        List<BranchResponse> responses = branchService.getAll(true);

        assertEquals(1, responses.size());
        assertEquals("CN01", responses.get(0).branchId());
        verify(branchRepository).findAllByActiveOrderByBranchIdAsc(true);
        verify(branchRepository, never()).findAll(any(Sort.class));
    }

    @Test
    void getAll_activeFalse_returnsInactiveOnly() {
        when(branchRepository.findAllByActiveOrderByBranchIdAsc(false)).thenReturn(List.of(branch2));

        List<BranchResponse> responses = branchService.getAll(false);

        assertEquals(1, responses.size());
        assertEquals("CN02", responses.get(0).branchId());
        verify(branchRepository).findAllByActiveOrderByBranchIdAsc(false);
    }

    @Test
    void getById_success() {
        when(branchRepository.findById("CN01")).thenReturn(Optional.of(branch1));

        BranchResponse response = branchService.getById("CN01");

        assertNotNull(response);
        assertEquals("CN01", response.branchId());
        assertEquals("Chi nhánh Quận 1", response.branchName());
        assertEquals("123 Le Loi, Q1, TP.HCM", response.address());
    }

    @Test
    void getById_nullOrBlank_throwsBranchNotFound() {
        AppException ex1 = assertThrows(AppException.class, () -> branchService.getById(null));
        assertEquals(ErrorCode.BRANCH_NOT_FOUND, ex1.getErrorCode());

        AppException ex2 = assertThrows(AppException.class, () -> branchService.getById("   "));
        assertEquals(ErrorCode.BRANCH_NOT_FOUND, ex2.getErrorCode());
    }

    @Test
    void getById_notFound_throwsBranchNotFound() {
        when(branchRepository.findById("CN99")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> branchService.getById("CN99"));
        assertEquals(ErrorCode.BRANCH_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_success() {
        CreateBranchRequest request = new CreateBranchRequest(
                "Chi nhánh Thủ Đức",
                "789 Vo Van Ngan, TP. Thu Duc",
                "0903333333");

        when(branchRepository.findAllByBranchIdStartingWith("CN")).thenReturn(List.of(branch1, branch2));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchResponse response = branchService.create(request);

        assertNotNull(response);
        assertEquals("CN003", response.branchId());
        assertEquals("Chi nhánh Thủ Đức", response.branchName());
        assertTrue(response.active());
    }
}
