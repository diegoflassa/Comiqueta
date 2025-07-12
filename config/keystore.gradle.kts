import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.Properties

private var keystoreGenerationResult: Int? = null

val props = Properties()
val propsFile = File("${rootDir}/config/config.properties")
if (propsFile.exists()) {
    val fileInputStream = FileInputStream(propsFile)
    fileInputStream.use { fis ->
        props.load(fis)
    }
} else {
    props["keystore.name"] == "keystore.jks"
    props["keystore.alias"] == "alias"
    props["keystore.password"] == "password"
    props["keystore.dname"] =
        "CN=Common Name, OU=Organizational Unit, O=Organization, L=Locality, ST=State, C=Country"
    props["keystore.validity"] = "7300"
    props["keystore.keyalg"] = "RSA"
    props["keystore.keysize"] = "2048"
}

// Creates a keystore with a random private key suitable for building dev
// and release builds during active development.
gradle.rootProject {
    tasks.register("generateKeystore", Exec::class) {
        executable = "keytool.exe"
        val keystoreFile = file("${rootDir}/${props["keystore.name"]}")
        args(
            "-genkey",
            "-keystore", keystoreFile.name,
            "-alias", props["keystore.alias"],
            "-keyalg", props["keystore.keyalg"],
            "-keysize", props["keystore.keysize"],
            "-keypass", props["keystore.password"],
            "-storepass", props["keystore.password"],
            "-validity", props["keystore.validity"],
            "-dname", props["keystore.dname"]
        )
        doLast {
            if (keystoreFile.exists().not()) {
                val standardError = ByteArrayOutputStream()
                standardOutput = standardError
                val errorMessage = "Error generating keystore: $standardError"
                throw GradleException(errorMessage)
            } else {
                logger.lifecycle("keystore.jks gerado com sucesso!")
            }
        }
    }
}

// checks that a keystore exists in the expected location, and generates one
// if it does not.
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
