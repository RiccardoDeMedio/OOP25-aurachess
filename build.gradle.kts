plugins {
    // Apply the java plugin to add support for Java
    java

    // Apply the application plugin to add support for building a CLI application
    // You can run your app via task "run": ./gradlew run
    application

    /*
     * Adds tasks to export a runnable jar.
     * In order to create it, launch the "shadowJar" task.
     * The runnable jar will be found in build/libs/projectname-all.jar
     */
    id("com.gradleup.shadow") version "9.4.2"
    id("org.danilopianini.gradle-java-qa") version "1.180.0"
}

repositories {
    mavenCentral()
}


java {
    toolchain {
        // Java version used to compile and run the project
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}


val javaFXModules = listOf("base", "controls", "fxml", "swing", "graphics")

val supportedPlatforms = listOf("linux", "mac", "win") // All required for OOP

dependencies {
    // Suppressions for SpotBugs
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.8")

    // JavaFX
    val javaFxVersion = "23.0.2"
    implementation("org.openjfx:javafx:$javaFxVersion")
    for (platform in supportedPlatforms) {
        for (module in javaFXModules) {
            implementation("org.openjfx:javafx-$module:$javaFxVersion:$platform")
        }
    }

    // SLF4J and Logback for logging
    val slf4jVersion = "2.0.18"
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.33")

    // JUnit 5 API and testing engine
    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

application {
    // Define the main class for the application
    mainClass.set("scacchi.app.AuraChess")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform() // Enables the engine of JUnit 5
    testLogging {
        events(*org.gradle.api.tasks.testing.logging.TestLogEvent.entries.toTypedArray())
        showStandardStreams = true
    }
}

// Prevent a poorly written Javadoc comment from causing your build to fail.
tasks.withType<Javadoc>().configureEach {
    isFailOnError = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// To avoid uploading code that doesn't compile
val installGitHooks by tasks.registering(Copy::class) {
    from(File(rootProject.rootDir, "config/git/pre-commit"))
    into(File(rootProject.rootDir, ".git/hooks"))
    filePermissions {
        unix("rwxrwxrwx")
    }
}

tasks.named("check") {
    dependsOn(installGitHooks)
}

tasks.named("compileJava") {
    dependsOn(installGitHooks)
}