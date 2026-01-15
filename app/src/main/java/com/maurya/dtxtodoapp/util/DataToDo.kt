package com.maurya.dtxtodoapp.util


import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DataToDo(
    val id: String,
    val taskName: String,
    val taskDetails: String,
    val taskCompleteUpToDate: String,
    var isImportant: Boolean,
    var isChecked: Boolean = false
)


fun formatDate(selectedDateMillis: Long): String {
    val currentDate = Calendar.getInstance()
    val selectedDate = Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
    }

    return when {
        isSameDay(selectedDate, currentDate) -> "Today"
        isSameDay(
            selectedDate,
            (currentDate.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }) -> "Tomorrow"

        isSameDay(
            selectedDate,
            (currentDate.clone() as Calendar).apply {
                add(
                    Calendar.DAY_OF_MONTH,
                    -1
                )
            }) -> "Yesterday"

        else -> {
            val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            simpleDateFormat.format(selectedDate.time)
        }
    }
}


fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
            cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
}

fun checkInternet(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val networkCapabilities =
        connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

    return when {
        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        else -> false
    }

}



