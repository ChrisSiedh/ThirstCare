package com.example.thirstcure

import android.R
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import com.example.thirstcure.databinding.FragmentOverviewDayBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot


class OverviewDayFragment : Fragment() {

    private var _binding: FragmentOverviewDayBinding? = null
    private val binding get() = _binding!!

    private val mFirebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private var list = ArrayList <String> ()
    private lateinit var adapter : ArrayAdapter<String>
    private lateinit var adapternewlist : ArrayAdapter<String>
    private lateinit var listView: ListView

    private lateinit var piechart:PieChart
    private val pieList: ArrayList<PieEntry> = ArrayList()
    private var newdrinkval:Float = 0.0f

    private var daylimit: Float = 0.0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        // Binding-Objekt erstellen
        _binding = FragmentOverviewDayBinding.inflate(inflater, container, false)
        listView = binding.listData
        list = ArrayList()
        adapter = ArrayAdapter(this.requireContext(), android.R.layout.simple_list_item_1, list)
        listView.adapter = adapter
        return binding.root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Bundle abrufen
        val args = this.arguments
        val date = args?.get("date").toString()
        val fullval = args?.get("drinkVal")?.toString()?.toFloat()

        //Datum und Trinkwert setzen
        binding.textView8.text = date
        binding.textView11.text = fullval.toString() + " ml"

        //Kuchendiagram
        piechart = binding.pieChart

        //Funktionen zum abrufen der Daten
        loadDrinks(date)
        loadDrinkData(date)
        getDailyLimit(fullval!!)
    }

    //Funktion zum laden der Getränke
    private fun loadDrinks(currentDate:String) {
        //Firebase ID abrufen
        val uid = mFirebaseAuth.currentUser!!.uid
        //Getränke des Datums laden
        db.collection("user").document(uid).collection("userData").document(uid).collection(currentDate)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    updateListView(task)
                } else {
                    // Log.d(TAG, "FEHLER: Daten lesen ", task.exception)
                }
            }
    }


    //Funktion zum laden der Trinkdaten
    private fun loadDrinkData(currentDate:String) {
        //Firebase ID abrufen
        val uid = mFirebaseAuth.currentUser!!.uid
        db.collection("user").document(uid).collection("userData").document(uid).collection(currentDate)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Liste erstellen
                    val newlist: MutableList<String> = ArrayList()
                    //Schleife zum Abfragen aller Dokumente
                    for (document in task.result!!) {
                        val drink = document.getString("drink")
                        val drinkvalue = document.getString("drinkValue")
                        val drinkValue = ("$drink $drinkvalue ml")
                        loadPieData(drink.toString(),uid,currentDate)
                        (newlist as ArrayList<String>).add(drinkValue)
                    }
                    //Vollständige Liste im ListView anzeigen
                    adapternewlist = ArrayAdapter( this.requireContext(), R.layout.simple_list_item_1,newlist)

                } else {
                    // Log.d(TAG, "FEHLER: Daten lesen ", task.exception)
                }
            }
    }


    //Funktion zum laden der Daten für das Kuchendiagram
    private fun loadPieData(Drink:String,uid:String,currentDate:String){
        //Datenbank nach Getränke filtern
        db.collection("user").document(uid).collection("userData").document(uid)
            .collection(currentDate)
            .whereEqualTo("drink", Drink)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    //Alle Dokumente durchlaufen
                    for (document in task.result!!) {
                        //Trinkmenge berechnen
                        val dv = document.getString("drinkValue")
                        newdrinkval += dv!!.toFloat()
                        //Log.d("test", drink + dv + " | " + task)
                    }

                    //Prüfen ob Getränk bereits in der Liste ist
                    if (!pieList.any { it.label == Drink}){
                        Log.d("test", "$Drink $newdrinkval | ")
                        //Eintrag hinzufügen
                        pieList.add(PieEntry(newdrinkval,Drink))
                    }

                    //Kuchendiagramm erstellen und Anpassen
                    val pieDataSet = PieDataSet(pieList,"")
                    pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS, 255)
                    pieDataSet.valueTextSize = 14f
                    pieDataSet.valueTextColor = Color.BLACK
                    val pieData = PieData(pieDataSet)
                    piechart.data = pieData
                    piechart.description.text = ""
                    piechart.centerText = ""
                    piechart.legend.textSize = 16f
                    piechart.animateY(1000)
                    piechart.setEntryLabelColor(Color.BLACK)
                    piechart.setEntryLabelTextSize(14f)
                    piechart.invalidate()
                }
                else {
                    // Log.d(TAG, "FEHLER: Daten lesen ", task.exception)
                }
                //newdrinkval auf zurücksetzen
                newdrinkval = 0.0f
            }
    }


    //Funktion zum abrufen des Tagesziels
    private fun getDailyLimit(fullval: Float) {
        //Firebase ID abrufen
        val uid = mFirebaseAuth.currentUser!!.uid

        db.collection("user").document(uid).collection("userSettings").document("dailyIntake")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Tagesziel abrufen
                    val doc = task.result
                    daylimit = doc.getString("daylimit")?.toFloatOrNull() ?: 0.0f

                    //Farbe dem Ziel anpassen und setzen
                    binding.textView9.text = if (fullval >= daylimit) {
                        binding.textView9.setTextColor(Color.GREEN)
                        "Tagesziel erreicht"
                    } else {
                        binding.textView9.setTextColor(Color.RED)
                        "Tagesziel nicht erreicht"
                    }
                } else {
                    // Handle error
                }
            }
    }


    //Funktion zum updaten der Liste
    private fun updateListView(task: Task<QuerySnapshot>) {
        list.clear()
        //Schleife zum Abfragen aller Dokumente
        for (document in task.result!!) {
            val drink = document.getString("drink")
            val drinkValue = document.getString("drinkValue")
            val listEnt = "$drink $drinkValue ml"
            list.add(listEnt)
        }
        //Vollständige Liste im ListView anzeigen
        adapter.notifyDataSetChanged()
    }
}


