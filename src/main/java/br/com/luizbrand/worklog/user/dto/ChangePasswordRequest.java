package br.com.luizbrand.worklog.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank(message = "A senha não pode estar em branca.")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "A senha deve conter pelo menos uma letra maiúscula, uma minúscula e um número."
        )
        String newPassword,
        @NotBlank String refreshToken) {
}
