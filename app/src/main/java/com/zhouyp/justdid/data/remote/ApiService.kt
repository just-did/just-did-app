package com.zhouyp.justdid.data.remote

import retrofit2.http.GET

interface ApiService {
    @GET("ping")
    suspend fun ping(): String
}
