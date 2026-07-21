/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.updater.data.source.network

import android.content.Context
import androidx.preference.PreferenceManager
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.lineageos.updater.R
import org.lineageos.updater.deviceinfo.DeviceInfoUtils
import org.lineageos.updater.misc.Constants
import java.io.IOException
import java.util.concurrent.TimeUnit

class UpdatesNetworkDataSource(private val context: Context) {
    private val serverUrl: String
        get() {
            val base = DeviceInfoUtils.updaterUri.trim().ifEmpty {
                context.getString(R.string.updater_server_url)
            }
            require(base.startsWith("https://")) {
                "Update server URL must use HTTPS: $base"
            }

            // Read the Beta Updates preference
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val betaUpdates = sharedPrefs.getBoolean(Constants.PREF_BETA_UPDATES, false)
            val type = if (betaUpdates) "beta" else DeviceInfoUtils.releaseType.lowercase()

            return base
                .replace("{device}", DeviceInfoUtils.device)
                .replace("{type}", type)
                .replace("{incr}", DeviceInfoUtils.buildVersionIncremental)
        }

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    fun fetchUpdates(): List<NetworkUpdate> {
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        val responseBody = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP status: ${response.code}")
            }

            response.body?.string() ?: throw IOException("Empty response body")
        }

        return Json.decodeFromString<List<NetworkUpdate>>(responseBody)
    }
}
