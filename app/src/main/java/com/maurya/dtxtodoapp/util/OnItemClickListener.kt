package com.maurya.dtxtodoapp.util


interface OnItemClickListener {
    fun onItemClickListener(position: Int,isComplete:Boolean)
    fun onItemCheckedChange(position: Int, isChecked: Boolean)

}