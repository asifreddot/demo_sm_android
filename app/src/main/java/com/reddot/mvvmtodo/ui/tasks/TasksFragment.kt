package com.reddot.mvvmtodo.ui.tasks

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.reddot.mvvmtodo.R
import com.reddot.mvvmtodo.data.SortOrder
import com.reddot.mvvmtodo.data.Task
import com.reddot.mvvmtodo.databinding.FragmentTasksBinding
import com.reddot.mvvmtodo.mapper.Mapper
import com.reddot.mvvmtodo.model.TaskCreate
import com.reddot.mvvmtodo.ui.Result
import com.reddot.mvvmtodo.util.exhaustive
import com.reddot.mvvmtodo.util.onQueryTextChange
import com.reddot.mvvmtodo.utility.Constant
import com.reddot.service.EventBody
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tasks.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

@AndroidEntryPoint
class TasksFragment : Fragment(R.layout.fragment_tasks), TasksAdapter.onItemClickListiner {

    private val TAG = "TasksFragment"
    private val viewModel: TasksViewModel by viewModels()
    private var isDataSavedToServerExecutedOnce = false
    private var isDataSavedToDbExecutedOnce = false
    private var isDataDeletedFromSwiped = false
    private var isLocalDataFetchedOnce = false
    private lateinit var taskAdapter: TasksAdapter

    @Subscribe
    fun onEvent(event: EventBody) {
        if (event.message) {
            fetchAllTaskOnNetworkChange()
        } else {
            isLocalDataFetchedOnce = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentTasksBinding.bind(view)
        registerEventBus()

        taskAdapter = TasksAdapter(this)

        binding.apply {
            recyclerViewTasks.apply {
                adapter = taskAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
            }

            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val task = taskAdapter.currentList[viewHolder.adapterPosition]
                    handleDeleteTaskResponse(task.server_task_id)
                    isDataDeletedFromSwiped = true
                    viewModel.onTaskSwiped(task)
                }
            }).attachToRecyclerView(recyclerViewTasks)
        }

        fab_add_tasks.setOnClickListener {
            viewModel.onAddNewTaskClick()
        }

        setFragmentResultListener("add_edit_request") { _, bundle ->
            val result = bundle.getInt("add_edit_result")
            viewModel.onAddEditResult(result)
        }

        viewModel.tasks.observe(viewLifecycleOwner, Observer {

            taskAdapter.submitList(it)
            fetchAllTask(it)
        })

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.tasksEvent.collect { event ->
                when (event) {
                    is TasksViewModel.TaskEvent.ShowUndoDeleteTaskMessage -> {
                        Snackbar.make(requireView(), "Task Deleted", Snackbar.LENGTH_SHORT)
                            .setAction("UNDO") {
                                viewModel.onUndoDeleteClick(event.task)
                            }.show()
                    }
                    is TasksViewModel.TaskEvent.NavigateToAddTaskScreen -> {
                        val action =
                            TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment(title = "New Task")
                        findNavController().navigate(action)
                    }
                    is TasksViewModel.TaskEvent.NavigateToEditTaskScreen -> {
                        val action =
                            TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment(
                                event.task,
                                "Edit Task"
                            )
                        findNavController().navigate(action)
                    }
                    is TasksViewModel.TaskEvent.ShowTaskSavedConfirmationMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                    TasksViewModel.TaskEvent.NavigateToDeleteAllCompletedScreen -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Confirm Deletion")
                            .setMessage("Are you sure you want to delete all completed tasks")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Yes") { _, _ ->
                                viewModel.onConfirmClick()
                            }
                            .create()
                            .show()
                    }
                }.exhaustive
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onItemClick(task: Task) {
        viewModel.onTaskSelected(task)
    }

    override fun onCheckBoxClick(task: Task, isChecked: Boolean) {
        viewModel.onTaskCheckedChange(task, isChecked)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_tasks, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.onQueryTextChange {
            viewModel.searchQuery.value = it
        }

        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.action_hide_completed_tasks).isChecked =
                viewModel.preferencesFlow.first().hideCompleted
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_by_name -> {
                viewModel.onSortOrderSelected(SortOrder.BY_NAME)
                true
            }
            R.id.action_sort_by_date_created -> {
                viewModel.onSortOrderSelected(SortOrder.BY_DATE)
                true
            }
            R.id.action_hide_completed_tasks -> {
                item.isChecked = !item.isChecked
                viewModel.onHideCompletedClick(item.isChecked)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun registerEventBus() {
        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unRegisterEventBus() {
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchAllTaskOnNetworkChange() {
        if (!isLocalDataFetchedOnce) {
            isLocalDataFetchedOnce = true

            viewModel.tasks.observe(viewLifecycleOwner, Observer {
                fetchAllTask(it)
            })
        }
    }

    private fun fetchAllTask(tasks: List<Task>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            viewModel.fetchAllTask().collect { response ->
                when {
                    response.status === Result.Status.LOADING -> {
                        Log.d(TAG, "-------------subscribeUi: LOADING-------------------")
                        // hide loader
                    }
                    response.status == Result.Status.SUCCESS -> {
                        Log.d(TAG, "---------subscribeUi: SUCCESS {$response}-----------")
                        // inserting data to room depending on logic
                        //Log.d("checkTasksData", Gson().toJson(response))
                        response.data?.data?.data?.let { checkData(tasks, it) }
//                        Toast.makeText(
//                            requireContext(),
//                            "Tasks have fetched successfully",
//                            Toast.LENGTH_LONG
//                        ).show()

                    }
                    response.status == Result.Status.ERROR -> {
                        Log.d(TAG, "-----------subscribeUi: ERROR {$response}------------")
                        // hide loader
                        // handle api error case
                        Log.d("checkTasksDataError", response.errorCode.toString())
                    }
                }
            }
        }
    }

    private fun handleCreateTaskResponse(task: TaskCreate) {
        //Log.d("checkTaskCreation", Gson().toJson(task))
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            viewModel.createTask(task).collect { response ->
                when {
                    response.status === Result.Status.LOADING -> {
                        Log.d(TAG, "-------------subscribeUi: LOADING-------------------")
                        // hide loader
                    }
                    response.status == Result.Status.SUCCESS -> {
                        Log.d(TAG, "---------subscribeUi: SUCCESS {$response}-----------")
                        //Log.d("checkTaskData", Gson().toJson(response))
                        //response.data?.data?.data?.let { checkData(tasks, it) }
                        taskAdapter.updateTaskSyncStatus(true)
//                        Toast.makeText(
//                            requireContext(),
//                            "Task has created successfully",
//                            Toast.LENGTH_LONG
//                        ).show()

                    }
                    response.status == Result.Status.ERROR -> {
                        Log.d(TAG, "-----------subscribeUi: ERROR {$response}------------")
                        // hide loader
                        // handle api error case
                        // Log.d("checkTaskDataError", response.errorCode.toString())
                    }
                }
            }
        }
    }

    private fun handleDeleteTaskResponse(serverTaskId: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            viewModel.deleteTask(serverTaskId).collect { response ->
                when {
                    response.status === Result.Status.LOADING -> {
                        Log.d(TAG, "-------------subscribeUi: LOADING-------------------")
                        // hide loader
                    }
                    response.status == Result.Status.SUCCESS -> {
                        Log.d(TAG, "---------subscribeUi: SUCCESS {$response}-----------")
                        //Log.d("checkTaskDeleteResponse", Gson().toJson(response))
                        //response.data?.data?.data?.let { checkData(tasks, it) }
//                        Toast.makeText(
//                            requireContext(),
//                            "Task has deleted successfully",
//                            Toast.LENGTH_LONG
//                        ).show()

                    }
                    response.status == Result.Status.ERROR -> {
                        Log.d(TAG, "-----------subscribeUi: ERROR {$response}------------")
                        // hide loader
                        // handle api error case
                        //Log.d("checkTaskDeleteError", response.errorCode.toString())
                    }
                }
            }
        }
    }

    private fun handleUpdateTaskResponse(task: TaskCreate, id: Int) {
        //Log.d("checkTaskUpdate", Gson().toJson(task))
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            viewModel.updateTask(task, id).collect { response ->
                when {
                    response.status === Result.Status.LOADING -> {
                        Log.d(TAG, "-------------subscribeUi: LOADING-------------------")
                        // hide loader
                    }
                    response.status == Result.Status.SUCCESS -> {
                        Log.d(TAG, "---------subscribeUi: SUCCESS {$response}-----------")
                        Log.d("checkTaskData", Gson().toJson(response))
                        //response.data?.data?.data?.let { checkData(tasks, it) }
                        taskAdapter.updateTaskSyncStatus(true)
//                        Toast.makeText(
//                            requireContext(),
//                            "Task has created successfully",
//                            Toast.LENGTH_LONG
//                        ).show()

                    }
                    response.status == Result.Status.ERROR -> {
                        Log.d(TAG, "-----------subscribeUi: ERROR {$response}------------")
                        // hide loader
                        // handle api error case
                        //Log.d("checkTaskDataError", response.errorCode.toString())
                    }
                }
            }
        }
    }

    private fun checkData(localData: List<Task>, serverdata: List<com.reddot.mvvmtodo.model.Task>) {
        if (localData.isEmpty() && serverdata.isEmpty()) {
            return //do nothing // fresh user
        } else if (localData.isEmpty() && serverdata.isNotEmpty() && !isDataSavedToDbExecutedOnce && !isDataDeletedFromSwiped) {
            ///insert into localdb ///
            isDataSavedToDbExecutedOnce = true
            val taskCreate = Mapper.convertTaskDataListFromTaskModel(serverdata)
            viewModel.onTasksInsert(Mapper.convertTaskDtoListToEntityList(taskCreate))
            //Toast.makeText(requireContext(), "all data saved in room", Toast.LENGTH_LONG).show()
            Log.d("currentAction1", "all data saved in room")
        } else if (localData.isNotEmpty() && serverdata.isEmpty() && !isDataSavedToServerExecutedOnce) {
            ///set all data to server ///
            isDataSavedToServerExecutedOnce = true
            val serverDatas = Mapper.convertTaskEntityListToDtoList(localData)
            for (serverData in serverDatas) {
                handleCreateTaskResponse(serverData)
            }

            for (local in localData) {
                viewModel.onTaskStatusUpdate(local)
            }

            //Toast.makeText(requireContext(), "all data saved in server", Toast.LENGTH_LONG).show()
            Log.d("currentAction2", "all data saved in server")
        }
        //both local data and server data aren't empty
        else {

            for (item: Task in localData) {

                for (itemServer: com.reddot.mvvmtodo.model.Task in serverdata) {

                    ///item match with itemserver id ///
                    if (item.id == itemServer.sync_id) {

                        when (item.status) {
                            Constant.SYN -> {} //do nothing//
                            Constant.UPDATED_NOT_SYN -> {
                                //do nothing//
                                //update in server
                                //update item status in dao

                                //TODO//
                                itemServer.id?.let {
                                    handleUpdateTaskResponse(
                                        Mapper.convertTaskEntityToDto(item),
                                        it
                                    )
                                }
                                viewModel.onTaskStatusUpdate(item)
//                                Toast.makeText(
//                                    requireContext(),
//                                    "data saved in server",
//                                    Toast.LENGTH_LONG
//                                ).show()
                                Log.d("currentAction3", "udpated not sync - data saved in server")
                            }
                            Constant.DELETED_NOT_SYN -> {
                                Log.d("currentAction9", "DELETED_NOT_SYN")
                            }  //do nothing//
                        }
                        break
                    }
                    ///item doesn't match with itemserver id ///
                    //come to last element///
                    if (itemServer == serverdata[serverdata.size - 1]) {
                        ///send item to server if CREATED_NOT_SYN UPDATE_NOT_SYN////
                        ///if Status is SYN_DELETED removed///
                        ////update  status syn_type///
                        when (item.status) {
                            Constant.CREATED_NOT_SYN -> {
                                handleCreateTaskResponse(Mapper.convertTaskEntityToDto(item))
                                viewModel.onTaskStatusUpdate(item)
//                                Toast.makeText(
//                                    requireContext(),
//                                    "last data saved in server",
//                                    Toast.LENGTH_LONG
//                                ).show()
                                Log.d("currentAction4", "CREATED_NOT_SYN- data saved in server")
                            }
                            Constant.UPDATED_NOT_SYN -> {
                                //should be updated
                                itemServer.id?.let {
                                    handleUpdateTaskResponse(
                                        Mapper.convertTaskEntityToDto(item),
                                        it
                                    )
                                }
                                viewModel.onTaskStatusUpdate(item)
//                                Toast.makeText(
//                                    requireContext(),
//                                    "last data saved in server",
//                                    Toast.LENGTH_LONG
//                                ).show()
                                Log.d(
                                    "currentAction5",
                                    "UPDATED_NOT_SYN - last data saved in server"
                                )
                            }
                            // SYN_DELETED
                            Constant.DELETED_NOT_SYN -> {
                                Log.d("currentAction8", "DELETED_NOT_SYN")
                                //delete item from server
                                //these 2 methods should be called when item is deleted from db
                                //viewModel.deleteTask(item.id) //id will be vanished
                                //viewModel.onTaskStatusUpdate(item)
                            }
                        }
                    }
                }
            }


        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unRegisterEventBus()
    }
}