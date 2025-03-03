package com.gentestrana.utils


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale

object LocationUtils {
    @SuppressLint("MissingPermission") // Suppress warning, permission check is done
    fun getCurrentLocationName(context: Context): OperationResult<String> {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        Log.d("LocationUtils", "getCurrentLocationName: Inizio funzione") // LOG: Inizio funzione

        // 1. Check for location permissions
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("LocationUtils", "Permessi non concessi") // LOG: Permessi non concessi
            return OperationResult.Error("Permessi di localizzazione non concessi") // Location permissions not granted
        }
        Log.d("LocationUtils", "Permessi concessi") // LOG: Permessi concessi

        // 2. Check if GPS is enabled
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isGpsEnabled) {
            Log.w("LocationUtils", "GPS non abilitato") // LOG: GPS non abilitato
            return OperationResult.Error("GPS non abilitato") // GPS is not enabled
        }
        Log.d("LocationUtils", "GPS abilitato") // LOG: GPS abilitato

        try {
            Log.d("LocationUtils", "Tentativo di ottenere lastKnownLocation...") // LOG: Tentativo lastKnownLocation
            // 3. Get last known location (can be null)
            val lastLocation: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if (lastLocation == null) {
                Log.w("LocationUtils", "lastLocation è NULL") // LOG: lastLocation è NULL
                return OperationResult.Error("Posizione non disponibile") // Last known location is null
            }
            Log.d("LocationUtils", "lastLocation ottenuta: $lastLocation") // LOG: lastLocation ottenuta

            // 4. Reverse Geocoding to get location name
            val geocoder = Geocoder(context, Locale.getDefault())
            Log.d("LocationUtils", "Geocoder creato, tentativo di reverse geocoding...") // LOG: Tentativo geocoding
            val addresses = geocoder.getFromLocation(
                lastLocation.latitude,
                lastLocation.longitude,
                1 // maxResults = 1 (we only need one best match)
            )

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: "" // Get city, fallback to subAdminArea, then adminArea
                val country = address.countryName ?: "" // Get country name
                val locationName = if (city.isNotBlank()) "$city, $country" else country // Format location name
                Log.d("LocationUtils", "Location trovata tramite Geocoding: $locationName") // LOG: Location trovata
                return OperationResult.Success(locationName) // Return success with location name
            } else {
                Log.w("LocationUtils", "Nessun indirizzo trovato per le coordinate") // LOG: Nessun indirizzo trovato
                return OperationResult.Error("Nome località non trovato") // No address found
            }

        } catch (e: SecurityException) {
            Log.e("LocationUtils", "SecurityException: ${e.message}")
            return OperationResult.Error("Errore di sicurezza localizzazione") // Security exception
        } catch (e: Exception) {
            Log.e("LocationUtils", "Geocoder exception: ${e.message}")
            return OperationResult.Error("Errore Geocoder: ${e.message}") // Geocoder exception
        }


    }
    @SuppressLint("MissingPermission")
    fun requestCurrentLocationName(context: Context, onLocationResult: (OperationResult<String>) -> Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        Log.d("LocationUtils", "requestCurrentLocationName: Inizio funzione") // LOG

        // 1. Check for location permissions (already checked)
        // 2. Check if GPS is enabled (already checked)


        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d("LocationUtils", "onLocationChanged: Posizione ricevuta: $location, Provider: ${location.provider}") // LOG - AGGIUNTO provider nei log
                locationManager.removeUpdates(this) // Remove updates
                resolveLocationName(context, location, onLocationResult) // Resolve location name
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d("LocationUtils", "onStatusChanged: Provider: $provider, Status: $status") // LOG
            }

            override fun onProviderEnabled(provider: String) {
                Log.d("LocationUtils", "onProviderEnabled: Provider: $provider") // LOG
            }

            override fun onProviderDisabled(provider: String) {
                Log.d("LocationUtils", "onProviderDisabled: Provider: $provider") // LOG
            }
        }



        // **FASE 1: Richiesta con NETWORK_PROVIDER (PIÙ VELOCE, MENO PRECISO) - PROVATO PER PRIMO**
        try {
            Log.d("LocationUtils", "FASE 1: Richiesta single location update con NETWORK_PROVIDER...") // LOG
            locationManager.requestSingleUpdate(
                LocationManager.NETWORK_PROVIDER, // **USA NETWORK_PROVIDER PER PRIMO**
                locationListener,
                Looper.getMainLooper()
            )

            // Timeout per NETWORK_PROVIDER (es. 10 secondi - PIÙ BREVE perché dovrebbe essere più veloce)
            Handler(Looper.getMainLooper()).postDelayed({
                // Log.w("LocationUtils", "Timeout GPS_PROVIDER: Rimozione listener e tentativo NETWORK_PROVIDER") // LOG - VECCHIO WARNING
                Log.d("LocationUtils", "Timeout GPS_PROVIDER: Rimozione listener e tentativo NETWORK_PROVIDER") // LOG - CAMBIATO A DEBUG
                locationManager.removeUpdates(locationListener) // Rimuovi listener GPS

                // FASE 2: FALLBACK con NETWORK_PROVIDER (SE GPS FALLISCE)
                try {
                    // ...

                    // Timeout anche per NETWORK_PROVIDER
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Log.w("LocationUtils", "Timeout NETWORK_PROVIDER: Rimozione listener (ENTRAMBI I PROVIDER FALLITI)") // LOG - VECCHIO WARNING
                        Log.d("LocationUtils", "Timeout NETWORK_PROVIDER: Rimozione listener (ENTRAMBI I PROVIDER FALLITI)") // LOG - CAMBIATO A DEBUG
                        locationManager.removeUpdates(locationListener)
                        onLocationResult(OperationResult.Error("Timeout localizzazione (Network e GPS)")) // Errore definitivo
                    }, 30000) // Timeout per NETWORK_PROVIDER

                } catch (e: SecurityException) {
                    Log.e("LocationUtils", "SecurityException fallback GPS_PROVIDER: ${e.message}")
                    onLocationResult(OperationResult.Error("Errore sicurezza GPS Provider (Fallback)"))
                } catch (e: Exception) {
                    Log.e("LocationUtils", "Exception fallback GPS_PROVIDER: ${e.message}")
                    onLocationResult(OperationResult.Error("Errore fallback GPS Provider: ${e.message}"))
                }
            }, 10000) // Timeout per NETWORK_PROVIDER (10 secondi - PIÙ BREVE)

        } catch (e: SecurityException) {
            Log.e("LocationUtils", "SecurityException NETWORK_PROVIDER: ${e.message}")
            onLocationResult(OperationResult.Error("Errore sicurezza Network Provider"))
        } catch (e: Exception) {
            Log.e("LocationUtils", "Exception NETWORK_PROVIDER: ${e.message}")
            onLocationResult(OperationResult.Error("Errore Network Provider: ${e.message}"))
        }
    }

    private fun resolveLocationName(context: Context, location: Location, onLocationResult: (OperationResult<String>) -> Unit) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            Log.d("LocationUtils", "resolveLocationName: Tentativo reverse geocoding...") // LOG
            val addresses = geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                1
            )
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                val country = address.countryName ?: ""
                val locationName = if (city.isNotBlank()) "$city, $country" else country
                Log.d("LocationUtils", "resolveLocationName: Location trovata: $locationName") // LOG
                onLocationResult(OperationResult.Success(locationName)) // Callback con Success
            } else {
                Log.w("LocationUtils", "resolveLocationName: Nessun indirizzo trovato") // LOG
                onLocationResult(OperationResult.Error("Nome località non trovato")) // Callback con Error
            }
        } catch (e: Exception) {
            Log.e("LocationUtils", "Geocoder exception in resolveLocationName: ${e.message}")
            onLocationResult(OperationResult.Error("Errore Geocoder: ${e.message}")) // Callback con Geocoder Error
        }
    }
}


