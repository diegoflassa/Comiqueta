@file:Suppress("unused")

package dev.diegoflassa.comiqueta.core.data.extensions

import android.app.Activity
import android.app.DownloadManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.diegoflassa.comiqueta.core.ui.extensions.findActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Enumeration
import kotlin.random.Random

fun Context.obterPackageInfo(): PackageInfo? = try {
    packageManager.getPackageInfo(packageName, 0)
} catch (nnfe: PackageManager.NameNotFoundException) {
    FirebaseCrashlytics.getInstance().recordException(nnfe)
    null
}

fun Context.obterVersaoDoApp(): String = try {
    val packageInfo: PackageInfo? = packageManager.getPackageInfo(packageName, 0)
    packageInfo?.versionName ?: ""
} catch (nnfe: PackageManager.NameNotFoundException) {
    FirebaseCrashlytics.getInstance().recordException(nnfe)
    ""
}

fun Context.obterNomeDoPacote(): String = try {
    val packageInfo: PackageInfo? = packageManager.getPackageInfo(packageName, 0)
    packageInfo?.packageName ?: ""
} catch (nnfe: PackageManager.NameNotFoundException) {
    FirebaseCrashlytics.getInstance().recordException(nnfe)
    ""
}

fun Context.modoDebugHabilitado(): Boolean = try {
    val packageManager = packageManager
    val applicationInfo = packageManager.getApplicationInfo(this.packageName, 0)
    (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
} catch (nnfe: PackageManager.NameNotFoundException) {
    FirebaseCrashlytics.getInstance().recordException(nnfe)
    false
}

fun Context.modoEscuroHabilitado(): Boolean = resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES

fun Context.dpParaPx(dps: Int): Int {
    val scale = resources.displayMetrics.density
    return (dps * scale + 0.5f).toInt()
}

fun Context.pxParaDp(px: Int): Int {
    val scale = resources.displayMetrics.density
    return (px / scale).toInt()
}

fun Context.enderecoIP(): String? {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val isVpnActive = estaUsandoVPN()

    val network = connectivityManager.activeNetwork ?: return null
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null

    if (!isVpnActive && !(
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    ) {
        return null // No suitable network connection
    }

    val interfaces: Enumeration<NetworkInterface>? = NetworkInterface.getNetworkInterfaces()
    while (interfaces?.hasMoreElements() == true) {
        val networkInterface = interfaces.nextElement()
        TimberLogger.logI("HVN", "[Comiqueta][ContextExtensions] networkInterface: $networkInterface")
        if (!networkInterface.isLoopback && networkInterface.isUp) {
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (address is InetAddress && !address.isLoopbackAddress) {
                    val ipAddress = address.hostAddress
                    if (ipAddress != null) {
                        if (ipAddress.indexOf(':') < 0) { // Check for IPv4 address
                            if (isVpnActive && networkInterface.displayName.contains(
                                    "tun",
                                    ignoreCase = true
                                )
                            ) {
                                return ipAddress
                            } else if (!isVpnActive) {
                                return ipAddress
                            }
                        }
                    }
                }
            }
        }
    }
    return null
}

fun Context.estaUsandoVPN(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
}

fun Context.verificarPermissao(permissao: String): Boolean = verificarPermissoes(listOf(permissao))

fun Context.verificarPermissoes(permissions: List<String>): Boolean =
    permissions.all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

fun Context.verificarERequerirPermissao(permissao: String): Int =
    verificarERequerirPermissoes(listOf(permissao))

fun Context.verificarERequerirPermissoes(permissoes: List<String>): Int {
    var ret = 0
    val possuiPermissoes = verificarPermissoes(permissoes)
    if (!possuiPermissoes) {
        ret = requerirPermissoes(permissoes)
    }
    return ret
}

fun Context.requerirPermissao(permissao: String): Int = requerirPermissoes(listOf(permissao))

fun Context.requerirPermissoes(permissoes: List<String>): Int {
    val minRequestCode = 1000
    val maxRequestCode = 9999
    val requestCode = Random.nextInt(minRequestCode, maxRequestCode)
    ActivityCompat.requestPermissions(this as Activity, permissoes.toTypedArray(), requestCode)
    return requestCode
}

fun Context.permissaoExiste(permissao: String): Boolean {
    val packageManager = packageManager
    try {
        packageManager.getPermissionInfo(permissao, 0)
        return true
    } catch (nnfe: PackageManager.NameNotFoundException) {
        FirebaseCrashlytics.getInstance().recordException(nnfe)
        return false
    }
}

fun Context.windowManager(): WindowManager? =
    ContextCompat.getSystemService(this, WindowManager::class.java)

fun Context.connectivityManager(): ConnectivityManager? =
    ContextCompat.getSystemService(this, ConnectivityManager::class.java)

fun Context.notificationManager(): NotificationManager? =
    ContextCompat.getSystemService(this, NotificationManager::class.java)

fun Context.downloadManager(): DownloadManager? =
    ContextCompat.getSystemService(this, DownloadManager::class.java)

fun Context.keyguardManager(): KeyguardManager? =
    ContextCompat.getSystemService(this, KeyguardManager::class.java)

fun Context.powerManager(): PowerManager? =
    ContextCompat.getSystemService(this, PowerManager::class.java)

fun Context.orientacaoDaTela(): Int = resources.configuration.orientation

// InputMethodManager.SHOW_IMPLICIT is deprecated, and the documented replacement for the pair is the
// window insets controller: the IME is a system window like any other, so it is asked to appear
// rather than told through a manager. WindowCompat.getInsetsController wants the Window named
// explicitly — ViewCompat.getWindowInsetsController(View), which infers it, is itself deprecated for
// getting this wrong inside dialogs.
fun Context.hideKeyboard(view: View) = imeController(view)?.hide(WindowInsetsCompat.Type.ime())

fun Context.showKeyboard(view: EditText) {
    view.requestFocus()
    imeController(view)?.show(WindowInsetsCompat.Type.ime())
}

/**
 * The insets controller for [view]'s window, or null when this context is not inside an Activity —
 * an application or service context has no window to control.
 *
 * Logged rather than returned silently: a keyboard that does not appear is reported as "the field is
 * broken", and without this line there is nothing in the capture to say the call was made at all
 * (`LOGGING_RULES.md` §8.2 — no silently swallowed branch).
 */
private fun Context.imeController(view: View): WindowInsetsControllerCompat? {
    val window = findActivity()?.window
    if (window == null) {
        TimberLogger.logW(
            "ContextExtensions",
            "[Comiqueta][Ime] no Activity window for this Context (${this::class.simpleName}); " +
                "IME request ignored",
        )
        return null
    }
    return WindowCompat.getInsetsController(window, view)
}
