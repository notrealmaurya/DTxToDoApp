package com.maurya.dtxtodoapp.util

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maurya.dtxtodoapp.R
import com.maurya.dtxtodoapp.databinding.TaskItemBinding

class AdapterToDo(
    private val listener: OnItemClickListener,
    private val incompleteList: MutableList<DataToDo> = mutableListOf(),
    private val completeList: MutableList<DataToDo> = mutableListOf(),
    private val isComplete: Boolean
) : RecyclerView.Adapter<AdapterToDo.ToDoViewHolder>() {

    private val currentList: List<DataToDo>
        get() = if (isComplete) completeList else incompleteList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToDoViewHolder {
        val binding = TaskItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ToDoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ToDoViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    override fun getItemCount(): Int = currentList.size

    inner class ToDoViewHolder(
        private val binding: TaskItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DataToDo) = with(binding) {

            taskNameTaskItem.text = item.taskName
            taskDetailsTaskItem.text = item.taskDetails
            taskCompleteUpToDateTaskItem.text = item.taskCompleteUpToDate

            taskNameTaskItem.paintFlags =
                if (isComplete) {
                    taskNameTaskItem.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    taskNameTaskItem.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

            taskDetailsTaskItem.visibility =
                if (item.taskDetails.isBlank()) View.GONE else View.VISIBLE

            taskImportantTaskItem.setImageResource(
                if (item.isImportant)
                    R.drawable.icon_mark_important
                else
                    R.drawable.icon_mark_not_important
            )

            checkboxTaskItem.setOnCheckedChangeListener(null)
            checkboxTaskItem.isChecked = item.isChecked
            checkboxTaskItem.setOnCheckedChangeListener { _, isChecked ->
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemCheckedChange(pos, isChecked, isComplete)
                }
            }

            root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClickListener(pos, isComplete)
                }
            }
        }
    }
}
