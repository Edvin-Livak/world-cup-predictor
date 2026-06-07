package com.sned.worldcuppredictor.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiFootballClient {
    val api: ApiFootballApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.football-data.org/v4/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiFootballApi::class.java)
    }
}