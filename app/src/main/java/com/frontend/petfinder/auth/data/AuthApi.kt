package com.frontend.petfinder.auth.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// =============================================================================
// DTOs de petición
// =============================================================================

data class MedioContactoDto(
    val tipo: String,  // WhatsApp | Celular | Fijo | Telegram
    val valor: String
)

data class RegisterRequest(
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val ci: String,
    val correoElectronico: String,
    val clave: String,
    val medioContacto: MedioContactoDto
)

data class LoginRequest(
    val correoElectronico: String,
    val clave: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

// =============================================================================
// DTOs de respuesta
// =============================================================================

data class UsuarioDto(
    val usuarioId: String,
    val correoElectronico: String,
    val nombre: String,
    val apellidoPaterno: String,
    val rol: String  // "usuario" | "admin"
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val usuario: UsuarioDto
)

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)

// =============================================================================
// Interfaz de la API
// =============================================================================

interface AuthApi {

    @POST("auth/register")
    suspend fun registerOwner(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun loginOwner(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<RefreshTokenResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Map<String, String>>
}
