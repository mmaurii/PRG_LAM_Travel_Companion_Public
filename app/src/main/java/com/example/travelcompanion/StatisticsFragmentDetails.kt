package com.example.travelcompanion

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import okhttp3.internal.platform.android.ConscryptSocketAdapter.Companion.factory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatisticsFragmentDetails : Fragment() {
    private lateinit var barChart: BarChart
    private lateinit var yearButton: Button
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)
    private lateinit var tripViewModel: TripViewModel

    val valueFormatter = object: ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return value.toInt().toString()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = TripViewModelFactory(requireActivity().application)
        tripViewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]

        barChart = view.findViewById(R.id.barChart)
        yearButton = view.findViewById(R.id.btnInsertYear)

        yearButton.text = selectedYear.toString()
        yearButton.setOnClickListener { showYearPicker() }

        styleBarChart(barChart)
        setupBarChart(selectedYear)
    }

    private fun showYearPicker() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, _, _ ->
                selectedYear = year
                yearButton.text = year.toString()
                setupBarChart(year)
            },
            selectedYear,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Limita la selezione solo all’anno
        (dialog.datePicker as? DatePicker)?.apply {
            findViewById<View>(
                resources.getIdentifier("day", "id", "android")
            )?.visibility = View.GONE
            findViewById<View>(
                resources.getIdentifier("month", "id", "android")
            )?.visibility = View.GONE
        }

        dialog.show()
    }

    private fun setupBarChart(year: Int) {
        val db = AppDatabase.getDatabase(requireContext())

        //eliminino eventuali viaggi corrotti
        tripViewModel.deleteCorruptedTrip()

        db.tripDao().getAllTripsWithPoints().observe(viewLifecycleOwner) { tripWithPointsList ->
            // Calcola il numero di viaggi per mese nel dato anno
            val monthlyTrips = IntArray(12) { 0 }

            for (tripWithPoints in tripWithPointsList) {
                val trip = tripWithPoints.trip
                val calendar = Calendar.getInstance()
                calendar.time = Date(trip.date.toLong())

                val tripYear = calendar.get(Calendar.YEAR)
                val tripMonth = calendar.get(Calendar.MONTH) // 0-based

                if (tripYear == year) {
                    monthlyTrips[tripMonth] += 1
                }
            }

            // Mappa su BarEntry
            val entries = monthlyTrips.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value.toFloat())
            }

            val dataSet = BarDataSet(entries, "Viaggi nel $year")
            dataSet.color = ContextCompat.getColor(requireContext(), R.color.nightPurple)
            dataSet.valueTextSize = 14f // dimensione del numero sopra la barra
            dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.nightPurple)
            dataSet.valueFormatter = valueFormatter

            val data = BarData(dataSet)
            data.barWidth = 0.9f
            barChart.data = data

            val months = listOf(
                "Gen",
                "Feb",
                "Mar",
                "Apr",
                "Mag",
                "Giu",
                "Lug",
                "Ago",
                "Set",
                "Ott",
                "Nov",
                "Dic"
            )
            val xAxis = barChart.xAxis
            xAxis.valueFormatter = IndexAxisValueFormatter(months)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.labelCount = 12

            barChart.axisRight.isEnabled = false
            barChart.axisLeft.axisMinimum = 0f
            barChart.description.isEnabled = false
            barChart.setFitBars(true)
            barChart.animateY(1000)
            barChart.invalidate()
        }
    }

    private fun styleBarChart(barChart: BarChart) {
        val textColor = R.color.nightPurple
        val gridColor = R.color.nightPurpleDark
        val barColor = R.color.purple

        val xAxis = barChart.xAxis
        xAxis.textSize = 14f  // dimensione del testo in sp (float)
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.blue)
        xAxis.textColor = textColor
        xAxis.gridColor = gridColor
        xAxis.axisLineColor = textColor

        val leftAxis = barChart.axisLeft
        leftAxis.textSize = 14f
        leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.blue)
        leftAxis.textColor = textColor
        leftAxis.gridColor = gridColor
        leftAxis.axisLineColor = textColor
        leftAxis.valueFormatter = valueFormatter

        barChart.axisRight.isEnabled = false

        barChart.legend.textColor = textColor
        barChart.description.isEnabled = false

        // Modifica anche il colore delle barre
        (barChart.data?.getDataSetByIndex(0) as? BarDataSet)?.color = barColor

        barChart.invalidate()
    }


}