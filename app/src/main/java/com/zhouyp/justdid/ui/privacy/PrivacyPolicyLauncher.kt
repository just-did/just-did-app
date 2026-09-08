package com.zhouyp.justdid.ui.privacy

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.zhouyp.justdid.domain.model.PrivacyPolicyConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

fun openPrivacyPolicy(context: Context): Boolean = runCatching {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(PrivacyPolicyConfig.URL))
    )
}.isSuccess

suspend fun openOnlinePrivacyPolicy(context: Context): Boolean = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()
    val reachable = runCatching {
        client.newCall(Request.Builder().url(PrivacyPolicyConfig.URL).get().build())
            .execute()
            .use { it.isSuccessful }
    }.getOrDefault(false)
    reachable && withContext(Dispatchers.Main) { openPrivacyPolicy(context) }
}
