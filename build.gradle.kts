plugins {
    id("java")
    id("com.google.protobuf") version "0.9.5"
    id("org.graalvm.buildtools.native") version "0.10.6"
}

group = "com.jasminesoftwaresolutions"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val jooqVersion = "3.21.1"
val protobufVersion = "4.31.1"
val grpcVersion = "1.75.0"

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    implementation("it.unimi.dsi:fastutil:8.5.18")
    implementation("org.openjdk.jol:jol-core:0.17")

    implementation("info.picocli:picocli:4.7.7")
    annotationProcessor("info.picocli:picocli-codegen:4.7.7")

    implementation("io.github.jamsesso:json-logic-java:1.1.0")

    // Protobuf
    implementation("com.google.protobuf:protobuf-java:${protobufVersion}")

    // gRPC (message types only for now; server/client stubs can be added later)
    implementation("io.grpc:grpc-protobuf:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")

    // jOOQ
    implementation("org.jooq:jooq:${jooqVersion}")
    implementation("org.postgresql:postgresql:42.7.6")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${protobufVersion}"
    }
}

sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
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

tasks.register<Jar>("fatJar") {
    dependsOn(tasks.compileJava)
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes("Main-Class" to "com.jasminesoftwaresolutions.witchwood.application.Application")
    }
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
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
