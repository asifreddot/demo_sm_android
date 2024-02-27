package com.reddot.mvvmtodo.network

import com.reddot.mvvmtodo.model.SignUpResponse
import com.reddot.mvvmtodo.model.Success
import com.reddot.mvvmtodo.model.TaskCreate
import com.reddot.mvvmtodo.model.TaskModel
import com.reddot.mvvmtodo.utility.Constant
import retrofit2.Response
import retrofit2.http.*

interface ApiInterface {

    @FormUrlEncoded
    @POST("${Constant.API_VERSION_TAG}/auth/login")
    suspend fun verifyUserLogin(
        @Field("email") email: String,
        @Field("password") password: String,
        ): Response<Success>

    @FormUrlEncoded
    @POST("${Constant.API_VERSION_TAG}/auth/social")
    suspend fun verifySocialLogin(
        @Field("provider_id") providerId: String,
        @Field("name") name: String,
        @Field("email") email: String,
    ): Response<Success>

    @FormUrlEncoded
    @POST("${Constant.API_VERSION_TAG}/auth/register")
    suspend fun registerUser(
        @Field("name") name:String?,
        @Field("email") email:String?,
        @Field("password") password:String?
        //@Field("role") role:String
    ): Response<SignUpResponse>

    @GET("${Constant.API_VERSION_TAG}/task/list")
    suspend fun getAllTask(
        @Header("Authorization") authorization:String
    ): Response<TaskModel>

    @FormUrlEncoded
    @POST("${Constant.API_VERSION_TAG}/task/create")
    suspend fun taskCreate(
        @Header("Authorization") authorization:String,
        @Field("title") title: String?,
        @Field("description") description: String?,
        @Field("start_date") startDate: String?,
        //@Field("due_date") dueDate: String?,
        //@Field("priority") priority: String?,
        //@Field("assignee_user_id") assigneeUserId: Int?,
    ): Response<Success>

    @FormUrlEncoded
    @POST("${Constant.API_VERSION_TAG}/task/update")
    suspend fun taskUpdate(
        @Header("Authorization") authorization:String,
        @Field("id") id: Int?,
        @Field("title") title: String?,
        @Field("description") description: String?,
        @Field("start_date") startDate: String?,
        //@Field("due_date") dueDate: String?,
        //@Field("priority") priority: String?,
        //@Field("assignee_user_id") assigneeUserId: Int?,
    ): Response<Success>

    @FormUrlEncoded
    @POST("${Constant.API_VERSION_TAG}/task/batch-create")
    suspend fun batchTaskCreate(
        @Header("Authorization") authorization:String,
        @Body tasks: List<TaskCreate>,
    ): Response<Success>


    @GET("${Constant.API_VERSION_TAG}/task/delete/{id}")
    suspend fun taskDelete(
        @Header("Authorization") authorization:String,
        @Path("id") taskId: Int,
    ): Response<Success>

    @FormUrlEncoded
    @POST("${Constant.API_VERSION_TAG}/task/change-status")
    suspend fun taskChangeStatus(
        @Header("Authorization") authorization:String,
        @Field("id") taskId: Int,
        @Field("status") status: String

    ): Response<Success>





}