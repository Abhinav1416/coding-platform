package com.Abhinav.backend.features.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequestBody {
    @NotBlank(message = "Email is mandatory")
    private String email;
    @Setter
    @NotBlank(message = "Password is mandatory")
    private String password;

}