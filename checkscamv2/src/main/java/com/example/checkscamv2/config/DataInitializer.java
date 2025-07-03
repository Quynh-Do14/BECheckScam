package com.example.checkscamv2.config;

import com.example.checkscamv2.constant.RoleName;
import com.example.checkscamv2.entity.Role;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.repository.RoleRepository;
import com.example.checkscamv2.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.Optional; // <-- THÊM IMPORT NÀY

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @PostConstruct
    public void init() {

        // Tạo các role nếu chưa tồn tại (USER, ADMIN, COLLAB)
        if (roleRepository.count() == 0) {
            Role userRole = Role.builder().name(RoleName.USER).build(); // Thêm vai trò USER
            Role adminRole = Role.builder().name(RoleName.ADMIN).build();
            Role collabRole = Role.builder().name(RoleName.COLLAB).build();

            roleRepository.save(userRole); // Lưu USER role
            roleRepository.save(adminRole);
            roleRepository.save(collabRole);
            System.out.println("Đã tạo các role USER, ADMIN và COLLAB.");
        }

        // Tạo user admin và collab mặc định nếu chưa tồn tại
        // Sẽ chỉ tạo nếu KHÔNG có user nào trong DB
        if (userRepository.count() == 0) {
            // Lấy role ADMIN
            // Đảm bảo findByName trả về Optional và xử lý nó
            Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: ADMIN role not found in DB!")); // <-- SỬA Ở ĐÂY

            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);

            User adminUser = new User();
            adminUser.setName("Admin User");
            adminUser.setEmail("admin@gmail.com");
            adminUser.setPassword(passwordEncoder.encode("123456")); // Thay đổi password mặc định
            adminUser.setRoles(adminRoles);
            userRepository.save(adminUser);
            System.out.println("Đã tạo tài khoản ADMIN mặc định.");

            // Lấy role COLLAB
            // Đảm bảo findByName trả về Optional và xử lý nó
            Role collabRole = roleRepository.findByName(RoleName.COLLAB)
                    .orElseThrow(() -> new RuntimeException("Error: COLLAB role not found in DB!")); // <-- SỬA Ở ĐÂY

            Set<Role> collabRoles = new HashSet<>();
            collabRoles.add(collabRole);

            User collabUser = new User();
            collabUser.setName("Collab User");
            collabUser.setEmail("collab@gmail.com");
            collabUser.setPassword(passwordEncoder.encode("123456")); // Thay đổi password mặc định
            collabUser.setRoles(collabRoles);
            userRepository.save(collabUser);
            System.out.println("Đã tạo tài khoản COLLAB mặc định.");
        }
    }
}