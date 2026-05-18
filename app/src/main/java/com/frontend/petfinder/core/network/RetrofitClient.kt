package com.frontend.petfinder.core.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Apuntamos al servidor de Render que tiene habilitado WebSockets y base de datos
    private const val BASE_URL = "https://backend-petfinder.onrender.com/"

    val authInterceptor = AuthInterceptor()
    val prismaErrorInterceptor = PrismaErrorInterceptor() // Instanciamos el nuevo interceptor

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(prismaErrorInterceptor) // Lo agregamos a la cadena de OkHttp
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}