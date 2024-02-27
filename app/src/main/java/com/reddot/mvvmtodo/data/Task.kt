package com.reddot.mvvmtodo.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.text.DateFormat

@Entity(tableName = "task_table")
@Parcelize
data class Task(
    val name: String?,
    val description: String?,
    val important: Boolean = false,
    val completed: Boolean = false,
    var is_syn: Boolean = false,
    var status: Int = 0,    ///1 for syn 2 update but_not syn, 3 delete
    val created: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var server_task_id: Int = 0
) : Parcelable {
    val createdDatedFormatted: String
        get() = DateFormat.getDateInstance().format(created)
}