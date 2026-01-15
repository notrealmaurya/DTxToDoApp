package com.maurya.dtxtodoapp.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.maurya.dtxtodoapp.R
import com.maurya.dtxtodoapp.databinding.FragmentSplashBinding
import androidx.navigation.fragment.findNavController

class SplashFragment : Fragment() {

    private lateinit var fragmentSplashBinding: FragmentSplashBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        fragmentSplashBinding = FragmentSplashBinding.inflate(inflater, container, false)
        return fragmentSplashBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       val auth = FirebaseAuth.getInstance()

        Handler(Looper.myLooper()!!).postDelayed(
            {
                if (auth.currentUser != null) {
                    findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
                } else {
                    findNavController().navigate(R.id.action_splashFragment_to_signInFragment)
                }
            }, 2000
        )
    }
}
