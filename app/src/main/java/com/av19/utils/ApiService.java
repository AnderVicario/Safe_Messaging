package com.av19.utils;

import com.av19.models.api.ApiResponse;
import com.av19.models.api.MessageCreate;
import com.av19.models.api.ProfilePictureResponse;
import com.av19.models.api.PublicKeyResponse;
import com.av19.models.api.RecieveMessageResponse;
import com.av19.models.api.SendMessageResponse;
import com.av19.models.api.UpdateProfilePicture;
import com.av19.models.api.UpdatePublicKey;
import com.av19.models.api.UserCreate;
import com.av19.models.api.UserLogin;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    // Endpoint para registrar un usuario
    @POST("users/register")
    Call<ApiResponse> registerUser(@Body UserCreate userCreateData);

    // Endpoint para iniciar sesión
    @POST("users/login")
    Call<ApiResponse> loginUser(@Body UserLogin userLoginData);

    // Endpoint para obtener la clave pública de un usuario
    @GET("users/get_key/{username}")
    Call<PublicKeyResponse> getPublicKey(@Path("username") String username);

    // Endpoint para obtener la foto de un usuario
    @GET("users/get_profile_picture/{username}")
    Call<ProfilePictureResponse> getProfilePicture(@Path("username") String username);

    // Endpoint para actualizar la clave pública
    @PUT("users/update_key")
    Call<ApiResponse> updatePublicKey(@Body UpdatePublicKey updatePublicKeyData);

    // Endpoint actualizar la imagen de perfil
    @PUT("users/update_profile_picture")
    Call<ApiResponse> updateProfilePicture(@Body UpdateProfilePicture updateProfilePictureData);

    // Endpoint para enviar un mensaje
    @POST("messages/send_message")
    Call<SendMessageResponse> sendMessage(@Body MessageCreate messageCreateData);

    // Endpoint para recibir mensajes de un usuario
    @GET("messages/receive_messages/{receiver}")
    Call<List<RecieveMessageResponse>> getMessages(@Path("receiver") String receiver);
}