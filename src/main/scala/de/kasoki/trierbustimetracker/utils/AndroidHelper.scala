package de.kasoki.trierbustimetracker.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

object AndroidHelper {
    def currentApiLevel:Int = android.os.Build.VERSION.SDK_INT

    def version(activity:Activity):String = {
        return activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName
    }

    def isNetworkAvailable(activity:Activity):Boolean = {
        val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
        val networkInfo = cm.getActiveNetworkInfo();

        // if there is no network available networkInfo is null
        if(networkInfo != null && networkInfo.isConnected()) {
            return true
        }

        return false
    }
}