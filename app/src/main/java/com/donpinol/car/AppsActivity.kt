package com.donpinol.car

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import java.util.concurrent.Executors

/** Cajón con todas las apps instaladas en la radio. */
class AppsActivity : Activity() {

    private val io = Executors.newSingleThreadExecutor()
    private var apps: List<AppLauncher.App> = emptyList()
    private val icons = HashMap<String, Drawable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apps)
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        val grid = findViewById<GridView>(R.id.grid)
        val adapter = Adapter()
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, pos, _ -> AppLauncher.open(this, apps[pos]) }

        io.execute {
            val list = AppLauncher.launchable(this)
            val pm = packageManager
            for (a in list) {
                try { icons[a.pkg + a.activity] = pm.getActivityIcon(android.content.ComponentName(a.pkg, a.activity)) } catch (_: Exception) {}
            }
            runOnUiThread { apps = list; adapter.notifyDataSetChanged() }
        }
    }

    override fun onDestroy() { io.shutdownNow(); super.onDestroy() }

    private inner class Adapter : BaseAdapter() {
        override fun getCount() = apps.size
        override fun getItem(p: Int) = apps[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, convert: View?, parent: ViewGroup): View {
            val v = convert ?: layoutInflater.inflate(R.layout.item_app, parent, false)
            val a = apps[p]
            v.findViewById<TextView>(R.id.label).text = a.label
            v.findViewById<ImageView>(R.id.icon).setImageDrawable(icons[a.pkg + a.activity])
            return v
        }
    }
}
