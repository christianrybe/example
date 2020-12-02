import groovy.xml.MarkupBuilder
import org.flywaydb.gradle.task.FlywayMigrateTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.codegen.GenerationTool
import java.io.StringWriter

buildscript {
    repositories {
        jcenter()
        mavenCentral()
        maven(url = "https://dl.bintray.com/arrow-kt/arrow-kt/")
    }

    dependencies {
        classpath("org.jooq:jooq-codegen:${Versions.jooq}")
        classpath("org.postgresql:postgresql:${Versions.postgres}")
    }
}

plugins {
    application
    kotlin("jvm") version Versions.kotlin
    kotlin("kapt") version Versions.kotlin
    id("org.flywaydb.flyway") version Versions.flyway
    id("com.avast.gradle.docker-compose") version Versions.dockerCompose
}

group = "com.example"
version = "0.0.1"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

val generatedDir = "src/generated/jooq"

kotlin.sourceSets["main"].kotlin.srcDirs("src/main")
project.the<SourceSetContainer>()["main"].java.srcDirs(generatedDir)
kotlin.sourceSets["test"].kotlin.srcDirs("src/test")

sourceSets["main"].resources.srcDirs("src/main/resources")
sourceSets["test"].resources.srcDirs("src/test/resources")

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    implementation("io.ktor:ktor-server-netty:${Versions.ktor}")
    implementation("ch.qos.logback:logback-classic:${Versions.logback}")
    implementation("ch.qos.logback:logback-core:${Versions.logback}")
    implementation("net.logstash.logback:logstash-logback-encoder:${Versions.logbackEncoder}")
    implementation("io.ktor:ktor-server-core:${Versions.ktor}")
    implementation("io.ktor:ktor-metrics-micrometer:${Versions.ktor}")
    implementation("org.flywaydb:flyway-core:${Versions.flyway}")
    implementation("io.github.config4k:config4k:${Versions.config4k}")
    implementation("com.zaxxer:HikariCP:${Versions.hikari}")
    implementation("org.postgresql:postgresql:${Versions.postgres}")
    implementation("org.jooq:jooq:${Versions.jooq}")
    implementation("org.jooq:jooq-codegen:${Versions.jooq}")
    implementation("org.jooq:jooq-meta:${Versions.jooq}")
    implementation("io.ktor:ktor-auth:${Versions.ktor}")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:${Versions.kotest}")
    testImplementation("io.kotest:kotest-assertions-core-jvm:${Versions.kotest}")
}

/**
 * DB PROPERTIES
 */
val dbUser = "docker"
val dbPassword = ""
val schema = "public"
val dbHost = System.getenv("DB_HOST")
val dbUrl = "jdbc:postgresql://$dbHost/example"

/**
 * FLYWAY
 */
flyway {
    url = dbUrl
    user = dbUser
    password = dbPassword
    schemas = arrayOf(schema)
}

requireDockerComposeIfNotDisabled(tasks["flywayMigrate"])

/**
 * JOOQ
 */
val generatedDirAbsolute = "$rootDir/$generatedDir"
val generateSchemaTaskName = "generateExampleJooqSchemaSource"

tasks.register(generateSchemaTaskName) {
    description = "Generates source files for Jooq"
    group = "jooq"

    doLast {
        val writer = StringWriter()
        MarkupBuilder(writer).withGroovyBuilder {
            "configuration"("xmlns" to "http://www.jooq.org/xsd/jooq-codegen-3.13.0.xsd") {
                "jdbc" {
                    "driver"("org.postgresql.Driver")
                    "url"(dbUrl)
                    "user"(dbUser)
                    "password"(dbPassword)
                }
                "generator" {
                    "database" {
                        "name"("org.jooq.meta.postgres.PostgresDatabase")
                        "inputSchema"(schema)
                    }
                    "generate" {
                        "javaTimeTypes"("true")
                    }
                    "strategy" {
                        "name"("org.jooq.codegen.example.JPrefixGeneratorStrategy")
                    }
                    "target" {
                        "packageName"("com.example")
                        "directory"(generatedDirAbsolute)
                    }
                }
            }
        }
        GenerationTool.generate(writer.toString())
    }
}

tasks.compileKotlin {
    dependsOn(tasks[generateSchemaTaskName])
}

tasks[generateSchemaTaskName].dependsOn(tasks.flywayMigrate)

tasks.clean {
    doLast {
        project.file(generatedDirAbsolute).deleteRecursively()
    }
}

/**
 * DOCKER
 */
dockerCompose {
    useComposeFiles = listOf("${project.projectDir}/docker-compose.yml")
    projectName = "example"
}

/**
 * HELPER FUNCTIONS
 */
fun isDockerComposeNotDisabled() = System.getenv("COMPOSE_DISABLED").isNullOrBlank()

fun requireDockerComposeIfNotDisabled(task: Task) {
    if (isDockerComposeNotDisabled()) {
        task.dependsOn("composeUp")
    }
}
