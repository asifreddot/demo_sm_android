package com.reddot.mvvmtodo.model

import com.google.gson.annotations.SerializedName

data class TaskCreate(
    var serverTaskId:Int = 0,
    var title: String? = "",
    var description: String? = "",
    @SerializedName("start_date")
    var startDate: String? = "",
    @SerializedName("due_date")
    var dueDate: String? = "",
    var priority: String? = "",
    @SerializedName("assignee_user_id")
    var assigneeUserId: Int? = 0
)