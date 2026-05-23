package com.frontend.petfinder.core.network

import com.frontend.petfinder.auth.data.AuthApi
import com.frontend.petfinder.geofencing.data.GeofencingApi
import com.frontend.petfinder.pets.data.PetApi
import com.frontend.petfinder.profile.data.UserApi
import com.frontend.petfinder.sightings.data.SightingsApi

object ApiServices {
    val auth: AuthApi by lazy { RetrofitClient.instance.create(AuthApi::class.java) }
    val pets: PetApi by lazy { RetrofitClient.instance.create(PetApi::class.java) }
    val geo: GeofencingApi by lazy { RetrofitClient.instance.create(GeofencingApi::class.java) }
    val user: UserApi by lazy { RetrofitClient.instance.create(UserApi::class.java) }
    val sightings: SightingsApi by lazy { RetrofitClient.instance.create(SightingsApi::class.java) }
}
