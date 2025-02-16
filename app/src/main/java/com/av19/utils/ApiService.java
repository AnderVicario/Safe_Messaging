package com.av19.utils;

import com.av19.models.api.ApiResponse;
import com.av19.models.api.MessageCreate;
import com.av19.models.api.MessageResponse;
import com.av19.models.api.PublicKeyResponse;
import com.av19.models.api.UpdatePublicKey;
import com.av19.models.api.UserCreate;
import com.av19.models.api.UserLogin;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import java.util.List;

public interface ApiService {

    // Endpoint para registrar un usuario
    @POST("register")
    Call<ApiResponse> registerUser(@Body UserCreate userCreateData);

    // Endpoint para iniciar sesión
    @POST("login")
    Call<ApiResponse> loginUser(@Body UserLogin userLoginData);

    // Endpoint para obtener la clave pública de un usuario
    @GET("get_key/{username}")
    Call<PublicKeyResponse> getPublicKey(@Path("username") String username);

    // Endpoint para actualizar la clave pública
    @PUT("update_key")
    Call<ApiResponse> updatePublicKey(@Body UpdatePublicKey updatePublicKeyData);

    // Endpoint para enviar un mensaje
    @POST("send_message")
    Call<ApiResponse> sendMessage(@Body MessageCreate messageCreateData);

    // Endpoint para recibir mensajes de un usuario
    @GET("receive_messages/{receiver}")
    Call<List<MessageResponse>> getMessages(@Path("receiver") String receiver);
}