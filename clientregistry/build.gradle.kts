plugins {
    application
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"}

group = "org.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
}

application {
    mainClass = "org.example.Main"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}



tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(sourceSets["main"].output)  // Change "test" to "main"
        archiveBaseName.set("clientregistry")
        mergeServiceFiles()
        manifest {
            attributes("Main-Class" to "org.example.Main")
        }
        configurations = listOf(project.configurations["compileClasspath"])
    }
}
