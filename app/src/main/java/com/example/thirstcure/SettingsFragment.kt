package com.example.thirstcure

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.thirstcure.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*


class SettingsFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val mFirebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private var timeList = mutableListOf<String>()
    private var timeArrayAdapter: ArrayAdapter<String>? = null


    private val sharedPreferences: SharedPreferences by lazy {
        requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Binding-Objekt erstellen
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Status der Benachrichtigungen aus den SharedPreferences laden
        val isNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", false)

        //Schalter setzen
        binding.btnSwitchNotification.isChecked = isNotificationsEnabled

        //Elemente anpassen
        binding.btnAddTime.isEnabled = isNotificationsEnabled
        binding.lvTimes.isEnabled = isNotificationsEnabled
        binding.btnAddTime.isClickable = isNotificationsEnabled

        //Liste der Benachrichtigungszeiten aus den SharedPreferences laden
        timeList = sharedPreferences.getStringSet("notification_times", setOf())?.toMutableList() ?: mutableListOf()

        //ArrayAdapter erstellen und verknüpfen
        timeArrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, timeList)
        binding.lvTimes.adapter = timeArrayAdapter

        //Switchbutton listener
        binding.btnSwitchNotification.setOnCheckedChangeListener { _, isChecked ->
            //Status speichern
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()

            //Elemente anpassen
            binding.btnAddTime.isEnabled = isChecked
            binding.lvTimes.isEnabled = isChecked
            binding.btnAddTime.isClickable = isChecked

            //Wenn der Switchbutton aktiviert wurde, richte die Benachrichtigung ein
            if (isChecked) {
                setNotifications()
            } else {
                cancelNotifications()
            }
        }

        //ListViewelement listener (longclick)
        binding.lvTimes?.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->

            //Ausgewählte Benachrichtigung entfernen und Adapter benachrichtigen
            timeList.removeAt(position)
            timeArrayAdapter?.notifyDataSetChanged()

            //Änderungen speichern
            sharedPreferences.edit().putStringSet("notification_times", timeList.toSet()).apply()
            if (binding.btnSwitchNotification.isChecked) {
                setNotifications()
            }

            //Snackbar Nachricht
            Snackbar.make(requireView(), "Zeit gelöscht", Snackbar.LENGTH_LONG).show()
            true
        }

        //Listener für den Button um Zeiten hinzuzufügen
        binding.btnAddTime?.setOnClickListener {

            //Kalenderobjekt erstellen und Stunden und Minuten entnehmen
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            //TimePickerDialog erstellen
            val timePickerDialog = TimePickerDialog(this.requireContext(), TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
                var selectedMinuteStr = selectedMinute.toString()

                //Wenn Minuten < 10 sind eine 0 an die Zeit hinzufügen (z.B. 12:00 statt 12:0)
                if (selectedMinute < 10) {
                    selectedMinuteStr = "0$selectedMinute"
                }

                val selectedTime = "$selectedHour:$selectedMinuteStr"

                //Liste überprüfen ob die Zeit schon existiert
                if (!timeList.contains(selectedTime)) {

                    //Ausgewählte Zeit zur Liste hinzufügen und speichern
                    timeList.add(selectedTime)
                    sharedPreferences.edit().putStringSet("notification_times", timeList.toSet()).apply()
                    timeArrayAdapter?.notifyDataSetChanged()
                    setNotifications()
                } else {
                    Snackbar.make(requireView(),"Diese Zeit befindet sich bereits in der Liste", Snackbar.LENGTH_LONG).show()
                }
            }, hour, minute, true)

            timePickerDialog.show()
        }

        //Einstellungen laden
        loadSettings()

        //Fragebutton
        binding.btnQuestionLimit.setOnClickListener(this)
        binding.button2.setOnClickListener(this)
    }

    //Funktion für die Einrichtung von Benachrichtigungen
    private fun setNotifications() {
        //AlarmManager für Benachrichtigungen
        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        //Intent zum auslösen des Broadcast Receivers
        val notificationIntent = Intent(requireContext(), NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        //Benachrichtigungskanal für Android Oreo und höher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "default_channel_id"
            val channelName = "Default Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        //Benachrichtigungen für jede Uhrzeit in der timeList einrichten
        for (timeString in timeList) {

            //Zeiten in Stunden und Minuten aufteilen
            val (hour, minute) = timeString.split(":").map { it.toInt() }

            //KalenderObjekt für die Benachrichtigungszeit einrichten
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            // Benachrichtigung einrichten
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        }
    }

    //Funktion zum löschen von Benachrichtigungen
    private fun cancelNotifications() {
        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationIntent = Intent(requireContext(), NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        //Alle ausstehenden Benachrichtigungen löschen
        alarmManager.cancel(pendingIntent)
    }


    //Buttonlistener Funktion
    override fun onClick(p0: View?) {
        when (p0?.id){
            binding.btnQuestionLimit.id -> {
                Snackbar.make(requireView(),"Geben Sie Hier die gewünschte tägliche Trinkmenge ein.", Snackbar.LENGTH_LONG)
                    .setAction("Tagesziel",null).show()
            }
            binding.button2.id ->{
                val limit:String = binding.editTextNumberDecimal.text.toString()
                saveSettings(limit)
            }
        }
    }

    //Funktion zum laden der Einstellungen aus der Datenbank
    private fun loadSettings() {

        //Firebase ID abrufen
        val uid = mFirebaseAuth.currentUser!!.uid

        //Tagesziel abrufen
        db.collection("user").document(uid).collection("userSettings").document("dailyIntake")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val doc = task.result

                    //Den Wert auslesen und setzen
                    val intakeMax = doc.getString("daylimit")
                    binding.editTextNumberDecimal.setText(intakeMax)

                } else {
                    // Log.d(TAG, "FEHLER: Daten lesen ", task.exception)
                }
            }
    }


    private fun saveSettings(limit: String){
        //Firebase ID abrufen
        val uid = mFirebaseAuth.currentUser!!.uid

        //Daylimit Objekt erstellen
        val Daylimit = userSettings()
        Daylimit.setDaylimit(limit)

        //Objekt in Firebase speichern
        db.collection("user").document(uid).collection("userSettings").document("dailyIntake").set(Daylimit)
            .addOnSuccessListener { Toast.makeText(activity,"Erfolg", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener{ Toast.makeText(activity,"fehler", Toast.LENGTH_SHORT).show() }
    }


    override fun onPause() {
        super.onPause()
        //Zeiten speichern
        sharedPreferences.edit().putStringSet("notification_times", timeList.toSet()).apply()
    }


    override fun onResume() {
        super.onResume()
        //Zeiten laden
        timeList = sharedPreferences.getStringSet("notification_times", setOf())?.toMutableList() ?: mutableListOf()
        timeArrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, timeList)
        binding.lvTimes.adapter = timeArrayAdapter
    }
}