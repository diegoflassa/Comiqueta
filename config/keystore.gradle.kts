import java.util.Properties
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import org.gradle.api.provider.ProviderFactory

val props = Properties()
val propsFile = File("${rootDir}/config/config.properties")

// Determine if running in a CI environment
val isCiServer = System.getenv("CI") != null

if (propsFile.exists()) {
    FileInputStream(propsFile).use { fis ->
        props.load(fis)
    }
} else {
    if (!isCiServer) {
        logger.info("INFO: config.properties not found. Using default keystore properties for local generation.")
    }
    // Fallback values (primarily for local generation)
    props["keystore.name"] = "keystore.jks"
    props["keystore.alias"] = "alias"
    props["keystore.password"] = "password"
    props["keystore.dname"] = "CN=Common Name, OU=Unit, O=Org, L=Locality, ST=State, C=Country"
    props["keystore.validity"] = "7300"
    props["keystore.keyalg"] = "RSA"
    props["keystore.keysize"] = "2048"
}

val keystoreFileName = props.getProperty("keystore.name", "keystore.jks")
val keystoreFile = File("${rootDir}/$keystoreFileName")

// Only register the task and related logic if NOT in CI
if (!isCiServer) {
    tasks.register("generateKeystore") {
        group = "build setup"
        description = "Generates a keystore if not found (for local development)"

        doLast {
            if (keystoreFile.exists()) {
                logger.lifecycle("✔ Keystore already exists at: ${keystoreFile.absolutePath}")
                return@doLast
            }

            // Make keytool command OS-aware for local builds
            val osName = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT)
            val keytoolCmd = if (osName.contains("win")) "keytool.exe" else "keytool"
            val whereCmd = if (osName.contains("win")) "where" else "which"

            val checkKeytool = try {
                val result = project.providers.exec {
                    commandLine(whereCmd, keytoolCmd)
                    isIgnoreExitValue = true
                }.result.get()
                result.exitValue == 0
            } catch (e: Exception) {
                false
            }

            if (!checkKeytool) {
                logger.warn("⚠ $keytoolCmd not found in PATH. Skipping keystore generation.")
                return@doLast
            }

            logger.lifecycle("Attempting to generate keystore locally at: ${keystoreFile.absolutePath}")
            try {
                project.providers.exec {
                    commandLine(
                        keytoolCmd,
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
            logger.lifecycle("Local build: ${keystoreFile.name} not found, triggering generation.")
            tasks.named("generateKeystore").get().actions.forEach {
                it.execute(tasks.named("generateKeystore").get())
            }
        }
    }
}

// Modify verifyKeystore to be CI-aware
extra["verifyKeystore"] = {
    if (isCiServer) {
        logger.lifecycle("CI environment: Skipping keystore verification/generation.")
    } else {
        val currentKeystoreFile = File("${rootDir}/${props.getProperty("keystore.name", "keystore.jks")}")
        if (!currentKeystoreFile.exists()) {
            logger.lifecycle("Local build: ${currentKeystoreFile.name} não encontrada - criando uma")
            // Ensure generateKeystore task is available before trying to access it
            if (tasks.findByName("generateKeystore") != null) {
                 tasks.named("generateKeystore").get().actions.forEach {
                    it.execute(tasks.named("generateKeystore").get())
                }
            } else {
                logger.warn("Local build: generateKeystore task not found. Cannot create keystore.")
            }
        } else {
            logger.lifecycle("Local build: ${currentKeystoreFile.name} encontrada!")
        }
    }
}
