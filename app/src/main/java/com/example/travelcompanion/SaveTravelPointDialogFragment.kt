package com.example.travelcompanion

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class SaveTravelPointDialogFragment(val onSave: (String, String) -> Unit, val onCancel: () -> Unit) :
        DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_save_travel_point, null)
            val titleInput = view.findViewById<EditText>(R.id.titleInput)
            val notesInput = view.findViewById<EditText>(R.id.notesInput)

            return AlertDialog.Builder(requireContext())
                .setTitle("Inserisci i dati del viaggio")
                .setView(view)
                .setPositiveButton("Salva") { _, _ ->
                    val title = titleInput.text.toString()
                    val notes = notesInput.text.toString()
                    onSave(title, notes)
                }
                .setNegativeButton("Annulla") { _, _ ->
                    onCancel()
                }
                .create()
        }
    }