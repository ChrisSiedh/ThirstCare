package com.example.thirstcure

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.thirstcure.databinding.FragmentOverviewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*


class OverviewFragment : Fragment() {

    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!

    private val mFirebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private var date: String = ""
    private var fullval: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Binding-Objekt erstellen
        _binding = FragmentOverviewBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Datum Formatieren
        val sdf = SimpleDateFormat("d.M.yyyy")
        date = sdf.format(Date(binding.calendarView.date))

        //Laden der Trinkdaten
        loadDrinkData(date)

        //Datum auswählen
        binding.calendarView.setOnDateChangeListener{ _, year, mounth, day ->

            date = (day.toString()+"."+(mounth+1).toString()+"."+year.toString())

            //Trinkdaten mit dem ausgewählten Datum aktualisieren
            loadDrinkData(date)
        }

        //Detailbutton listener
        binding.button.setOnClickListener {

            //Bundle mit Datum und Trinkmenge erstellen
            val bundle = Bundle()
            bundle.putString("date", date)
            bundle.putInt("drinkVal", fullval)

            //Bundle setzen
            val OverviewDayFragment = OverviewDayFragment()
            OverviewDayFragment.arguments = bundle

            //OverviewDayFragment öffnen
            val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
            transaction.replace(R.id.fragmentContainerView,OverviewDayFragment)
            transaction.commit()
        }

    }

    //Funktion zum laden der Trinkdaten
    private fun loadDrinkData(currentDate:String) {
        //Firebase ID abrufen
        val uid = mFirebaseAuth.currentUser!!.uid
        //Gesamttrinkmenge zurücksetzen
        fullval = 0
        //Daten aus der Datenbank abrufen und Trinkmenge berechnen
        db.collection("user").document(uid).collection("userData").document(uid).collection(currentDate)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    for (document in task.result) {
                        var drinkval = document.getString("drinkValue")!!
                        fullval += drinkval.toInt()
                    }
                    //Gesamttrinkmenge setzen
                    binding.textView6.setText("$fullval ml")
                } else {
                    // Log.d(TAG, "FEHLER: Daten lesen ", task.exception)
                }
            }
    }
}
