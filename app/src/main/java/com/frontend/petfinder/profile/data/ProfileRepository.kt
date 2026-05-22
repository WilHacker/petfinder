package com.frontend.petfinder.profile.data

import com.frontend.petfinder.core.network.ApiServices
import com.frontend.petfinder.profile.data.dto.*
import okhttp3.MultipartBody
import retrofit2.HttpException

object ProfileRepository {

    suspend fun getMyProfile(): Result<UserProfileDto> = runCatching {
        val r = ApiServices.user.getMyProfile()
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Result<Unit> = runCatching {
        val r = ApiServices.user.updateProfile(request)
        if (r.isSuccessful) Unit else throw HttpException(r)
    }

    suspend fun updateProfilePhoto(part: MultipartBody.Part): Result<Unit> = runCatching {
        val r = ApiServices.user.updateProfilePhoto(part)
        if (r.isSuccessful) Unit else throw HttpException(r)
    }

    suspend fun updateFcmToken(request: FcmTokenRequest): Result<Unit> = runCatching {
        val r = ApiServices.user.updateFcmToken(request)
        if (r.isSuccessful) Unit else throw HttpException(r)
    }

    suspend fun updateLocation(lat: Double, lng: Double): Result<Unit> = runCatching {
        val r = ApiServices.user.updateLocation(LocationRequest(lat, lng))
        if (r.isSuccessful) Unit else throw HttpException(r)
    }
}
