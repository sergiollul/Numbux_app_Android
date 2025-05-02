package com.example.numbux.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.numbux.R

@SuppressLint("MissingPermission")
object NotificationUtils {
    private const val CHANNEL_ID = "student_channel"
    private const val NOTIFICATION_ID = 2001

    /**
     * Muestra una notificación fija (ongoing) pidiendo activar accesibilidad.
     */
    fun showPersistentAccessibilityNotification(context: Context) {
        createNotificationChannel(context)

        // Al pulsar, abrir Ajustes de Accesibilidad
        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Activa Accesibilidad")
            .setContentText("Ve a: Apps Instaladas ->NumbuX ->Activar y Aceptar")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID, builder.build())
        } catch (se: SecurityException) {
            // Permiso POST_NOTIFICATIONS pudo haber sido rechazado
            se.printStackTrace()
        }
    }

    /**
     * Cancela la notificación si ya se activó accesibilidad.
     */
    fun cancelAccessibilityNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Student Notifications"
            val description = "Canal para notificaciones fijas de accesibilidad"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}