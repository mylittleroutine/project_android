package com.example.realtodoapp.util

import android.util.Log
import com.example.realtodoapp.connect.RetrofitInterface
import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitUtil {
    companion object {
        val url = "http://10.0.2.2:3000"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        var server = retrofit.create(RetrofitInterface::class.java) // 만들어둔 interface와 연결

        fun getAllFeeds(){
            server.getAllFeeds().enqueue(object: Callback<JsonElement> {
                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    Log.d("getAllFeedsFail : ", "Fail")
                }
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    Log.d("getAllFeeds : ", response.body().toString())
                }
            })
        }
    }
}
