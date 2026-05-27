package com.musicapp.network;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    // Для Android Emulator: localhost = 10.0.2.2
    // Для реального устройства: укажи IP компьютера в локальной сети
    public static final String BASE_URL = "http://10.0.2.2:8000/";

    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            retrofit = buildRetrofit(context);
            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }

    private static Retrofit buildRetrofit(Context context) {
        // Logging interceptor (DEBUG only)
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Auth interceptor — добавляет Bearer токен в каждый запрос
        Interceptor authInterceptor = chain -> {
            SharedPreferences prefs = context.getSharedPreferences("MusicAppPrefs", Context.MODE_PRIVATE);
            String token = prefs.getString("access_token", null);

            Request original = chain.request();
            if (token != null && !original.url().encodedPath().contains("/auth/")) {
                Request request = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(request);
            }
            return chain.proceed(original);
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static void reset() {
        retrofit = null;
        apiService = null;
    }
}
