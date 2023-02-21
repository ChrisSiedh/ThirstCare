package com.example.thirstcure

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.thirstcure.databinding.FragmentDrinksBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class DrinksFragment : Fragment(R.layout.fragment_drinks) {

    private var _binding: FragmentDrinksBinding? = null
    private val binding get() = _binding!!

    private val mFirebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var db:FirebaseFirestore = FirebaseFirestore.getInstance()

    private var list = ArrayList <String> ()
    private lateinit var adapter : ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Binding-Objekt erstellen
        _binding = FragmentDrinksBinding.inflate(inflater, container, false)
        adapter = ArrayAdapter( this.requireContext(),android.R.layout.simple_list_item_1,list)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lvDrinks.adapter = adapter

        //Getränke laden
        loadDrinks()

        //FloatingActionButton listener
        binding.fabAddDrink.setOnClickListener{

            //AlertDialog erstellen zum hinzufügen von Getränken
            val builder = AlertDialog.Builder(activity)
            val inflater = layoutInflater
            val dialogLayout:View = inflater.inflate(R.layout.edit_text_layout, null)
            val editText = dialogLayout.findViewById<EditText>(R.id.et_pwreset)
            editText.hint = getString(R.string.et_drink_hint)
            with(builder){
                setTitle(R.string.dialog_drink_title)
                setPositiveButton(R.string.dialog_drink_pos){dialog, which ->
                    val drink = editText.text.toString().trim()
                    if (drink.isNotEmpty()) {

                        //Getränk in DB speichern und der Liste hinzufügen
                        insertDrinkInDB(drink)
                        list.add(drink)
                        adapter.notifyDataSetChanged()
                    } else {
                    //
                    }
                }

                //Dialog abbrechen
                setNegativeButton(R.string.dialog_neg){dialog, which ->
                }
                setView(dialogLayout)
                show()
            }
        }


        //Listview Item anwählen
        binding.lvDrinks.setOnItemClickListener { adapterView, view, i, l ->

            //Angeklickte Item als String speichern
            val s: String = binding.lvDrinks.getItemAtPosition(i).toString()
            i
            //AlertDialog erstellen zum hinzufügen von Trinkmengen
            val builder = AlertDialog.Builder(activity)
            val inflater = layoutInflater
            val dialogLayout:View = inflater.inflate(R.layout.edit_text_layout, null)
            val editText = dialogLayout.findViewById<EditText>(R.id.et_pwreset)
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            editText.hint = getString(R.string.et_drink_hint)

            with(builder){
                setTitle("Wie viel haben Sie getrunken?")
                setPositiveButton("Speichern"){dialog, which ->
                    val drinkValue = editText.text.toString().trim()

                    //Wenn das Textfeld nicht Leer ist, wird die Funktion ausgeführt
                    if (drinkValue.isNotEmpty()) {
                        saveDrinkValue(s,drinkValue)
                    } else {
                        //
                    }
                }
                //Dialog abbrechen
                setNegativeButton(R.string.dialog_neg){dialog, which ->

                }
                setView(dialogLayout)
                show()
           }
        }

    }


    //Funktion zum Speichern von hinzugefügten Getränken
    private fun insertDrinkInDB(drink: String){
        //Firebase ID abrufen
        val uid = mFirebaseAuth.currentUser!!.uid

        //Objekt erstellen
        val drinks = userDrinks()
        drinks.setDrink(drink)

        //Getränke-Obkekt in DB speichern
        db.collection("user").document(uid).collection("userDrinks").add(drinks)
            .addOnSuccessListener { Toast.makeText(activity,"Erfolg",Toast.LENGTH_SHORT).show() }
            .addOnFailureListener{ Toast.makeText(activity,"fehler",Toast.LENGTH_SHORT).show() }

    }


    //Funktion zum Speichern der Trinkmenge
    private fun saveDrinkValue(drink: String, drinkValue: String){
        //Firebase ID abrufen
        val uid = mFirebaseAuth.currentUser!!.uid

        //Datum generieren
        var datetimestamp: Date? = null
        val dateFormat = SimpleDateFormat ("d.M.yyyy")
        val currentDate = dateFormat.format(Date()).toString()

        //Datum in ein Date-Objekt konvertieren
        try {
            datetimestamp = dateFormat.parse(currentDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        //Objekt erstellen
        val drinkData = userData()
        drinkData.setDrink(drink)
        drinkData.setValue(drinkValue)
        drinkData.setDateTimestamp(datetimestamp)

        //DrinkData-Objekt in DB speichern
        db.collection("user").document(uid).collection("userData").document(uid).collection(currentDate).add(drinkData)
            .addOnSuccessListener { Toast.makeText(activity,"Erfolg",Toast.LENGTH_SHORT).show() }
            .addOnFailureListener{ Toast.makeText(activity,"fehler",Toast.LENGTH_SHORT).show() }
    }


    private fun loadDrinks() {
        //Firebase ID abrufen
        val uid = mFirebaseAuth.currentUser!!.uid

        db.collection("user").document(uid).collection("userDrinks")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //ListView updaten
                    updateListView(task)
                } else {
                   // Log.d(TAG, "FEHLER: Daten lesen ", task.exception)
                }
            }
    }


    //Funktion zum updaten der Liste
    private fun updateListView(task: Task<QuerySnapshot>) {
        list = ArrayList()
        //Schleife zum Abfragen aller Dokumente
        for (document in task.result!!) {
            (list as ArrayList<userDrinks>).add(document.toObject(userDrinks::class.java))
        }
        //Vollständige Liste im ListView anzeigen
        adapter = ArrayAdapter( this.requireContext(),android.R.layout.simple_list_item_1,list)
        binding.lvDrinks.adapter = adapter
    }
}