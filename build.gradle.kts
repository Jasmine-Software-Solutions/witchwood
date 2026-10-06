plugins {
    id("java")
    id("maven-publish")
    id("com.google.protobuf") version "0.9.5"
    id("org.graalvm.buildtools.native") version "0.10.6"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.jasminesoftwaresolutions"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val jooqVersion = "3.21.1"
val protobufVersion = "4.31.1"
val grpcVersion = "1.75.0"
val jjwtVersion = "0.12.6"
val gsonVersion = "2.12.1"

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // gRPC in-process transport for benchmarking
    implementation("io.grpc:grpc-inprocess:${grpcVersion}")

    // gRPC in-process transport for testing
    testImplementation("io.grpc:grpc-testing:${grpcVersion}")
    testImplementation("io.grpc:grpc-inprocess:${grpcVersion}")
    testImplementation("org.mockito:mockito-core:5.17.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.17.0")

    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    implementation("it.unimi.dsi:fastutil:8.5.18")
    implementation("org.openjdk.jol:jol-core:0.17")

    implementation("info.picocli:picocli:4.7.7")
    annotationProcessor("info.picocli:picocli-codegen:4.7.7")

    implementation("io.github.jamsesso:json-logic-java:1.1.0")

    // Protobuf
    implementation("com.google.protobuf:protobuf-java:${protobufVersion}")

    // gRPC
    implementation("io.grpc:grpc-protobuf:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")
    implementation("io.grpc:grpc-netty-shaded:${grpcVersion}")
    implementation("io.grpc:grpc-netty:${grpcVersion}")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:${jjwtVersion}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${jjwtVersion}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${jjwtVersion}")

    // JSON
    implementation("com.google.code.gson:gson:${gsonVersion}")

    // jOOQ
    implementation("org.jooq:jooq:${jooqVersion}")
    implementation("org.postgresql:postgresql:42.7.6")

    // Flyway for database migrations
    implementation("org.flywaydb:flyway-core:11.10.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.10.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.38")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.30.0-alpha")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${protobufVersion}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
            srcDir("build/generated/source/proto/main/grpc")
        }
    }
}

tasks.compileJava {
    dependsOn(tasks.named("generateProto"))
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("all")
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    manifest {
        attributes("Main-Class" to "com.jasminesoftwaresolutions.witchwood.application.Application")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("witchwood")
            mainClass.set("com.jasminesoftwaresolutions.witchwood.application.Application")

            // jOOQ uses reflection for record mapping
            buildArgs.add("--initialize-at-build-time=org.jooq")
            buildArgs.add("-H:+AddAllCharsets")

            // PostgreSQL driver needs javax.net.ssl at runtime
            buildArgs.add("--enable-url-protocols=https")

            // picocli annotation-based reflection
            buildArgs.add("-H:ReflectionConfigurationResources=META-INF/native-image/reflect-config.json")

            // jOOQ uses MethodHandles for record construction
            buildArgs.add("--enable-preview")
        }
    }
}
