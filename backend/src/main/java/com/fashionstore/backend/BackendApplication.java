package com.fashionstore.backend;

import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    @Profile("local")
    CommandLineRunner createInitialAdmin(
            AccountRepository accountRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            @Value("${bootstrap.admin.employee-id:NV00000001}") String employeeId) {
        return args -> {
            var existingAdmin = accountRepository.findByUsername("admin");
            if (existingAdmin.isPresent()) {
                if (existingAdmin.get().getRole() != Role.ADMIN) {
                    throw new IllegalStateException(
                            "Tài khoản 'admin' đã tồn tại nhưng chưa có vai trò ADMIN.");
                }
                return;
            }

            if (!employeeRepository.existsById(employeeId)) {
                throw new IllegalStateException("Không tìm thấy nhân viên " + employeeId
                        + " để gắn tài khoản admin. Cấu hình bootstrap.admin.employee-id "
                        + "thành mã nhân viên đã tồn tại.");
            }

            if (accountRepository.existsByEmployeeId(employeeId)) {
                throw new IllegalStateException("Nhân viên " + employeeId
                        + " đã có tài khoản; không thể tạo thêm tài khoản admin cho nhân viên này.");
            }

            accountRepository.save(Account.builder()
                    .employeeId(employeeId)
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .role(Role.ADMIN)
                    .build());
        };
    }
}
