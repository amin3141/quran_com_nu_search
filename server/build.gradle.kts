plugins {
    id("java")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin-bundle:7.0.0-beta.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    implementation("org.slf4j:slf4j-simple:2.1.0-alpha1")
    implementation("org.xerial:sqlite-jdbc:3.51.1.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.quran.omni.Main")
}
