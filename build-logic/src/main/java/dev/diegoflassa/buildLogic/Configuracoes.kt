package dev.diegoflassa.buildLogic

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
object Configuracoes {
    const val APP_PREFIX = "cmqt"
    const val DIEGOFLASSA_ID = "dev.diegoflassa"
    const val APPLICATION_ID = "$DIEGOFLASSA_ID.comiqueta"
    const val MINIMUM_SDK = 28
    const val COMPILE_SDK = 36
    const val TARGET_SDK = 36
    const val BUILD_TOOLS_VERSION = "36.0.0"
    const val VERSION_CODE = 100
    const val VERSION_NAME = "1.0.0"

    fun buildCount(projectRootDir: File): Int {
        val versionProps = Properties()
        val versionPropsFile = File(projectRootDir, "version.properties")
        if (versionPropsFile.exists()) {
            val fileInputStream = FileInputStream(versionPropsFile)
            fileInputStream.use { fis ->
                versionProps.load(fis)
            }
        } else {
            versionProps["VERSION_CODE"] = "0"
        }
        val code = (versionProps["VERSION_CODE"] ?: "0").toString().toInt() + 1
        versionProps["VERSION_CODE"] = code.toString()
        FileOutputStream(versionPropsFile).use { fos ->
            versionProps.store(
                fos,
                "Build version counter"
            )
        }
        return code
    }

    fun buildAppName(name: String, versionName: String, buildCount: Int): String {
        var builtName = "${APP_PREFIX}-app-${name}-${versionName}-build_$buildCount"
        return builtName
    }
}
