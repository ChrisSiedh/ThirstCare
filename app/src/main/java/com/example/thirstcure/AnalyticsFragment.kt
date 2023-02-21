package com.example.thirstcure
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.thirstcure.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsFragment : Fragment(com.example.thirstcure.R.layout.fragment_analytics) {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val mFirebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    lateinit var barChart: BarChart
    lateinit var pieChart: PieChart

    private var chartType = "bar"

    private lateinit var startDate: String
    private lateinit var endDate: String

    private lateinit var startCalendar: Calendar
    private lateinit var endCalendar: Calendar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Binding-Objekt erstellen
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        barChart = binding.barChart
        pieChart = binding.pieChart

        //Datum initialisieren
        startCalendar = Calendar.getInstance()
        endCalendar = Calendar.getInstance()

        //Startdatum auf die letzten 7 Tage setzen
        startCalendar.add(Calendar.DAY_OF_MONTH, -7)

        //Datum formatieren
        startDate = formatDate(startCalendar.timeInMillis)
        endDate = formatDate(endCalendar.timeInMillis)

        //Datum setzen
        binding.textView12.text = formatDate(startCalendar.timeInMillis)
        binding.textView13.text = formatDate(endCalendar.timeInMillis)

        binding.btnStartDate.text = formatDate(startCalendar.timeInMillis)
        binding.btnEndDate.text = formatDate(endCalendar.timeInMillis)

        //Balkendiagramm mit den Daten der letzten 7 Tagen aktualisieren
        getBarData(startDate,endDate)

        //Datepicker anzeigen
        binding.btnStartDate.setOnClickListener {
            showDatePicker(true)
        }
        binding.btnEndDate.setOnClickListener {
            showDatePicker(false)
        }

        //Diagramm wechseln
        binding.btnSwitchChart.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chartType = "pie"
                barChart.visibility = View.GONE
                pieChart.visibility = View.VISIBLE
                // Kuchendiagramm wird mit den aktuellen Daten aktualisiert
                getPieData(startDate, endDate)
            } else {
                chartType = "bar"
                barChart.visibility = View.VISIBLE
                pieChart.visibility = View.GONE
                // Balkendiagramm wird mit den aktuellen Daten aktualisiert
                getBarData(startDate, endDate)
            }
        }

    }


    //Funktion für den DatePicker
    private fun showDatePicker(start: Boolean) {
        //Kalenderobjekt erstellen
        val calendar = Calendar.getInstance()
        //DatePickerDialog erstellen
        val datePickerDialog = DatePickerDialog(
            this.requireContext(),
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                //Wenn Startdatum ausgewählt wird, wird dieses aktualisiert
                if (start) {
                    startCalendar.set(Calendar.YEAR, year)
                    startCalendar.set(Calendar.MONTH, month)
                    startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    //Datum anpassen und setzen
                    binding.btnStartDate.text = formatDate(startCalendar.timeInMillis)
                    binding.textView12.text = formatDate(startCalendar.timeInMillis)
                    startDate = formatDate(startCalendar.timeInMillis)

                    //Funktion je nach Diagrammtyp starten
                    if (chartType == "bar") {
                        getBarData(startDate, endDate)
                    }
                    else {
                        getPieData(startDate, endDate)
                    }
                } else {

                    //Wenn Enddatum ausgewählt wird, wird dieses aktualisiert
                    endCalendar.set(Calendar.YEAR, year)
                    endCalendar.set(Calendar.MONTH, month)
                    endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    //Datum anpassen und setzen
                    binding.btnEndDate.text = formatDate(endCalendar.timeInMillis)
                    binding.textView13.text = formatDate(endCalendar.timeInMillis)
                    endDate = formatDate(endCalendar.timeInMillis)

                    //Funktion je nach Diagrammtyp starten
                    if (chartType == "bar") {
                        getBarData(startDate, endDate)
                    }
                    else {
                        getPieData(startDate, endDate)
                    }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }


    //Funktion zum umwandeln von timestamps in Strings
    private fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("d.M.yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }


    //Funktion zum abrufen der Daten für das Balkendiagramm
    fun getBarData(startDate: String, endDate: String) {

        //Zeitraum erstellen
        val dateRange = getDateRange(startDate, endDate)

        //Firebase ID abrufen
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val userDataRef = FirebaseFirestore.getInstance().collection("user").document(uid).collection("userData").document(uid)

        //Map erstellen für tägliche Getränkewerte
        val drinkValuesByDate = mutableMapOf<String, Double>()

        //Task erstellen
        val tasks = mutableListOf<Task<Double>>()

        //Jedes Kollektion im Zeitraum durchlaufen
        for (date in dateRange) {
            tasks.add(userDataRef.collection(date).get().continueWith { task ->

                //Gesamtgetränkewert für den jeweiligen Tag berechnen
                var totalDrinkValue = 0.0
                for (document in task.result!!) {
                    totalDrinkValue += document.get("drinkValue").toString().toDouble()
                }

                //Wert speichern
                drinkValuesByDate[date] = totalDrinkValue
                totalDrinkValue
            })
        }

        //Wenn alle Tasks abegeschlossen sind wird die Map sortiert (Datum)
        Tasks.whenAllSuccess<Double>(tasks).addOnSuccessListener {
            val sortedDrinkValuesByDate = drinkValuesByDate.toSortedMap(compareBy {
                SimpleDateFormat("d.M.yyyy", Locale.getDefault()).parse(it)
            })

            //Balkendiagramm erstellen
            updateBarChart(sortedDrinkValuesByDate)
        }
    }


    //Funktion zum abrufen der Daten für das Kuchendiagramm
    fun getPieData(startDate: String, endDate: String) {

        //Zeitraum erstellen
        val dateRange = getDateRange(startDate, endDate)

        //Firebase ID abrufen
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val userDataRef = FirebaseFirestore.getInstance().collection("user").document(uid).collection("userData").document(uid)

        //Map erstellen für Getränke und Trinkmenge
        val drinkByType = mutableMapOf<String, Double>()

        //Task erstellen
        val tasks = mutableListOf<Task<Double>>()

        //Jedes Kollektion im Zeitraum durchlaufen
        for (date in dateRange) {
            tasks.add(userDataRef.collection(date).get().continueWith { task ->
                var totalDrinkValue = 0.0

                //Jedes Dokument der Kollektionen durchlaufen und Trinkdaten holen
                for (document in task.result!!) {
                    val drink = document.get("drink").toString()
                    val drinkValue = document.get("drinkValue").toString().toDouble()

                    totalDrinkValue += drinkValue

                    //Wenn die Map das Getränk enthält wird dieses aufaddiert
                    if (drinkByType.containsKey(drink)) {
                        drinkByType[drink] = drinkByType[drink]!! + drinkValue
                    } else {
                        drinkByType[drink] = drinkValue
                    }
                }
                totalDrinkValue
            })
        }

        //Kuchendiagramm aktualisieren
        Tasks.whenAll(tasks).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updatePieChart(drinkByType)
            } else {
                // Handle error
            }
        }
    }


    //Funktion zum erstellen einer Liste mit Tagen die im ausgewählten Zeitraum liegen
    private fun getDateRange(startDate: String, endDate: String): List<String> {
        //Liste erstellen
        val dateRange = mutableListOf<String>()

        //Datum umwandeln
        val start = SimpleDateFormat("d.M.yyyy", Locale.getDefault()).parse(startDate)
        val end = SimpleDateFormat("d.M.yyyy", Locale.getDefault()).parse(endDate)

        //Jedes Datum zwischen Start- und Enddatum der Liste hinzufügen
        val calendar = Calendar.getInstance()
        calendar.time = start
        while (calendar.time.before(end) || calendar.time.equals(end)) {
            dateRange.add(SimpleDateFormat("d.M.yyyy", Locale.getDefault()).format(calendar.time))
            calendar.add(Calendar.DATE, 1)
        }

        //Liste sortieren und zurückgeben
        return dateRange.sortedWith(Comparator { date1, date2 ->
            SimpleDateFormat("d.M.yyyy", Locale.getDefault()).parse(date1).compareTo(
                SimpleDateFormat("d.M.yyyy", Locale.getDefault()).parse(date2)
            )
        })
    }


    //Funktion zum aktualisieren des Balkendiagramms
    private fun updateBarChart(drinkValuesByDate: MutableMap<String, Double>) {

        //Liste für Balkeneinträge erstellen
        val barEntries = mutableListOf<BarEntry>()

        //Index für Einträge
        var i = 0f

        //Firebase ID abrufen
        val uid = mFirebaseAuth.currentUser!!.uid

        //Grenzlinie (Tagesziel)
        getLimit(uid) { limit ->

            //Liste von Farben erstellen
            val colors = mutableListOf<Int>()

            //Farben für jeden Eintrag zuweisen
            for ((date, totalDrinkValue) in drinkValuesByDate) {
                val color = if (totalDrinkValue > limit) Color.BLUE else Color.RED
                barEntries.add(BarEntry(i, totalDrinkValue.toFloat()))
                colors.add(color)
                i++
            }

            //Datensatz erstellen und Balkendiagramm anpassen
            val barDataSet = BarDataSet(barEntries, "Drink Value")
            barDataSet.colors = colors
            barDataSet.valueTextColor = Color.BLACK
            barDataSet.valueTextSize = 14f
            barDataSet.valueFormatter = object : ValueFormatter() {

                //" ml" hinzufügen
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()} ml"
                }
            }

            val barData = BarData(barDataSet)

            val yAxis = barChart.axisLeft

            val limitLine = LimitLine(limit)
            limitLine.lineWidth = 2f
            limitLine.lineColor = Color.GREEN
            limitLine.textColor = Color.GREEN
            limitLine.textSize = 10f

            yAxis.addLimitLine(limitLine)

            barChart.data = barData
            barChart.xAxis.textSize = 12f
            barChart.xAxis.valueFormatter = IndexAxisValueFormatter(drinkValuesByDate.keys.toList())
            barChart.xAxis.granularity = 1f
            barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            barChart.xAxis.setDrawGridLines(false)
            barChart.axisLeft.textSize = 12f
            barChart.axisRight.isEnabled = false
            barChart.legend.isEnabled = false
            barChart.description.isEnabled = false
            barChart.animateY(1000)
            barChart.invalidate()
        }
    }


    //Funktion zum abrufen des Trinkziels
    private fun getLimit(uid: String, callback: (Float) -> Unit) {
        db.collection("user").document(uid).collection("userSettings").document("dailyIntake")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val doc = task.result

                    //Limit in Float umwandeln
                    val limit = doc.getString("daylimit").toString().toFloat()

                    //Limit als Parameter zurückgeben
                    callback(limit)
                } else {
                    // Log.d(TAG, "FEHLER: Daten lesen ", task.exception)
                }
            }
    }


    //Funktion zum aktualisieren des Kuchendiagramms
    private fun updatePieChart(drinkData: Map<String, Double>) {

        //Liste für Einträge im Kreisdiagramm erstellen
        val entries = mutableListOf<PieEntry>()

        //Liste für die Farben
        val colors = mutableListOf<Int>()
        var i = 0

        //Trinkdaten durchlaufen und Einträge erstellen
        for ((drink, drinkValue) in drinkData) {
            entries.add(PieEntry(drinkValue.toFloat(), drink))
            colors.add(ColorTemplate.COLORFUL_COLORS[i % ColorTemplate.COLORFUL_COLORS.size])
            i++
        }

        //Datensatz erstellen und Einträge anpassen
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()} ml"
            }
        }
        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.animateY(1000)
        pieChart.legend.isEnabled = true
        pieChart.legend.textSize = 16f
        pieChart.legend.form = Legend.LegendForm.CIRCLE
        pieChart.legend.formSize = 14f
        pieChart.legend.xEntrySpace = 5f
        pieChart.legend.yEntrySpace = 5f
        pieChart.legend.textColor = Color.BLACK
        pieChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        pieChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        pieChart.legend.orientation = Legend.LegendOrientation.VERTICAL
        pieChart.legend.setDrawInside(false)
        pieChart.legend.yOffset = 0f
        pieChart.legend.xOffset = 10f
        pieChart.legend.yEntrySpace = 0f
        pieChart.description.isEnabled = false
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(14f)
        pieChart.invalidate()
    }

}


