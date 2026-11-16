package com.donpinol.car

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.widget.Toast

/** Utilidades para encontrar y abrir las apps que ya trae la radio. */
object AppLauncher {

    data class App(val label: String, val pkg: String, val activity: String)

    fun launchable(ctx: Context): List<App> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
        return list
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { App(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name) }
            .distinctBy { it.pkg + "/" + it.activity }
            .sortedBy { it.label.lowercase() }
    }

    fun open(ctx: Context, app: App) {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.pkg, app.activity))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        start(ctx, i)
    }

    fun openPackage(ctx: Context, pkg: String): Boolean {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(ctx, i)
    }

    /** Busca una app por palabras en su nombre (ej. "radio", "fm"). */
    fun findByLabel(ctx: Context, vararg words: String): App? {
        val apps = launchable(ctx)
        for (w in words) {
            apps.firstOrNull { it.label.lowercase().contains(w.lowercase()) }?.let { return it }
        }
        return null
    }

    fun isInstalled(ctx: Context, pkg: String): Boolean = try {
        ctx.packageManager.getPackageInfo(pkg, 0); true
    } catch (e: PackageManager.NameNotFoundException) { false }

    fun openMaps(ctx: Context) {
        for (pkg in listOf("com.google.android.apps.maps", "com.waze", "com.google.android.apps.mapslite")) {
            if (openPackage(ctx, pkg)) return
        }
        if (!start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))) {
            toast(ctx, "No hay app de mapas instalada")
        }
    }

    fun navigateTo(ctx: Context, address: String) {
        val q = Uri.encode(address)
        if (isInstalled(ctx, "com.google.android.apps.maps")) {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$q"))
                .setPackage("com.google.android.apps.maps").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (start(ctx, i)) return
        }
        if (isInstalled(ctx, "com.waze")) {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=$q&navigate=yes"))
                .setPackage("com.waze").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (start(ctx, i)) return
        }
        if (!start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))) {
            toast(ctx, "No hay app de mapas instalada")
        }
    }

    fun start(ctx: Context, i: Intent): Boolean = try {
        ctx.startActivity(i); true
    } catch (e: ActivityNotFoundException) { false } catch (e: SecurityException) { false }

    fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}
