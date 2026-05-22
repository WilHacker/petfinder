package com.frontend.petfinder.core.network

import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject

class PrismaErrorInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            val responseBodyString = response.peekBody(Long.MAX_VALUE).string()

            try {
                val jsonObject = JSONObject(responseBodyString)
                val statusCode = jsonObject.optInt("statusCode", response.code)
                val message = jsonObject.optString("message", "")

                when (statusCode) {
                    409 -> {
                        if (message.contains("P2002")) {
                            throw PrismaException("El correo electrónico o CI ya está registrado.", "P2002")
                        }
                        if (message.contains("P2014")) {
                            throw PrismaException("La relación que intentas crear ya existe.", "P2014")
                        }
                    }
                    400 -> {
                        if (message.contains("P2003")) {
                            throw PrismaException("Referencia inválida. Verifica los datos enviados.", "P2003")
                        }
                    }
                    404 -> {
                        if (message.contains("P2025")) {
                            throw PrismaException("El registro solicitado no fue encontrado.", "P2025")
                        }
                    }
                }
            } catch (e: org.json.JSONException) {
                android.util.Log.d("PrismaErrorInterceptor", "Response no es JSON Prisma: ${e.message}")
            }
        }
        return response
    }
}