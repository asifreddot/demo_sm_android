package com.reddot.mvvmtodo.repository

import com.reddot.mvvmtodo.data.PreferencesManager
import com.reddot.mvvmtodo.model.SignUpResponse
import com.reddot.mvvmtodo.model.Success
import com.reddot.mvvmtodo.model.TaskCreate
import com.reddot.mvvmtodo.model.TaskModel
import com.reddot.mvvmtodo.network.ApiInterface
import com.reddot.mvvmtodo.ui.Result
import com.reddot.mvvmtodo.ui.baseClass.BaseDataSource
import com.reddot.mvvmtodo.ui.signUp.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DataSourceRepository @Inject constructor(
    private val apiInterface: ApiInterface,
    private val preferencesManager: PreferencesManager
) :
    BaseDataSource() {

    fun verifyUserLogin(email: String, password: String): Flow<Result<Success>> =
        flow {
            emit(Result.loading())
            val result =
                getResult { apiInterface.verifyUserLogin(email, password) }
            emit(result)
        }.flowOn(Dispatchers.IO)

    fun verifyUserSocialLogin(providerId:String, name:String, email: String): Flow<Result<Success>> =
        flow {
            emit(Result.loading())
            val result =
                getResult { apiInterface.verifySocialLogin(providerId, name,email) }
            emit(result)
        }.flowOn(Dispatchers.IO)

    fun registerUser(user: User): Flow<Result<SignUpResponse>> =
        flow {
            emit(Result.loading())
            val result =
                getResult {
                    apiInterface.registerUser(user.name,user.email,user.password)
                }
            emit(result)
        }.flowOn(Dispatchers.IO)

    fun fetchAllTask(): Flow<Result<TaskModel>> =
        flow {
            emit(Result.loading())
            val accessToken = preferencesManager.getAccessToken()
            if (!accessToken.isNullOrEmpty()) {
                val result =
                    getResult { apiInterface.getAllTask(accessToken) }
                emit(result)
            }
        }.flowOn(Dispatchers.IO)

    fun createTask(task: TaskCreate): Flow<Result<Success>> =
        flow {
            emit(Result.loading())
            val accessToken = preferencesManager.getAccessToken()
            if (!accessToken.isNullOrEmpty()) {
                val result =
                    getResult {
                        apiInterface.taskCreate(
                            accessToken,
                            task.title,
                            task.description,
                            task.startDate
                        )
                    }
                emit(result)
            }
        }.flowOn(Dispatchers.IO)

    fun updateTask(task: TaskCreate, id: Int): Flow<Result<Success>> =
        flow {
            emit(Result.loading())

            val accessToken = preferencesManager.getAccessToken()
            if (!accessToken.isNullOrEmpty()) {
                val result =
                    getResult {
                        apiInterface.taskUpdate(
                            accessToken,
                            id,
                            task.title,
                            task.description,
                            task.startDate
                        )
                    }
                emit(result)
            }
        }.flowOn(Dispatchers.IO)

//    fun createBatchTask(tasks: List<TaskCreate>): Flow<Result<Success>> =
//        flow {
//            emit(Result.loading())
//            val accessToken = preferencesManager.getAccessToken()
//            if(!accessToken.isNullOrEmpty()) {
//                val result =
//                    getResult { apiInterface.batchTaskCreate(accessToken, tasks) }
//                emit(result)
//            }
//        }.flowOn(Dispatchers.IO)

    fun deleteTask(taskId: Int): Flow<Result<Success>> =
        flow {
            emit(Result.loading())
            val accessToken = preferencesManager.getAccessToken()
            if (!accessToken.isNullOrEmpty()) {
                val result =
                    getResult {
                        //apiInterface.taskDelete(accessToken, taskId)
                        apiInterface.taskChangeStatus(accessToken, taskId, "delete")
                    }
                emit(result)
            }
        }.flowOn(Dispatchers.IO)

}