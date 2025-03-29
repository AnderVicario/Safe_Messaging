package com.av19.utils;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;
    private static final String BASE_URL = "https://umbra.ddns.net:8000/";

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // Crear el interceptor para logging
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);  // Log completo

            // Obtener cliente inseguro + logging
            OkHttpClient okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient()
                    .newBuilder()
                    .addInterceptor(logging) // Agregar logging
                    .build();

            // Configurar Retrofit con el cliente HTTP modificado
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
