package com.myalbum.app.security

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import java.security.MessageDigest

class VencaApplication : Application() {

    companion object {
        const val VENCA_VERSION = "1.0.0"
        private const val TAG = "VenCA"
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        base?.let { Venca.init(it) }
    }

    override fun onCreate() {
        super.onCreate()
        Venca.init(this)
        Venca.performIntegrityCheck(this)
    }
}

object Venca {
    private const val TAG = "VenCA"
    private const val VENCA_VERSION = "1.0.0"
    private var isInitialized = false
    private var appSignatureHash = ""

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        appSignatureHash = getAppSignatureHash(context)
        Log.d(TAG, "VenCA v$VENCA_VERSION initialized")
    }

    fun performIntegrityCheck(context: Context) {
        val currentHash = getAppSignatureHash(context)
        if (currentHash.isNotEmpty() && appSignatureHash.isNotEmpty() && currentHash != appSignatureHash) {
            Log.w(TAG, "Signature mismatch detected")
        }
    }

    fun getVersion(): String = VENCA_VERSION

    fun isInitialized(): Boolean = isInitialized

    private fun getAppSignatureHash(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNATURES
                )
            }
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners ?: arrayOf()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures ?: arrayOf()
            }
            if (signatures.isEmpty()) return ""
            val md = MessageDigest.getInstance("SHA-256")
            md.update(signatures[0].toByteArray())
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get app signature", e)
            ""
        }
    }
}
