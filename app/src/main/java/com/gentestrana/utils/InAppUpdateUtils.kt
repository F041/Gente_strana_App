package com.gentestrana.utils

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

object InAppUpdateUtils {
    private const val TAG = "InAppUpdateUtils"
    private const val UPDATE_REQUEST_CODE = 1001

    /**
     * Controlla se è disponibile un aggiornamento dell'app su Google Play Store
     * e avvia il flusso in-app update (FLEXIBLE: avviso non bloccante con download in background).
     */
    fun checkForUpdate(activity: Activity) {
        try {
            val appUpdateManager = AppUpdateManagerFactory.create(activity)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                Log.d(TAG, "Disponibilità aggiornamento: ${appUpdateInfo.updateAvailability()}")
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    Log.d(TAG, "Nuovo aggiornamento disponibile! Avvio del flusso In-App Update...")
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activity,
                            AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                            UPDATE_REQUEST_CODE
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore durante l'avvio del flusso di aggiornamento", e)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Impossibile verificare gli aggiornamenti in-app", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante l'inizializzazione di AppUpdateManager", e)
        }
    }

    /**
     * Ripristina il flusso di aggiornamento se era già stato avviato ed è ancora in corso.
     */
    fun checkResumeUpdate(activity: Activity) {
        try {
            val appUpdateManager = AppUpdateManagerFactory.create(activity)
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activity,
                            AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                            UPDATE_REQUEST_CODE
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore durante il ripristino dell'aggiornamento", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore in checkResumeUpdate", e)
        }
    }
}
