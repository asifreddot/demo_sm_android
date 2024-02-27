package com.reddot.mvvmtodo.mapper


import com.reddot.mvvmtodo.data.Task as TaskEntity
import com.reddot.mvvmtodo.model.Task as TaskDto
import com.reddot.mvvmtodo.model.TaskCreate
import com.reddot.mvvmtodo.utility.DateTimeUtils

class Mapper {
    companion object {

        fun convertTaskDataFromTaskModel(taskDto: TaskDto):TaskCreate {
            val task = TaskCreate()
            task.serverTaskId = taskDto.id
            task.title = taskDto.title
            task.description = taskDto.description
            task.startDate = taskDto.created_at
            //task.dueDate = taskDto.due_date
            //task.priority = taskDto.priority
            //task.assigneeUserId = taskDto.assignee_user_id

            return task
        }

        fun convertTaskDataListFromTaskModel(taskDtos:List<TaskDto>): List<TaskCreate> {
            val tasks = ArrayList<TaskCreate>()
            for(taskDto in taskDtos) {
                tasks.add(convertTaskDataFromTaskModel(taskDto))
            }

            return tasks
        }

        // for sending local data to server
        fun convertTaskEntityToDto(taskEntity: TaskEntity): TaskCreate {
            val taskDto = TaskCreate()
            //taskDto.id = taskEntity.id
            taskDto.title = taskEntity.name
            taskDto.description = taskEntity.description
            taskDto.startDate = DateTimeUtils.formatServerDateTime(taskEntity.created)
            //taskDto.dueDate = ""
            //taskDto.priority = ""
            //taskDto.assigneeUserId = 0

            return taskDto
        }

        fun convertTaskEntityListToDtoList(taskEntityList: List<TaskEntity>): List<TaskCreate> {
            var taskDtoList = ArrayList<TaskCreate>()
            for (taskEntity in taskEntityList) {
                taskDtoList.add(convertTaskEntityToDto(taskEntity))
            }
            return taskDtoList
        }

        //for storing server data to local storage
        fun convertTaskDtoToEntity(taskDto: TaskCreate): TaskEntity {

            return TaskEntity(server_task_id = taskDto.serverTaskId, name = taskDto.title, description = taskDto.description)
        }

        fun convertTaskDtoListToEntityList(taskDtoList: List<TaskCreate>): List<TaskEntity> {
            var taskEntityList = ArrayList<TaskEntity>()
            for (taskDto in taskDtoList) {
                taskEntityList.add(convertTaskDtoToEntity(taskDto))
            }
            return taskEntityList
        }
    }
}