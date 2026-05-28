package com.example.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    /**
     * Fetches electrical load telemetry data from a Google Apps Script Web App JSON API.
     * Uses dynamic @Url to allow full user configuration in Settings.
     */
    @GET
    suspend fun fetchLoadData(@Url url: String): Response<ResponseBody>
}
