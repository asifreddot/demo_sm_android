package com.reddot.mvvmtodo.data

import android.provider.SyncStateContract.Constants
import androidx.room.*
import com.reddot.mvvmtodo.utility.Constant
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    fun getTasks(query: String,sortOrder: SortOrder,hideComplete: Boolean) : Flow<List<Task>> =
        when(sortOrder){
            SortOrder.BY_DATE -> {
                getTasksSortedByDate(query,hideComplete)
            }
            SortOrder.BY_NAME -> {
                getTasksSortedByName(query,hideComplete)
            }
        }

    @Query("SELECT * FROM task_table  WHERE (status != ${Constant.DELETED_NOT_SYN}) AND (completed != :hideComplete OR completed = 0) AND name LIKE '%' || :searchQuery || '%' ORDER BY important DESC , name")
    fun getTasksSortedByName(searchQuery: String, hideComplete: Boolean): Flow<List<Task>>

    @Query("SELECT * FROM task_table WHERE (status != ${Constant.DELETED_NOT_SYN}) AND (completed != :hideComplete OR completed = 0) AND name LIKE '%' || :searchQuery || '%' ORDER BY important DESC , created")
    fun getTasksSortedByDate(searchQuery: String, hideComplete: Boolean): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM task_table WHERE completed = 1")
    suspend fun deleteCompletedTasks()
}