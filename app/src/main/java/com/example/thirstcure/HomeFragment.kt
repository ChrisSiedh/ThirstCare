package com.example.thirstcure

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.thirstcure.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val mFirebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Binding-Objekt erstellen
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Aktuelle Daten laden
        loadDayReport()

        //FloatinActionButton Listener
        binding.fabDrink.setOnClickListener{
            //DrinkFragment öffnen
            val DrinksFragment = DrinksFragment()
            val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
            transaction.replace(R.id.fragmentContainerView,DrinksFragment)
            transaction.commit()
        }
    }

    //Funktion für den aktuellen Trinkstatus
    private fun loadDayReport() {

        //Firebase ID abrufen
        val uid = mFirebaseAuth.currentUser!!.uid

        //Aktuelles Datum formatieren und generieren
        val dateFormat = SimpleDateFormat ("d.M.yyyy")
        val currentDate = dateFormat.format(Date()).toString()

        //Tagesziel mit dem aktuellen Datum laden
        db.collection("user").document(uid).collection("userSettings").document("dailyIntake")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val doc = task.result

                    //Tagesziel auslesen
                    val intakeMax = doc.getString("daylimit")

                    //Progessbar und TextView aktualisieren
                    if (!intakeMax.isNullOrEmpty()) {
                        val progressmax = intakeMax.toInt() * 100
                        binding.progressBar.max = progressmax
                        binding.tvTargetLimit.text = "$intakeMax ml"
                        binding.textView5.text = intakeMax

                        //ProgressBar updaten
                        updateProgressBar(uid, currentDate)
                    }
                } else {
                    // Log.d(TAG, "FEHLER: Daten lesen ", task.exception)
                }
            }
    }


    //Funktion zum updaten der progressBar und für die aktuelle Trinkmenge
    private fun updateProgressBar(uid: String, currentDate: String) {
        db.collection("user").document(uid).collection("userData").document(uid).collection(currentDate)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    //Daten berechnen und formatieren
                    var fullval: Double = 0.0
                    val df = DecimalFormat("#")
                    for (document in task.result) {
                        var drinkval = document.getString("drinkValue")!!
                        fullval += (drinkval.toFloat())
                    }

                    //Progressbar updaten
                    binding.progressBar.progress = (fullval * 100).toInt()
                    binding.textView4.text = df.format(fullval).toString()

                } else {
                    // Log.d(TAG, "FEHLER: Daten lesen ", task.exception)
                }
            }
    }
}