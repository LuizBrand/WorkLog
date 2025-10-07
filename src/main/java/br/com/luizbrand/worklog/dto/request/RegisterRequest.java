package br.com.luizbrand.worklog.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(@NotBlank(message = "O email não pode estar em branco.")
                              @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres.")
                              String name,
                              @Email(message = "O email deve ser válido.")
                              @NotBlank(message = "O email não pode estar em branco.")
                              String email,
                              @NotBlank(message = "A senha não pode estar em branca.")
                              @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
                              @Pattern(
                                      regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                                      message = "A senha deve conter pelo menos uma letra maiúscula, uma minúscula e um número."
                              )
                              String password
) {
}
