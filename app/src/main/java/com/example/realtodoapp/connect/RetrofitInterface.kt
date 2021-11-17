package com.example.realtodoapp.connect

import retrofit2.Call
import retrofit2.http.*

import com.google.gson.JsonElement

interface RetrofitInterface {
    @GET("/getAllFeeds")
    fun getAllFeeds(): Call<JsonElement>

    @GET("/uploadFeed")
    fun uploadFeed(@Query("uploader") uploader: String, @Query("title") title: String,
                   @Query("contents") contents: String): Call<JsonElement>
}

