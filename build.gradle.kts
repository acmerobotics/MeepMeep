import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the java-library plugin for API and implementation separation.
    java
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.9.0"
}

val githubRepo = "acmerobotics/MeepMeep"
val githubReadme = "README.md"

val pomUrl = "https://github.com/$githubRepo"
val pomScmUrl = "https://github.com/$githubRepo"
val pomIssueUrl = "https://github.com/$githubRepo/issues"
val pomDesc = "https://github.com/$githubRepo"

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here
    maven(url = "https://maven.brott.dev/")
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    api("com.acmerobotics.roadrunner:core:1.0.1")
    api("com.acmerobotics.roadrunner:actions:1.0.1")
}

sourceSets["main"].java {
    srcDir("src/main/kotlin")
}

sourceSets["test"].java {
    srcDir("src/test/kotlin")
}

// Create sources Jar from main kotlin sources
val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>("meepmeep") {
            groupId = "com.acmerobotics.roadrunner"
            artifactId = "MeepMeep"
            version = "0.1.7"

            from(components["java"])
            artifact(sourcesJar)

            pom {
                packaging = "jar"
                name.set(rootProject.name)
                description.set(pomDesc)
                url.set(pomUrl)
                scm {
                    url.set(pomScmUrl)
                }
            }
        }
    }

    repositories {
        maven {
            url = uri("$buildDir/repository")
        }
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
