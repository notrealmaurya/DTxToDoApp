package com.maurya.dtxtodoapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.maurya.dtxtodoapp.databinding.FragmentSignInBinding
import com.maurya.dtxtodoapp.databinding.FragmentSignUpBinding


class SignUpFragment : Fragment() {

    private lateinit var fragmentSignUpBinding: FragmentSignUpBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fragmentSignUpBinding = FragmentSignUpBinding.inflate(inflater, container, false)
        val view = fragmentSignUpBinding.root










        listeners()




        return view;
    }

    private fun listeners() {





    }


}