package com.maurya.dtxtodoapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.maurya.dtxtodoapp.databinding.FragmentSignInBinding
import com.maurya.dtxtodoapp.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    private lateinit var fragmentSplashBinding: FragmentSplashBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        fragmentSplashBinding = FragmentSplashBinding.inflate(inflater, container, false)
        val view = fragmentSplashBinding.root













        return view;
    }



}