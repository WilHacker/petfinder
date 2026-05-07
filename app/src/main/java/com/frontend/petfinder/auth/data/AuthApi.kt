package com.frontend.petfinder.auth.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// --- OBJETOS DE TRANSFERENCIA DE DATOS (DTOs) ---

data class MedioContactoDto(
    val tipo: String,
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

data class UsuarioDto(
    val usuarioId: String,
    val correoElectronico: String,
    val nombre: String,
    val apellidoPaterno: String
)

data class LoginResponse(
    val accessToken: String,
    val usuario: UsuarioDto
)

// --- INTERFAZ DE LA API ---

interface AuthApi {

    @POST("auth/register")
    suspend fun registerOwner(
        @Body request: RegisterRequest
    ): Response<Unit>

    @POST("auth/login")
    suspend fun loginOwner(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}