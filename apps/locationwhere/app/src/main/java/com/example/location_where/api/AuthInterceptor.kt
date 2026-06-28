package com.example.location_where.api

import com.example.location_where.data.RefreshRequest
import com.example.location_where.utils.TokenManager
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val apiService: Lazy<ApiService>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = runBlocking { tokenManager.getAccessToken() }
        val originalRequest = chain.request()
        
        val requestWithToken = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        val response = chain.proceed(requestWithToken)

        if (response.code == 401) {
            val refreshToken = runBlocking { tokenManager.getRefreshToken() }
            if (refreshToken != null) {
                synchronized(this) {
                    val currentToken = runBlocking { tokenManager.getAccessToken() }
                    // If token changed while waiting, just retry with new token
                    val nextToken = if (currentToken != accessToken) {
                        currentToken
                    } else {
                        runBlocking {
                            val refreshResponse = apiService.get().refreshToken(RefreshRequest(refreshToken))
                            if (refreshResponse.isSuccessful) {
                                val refreshedToken = refreshResponse.body()?.data?.accessToken
                                if (refreshedToken != null) {
                                    tokenManager.saveTokens(refreshedToken, refreshToken)
                                }
                                refreshedToken
                            } else {
                                null
                            }
                        }
                    }

                    if (nextToken != null) {
                        response.close()
                        val retryRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $nextToken")
                            .build()
                        return chain.proceed(retryRequest)
                    }
                }
            }
        }

        return response
    }
}
