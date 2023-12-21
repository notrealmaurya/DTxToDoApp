package com.maurya.dtxtodoapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.maurya.dtxtodoapp.databinding.FragmentSignInBinding


class SignInFragment : Fragment() {


private lateinit var fragmentSignInBinding: FragmentSignInBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentSignInBinding = FragmentSignInBinding.inflate(inflater, container, false)
        val view = fragmentSignInBinding.root













        return view;
    }



}