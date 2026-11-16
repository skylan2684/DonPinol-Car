package com.donpinol.car

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), LocationListener {

    private lateinit var prefs: SharedPreferences
    private val ui = Handler(Looper.getMainLooper())

    // Vistas
    private lateinit var gauge: GaugeView
    private lateinit var heading: TextView
    private lateinit var trip: TextView
    private lateinit var date: TextView
    private lateinit var wifi: ImageView
    private lateinit var bt: ImageView
    private lateinit var title: TextView
    private lateinit var artist: TextView
    private lateinit var source: TextView
    private lateinit var cover: ImageView
    private lateinit var progress: ProgressBar
    private lateinit var btnPlay: ImageButton

    // GPS
    private var locationManager: LocationManager? = null
    private var lastLoc: Location? = null
    private var tripMeters = 0.0

    // Música
    private var sessionManager: MediaSessionManager? = null
    private var controller: MediaController? = null
    private val listenerComponent by lazy { ComponentName(this, MediaListenerService::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("donpinol", MODE_PRIVATE)
        tripMeters = prefs.getFloat("trip_m", 0f).toDouble()

        gauge = findViewById(R.id.gauge)
        heading = findViewById(R.id.heading)
        trip = findViewById(R.id.trip)
        date = findViewById(R.id.date)
        wifi = findViewById(R.id.wifi)
        bt = findViewById(R.id.bt)
        title = findViewById(R.id.title)
        artist = findViewById(R.id.artist)
        source = findViewById(R.id.source)
        cover = findViewById(R.id.cover)
        progress = findViewById(R.id.progress)
        btnPlay = findViewById(R.id.btnPlay)

        val logo = SpannableString("DONPINOL CAR")
        logo.setSpan(ForegroundColorSpan(getColor(R.color.orange)), 9, 12, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        findViewById<TextView>(R.id.logo).text = logo

        setupButtons()
        showTrip()
        gauge.setSpeed(0f)
    }

    override fun onResume() {
        super.onResume()
        updateDate()
        updateStatusIcons()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(tick, filter, RECEIVER_EXPORTED)
        else registerReceiver(tick, filter)
        startGps()
        startMedia()
        ui.post(progressTick)
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(tick) } catch (_: Exception) {}
        stopGps()
        stopMedia()
        ui.removeCallbacks(progressTick)
        prefs.edit().putFloat("trip_m", tripMeters.toFloat()).apply()
    }

    @Deprecated("Launcher: el botón Atrás no hace nada en la pantalla principal")
    override fun onBackPressed() { /* Pantalla principal: no salir */ }

    // ---------------------------------------------------------------- Botones

    private fun setupButtons() {
        findViewById<View>(R.id.btnOpenMap).setOnClickListener { AppLauncher.openMaps(this) }
        findViewById<View>(R.id.dockMaps).setOnClickListener { AppLauncher.openMaps(this) }

        setupPlace(R.id.btnHome, "addr_home", "Casa")
        setupPlace(R.id.btnWork, "addr_work", "Trabajo")

        findViewById<View>(R.id.tripBox).setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reiniciar viaje")
                .setMessage("¿Poner el contador de kilómetros del viaje en cero?")
                .setPositiveButton("Reiniciar") { _, _ -> tripMeters = 0.0; showTrip() }
                .setNegativeButton("Cancelar", null)
                .show()
            true
        }

        findViewById<View>(R.id.dockMusic).setOnClickListener { openMusicApp() }
        findViewById<View>(R.id.musicCard).setOnClickListener {
            if (BuildConfig.MUSIC_ACCESS && !hasMediaAccess()) askMediaAccess() else openMusicApp()
        }
        findViewById<View>(R.id.dockRadio).setOnClickListener {
            val app = AppLauncher.findByLabel(this, "radio", "fm")
            if (app != null) AppLauncher.open(this, app) else AppLauncher.toast(this, "No encontré la app de radio FM")
        }
        findViewById<View>(R.id.dockPhone).setOnClickListener {
            val app = AppLauncher.findByLabel(this, "bluetooth", "teléfono", "telefono", "phone", "bt")
            if (app != null) AppLauncher.open(this, app)
            else if (!AppLauncher.start(this, Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
                AppLauncher.toast(this, "No encontré la app de teléfono")
        }
        findViewById<View>(R.id.dockApps).setOnClickListener {
            startActivity(Intent(this, AppsActivity::class.java))
        }
        findViewById<View>(R.id.dockSettings).setOnClickListener {
            AppLauncher.start(this, Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        findViewById<View>(R.id.btnPrev).setOnClickListener { controller?.transportControls?.skipToPrevious() ?: noMusic() }
        findViewById<View>(R.id.btnNext).setOnClickListener { controller?.transportControls?.skipToNext() ?: noMusic() }
        btnPlay.setOnClickListener {
            val c = controller ?: return@setOnClickListener noMusic()
            if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
            else c.transportControls.play()
        }
    }

    private fun noMusic() {
        when {
            !BuildConfig.MUSIC_ACCESS -> openMusicApp()
            !hasMediaAccess() -> askMediaAccess()
            else -> AppLauncher.toast(this, "No hay música sonando")
        }
    }

    private fun setupPlace(id: Int, key: String, name: String) {
        val v = findViewById<View>(id)
        v.setOnClickListener {
            val addr = prefs.getString(key, null)
            if (addr.isNullOrBlank()) editPlace(key, name) else AppLauncher.navigateTo(this, addr)
        }
        v.setOnLongClickListener { editPlace(key, name); true }
    }

    private fun editPlace(key: String, name: String) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
            setText(prefs.getString(key, ""))
            hint = "Dirección o lugar"
        }
        AlertDialog.Builder(this)
            .setTitle("Dirección de $name")
            .setMessage("Se abrirá la navegación hacia este lugar. Mantén presionado el botón para cambiarla.")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                prefs.edit().putString(key, input.text.toString().trim()).apply()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---------------------------------------------------------------- Hora y estado

    private val tick = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            updateDate(); updateStatusIcons()
        }
    }

    private fun updateDate() {
        val s = SimpleDateFormat("EEEE d MMM", Locale("es")).format(Date())
        date.text = s.replaceFirstChar { it.uppercase() }
    }

    private fun updateStatusIcons() {
        val cm = getSystemService(ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        wifi.alpha = if (online) 1f else 0.25f

        val btOn = try {
            if (Build.VERSION.SDK_INT >= 31 &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) false
            else BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
        } catch (_: SecurityException) { false }
        bt.alpha = if (btOn) 1f else 0.25f
    }

    // ---------------------------------------------------------------- GPS

    private fun startGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQ_LOC)
            return
        }
        val lm = getSystemService(LocationManager::class.java) ?: return
        locationManager = lm
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
            } else {
                gauge.noSignal = true; heading.text = "GPS off"
            }
        } catch (_: SecurityException) {}
    }

    private fun stopGps() {
        try { locationManager?.removeUpdates(this) } catch (_: Exception) {}
        lastLoc = null
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == REQ_LOC && results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) startGps()
    }

    override fun onLocationChanged(loc: Location) {
        val kmh = if (loc.hasSpeed()) loc.speed * 3.6f else 0f
        val shownKmh = if (kmh < 2f) 0f else kmh   // evita que "baile" estando parado
        gauge.noSignal = false
        gauge.setSpeed(shownKmh)

        if (loc.hasBearing() && shownKmh > 3f) {
            val b = loc.bearing.toInt()
            heading.text = "${compass(loc.bearing)} $b°"
        }

        val prev = lastLoc
        if (prev != null && loc.accuracy < 30f && shownKmh > 3f) {
            val dist = prev.distanceTo(loc)
            if (dist < 200f) { tripMeters += dist; showTrip() }
        }
        if (loc.accuracy < 30f) lastLoc = loc
    }

    @Deprecated("Requerido en Android 9 y anteriores")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) { startGps() }
    override fun onProviderDisabled(provider: String) { gauge.noSignal = true; heading.text = "GPS off" }

    private fun showTrip() {
        trip.text = String.format(Locale.US, "%.1f km", tripMeters / 1000.0)
    }

    private fun compass(b: Float): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
        return dirs[(((b % 360) + 360) % 360 / 45f + 0.5f).toInt() % 8]
    }

    // ---------------------------------------------------------------- Música

    private fun hasMediaAccess(): Boolean {
        if (!BuildConfig.MUSIC_ACCESS) return false
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(listenerComponent.flattenToString()) || flat.contains(packageName)
    }

    private fun askMediaAccess() {
        AlertDialog.Builder(this)
            .setTitle("Mostrar la música")
            .setMessage("Para ver la canción y controlarla desde aquí, activa \"DonPinol Car\" en la siguiente pantalla (Acceso a notificaciones). La app no lee ni guarda tus notificaciones.")
            .setPositiveButton("Activar") { _, _ ->
                AppLauncher.start(this, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            .setNegativeButton("Ahora no", null)
            .show()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        pickController(list)
    }

    private fun startMedia() {
        if (!BuildConfig.MUSIC_ACCESS) {
            title.text = "Tu música"
            artist.text = "Toca para abrir el reproductor"
            return
        }
        if (!hasMediaAccess()) {
            title.text = "Música"
            artist.text = "Toca para activar el control"
            return
        }
        try {
            val sm = getSystemService(MediaSessionManager::class.java) ?: return
            sessionManager = sm
            sm.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent)
            pickController(sm.getActiveSessions(listenerComponent))
        } catch (_: SecurityException) {
            title.text = "Música"
            artist.text = "Toca para activar el control"
        }
    }

    private fun stopMedia() {
        try { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) {}
        controller?.unregisterCallback(mediaCallback)
        controller = null
    }

    private fun pickController(list: List<MediaController>?) {
        val best = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        if (best?.sessionToken == controller?.sessionToken) return
        controller?.unregisterCallback(mediaCallback)
        controller = best
        best?.registerCallback(mediaCallback, ui)
        if (best != null) prefs.edit().putString("last_music_pkg", best.packageName).apply()
        renderMedia()
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = renderMedia()
        override fun onPlaybackStateChanged(state: PlaybackState?) = renderMedia()
        override fun onSessionDestroyed() { controller = null; renderMedia() }
    }

    private fun renderMedia() {
        val c = controller
        if (c == null) {
            title.text = "Sin reproducción"
            artist.text = "Toca para elegir música"
            source.visibility = View.GONE
            cover.setImageDrawable(null)
            progress.progress = 0
            btnPlay.setImageResource(R.drawable.ic_play)
            return
        }
        val md = c.metadata
        title.text = md?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: "Sin título"
        artist.text = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        val art: Bitmap? = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        cover.setImageBitmap(art)
        source.text = appLabel(c.packageName)
        source.visibility = View.VISIBLE
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        btnPlay.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        updateProgress()
    }

    private val progressTick = object : Runnable {
        override fun run() { updateProgress(); ui.postDelayed(this, 1000) }
    }

    private fun updateProgress() {
        val c = controller ?: return
        val dur = c.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val st = c.playbackState ?: return
        if (dur <= 0) { progress.progress = 0; return }
        var pos = st.position
        if (st.state == PlaybackState.STATE_PLAYING && st.lastPositionUpdateTime > 0) {
            pos += ((android.os.SystemClock.elapsedRealtime() - st.lastPositionUpdateTime) * st.playbackSpeed).toLong()
        }
        progress.progress = ((pos.coerceIn(0, dur) * 1000) / dur).toInt()
    }

    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) { pkg.substringAfterLast('.') }

    private fun openMusicApp() {
        val last = controller?.packageName ?: prefs.getString("last_music_pkg", null)
        if (last != null && AppLauncher.openPackage(this, last)) return
        val sel = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (AppLauncher.start(this, sel)) return
        val app = AppLauncher.findByLabel(this, "música", "musica", "music", "spotify", "youtube music")
        if (app != null) AppLauncher.open(this, app) else AppLauncher.toast(this, "No encontré app de música")
    }

    companion object {
        private const val REQ_LOC = 10
    }
}
