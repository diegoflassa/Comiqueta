@file:Suppress("unused", "DEPRECATION")

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
import android.os.Build
import android.os.PowerManager
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Enumeration
import kotlin.random.Random

fun Context.obterPackageInfo(): PackageInfo? = try {
    packageManager.getPackageInfo(packageName, 0)
} catch (nnfe: PackageManager.NameNotFoundException) {
    nnfe.printStackTrace()
    null
}

fun Context.obterVersaoDoApp(): String = try {
    val packageInfo: PackageInfo? = packageManager.getPackageInfo(packageName, 0)
    packageInfo?.versionName ?: ""
} catch (nnfe: PackageManager.NameNotFoundException) {
    nnfe.printStackTrace()
    ""
}

fun Context.obterNomeDoPacote(): String = try {
    val packageInfo: PackageInfo? = packageManager.getPackageInfo(packageName, 0)
    packageInfo?.packageName ?: ""
} catch (nnfe: PackageManager.NameNotFoundException) {
    nnfe.printStackTrace()
    ""
}

fun Context.modoDebugHabilitado(): Boolean = try {
    val packageManager = packageManager
    val applicationInfo = packageManager.getApplicationInfo(this.packageName, 0)
    (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
} catch (nnfe: PackageManager.NameNotFoundException) {
    nnfe.printStackTrace()
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
        TimberLogger.logI("HVN", "networkInterface: $networkInterface")
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
        nnfe.printStackTrace()
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

data class Size(val width: Int, val height: Int)

@Suppress("DEPRECATION")
fun Context.tamanhoDaTela(): Size {
    val windowManager = ContextCompat.getSystemService(this, WindowManager::class.java)
    val size = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val metrics = windowManager?.currentWindowMetrics
        val windowInsets = metrics?.windowInsets
        val insets = windowInsets?.getInsetsIgnoringVisibility(
            WindowInsets.Type.navigationBars()
                    or WindowInsets.Type.displayCutout()
        )

        val insetsWidth: Int = (insets?.right ?: 0) + (insets?.left ?: 0)
        val insetsHeight: Int = (insets?.top ?: 0) + (insets?.bottom ?: 0)
        val bounds = metrics?.bounds
        Size(
            (bounds?.width() ?: 0) - insetsWidth,
            (bounds?.height() ?: 0) - insetsHeight
        )
    } else {
        val displayMetrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        Size(width, height)
    }
    return size
}

fun Context.orientacaoDaTela(): Int = resources.configuration.orientation

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.showKeyboard(view: EditText) {
    view.requestFocus()
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}
