package com.example.thirstcure

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        //ID erstellen
        val channelId = "default_channel_id"

        //Titel anpassen
        val title = "ThirstCare Erinnerung"

        //Nachricht anpassen
        val message = "Es ist Zeit etwas zu trinken"

        //Activity öffnen wenn angekickt
        val notificationIntent = Intent(context, LoginActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        //Benachrichtigungsobjekt mit den oben definierten Attributen erstellen
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_baseline_local_drink_24)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        //Abrufen des Benachrichtigungsmanagers und Anzeigen der Benachrichtigung
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notification)
    }
}
