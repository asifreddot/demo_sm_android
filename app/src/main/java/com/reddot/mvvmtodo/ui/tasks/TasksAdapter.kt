package com.reddot.mvvmtodo.ui.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reddot.mvvmtodo.data.Task
import com.reddot.mvvmtodo.databinding.ItemTaskBinding


class TasksAdapter(private val listiner : onItemClickListiner) : ListAdapter<Task, TasksAdapter.TasksViewHolder>(DiffCallback()) {

    private var isTaskSynced = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TasksViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TasksViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TasksViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class TasksViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                root.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION){
                        val task = getItem(position)
                        listiner.onItemClick(task)
                    }
                }
                checkBoxCompleted.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION){
                        val task = getItem(position)
                        listiner.onCheckBoxClick(task,checkBoxCompleted.isChecked)
                    }
                }
            }
        }
        fun bind(task: Task) {
            binding.apply {
                checkBoxCompleted.isChecked = task.completed
                textViewName.text = task.name
                textViewDescription.text = task.description
                textViewName.paint.isStrikeThruText = task.completed
                labelPriority.isVisible = task.important
                //textViewSyn.isVisible = !task.is_syn
                if(task.is_syn || isTaskSynced) {
                    textViewSyn.text = "Synced"
                } else {
                    textViewSyn.text = "Not Synced"
                }
            }
        }
    }

    fun updateTaskSyncStatus(isTaskSynced:Boolean) {
        this.isTaskSynced = isTaskSynced
        notifyDataSetChanged()
    }

    interface onItemClickListiner{
        fun onItemClick(task: Task)
        fun onCheckBoxClick (task: Task,isChecked: Boolean)
    }

    class DiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Task, newItem: Task) =
            oldItem == newItem


    }
}