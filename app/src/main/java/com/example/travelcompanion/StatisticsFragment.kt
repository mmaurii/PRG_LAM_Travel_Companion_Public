package com.example.travelcompanion

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import androidx.navigation.fragment.findNavController

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class StatisticsFragment : Fragment() {
        private lateinit var btnMap: Button
        private  lateinit var btnStats: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_statistics, container, false)
    }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            btnMap = view.findViewById(R.id.btnViewHeatMap)
            btnStats = view.findViewById(R.id.btnViewStatsDetails)

            btnMap.setOnClickListener {
                findNavController().navigate(R.id.acttion_heat_Map)
            }

            btnStats.setOnClickListener {
                findNavController().navigate(R.id.acttion_statistics_details)
            }
        }
    }
