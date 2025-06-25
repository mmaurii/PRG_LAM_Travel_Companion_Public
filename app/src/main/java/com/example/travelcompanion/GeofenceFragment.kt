package com.example.travelcompanion

import GeofenceLocationDao
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.travelcompanion.databinding.FragmentGeofenceBinding

class GeofenceFragment : Fragment() {

    private var _binding: FragmentGeofenceBinding? = null
    private val binding get() = _binding!!
    private lateinit var dao: GeofenceLocationDao
    private lateinit var repository: GeofenceRepository
    private lateinit var factory:GeofenceViewModelFactory
    private lateinit var viewModel: GeofenceViewModel
    private lateinit var geofenceAdapter: GeofenceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentGeofenceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dao = AppDatabase.getDatabase(requireContext()).geofenceDao()
        repository = GeofenceRepository(dao)
        factory = GeofenceViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[GeofenceViewModel::class.java]

        val adapter = GeofenceAdapter { point ->
            viewModel.deleteGeofencePoint(point)
        }

        binding.rvGeofences.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        viewModel.geofencePoints.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        binding.btnAddGeofence.setOnClickListener {
            findNavController().navigate(R.id.action_geofence_list_to_add_geofence)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
