package com.reddot.mvvmtodo.ui.tasks

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.reddot.mvvmtodo.data.PreferencesManager
import com.reddot.mvvmtodo.data.SortOrder
import com.reddot.mvvmtodo.data.Task
import com.reddot.mvvmtodo.data.TaskDao
import com.reddot.mvvmtodo.model.TaskCreate
import com.reddot.mvvmtodo.repository.DataSourceRepository
import com.reddot.mvvmtodo.ui.ADD_TASK_RESULT_OK
import com.reddot.mvvmtodo.ui.EDIT_TASK_RESULT_OK
import com.reddot.mvvmtodo.utility.Constant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TasksViewModel @ViewModelInject constructor(
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    @Assisted private val state : SavedStateHandle,
    private val repository: DataSourceRepository
) : ViewModel() {

    val searchQuery = state.getLiveData("searchQuery","")

    val preferencesFlow = preferencesManager.preferencesFlow

    private val taskEventChannel = Channel<TaskEvent>()
    val tasksEvent = taskEventChannel.receiveAsFlow()

    private val taskFlow = combine(
        searchQuery.asFlow(),
        preferencesFlow
    ) { query,filterPrefernces ->
        Pair(query, filterPrefernces)
    }.flatMapLatest { (query, filterPrefernces) ->
        taskDao.getTasks(query, filterPrefernces.sortOrder,filterPrefernces.hideCompleted)
    }

    val tasks = taskFlow.asLiveData()

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    fun onHideCompletedClick(hideCompleted : Boolean) = viewModelScope.launch {
        preferencesManager.updateHideCompleted(hideCompleted)
    }

    fun onTaskSelected(task:Task) = viewModelScope.launch {
        taskEventChannel.send(TaskEvent.NavigateToEditTaskScreen(task))
    }

    fun onTaskCheckedChange(task: Task, isChecked: Boolean) = viewModelScope.launch {
        taskDao.update(task.copy(completed = isChecked))
    }

    fun onTaskSwiped(task:Task) = viewModelScope.launch {
        task.status = Constant.DELETED_NOT_SYN
        taskDao.delete(task)
        //taskEventChannel.send(TaskEvent.ShowUndoDeleteTaskMessage(task))
    }

    fun onUndoDeleteClick(task: Task) = viewModelScope.launch {
        taskDao.insert(task)
    }

    fun onAddNewTaskClick() = viewModelScope.launch {
        taskEventChannel.send(TaskEvent.NavigateToAddTaskScreen)
    }

    fun onAddEditResult(result : Int){
        when(result){
            ADD_TASK_RESULT_OK -> showTaskSavedConfirmationMessage("Task Added")
            EDIT_TASK_RESULT_OK -> showTaskSavedConfirmationMessage("Task Updated")
        }
    }

    private fun showTaskSavedConfirmationMessage(text: String) = viewModelScope.launch {
        taskEventChannel.send(TaskEvent.ShowTaskSavedConfirmationMessage(text))
    }

    fun onDeleteAllCompletedClick() = viewModelScope.launch {
        taskEventChannel.send(TaskEvent.NavigateToDeleteAllCompletedScreen)
    }

    fun onConfirmClick() = viewModelScope.launch {
        taskDao.deleteCompletedTasks()
    }

    fun onTaskStatusUpdate(task:Task) = viewModelScope.launch {
        task.status = Constant.SYN
        task.is_syn = true
        taskDao.update(task)
    }

//    fun onTaskInsert(task:Task) = viewModelScope.launch {
//        taskDao.insert(task)
//    }

    fun onTasksInsert(tasks:List<Task>) = viewModelScope.launch {
        tasks.forEach { task ->
            task.is_syn = true
            task.status = Constant.SYN
        }
        taskDao.insertAll(tasks)
    }

    fun fetchAllTask() = repository.fetchAllTask()

    fun createTask(task: TaskCreate) = repository.createTask(task)
    fun updateTask(task: TaskCreate, id:Int) = repository.updateTask(task, id)

//  fun createBatchTask(tasks: List<TaskCreate>) = repository.createBatchTask(tasks)

    fun deleteTask(taskId: Int) = repository.deleteTask(taskId)

    sealed class TaskEvent {
        object NavigateToAddTaskScreen : TaskEvent()
        data class NavigateToEditTaskScreen(val task: Task) : TaskEvent()
        data class ShowUndoDeleteTaskMessage(val task: Task) : TaskEvent()
        data class ShowTaskSavedConfirmationMessage(val msg : String) : TaskEvent()
        object NavigateToDeleteAllCompletedScreen :TaskEvent()
    }

}

