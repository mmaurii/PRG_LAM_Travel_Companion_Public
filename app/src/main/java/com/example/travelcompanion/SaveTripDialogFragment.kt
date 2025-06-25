package com.example.travelcompanion

import android.app.Dialog
import android.icu.text.CaseMap.Title
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class SaveTripDialogFragment(
    val onSave: (String, String, String) -> Unit,
    val onCancel: () -> Unit,
    val disableRadioOptions: Boolean
) :
    DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_save_trip, null)
        val titleInput = view.findViewById<EditText>(R.id.titleInput)
        val notesInput = view.findViewById<EditText>(R.id.notesInput)
        val radioGroup = view.findViewById<RadioGroup>(R.id.rgTripType)

        // Disattiva le opzioni se necessario
        if (disableRadioOptions) {
            view.findViewById<RadioButton>(R.id.rbLocalTrip).isEnabled = false
            view.findViewById<RadioButton>(R.id.rbDayTrip).isEnabled = false
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Inserisci i dati del viaggio")
            .setView(view)
            .setNegativeButton("Annulla") { _, _ ->
                onCancel()
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val title = titleInput.text.toString()
                val notes = notesInput.text.toString()
                val selectedOptionId = radioGroup.checkedRadioButtonId
                val selectedOption =
                    view.findViewById<RadioButton>(selectedOptionId)?.text?.toString() ?: ""

                if (title.isNotEmpty() && selectedOption.isNotEmpty()) {
                    onSave(title, notes, selectedOption)
                    dialog.dismiss()  // chiudi manualmente solo se ok
                } else {
                    Toast.makeText(context, "Completa i campi necessari", Toast.LENGTH_SHORT).show()
                    // NON chiudo il dialog
                }
            }
        }

        // aggiungi anche il pulsante "Salva"
        dialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            "Salva"
        ) { _, _ -> /* Vuoto, gestito sopra */ }

        return dialog
    }
}
