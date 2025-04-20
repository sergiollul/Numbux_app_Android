package com.example.numbux.utils

import android.content.Context
import android.content.Intent

fun getAllInstalledAppPackages(context: Context): List<String> {
    val pm = context.packageManager
    val apps = pm.getInstalledPackages(0)
    return apps.map { it.packageName }
}

fun getDefaultLauncherPackage(context: Context): String? {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_HOME)
    val res = context.packageManager.resolveActivity(intent, 0)
    return res?.activityInfo?.packageName
}
