package com.reddot.mvvmtodo.ui.add_edit_task

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddot.mvvmtodo.data.Task
import com.reddot.mvvmtodo.data.TaskDao
import com.reddot.mvvmtodo.ui.ADD_TASK_RESULT_OK
import com.reddot.mvvmtodo.ui.EDIT_TASK_RESULT_OK
import com.reddot.mvvmtodo.utility.Constant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AddEditTaskViewModel @ViewModelInject constructor(
    private val taskDao: TaskDao,
    @Assisted private val state: SavedStateHandle
) : ViewModel() {
    val task = state.get<Task>("task")

    var taskName = state.get<String>("taskName") ?: task?.name ?: ""
        set(value) {
            field = value
            state.set("taskName",value)
        }

    var taskDescription = state.get<String>("taskDescription") ?: task?.description ?: ""
        set(value) {
            field = value
            state.set("taskDescription",value)
        }

    var taskImportance = state.get<Boolean>("taskImportance") ?: task?.important ?: false
        set(value) {
            field = value
            state.set("taskImportance",value)
        }

    private val addEditTaskEventChannel = Channel<AddEditTaskEvent>()
    val addEditTaskEvent = addEditTaskEventChannel.receiveAsFlow()

    fun OnSaveClick(){
        if (taskName.isBlank()){
            showInvalidInputMessage("Name cannot be empty")
            return
        }
        if (taskDescription.isBlank()){
            showInvalidInputMessage("Description cannot be empty")
            return
        }

        if (task != null){
            val updatedTask = task.copy(name = taskName, description = taskDescription, important = taskImportance, status = Constant.UPDATED_NOT_SYN)
            updateTask(updatedTask)
        }else {
            val newTask = Task(name = taskName,description = taskDescription, important = taskImportance, status = Constant.CREATED_NOT_SYN)
            createTask(newTask)
        }
    }

    private fun showInvalidInputMessage(text : String) = viewModelScope.launch {
        addEditTaskEventChannel.send(AddEditTaskEvent.ShowInvalidInputMessage(text))
    }

    private fun createTask(newTask: Task) = viewModelScope.launch {
        taskDao.insert(newTask)
        addEditTaskEventChannel.send(AddEditTaskEvent.NavigateBackWithResult(ADD_TASK_RESULT_OK))
    }

    private fun updateTask(updatedTask: Task) = viewModelScope.launch {
        taskDao.update(updatedTask)
        addEditTaskEventChannel.send(AddEditTaskEvent.NavigateBackWithResult(EDIT_TASK_RESULT_OK))
    }

    sealed class AddEditTaskEvent {
        data class ShowInvalidInputMessage(val msg : String) : AddEditTaskEvent()
        data class NavigateBackWithResult(val result: Int) : AddEditTaskEvent()
    }
}