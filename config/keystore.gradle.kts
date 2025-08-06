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

            val osName = System.getProperty("os.name").lowercase()
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

    // Do NOT attempt to run task actions during configuration for local builds either.
    // If keystore generation is needed, it should be a task dependency or run manually.
    // gradle.taskGraph.whenReady { ... } // REMOVED THIS BLOCK

} // End of if(!isCiServer) for task registration

// Modify verifyKeystore to be CI-aware and NOT execute task actions directly
extra["verifyKeystore"] = lambda@{ // Using a label for early return
    if (isCiServer) {
        logger.lifecycle("CI environment: Skipping keystore verification/generation.")
        return@lambda // Exit the lambda
    }

    // For local builds, just check and warn. Do not attempt to create here.
    val currentKeystoreFile =
        File("${rootDir}/${props.getProperty("keystore.name", "keystore.jks")}")
    if (!currentKeystoreFile.exists()) {
        logger.warn("Local build: ${currentKeystoreFile.name} não encontrada.")
        logger.warn("Please run './gradlew generateKeystore' manually or ensure it runs as a dependency if needed.")
    } else {
        logger.lifecycle("Local build: ${currentKeystoreFile.name} encontrada!")
    }
}
