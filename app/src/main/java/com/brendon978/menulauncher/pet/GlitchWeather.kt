package com.brendon978.menulauncher.pet

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class Weather(val wear: Accessory, val line: String) {
    RAIN(Accessory.UMBRELLA, "Tá chovendo aí fora!"),
    SUN(Accessory.SUNGLASSES, "Sol de rachar!"),
    COLD(Accessory.SCARF, "Brrr, que frio!"),
}

/** Códigos WMO do Open-Meteo: garoa, chuva, pancadas e trovoada viram chuva; o resto depende do céu e da temperatura. */
fun weatherOf(code: Int, tempC: Float, hour: Int): Weather? = when {
    code in 51..67 || code in 80..82 || code in 95..99 -> Weather.RAIN
    tempC <= 16f -> Weather.COLD
    code in 0..1 && tempC >= 28f && hour in 8..17 -> Weather.SUN
    else -> null
}

/** Lê um número de um JSON simples, sem depender do Android (dá para testar na JVM). */
fun jsonNumber(json: String, key: String): Float? =
    Regex("\"" + key + "\"\\s*:\\s*(-?[0-9.]+)").find(json)?.groupValues?.get(1)?.toFloatOrNull()

/** Clima guardado por 30 min. Só consulta a internet se você escreveu sua cidade nos Ajustes. */
object GlitchWeather {
    val current = mutableStateOf<Weather?>(null)
    private var fetchedAt = 0L
    private var fetchedCity = ""

    suspend fun refresh(city: String, hour: Int) {
        val name = city.trim()
        if (name.isEmpty()) { current.value = null; return }
        val now = System.currentTimeMillis()
        if (name == fetchedCity && now - fetchedAt < 30 * 60_000L) return
        val found = withContext(Dispatchers.IO) {
            runCatching {
                val place = get("https://geocoding-api.open-meteo.com/v1/search?count=1&language=pt&name=" + URLEncoder.encode(name, "UTF-8"))
                val lat = jsonNumber(place, "latitude") ?: return@runCatching null
                val lon = jsonNumber(place, "longitude") ?: return@runCatching null
                val w = get("https://api.open-meteo.com/v1/forecast?current=temperature_2m,weather_code&latitude=$lat&longitude=$lon")
                val block = w.substringAfter("\"current\":", w)
                val temp = jsonNumber(block, "temperature_2m") ?: return@runCatching null
                val code = jsonNumber(block, "weather_code")?.toInt() ?: return@runCatching null
                weatherOf(code, temp, hour)
            }
        }
        if (found.isSuccess) {
            fetchedAt = now
            fetchedCity = name
            current.value = found.getOrNull()
        }
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6_000
        c.readTimeout = 6_000
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }
}

/** Quem está ouvindo ou pensando quando você fala com a IA pelo Glitch. */
object GlitchTalk {
    val state = mutableStateOf<String?>(null)
    val requests = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
}
