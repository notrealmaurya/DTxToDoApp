package com.maurya.dtxtodoapp.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maurya.dtxtodoapp.R
import com.maurya.dtxtodoapp.databinding.TaskItemBinding

class AdapterToDo(
    private val context: Context,
    private var listener: OnItemClickListener,
    private val incompleteList: MutableList<DataToDo> = mutableListOf(),
    private val completeList: MutableList<DataToDo> = mutableListOf(),
    private val isComplete: Boolean
) : RecyclerView.Adapter<AdapterToDo.ToDoFileHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToDoFileHolder {
        val binding = TaskItemBinding.inflate(LayoutInflater.from(context), parent, false)

        return ToDoFileHolder(binding)
    }

    override fun onBindViewHolder(holder: ToDoFileHolder, position: Int) {

        val currentList = if (isComplete) completeList else incompleteList
        val currentItem = currentList[position]

        with(holder) {
            taskName.text = currentItem.taskName
            taskDetails.text = currentItem.taskDetails
            taskCompleteUpToDate.text = currentItem.taskCompleteUpToDate

            if (currentItem.taskDetails.isEmpty()) {
                taskDetails.visibility = View.GONE
            } else {
                taskDetails.visibility = View.VISIBLE
            }

            if (!currentItem.isImportant) {
                important.setImageResource(R.drawable.icon_mark_not_important)
            } else {
                important.setImageResource(R.drawable.icon_mark_important)
            }
            checkbox.isChecked = currentItem.isChecked
        }


    }

    fun addInCompleteItem(item: DataToDo) {
        incompleteList.add(item)
        notifyItemInserted(incompleteList.size - 1)
    }

    fun removeInCompleteItem(position: Int) {
        incompleteList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun addCompleteItem(item: DataToDo) {
        completeList.add(item)
        notifyItemInserted(completeList.size - 1)
    }

    fun removeCompleteItem(position: Int) {
        completeList.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun getItemCount(): Int {
        return if (isComplete) completeList.size else incompleteList.size
    }


    inner class ToDoFileHolder(binding: TaskItemBinding) :
        RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {
        val taskName = binding.taskNameTaskItem
        val taskDetails = binding.taskDetailsTaskItem
        val taskCompleteUpToDate = binding.taskCompleteUpToDateTaskItem
        val root = binding.root
        val checkbox = binding.checkboxTaskItem
        val important = binding.taskImportantTaskItem

        init {
            root.setOnClickListener(this)
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemCheckedChange(position, isChecked)
                }
            }
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClickListener(position, isComplete)
            }
        }

    }
}