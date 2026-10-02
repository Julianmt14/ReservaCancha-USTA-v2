package co.edu.usta.reservacancha.auth;

import co.edu.usta.reservacancha.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank String nombre,
    @Email @NotBlank String email,
    @NotBlank String password,
    Role rol) {}
