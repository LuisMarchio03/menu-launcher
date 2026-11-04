package com.brendon978.menulauncher.pet

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private val HEADSET_TYPES = setOf(
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET,
)

/** Tem fone conectado (com fio, USB ou bluetooth)? */
@Composable
fun rememberHeadset(): Boolean {
    val context = LocalContext.current
    val am = remember { context.getSystemService(AudioManager::class.java) }
    fun check() = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in HEADSET_TYPES }
    var on by remember { mutableStateOf(check()) }
    DisposableEffect(am) {
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) { on = check() }
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) { on = check() }
        }
        am.registerAudioDeviceCallback(cb, Handler(Looper.getMainLooper()))
        onDispose { am.unregisterAudioDeviceCallback(cb) }
    }
    return on
}

/** Tem internet? (rede padrão com acesso validado) */
@Composable
fun rememberOnline(): Boolean {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(ConnectivityManager::class.java) }
    fun check(): Boolean = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    var on by remember { mutableStateOf(check()) }
    DisposableEffect(cm) {
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { on = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) }
            override fun onLost(network: Network) { on = check() }
            override fun onAvailable(network: Network) { on = check() }
        }
        cm.registerDefaultNetworkCallback(cb, Handler(Looper.getMainLooper()))
        onDispose { cm.unregisterNetworkCallback(cb) }
    }
    return on
}

fun todayInt(now: Long = System.currentTimeMillis()): Int {
    val c = java.util.Calendar.getInstance().apply { timeInMillis = now }
    return c.get(java.util.Calendar.YEAR) * 10_000 + (c.get(java.util.Calendar.MONTH) + 1) * 100 + c.get(java.util.Calendar.DAY_OF_MONTH)
}

/** Não perturbe ligado? (qualquer filtro além de "tudo") */
@Composable
fun rememberDoNotDisturb(): Boolean {
    val context = LocalContext.current
    val nm = remember { context.getSystemService(android.app.NotificationManager::class.java) }
    fun check() = nm.currentInterruptionFilter.let { it != android.app.NotificationManager.INTERRUPTION_FILTER_ALL && it != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN }
    var on by remember { mutableStateOf(check()) }
    DisposableEffect(nm) {
        val r = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, i: android.content.Intent?) { on = check() }
        }
        context.registerReceiver(r, android.content.IntentFilter(android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED), Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(r) }
    }
    return on
}

/** Acelerômetro só enquanto a tela inicial está à vista. Filtra a gravidade para a inclinação não tremer. */
@Composable
fun MotionEffect(active: Boolean, onTilt: (Tilt) -> Unit, onShake: () -> Unit) {
    val context = LocalContext.current
    val tilt = androidx.compose.runtime.rememberUpdatedState(onTilt)
    val shake = androidx.compose.runtime.rememberUpdatedState(onShake)
    DisposableEffect(active) {
        val sm = context.getSystemService(android.hardware.SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        if (!active || sensor == null) return@DisposableEffect onDispose { }
        var meter = ShakeMeter()
        val g = FloatArray(3)
        var last = Tilt.LEVEL
        var first = true
        val l = object : android.hardware.SensorEventListener {
            override fun onAccuracyChanged(s: android.hardware.Sensor?, a: Int) = Unit
            override fun onSensorChanged(e: android.hardware.SensorEvent) {
                val (x, y, z) = e.values
                for (i in 0..2) g[i] = if (first) e.values[i] else g[i] * .88f + e.values[i] * .12f
                first = false
                val (m, shaken) = meter.sample(kotlin.math.sqrt(x * x + y * y + z * z), System.currentTimeMillis())
                meter = m
                if (shaken) shake.value()
                val now = tiltOf(g[0], g[1], g[2])
                if (now != last) { last = now; tilt.value(now) }
            }
        }
        sm.registerListener(l, sensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sm.unregisterListener(l)
            tilt.value(Tilt.LEVEL) // desligou o sensor: não pode ficar preso de lado
        }
    }
}
