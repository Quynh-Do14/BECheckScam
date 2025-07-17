package com.example.checkscamv2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterDTO {
    @NotBlank(message = "Tên không được để trống")
    @Size(min = 5, max = 20, message = "Tên chỉ được phép có từ 5 đến 20 ký tự")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Tên không được chứa số hoặc ký tự đặc biệt (chỉ chấp nhận chữ cái và dấu cách).")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;
}