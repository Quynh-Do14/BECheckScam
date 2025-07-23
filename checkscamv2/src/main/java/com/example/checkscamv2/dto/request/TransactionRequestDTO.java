package com.example.checkscamv2.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequestDTO {

    // Thông tin giao dịch viên
    @NotBlank(message = "Tên giao dịch viên không được để trống")
    private String dealerName;

    @Email(message = "Email giao dịch viên không hợp lệ")
    @NotBlank(message = "Email giao dịch viên không được để trống")
    private String dealerEmail;

    // Thông tin bên A (người tạo giao dịch)
    @NotBlank(message = "Tên bên A không được để trống")
    private String partyAName;

    @Email(message = "Email bên A không hợp lệ")
    @NotBlank(message = "Email bên A không được để trống")
    private String partyAEmail;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại bên A không hợp lệ")
    private String partyAPhone;

    // Thông tin bên B
    @NotBlank(message = "Tên bên B không được để trống")
    private String partyBName;

    @Email(message = "Email bên B không hợp lệ")
    @NotBlank(message = "Email bên B không được để trống")
    private String partyBEmail;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại bên B không hợp lệ")
    private String partyBPhone;

    // Tên phòng
    @NotBlank(message = "Tên phòng không được để trống")
    private String roomName;


    private String transactionCode;



}
