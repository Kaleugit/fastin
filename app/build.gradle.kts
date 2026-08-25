import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

/**
 * Assinatura de release lida de `keystore.properties` (não versionado).
 *
 * Ausente o arquivo, o build de release continua funcionando mas sai **sem assinatura** —
 * assim clonar o repo em outra máquina não quebra o build, só não produz APK instalável.
 */
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "dev.kaleu.fastin"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.kaleu.fastin"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFileName = keystoreProps.getProperty("storeFile")
            if (storeFileName != null) {
                storeFile = rootProject.file(storeFileName)
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
                // v2 basta para minSdk 26; v3 habilita rotação de chave no futuro sem
                // reinstalar o app.
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (keystoreProps.getProperty("storeFile") != null) {
                signingConfigs.getByName("release")
            } else {
                null
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

/**
 * JVM de teste em x64.
 *
 * Robolectric só publica binário nativo para linux-x64, mac-x64, mac-arm64 e windows-x64 —
 * não existe windows-arm64. Uma JVM ARM64 não carrega DLL x64, então Room e Compose UI
 * tests morrem no carregamento do SQLite.
 *
 * Isolamos o problema onde ele existe: **só a JVM de teste** roda em x64 (traduzido pelo
 * Windows). Compilação, Kotlin, KSP, R8 e o APK continuam 100% ARM64 nativo.
 *
 * Configure `fastin.testJdkX64` em `gradle.properties`. Ausente, os testes rodam na JVM
 * padrão — o que funciona em qualquer máquina x64.
 */
val testJdkX64: String? = providers.gradleProperty("fastin.testJdkX64").orNull

tasks.withType<Test>().configureEach {
    testJdkX64?.let { executable = "$it/bin/java.exe" }
}

/**
 * Testes unitários rodam só na variante debug.
 *
 * `compose.ui.test.manifest` — que fornece a Activity que `createComposeRule()` precisa — é
 * `debugImplementation`, e promovê-lo a `implementation` o empacotaria no APK de release.
 * Testes unitários rodam sobre classes **não minificadas** em qualquer variante, então
 * `testReleaseUnitTest` executaria exatamente o mesmo código: só custo, zero cobertura nova.
 *
 * Isto faz `./gradlew test` significar "a suíte inteira, uma vez".
 */
tasks.matching { it.name == "testReleaseUnitTest" }.configureEach { enabled = false }

// Schemas do Room versionados, para migração futura ser verificável.
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.work)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.testing)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
