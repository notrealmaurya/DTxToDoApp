package com.maurya.dtxtodoapp.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.maurya.dtxtodoapp.R
import com.maurya.dtxtodoapp.databinding.FragmentHomeBinding
import com.maurya.dtxtodoapp.util.AdapterToDo
import com.maurya.dtxtodoapp.util.DataToDo
import com.maurya.dtxtodoapp.util.OnItemClickListener
import com.maurya.dtxtodoapp.util.formatDate
import java.util.Calendar

class HomeFragment : Fragment(), OnItemClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val incompleteTasks = mutableListOf<DataToDo>()
    private val completeTasks = mutableListOf<DataToDo>()

    private lateinit var incompleteAdapter: AdapterToDo
    private lateinit var completeAdapter: AdapterToDo

    private var isCompletedVisible = false

    /* ---------------- LIFECYCLE ---------------- */

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerViews()
        observeTasks()
        setupUiActions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /* ---------------- SETUP ---------------- */

    private fun setupRecyclerViews() {
        incompleteAdapter = AdapterToDo(this, incompleteTasks, completeTasks, false)
        completeAdapter = AdapterToDo(this, incompleteTasks, completeTasks, true)

        binding.recyclerViewInCompleteHomeFragment.setup(incompleteAdapter)
        binding.recyclerViewCompleteHomeFragment.setup(completeAdapter)
    }

    private fun observeTasks() {
        val userId = auth.currentUser?.uid ?: return

        db.userTasks(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    toast(error.localizedMessage.toString())
                    return@addSnapshotListener
                }

                val newIncomplete = mutableListOf<DataToDo>()
                val newComplete = mutableListOf<DataToDo>()

                snapshot?.documents?.forEach { doc ->
                    doc.toObject(DataToDo::class.java)
                        ?.copy(id = doc.id)
                        ?.let {
                            if (it.isChecked) newComplete.add(it)
                            else newIncomplete.add(it)
                        }
                }

                incompleteTasks.replaceWith(newIncomplete)
                completeTasks.replaceWith(newComplete)

                incompleteAdapter.notifyDataSetChanged()
                completeAdapter.notifyDataSetChanged()

                binding.completedLayout.visibility =
                    if (completeTasks.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    private fun setupUiActions() {
        binding.addTaskHomeFragment.setOnClickListener { showAddTaskDialog() }

        binding.completedLayout.setOnClickListener {
            isCompletedVisible = !isCompletedVisible
            binding.recyclerViewCompleteHomeFragment.visibility =
                if (isCompletedVisible) View.VISIBLE else View.GONE
            binding.completedImage.setImageResource(
                if (isCompletedVisible)
                    R.drawable.icon_completed_visible
                else
                    R.drawable.icon_completed_invisible
            )
        }

        binding.logOutUser.setOnClickListener { logout() }
    }

    /* ---------------- ADD / EDIT ---------------- */

    private fun showAddTaskDialog() {
        val userId = auth.currentUser?.uid ?: return

        showTaskDialog(
            onSubmit = { task ->
                db.userTasks(userId).add(task).onSuccess {
                    toast("Task Added")
                }
            }
        )
    }

    override fun onItemClickListener(position: Int, isComplete: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        val item = (if (isComplete) completeTasks else incompleteTasks)
            .getOrNull(position) ?: return

        showTaskDialog(
            task = item,
            onSubmit = { updated ->
                db.userTasks(userId)
                    .document(item.id)
                    .set(updated)
                    .onSuccess { toast("Task Updated") }
            },
            onDelete = {
                db.userTasks(userId)
                    .document(item.id)
                    .delete()
                    .onSuccess { toast("Task Deleted") }
            }
        )
    }

    override fun onItemCheckedChange(position: Int, isChecked: Boolean, isCompleteList: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        val item = (if (isCompleteList) completeTasks else incompleteTasks)
            .getOrNull(position) ?: return

        db.userTasks(userId)
            .document(item.id)
            .update("isChecked", isChecked)
            .onSuccess {
                toast(if (isChecked) "Task Completed" else "Task Marked Incomplete")
            }
    }

    /* ---------------- DIALOG ---------------- */

    private fun showTaskDialog(
        task: DataToDo? = null,
        onSubmit: (DataToDo) -> Unit,
        onDelete: (() -> Unit)? = null
    ) {
        val dialog = BottomSheetDialog(
            requireContext(),
            R.style.ThemeOverlay_App_BottomSheetDialog
        )

        val view = layoutInflater.inflate(R.layout.bottomsheet_add_task, null)
        dialog.setContentView(view)

        val ui = TaskDialogUi(view, ::showDatePickerDialog)
        task?.let { ui.bindData(it).enableEditMode() }

        ui.onSubmit {
            onSubmit(
                (task ?: DataToDo()).copy(
                    taskName = ui.taskNameText,
                    taskDetails = ui.taskDetailsText,
                    taskCompleteUpToDate = ui.taskDateText,
                    isImportant = ui.important
                )
            )
            dialog.dismiss()
        }

        ui.onDelete { onDelete?.invoke(); dialog.dismiss() }

        dialog.show()
    }


    private class TaskDialogUi(
        view: View,
        private val showDatePicker: (Chip, ImageView) -> Unit
    ) {
        private val taskName = view.findViewById<EditText>(R.id.addTaskEditText)
        private val taskDetails = view.findViewById<EditText>(R.id.addDetailsEditText)
        private val dateChip = view.findViewById<Chip>(R.id.addDateChipText)

        private val updateBtn = view.findViewById<TextView>(R.id.addTaskOKText)
        private val deleteBtn = view.findViewById<TextView>(R.id.addTaskDeleteText)

        private val importantBtn = view.findViewById<ImageView>(R.id.addImportantButton)
        private val detailsBtn = view.findViewById<ImageView>(R.id.addDetailsButton)
        private val dateBtn = view.findViewById<ImageView>(R.id.addDateButton)

        private val updateImage = view.findViewById<ImageView>(R.id.updateImage)
        private val lottie = view.findViewById<LottieAnimationView>(R.id.lotteView)

        private var isImportant = false
        private var isDetailsVisible = false
        private var isDateVisible = false

        val taskNameText get() = taskName.text.toString().trim()
        val taskDetailsText get() = taskDetails.text.toString()
        val taskDateText get() = dateChip.text.toString()
        val important get() = isImportant

        init {
            setupImportantToggle()
            setupDetailsToggle()
            setupDateToggle()
        }

        fun bindData(item: DataToDo) = apply {
            taskName.setText(item.taskName)
            taskDetails.setText(item.taskDetails)
            dateChip.text = item.taskCompleteUpToDate
            dateChip.visibility = View.VISIBLE

            isImportant = item.isImportant
            isDetailsVisible = true
            isDateVisible = item.taskCompleteUpToDate.isNotBlank()

            taskDetails.visibility = View.VISIBLE
            updateImportantIcon()
            updateDetailsIcon()
            updateDateIcon()
        }

        fun enableEditMode() = apply {
            updateBtn.text = "Update"
            updateImage.visibility = View.VISIBLE
            deleteBtn.visibility = View.VISIBLE
            lottie.visibility = View.GONE
        }

        fun onSubmit(block: () -> Unit) {
            updateBtn.setOnClickListener { block() }
        }

        fun onDelete(block: () -> Unit) {
            deleteBtn.setOnClickListener { block() }
        }

        /* ---------------- PRIVATE UI LOGIC ---------------- */

        private fun setupImportantToggle() {
            importantBtn.setOnClickListener {
                isImportant = !isImportant
                updateImportantIcon()
            }
        }

        private fun setupDetailsToggle() {
            detailsBtn.setOnClickListener {
                isDetailsVisible = !isDetailsVisible
                taskDetails.visibility = if (isDetailsVisible) View.VISIBLE else View.GONE
                if (!isDetailsVisible) taskDetails.text.clear()
                updateDetailsIcon()
            }
        }

        private fun setupDateToggle() {
            dateBtn.setOnClickListener {
                if (!isDateVisible) {
                    showDatePicker(dateChip, dateBtn)
                    isDateVisible = true
                } else {
                    dateChip.text = ""
                    dateChip.visibility = View.GONE
                    isDateVisible = false
                    updateDateIcon()
                }
            }

            dateChip.setOnClickListener {
                showDatePicker(dateChip, dateBtn)
                isDateVisible = true
                updateDateIcon()
            }
        }

        private fun updateImportantIcon() {
            importantBtn.setImageResource(
                if (isImportant) R.drawable.icon_mark_important
                else R.drawable.icon_mark_not_important
            )
        }

        private fun updateDetailsIcon() {
            detailsBtn.setImageResource(
                if (isDetailsVisible) R.drawable.icon_details_selected
                else R.drawable.icon_details
            )
        }

        private fun updateDateIcon() {
            dateBtn.setImageResource(
                if (isDateVisible) R.drawable.icon_date_selected
                else R.drawable.icon_date
            )
        }
    }


    /* ---------------- HELPERS ---------------- */

    private fun FirebaseFirestore.userTasks(userId: String) =
        collection("tasks").document(userId).collection("content")

    private fun RecyclerView.setup(adapter: RecyclerView.Adapter<*>) = apply {
        setHasFixedSize(true)
        setItemViewCacheSize(13)
        layoutManager = LinearLayoutManager(context)
        this.adapter = adapter
    }

    private fun MutableList<DataToDo>.replaceWith(newList: List<DataToDo>) {
        clear()
        addAll(newList)
    }

    private fun showDatePickerDialog(chip: Chip, btn: ImageView) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                chip.text = formatDate(
                    Calendar.getInstance().apply { set(y, m, d) }.timeInMillis
                )
                chip.visibility = View.VISIBLE
                btn.setImageResource(R.drawable.icon_date_selected)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun logout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                findNavController().navigate(R.id.action_homeFragment_to_signInFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    private fun <T> Task<T>.onSuccess(block: () -> Unit): Task<T> =
        addOnSuccessListener { block() }

}

