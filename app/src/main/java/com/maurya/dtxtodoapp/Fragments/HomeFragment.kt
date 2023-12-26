package com.maurya.dtxtodoapp.Fragments

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.maurya.dtxtodoapp.R
import com.maurya.dtxtodoapp.databinding.FragmentHomeBinding
import com.maurya.dtxtodoapp.util.AdapterToDo
import com.maurya.dtxtodoapp.util.DataToDo
import com.maurya.dtxtodoapp.util.OnItemClickListener
import com.maurya.dtxtodoapp.util.formatDate
import java.util.Calendar

class HomeFragment : Fragment(), OnItemClickListener {

    private lateinit var fragmentHomeBinding: FragmentHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController
    private lateinit var dataBaseRef: DatabaseReference
    private var isDetailVisible = false
    private var isDateVisible = false
    private var isImportant: Boolean = false
    private var isChecked: Boolean = false
    private lateinit var adapterToDoComplete: AdapterToDo
    private lateinit var adapterToDoInComplete: AdapterToDo
    private lateinit var inCompleteList: MutableList<DataToDo>
    private lateinit var completeList: MutableList<DataToDo>

    companion object {
        private const val KEY_SELECTED_DATE = "KEY_SELECTED_DATE"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = fragmentHomeBinding.root


        auth = FirebaseAuth.getInstance()
        dataBaseRef = FirebaseDatabase.getInstance().reference.child("Tasks")
            .child(auth.currentUser?.uid.toString())

        inCompleteList = mutableListOf()
        completeList = mutableListOf()


        fetchDataFromDatabase()
        listeners()
        displayItems()

        return view;
    }

    private fun fetchDataFromDatabase() {

        dataBaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newInCompleteList = mutableListOf<DataToDo>()
                val newCompleteList = mutableListOf<DataToDo>()


                inCompleteList.clear()
                completeList.clear()

                val completeTasksSnapshot = snapshot.child("completeTasks")
                val incompleteTasksSnapshot = snapshot.child("incompleteTasks")

                for (taskSnapshot in incompleteTasksSnapshot.children) {
                    processTaskSnapshot(taskSnapshot, newInCompleteList, newCompleteList)
                }

                // Process Complete Tasks
                for (taskSnapshot in completeTasksSnapshot.children) {
                    processTaskSnapshot(taskSnapshot, newInCompleteList, newCompleteList)
                }

                inCompleteList.clear()
                inCompleteList.addAll(newInCompleteList)

                completeList.clear()
                completeList.addAll(newCompleteList)

                adapterToDoInComplete.notifyDataSetChanged()
                adapterToDoComplete.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun processTaskSnapshot(
        taskSnapshot: DataSnapshot,
        newInCompleteList: MutableList<DataToDo>,
        newCompleteList: MutableList<DataToDo>
    ) {

        val taskId = taskSnapshot.key.orEmpty()
        val taskName = taskSnapshot.child("taskName").getValue(String::class.java).orEmpty()
        val taskDetails = taskSnapshot.child("taskDetails").getValue(String::class.java).orEmpty()
        val taskCompleteUpToDate =
            taskSnapshot.child("taskCompleteUpToDate").getValue(String::class.java).orEmpty()
        val isImportant = taskSnapshot.child("important").getValue(Boolean::class.java) ?: false
        var isChecked = taskSnapshot.child("checked").getValue(Boolean::class.java) ?: false
        val toDoTask =
            DataToDo(taskId, taskName, taskDetails, taskCompleteUpToDate, isImportant, isChecked)

        if (isChecked) {
            Log.d("CheckboxClick", "InProcessTaskisChecked")
            newCompleteList.add(toDoTask)
        } else {
            Log.d("CheckboxClick", "InProcessTaskNotChecked")
            newInCompleteList.add(toDoTask)
        }
    }

    private fun displayItems() {

        fragmentHomeBinding.recyclerViewInCompleteHomeFragment.setHasFixedSize(true)
        fragmentHomeBinding.recyclerViewInCompleteHomeFragment.setItemViewCacheSize(13)
        fragmentHomeBinding.recyclerViewInCompleteHomeFragment.layoutManager =
            LinearLayoutManager(context)
        adapterToDoInComplete =
            AdapterToDo(requireContext(), this, inCompleteList, completeList, false)
        fragmentHomeBinding.recyclerViewInCompleteHomeFragment.adapter = adapterToDoInComplete


        fragmentHomeBinding.recyclerViewCompleteHomeFragment.setHasFixedSize(true)
        fragmentHomeBinding.recyclerViewCompleteHomeFragment.setItemViewCacheSize(13)
        fragmentHomeBinding.recyclerViewCompleteHomeFragment.layoutManager =
            LinearLayoutManager(context)
        adapterToDoComplete =
            AdapterToDo(requireContext(), this, inCompleteList, completeList, true)
        fragmentHomeBinding.recyclerViewCompleteHomeFragment.adapter = adapterToDoComplete


    }

    private fun listeners() {

        val incompleteTasksRef = dataBaseRef.child("incompleteTasks")
        val completeTasksRef = dataBaseRef.child("completeTasks")

        //ToolbarAddTask
        fragmentHomeBinding.addTaskHomeFragment.setOnClickListener {
            val AddTaskDialog =
                BottomSheetDialog(requireContext(), R.style.ThemeOverlay_App_BottomSheetDialog)
            val AddTaskView = layoutInflater.inflate(R.layout.bottomsheet_add_task, null)
            AddTaskDialog.setContentView(AddTaskView)
            AddTaskDialog.setCanceledOnTouchOutside(true)

            val addTaskText = AddTaskView.findViewById<EditText>(R.id.addTaskEditText)
            val addTaskDetailsText = AddTaskView.findViewById<EditText>(R.id.addDetailsEditText)
            val addTaskOKText = AddTaskView.findViewById<TextView>(R.id.addTaskOKText)
            val addDateButton = AddTaskView.findViewById<ImageView>(R.id.addDateButton)
            val addDetailsButton = AddTaskView.findViewById<ImageView>(R.id.addDetailsButton)
            val addImportantButton = AddTaskView.findViewById<ImageView>(R.id.addImportantButton)
            val addDateChipText = AddTaskView.findViewById<Chip>(R.id.addDateChipText)

            addTaskText.requestFocus()
            AddTaskDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

            addDetailsButton.setOnClickListener {
                if (!isDetailVisible) {
                    addTaskDetailsText.visibility = View.VISIBLE
                    addDetailsButton.setImageResource(R.drawable.icon_details_selected)
                    isDetailVisible = true
                } else {
                    addTaskDetailsText.text.clear()
                    addTaskDetailsText.visibility = View.GONE
                    addDetailsButton.setImageResource(R.drawable.icon_details)
                    isDetailVisible = false
                }

            }

            addDateButton.setOnClickListener {
                if (!isDateVisible) {
                    showDatePickerDialog(addDateChipText, addDateButton)
                } else {
                    addDateChipText.text = ""
                    addDateChipText.visibility = View.GONE
                    addDateButton.setImageResource(R.drawable.icon_date)
                    val sharedPreferences =
                        requireContext().getSharedPreferences("DateCalender", Context.MODE_PRIVATE)
                    sharedPreferences.edit().clear().apply()
                    isDateVisible = false
                }
            }

            addDateChipText.setOnClickListener {
                showDatePickerDialog(addDateChipText, addDateButton)
            }

            addImportantButton.setOnClickListener {
                if (!isImportant) {
                    addImportantButton.setImageResource(R.drawable.icon_mark_important)
                    isImportant = true
                } else {
                    isImportant = false
                    addImportantButton.setImageResource(R.drawable.icon_mark_not_important)
                }
            }

            addTaskOKText.setOnClickListener {
                val taskName = addTaskText.text.toString()
                val taskDetails = addTaskDetailsText.text.toString()
                val taskDate = addDateChipText.text.toString()
                val taskId = ""

                val task = DataToDo(taskId, taskName, taskDetails, taskDate, isImportant)

                if (taskName.isNotEmpty()) {
                    incompleteTasksRef.push().setValue(task).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(context, "Task Added", Toast.LENGTH_SHORT).show()
                        }
                    }
                        .addOnFailureListener {
                            Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()
                        }
                    AddTaskDialog.dismiss()

                } else {
                    Toast.makeText(context, "Please Enter Task Name", Toast.LENGTH_SHORT).show()
                }
            }

            AddTaskDialog.setOnDismissListener {
                isDateVisible = false
                isImportant = false
                isDetailVisible = false
            }

            AddTaskDialog.show()

        }

    }


    override fun onItemClickListener(position: Int, isComplete: Boolean) {

        if (isComplete) {
            val completeTasksRef = dataBaseRef.child("completeTasks")
            val completeItems = completeList[position]

            val AddTaskDialog =
                BottomSheetDialog(requireContext(), R.style.ThemeOverlay_App_BottomSheetDialog)
            val AddTaskView = layoutInflater.inflate(R.layout.bottomsheet_add_task, null)
            AddTaskDialog.setContentView(AddTaskView)
            AddTaskDialog.setCanceledOnTouchOutside(true)

            val updateImage = AddTaskView.findViewById<ImageView>(R.id.updateImage)
            val lotteImage = AddTaskView.findViewById<LottieAnimationView>(R.id.lotteView)
            val deleteUpdateLayout = AddTaskView.findViewById<LinearLayout>(R.id.updateDeleteLayout)

            val addTaskText = AddTaskView.findViewById<EditText>(R.id.addTaskEditText)
            val addTaskDetailsText = AddTaskView.findViewById<EditText>(R.id.addDetailsEditText)
            val addDateChipText = AddTaskView.findViewById<Chip>(R.id.addDateChipText)
            val addTaskUpdateText = AddTaskView.findViewById<TextView>(R.id.addTaskOKText)
            val addTaskDeleteText = AddTaskView.findViewById<TextView>(R.id.addTaskDeleteText)
            val addTaskMarkAsCompletedText =
                AddTaskView.findViewById<TextView>(R.id.addTaskMarkAsCompletedText)

            val addDateButton = AddTaskView.findViewById<ImageView>(R.id.addDateButton)
            val addDetailsButton = AddTaskView.findViewById<ImageView>(R.id.addDetailsButton)
            val addImportantButton = AddTaskView.findViewById<ImageView>(R.id.addImportantButton)

            updateImage.visibility = View.VISIBLE
            deleteUpdateLayout.visibility = View.VISIBLE
            lotteImage.visibility = View.GONE

            addTaskText.requestFocus()
            AddTaskDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            addTaskDetailsText.visibility = View.VISIBLE
            addDateChipText.visibility = View.VISIBLE
            addTaskUpdateText.text = "Update"

            addTaskText.setText(completeItems.taskName)
            addTaskDetailsText.setText(completeItems.taskDetails)
            addDateChipText.text = completeItems.taskCompleteUpToDate

            if (!completeItems.isImportant) {
                addImportantButton.setImageResource(R.drawable.icon_mark_not_important)
            } else {
                addImportantButton.setImageResource(R.drawable.icon_mark_important)
            }

            isDetailVisible = true
            addDetailsButton.setImageResource(R.drawable.icon_details_selected)
            addDateButton.setImageResource(R.drawable.icon_date_selected)

            addDetailsButton.setOnClickListener {
                if (!isDetailVisible) {
                    addTaskDetailsText.visibility = View.VISIBLE
                    addDetailsButton.setImageResource(R.drawable.icon_details_selected)
                    isDetailVisible = true
                } else {
                    addTaskDetailsText.text.clear()
                    addTaskDetailsText.visibility = View.GONE
                    addDetailsButton.setImageResource(R.drawable.icon_details)
                    isDetailVisible = false
                }
            }

            addDateButton.setOnClickListener {
                if (!isDateVisible) {
                    addDateButton.setImageResource(R.drawable.icon_date_selected)
                    showDatePickerDialog(addDateChipText, addDateButton)
                } else {
                    addDateChipText.text = ""
                    addDateChipText.visibility = View.GONE
                    addDateButton.setImageResource(R.drawable.icon_date)
                    val sharedPreferences =
                        requireContext().getSharedPreferences("DateCalender", Context.MODE_PRIVATE)
                    sharedPreferences.edit().clear().apply()
                    isDateVisible = false
                }
            }

            addDateChipText.setOnClickListener {
                showDatePickerDialog(addDateChipText, addDateButton)
            }

            addImportantButton.setOnClickListener {
                if (!completeItems.isImportant) {
                    addImportantButton.setImageResource(R.drawable.icon_mark_important)
                    completeItems.isImportant = true
                } else {
                    completeItems.isImportant = false
                    addImportantButton.setImageResource(R.drawable.icon_mark_not_important)
                }
            }

            addTaskUpdateText.setOnClickListener {
                val taskName = addTaskText.text.toString()
                val taskDetails = addTaskDetailsText.text.toString()
                val taskDate = addDateChipText.text.toString()
                val taskId = completeItems.id
                val important = completeItems.isImportant

                if (taskName.isNotEmpty()) {
                    val taskReference = completeTasksRef.child(taskId)
                    val updatedTask =
                        DataToDo(taskId, taskName, taskDetails, taskDate, important)
                    taskReference.setValue(updatedTask).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(context, "Task Updated", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                    AddTaskDialog.dismiss()
                } else {
                    Toast.makeText(context, "Please Enter Task Name", Toast.LENGTH_SHORT).show()
                }
            }

            addTaskDeleteText.setOnClickListener {
                val taskId = completeItems.id
                if (taskId.isNotEmpty()) {
                    val taskReference = completeTasksRef.child(taskId)
                    taskReference.removeValue().addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(context, "Task Deleted", Toast.LENGTH_SHORT).show()
                            adapterToDoInComplete.notifyDataSetChanged()
                        } else {
                            Toast.makeText(context, "Failed to delete task", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    AddTaskDialog.dismiss()
                }
            }

            AddTaskDialog.show()

        } else {
            val incompleteTasksRef = dataBaseRef.child("incompleteTasks")
            val inCompleteItems = inCompleteList[position]

            val AddTaskDialog =
                BottomSheetDialog(requireContext(), R.style.ThemeOverlay_App_BottomSheetDialog)
            val AddTaskView = layoutInflater.inflate(R.layout.bottomsheet_add_task, null)
            AddTaskDialog.setContentView(AddTaskView)
            AddTaskDialog.setCanceledOnTouchOutside(true)

            val updateImage = AddTaskView.findViewById<ImageView>(R.id.updateImage)
            val lotteImage = AddTaskView.findViewById<LottieAnimationView>(R.id.lotteView)
            val deleteUpdateLayout = AddTaskView.findViewById<LinearLayout>(R.id.updateDeleteLayout)

            val addTaskText = AddTaskView.findViewById<EditText>(R.id.addTaskEditText)
            val addTaskDetailsText = AddTaskView.findViewById<EditText>(R.id.addDetailsEditText)
            val addDateChipText = AddTaskView.findViewById<Chip>(R.id.addDateChipText)
            val addTaskUpdateText = AddTaskView.findViewById<TextView>(R.id.addTaskOKText)
            val addTaskDeleteText = AddTaskView.findViewById<TextView>(R.id.addTaskDeleteText)
            val addTaskMarkAsCompletedText =
                AddTaskView.findViewById<TextView>(R.id.addTaskMarkAsCompletedText)

            val addDateButton = AddTaskView.findViewById<ImageView>(R.id.addDateButton)
            val addDetailsButton = AddTaskView.findViewById<ImageView>(R.id.addDetailsButton)
            val addImportantButton = AddTaskView.findViewById<ImageView>(R.id.addImportantButton)

            updateImage.visibility = View.VISIBLE
            deleteUpdateLayout.visibility = View.VISIBLE
            lotteImage.visibility = View.GONE

            addTaskText.requestFocus()
            AddTaskDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            addTaskDetailsText.visibility = View.VISIBLE
            addDateChipText.visibility = View.VISIBLE
            addTaskUpdateText.text = "Update"

            addTaskText.setText(inCompleteItems.taskName)
            addTaskDetailsText.setText(inCompleteItems.taskDetails)
            addDateChipText.text = inCompleteItems.taskCompleteUpToDate

            if (!inCompleteItems.isImportant) {
                addImportantButton.setImageResource(R.drawable.icon_mark_not_important)
            } else {
                addImportantButton.setImageResource(R.drawable.icon_mark_important)
            }

            isDetailVisible = true
            addDetailsButton.setImageResource(R.drawable.icon_details_selected)
            addDateButton.setImageResource(R.drawable.icon_date_selected)

            addDetailsButton.setOnClickListener {
                if (!isDetailVisible) {
                    addTaskDetailsText.visibility = View.VISIBLE
                    addDetailsButton.setImageResource(R.drawable.icon_details_selected)
                    isDetailVisible = true
                } else {
                    addTaskDetailsText.text.clear()
                    addTaskDetailsText.visibility = View.GONE
                    addDetailsButton.setImageResource(R.drawable.icon_details)
                    isDetailVisible = false
                }
            }

            addDateButton.setOnClickListener {
                if (!isDateVisible) {
                    addDateButton.setImageResource(R.drawable.icon_date_selected)
                    showDatePickerDialog(addDateChipText, addDateButton)
                } else {
                    addDateChipText.text = ""
                    addDateChipText.visibility = View.GONE
                    addDateButton.setImageResource(R.drawable.icon_date)
                    val sharedPreferences =
                        requireContext().getSharedPreferences("DateCalender", Context.MODE_PRIVATE)
                    sharedPreferences.edit().clear().apply()
                    isDateVisible = false
                }
            }

            addDateChipText.setOnClickListener {
                showDatePickerDialog(addDateChipText, addDateButton)
            }

            addImportantButton.setOnClickListener {
                if (!inCompleteItems.isImportant) {
                    addImportantButton.setImageResource(R.drawable.icon_mark_important)
                    inCompleteItems.isImportant = true
                } else {
                    inCompleteItems.isImportant = false
                    addImportantButton.setImageResource(R.drawable.icon_mark_not_important)
                }
            }

            addTaskUpdateText.setOnClickListener {
                val taskName = addTaskText.text.toString()
                val taskDetails = addTaskDetailsText.text.toString()
                val taskDate = addDateChipText.text.toString()
                val taskId = inCompleteItems.id
                val important = inCompleteItems.isImportant

                if (taskName.isNotEmpty()) {
                    val taskReference = incompleteTasksRef.child(taskId)
                    val updatedTask =
                        DataToDo(taskId, taskName, taskDetails, taskDate, important)
                    taskReference.setValue(updatedTask).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(context, "Task Updated", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                    AddTaskDialog.dismiss()
                } else {
                    Toast.makeText(context, "Please Enter Task Name", Toast.LENGTH_SHORT).show()
                }
            }

            addTaskDeleteText.setOnClickListener {
                val taskId = inCompleteItems.id
                if (taskId.isNotEmpty()) {
                    val taskReference = incompleteTasksRef.child(taskId)
                    taskReference.removeValue().addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(context, "Task Deleted", Toast.LENGTH_SHORT).show()
                            adapterToDoInComplete.notifyDataSetChanged()
                        } else {
                            Toast.makeText(context, "Failed to delete task", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    AddTaskDialog.dismiss()
                }
            }

            AddTaskDialog.show()
        }

    }

    override fun onItemCheckedChange(position: Int, isChecked: Boolean) {

        Log.d(
            "CheckboxClick",
            "onItemCheckedChange called with position=$position, isChecked=$isChecked"
        )

        val incompleteTasksRef = dataBaseRef.child("incompleteTasks")
        val completeTasksRef = dataBaseRef.child("completeTasks")

        if (isChecked && inCompleteList.size > position) {
            val inCompletedItem = inCompleteList[position]
            Log.d("CheckboxClick", "Moving item to completeTasks")

            val taskId = inCompletedItem.id
            incompleteTasksRef.child(taskId).removeValue()

            val completeTask = DataToDo(
                taskId,
                inCompletedItem.taskName,
                inCompletedItem.taskDetails,
                inCompletedItem.taskCompleteUpToDate,
                inCompletedItem.isImportant,
                true
            )
            completeTasksRef.child(taskId).setValue(completeTask)

            adapterToDoComplete.notifyDataSetChanged()
            adapterToDoInComplete.notifyDataSetChanged()
            Toast.makeText(context, "Task Completed", Toast.LENGTH_SHORT).show()
        } else if (!isChecked && completeList.size > position) {
            val completedItem = completeList[position]
            Log.d("CheckboxClick", "Moving item back to incompleteTasks")

            val taskId = completedItem.id

            completeTasksRef.child(taskId).removeValue()

            val incompleteTask = DataToDo(
                taskId,
                completedItem.taskName,
                completedItem.taskDetails,
                completedItem.taskCompleteUpToDate,
                completedItem.isImportant,
                false
            )
            incompleteTasksRef.child(taskId).setValue(incompleteTask)

            adapterToDoComplete.notifyDataSetChanged()
            adapterToDoInComplete.notifyDataSetChanged()
            Toast.makeText(context, "Task Marked as InComplete", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showDatePickerDialog(addDateChipText: Chip, addDateButton: ImageView) {

        val currentDate = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        val day = currentDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                val selectedDateText = formatDate(selectedDate.timeInMillis)
                addDateChipText.text = selectedDateText
                addDateChipText.visibility = View.VISIBLE
                addDateButton.setImageResource(R.drawable.icon_date_selected)
                saveSelectedDate(selectedYear, selectedMonth, selectedDay)
                isDateVisible = true
            },
            year,
            month,
            day
        )

        // Retrieve the pre-selected date from SharedPreferences
        val sharedPreferences =
            requireContext().getSharedPreferences("DateCalender", Context.MODE_PRIVATE)
        val selectedDate = sharedPreferences.getLong(KEY_SELECTED_DATE, 0)

        if (selectedDate > 0) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate

            datePickerDialog.updateDate(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }

        datePickerDialog.show()
    }

    private fun saveSelectedDate(year: Int, month: Int, day: Int) {
        val selectedDateMillis = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }.timeInMillis

        // Save the selected date in SharedPreferences
        val sharedPreferences =
            requireContext().getSharedPreferences("DateCalender", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong(KEY_SELECTED_DATE, selectedDateMillis).apply()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences =
            requireContext().getSharedPreferences("DateCalender", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }


}

