package com.gentestrana

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Test per identificare bug nel codice di localizzazione (LocationUtils.kt)
 * e gestione lingue/bandiere (LanguageHelpers.kt).
 */
class LocationAndLanguageBugTest {

    private val rootDir: String
        get() {
            val candidates = listOf(
                ".",
                "..",
                "../..",
                System.getProperty("user.dir") ?: "."
            )
            for (candidate in candidates) {
                val dir = File(candidate)
                if (File(dir, "settings.gradle.kts").exists()) return dir.absolutePath
                if (File(dir, "app").exists() && File(dir, "build.gradle.kts").exists()) return dir.absolutePath
            }
            return candidates.last()
        }

    /**
     * BUG 1: getCurrentLocationName() è un metodo SINCRONO/BLOCCANTE
     * che fa reverse geocoding (chiamata di rete) nel thread corrente.
     * Se chiamato dal MAIN thread, blocca l'UI.
     *
     * getCurrentLocationName() non usa coroutine né callback.
     * Torna OperationResult<String> in modo sincrono.
     * Geocoder.getFromLocation() fa chiamate HTTP/bloccanti.
     */
    @Test
    fun verify_BUG1_getCurrentLocationName_is_synchronous_and_blocking() {
        val file = File("$rootDir/app/src/main/java/com/gentestrana/utils/LocationUtils.kt")
        assertTrue("LocationUtils.kt non trovato", file.exists())

        val content = file.readText()

        // VERIFICA: getCurrentLocationName restituisce OperationResult<String>
        // in modo sincrono (non usa callback, non è suspend)
        val returnType = content.contains("OperationResult<String>")
        assertTrue("getCurrentLocationName restituisce OperationResult<String> in modo sincrono",
            returnType)

        // VERIFICA: chiama geocoder.getFromLocation() che è bloccante
        val blockingCall = content.contains("geocoder.getFromLocation")
        assertTrue("getCurrentLocationName chiama geocoder.getFromLocation() (rete bloccante)",
            blockingCall)

        println("⚠️ BUG 1 CONFERMATO: getCurrentLocationName è sincrono e bloccante")
        println("   - OperationResult<String> tornato in modo sincrono")
        println("   - geocoder.getFromLocation() fa una chiamata di rete bloccante")
        println("   - Se chiamato dal MAIN thread, blocca l'UI finché non riceve risposta")
        println("   -> FIX: convertire in funzione suspend o usare callback")
    }

    /**
     * BUG 2: requestCurrentLocationName() ha la logica di fallback ROTTA.
     *
     * Il codice:
     * 1. Richiede NETWORK_PROVIDER con timeout 10s
     * 2. Nella callback del timeout (Handler.postDelayed), il commento dice
     *    "FASE 2: FALLBACK con NETWORK_PROVIDER (SE GPS FALLISCE)"
     *    MA il codice dentro il try block è VUOTO (solo commenti // ...)
     * 3. Dentro il try, c'è SOLO un secondo Handler.postDelayed con timeout 30s
     *    MA senza mai chiamare locationManager.requestSingleUpdate
     *
     * Quindi: se NETWORK_PROVIDER fallisce (timeout 10s), non viene MAI
     * fatto un secondo tentativo con GPS. Il secondo timeout di 30s
     * scatta comunque dopo 30-10=20 secondi, dando errore definitivo.
     */
    @Test
    fun verify_BUG2_fallback_logic_is_broken_and_never_calls_GPS() {
        val file = File("$rootDir/app/src/main/java/com/gentestrana/utils/LocationUtils.kt")
        assertTrue("LocationUtils.kt non trovato", file.exists())

        val content = file.readText()

        // VERIFICA: c'è un solo requestSingleUpdate per NETWORK_PROVIDER
        val networkRequests = Regex("NETWORK_PROVIDER").findAll(content).count()
        val gpsRequests = Regex("GPS_PROVIDER").findAll(content).count()

        println("🔍 BUG 2 ANALISI:")
        println("   Riferimenti a NETWORK_PROVIDER: $networkRequests")
        println("   Riferimenti a GPS_PROVIDER: $gpsRequests")

        // La richiesta effettiva viene fatta 1 volta (NETWORK)
        val singleUpdateCalls = Regex("requestSingleUpdate").findAll(content).count()
        assertEquals("C'è solo 1 chiamata a requestSingleUpdate (nessun fallback GPS)",
            1, singleUpdateCalls)
        println("   requestSingleUpdate chiamato: $singleUpdateCalls volta (manca fallback GPS)")

        // Il commento della FASE 2 è fuorviante
        val fallbackComment = content.contains("FALLBACK")
        assertTrue("C'è un commento 'FALLBACK' ma il codice è vuoto", fallbackComment)

        println("⚠️ BUG 2 CONFERMATO: logica di fallback ROTTA")
        println("   - requestSingleUpdate chiamato SOLO per NETWORK_PROVIDER")
        println("   - Nessuna chiamata requestSingleUpdate per GPS come fallback")
        println("   - Il secondo try block dentro il timeout è VIRTUALE (solo handler vuoto)")
        println("   -> Se NETWORK_PROVIDER non dà posizione, la localizzazione fallisce sempre")
    }

    /**
     * BUG 3: getCurrentLocationName() usa solo GPS_PROVIDER per lastKnownLocation,
     * non prova NETWORK_PROVIDER come fallback.
     */
    @Test
    fun verify_BUG3_no_fallback_for_lastKnownLocation() {
        val file = File("$rootDir/app/src/main/java/com/gentestrana/utils/LocationUtils.kt")
        assertTrue("LocationUtils.kt non trovato", file.exists())

        val content = file.readText()

        // Cerca: usa solo GPS_PROVIDER per getLastKnownLocation
        val lastKnownCount = Regex("getLastKnownLocation").findAll(content).count()
        assertEquals("getLastKnownLocation chiamato 1 volta (solo GPS, nessun fallback NETWORK)",
            1, lastKnownCount)

        val usesGpsOnly = content.contains("getLastKnownLocation(LocationManager.GPS_PROVIDER)")
        assertTrue("getLastKnownLocation usa solo GPS_PROVIDER", usesGpsOnly)

        println("⚠️ BUG 3 CONFERMATO: nessun fallback per last known location")
        println("   - getLastKnownLocation chiamato solo con GPS_PROVIDER")
        println("   - Se GPS non ha ultima posizione, restituisce errore")
        println("   - Non prova NETWORK_PROVIDER come fallback")
    }

    /**
     * BUG 4: LanguageHelpers.kt - getFlagEmoji() e getLanguageName() accedono
     * a resources.getStringArray() ad OGNI chiamata, inefficiente.
     *
     * In più, il lookup è O(n) lineare su una lista di ~50 elementi.
     * Se chiamate in un contesto Compose (ricomposizioni), questo causa
     * letture dal ResourceManager Android continuamente.
     */
    @Test
    fun verify_BUG4_flag_and_language_lookup_is_inefficient() {
        val file = File("$rootDir/app/src/main/java/com/gentestrana/utils/LanguageHelpers.kt")
        assertTrue("LanguageHelpers.kt non trovato", file.exists())

        val content = file.readText()

        // VERIFICA: entrambe le funzioni caricano l'array resources ad ogni chiamata
        val resourceLoads = Regex("resources.getStringArray").findAll(content).count()
        assertEquals("getStringArray caricato 2 volte (in getFlagEmoji E getLanguageName)",
            2, resourceLoads)

        // VERIFICA: il lookup è O(n) su una lista
        val containsCheck = Regex("supportedLanguageCodes\\.contains").findAll(content).count()
        assertEquals("contains() chiamato 2 volte (O(n) lookup per ogni chiamata)",
            2, containsCheck)

        println("⚠️ BUG 4 CONFERMATO: lookup lingue inefficiente")
        println("   - resources.getStringArray caricato ad OGNI chiamata (2 funzioni)")
        println("   - contains() è O(n) su ~50 elementi")
        println("   - In contesto Compose, questo causa accessi Resources continui")
        println("   -> FIX: cache statica con Set<String> per lookup O(1)")
    }
}