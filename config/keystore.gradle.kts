import java.util.Properties
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import org.gradle.api.provider.ProviderFactory

val props = Properties()
val propsFile = File("${rootDir}/config/config.properties")

if (propsFile.exists()) {
    FileInputStream(propsFile).use { fis ->
        props.load(fis)
    }
} else {
    // Fallback values
    props["keystore.name"] = "keystore.jks"
    props["keystore.alias"] = "alias"
    props["keystore.password"] = "password"
    props["keystore.dname"] = "CN=Common Name, OU=Unit, O=Org, L=Locality, ST=State, C=Country"
    props["keystore.validity"] = "7300"
    props["keystore.keyalg"] = "RSA"
    props["keystore.keysize"] = "2048"
}

val keystoreFile = File("${rootDir}/${props["keystore.name"]}")

// Register task
tasks.register("generateKeystore") {
    group = "build setup"
    description = "Generates a debug keystore if not found"

    doLast {
        if (keystoreFile.exists()) {
            logger.lifecycle("✔ Keystore already exists at: ${keystoreFile.absolutePath}")
            return@doLast
        }

        val keytool = "keytool.exe"

        val checkKeytool = try {
            val result = project.providers.exec {
                commandLine("where", keytool)
                isIgnoreExitValue = true
            }.result.get()
            result.exitValue == 0
        } catch (e: Exception) {
            false
        }

        if (!checkKeytool) {
            logger.warn("⚠ keytool.exe not found in PATH. Skipping keystore generation.")
            return@doLast
        }

        try {
            project.providers.exec {
                commandLine(
                    keytool,
                    "-genkey",
                    "-keystore", keystoreFile.absolutePath,
                    "-alias", props["keystore.alias"],
                    "-keyalg", props["keystore.keyalg"],
                    "-keysize", props["keystore.keysize"],
                    "-keypass", props["keystore.password"],
                    "-storepass", props["keystore.password"],
                    "-validity", props["keystore.validity"],
                    "-dname", props["keystore.dname"]
                )
            }.result.get()
            logger.lifecycle("✅ Keystore successfully generated at: ${keystoreFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("❌ Failed to generate keystore: ${e.message}")
            logger.warn("⚠ Build will continue, but signing may fail without a keystore.")
        }
    }
}

gradle.taskGraph.whenReady {
    if (!keystoreFile.exists()) {
        tasks.named("generateKeystore").get().actions.forEach {
            it.execute(tasks.named("generateKeystore").get())
        }
    }
}

extra["verifyKeystore"] = {
    val keystoreFile = file("${rootDir}/${props["keystore.name"]}")
    if (!keystoreFile.exists()) {
        logger.lifecycle("keystore.jks não encontrada - criando uma")
        tasks.named("generateKeystore").get().actions.forEach {
            it.execute(tasks.named("generateKeystore").get())
        }
    } else {
        logger.lifecycle("keystore.jks encontrada!")
    }
}
