import org.jreleaser.model.Active

plugins {
    glass(JAVA)
    glass(PUBLISHING)
    glass(SIGNING)
    spotless(GRADLE)
    spotless(JAVA)
    alias(libs.plugins.jreleaser)
}

group = "team.idealstate.sugar"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

glass {
    release.set(8)

    withCopyright()
    withMavenPom()

    withSourcesJar()
    withJavadocJar()

    withInternal()
    withShadow()

    withJUnitTest()
}

repositories {
    mavenLocal()
    aliyun()
    sonatype()
    sonatype(SNAPSHOT)
    mavenCentral()
}

dependencies {
    api(libs.sugar)

    implementation(libs.byte.buddy)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

publishing {
    repositories {
        project(project)
    }
}

jreleaser {
    deploy {
        maven {
            mavenCentral {
                create("release") {
                    active.set(Active.RELEASE)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    sign.set(false)
                    stagingRepository("build/repository")
                }
            }
            nexus2 {
                create("snapshot") {
                    active.set(Active.SNAPSHOT)
                    url.set("https://central.sonatype.com/repository/maven-snapshots")
                    snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots")
                    sign.set(false)
                    applyMavenCentralRules.set(true)
                    snapshotSupported.set(true)
                    closeRepository.set(true)
                    releaseRepository.set(true)
                    verifyPom.set(false)
                    stagingRepository("build/repository")
                }
            }
        }
    }
}

tasks.register("doDeploy") {
    dependsOn(tasks.named("test"))
    dependsOn(tasks.named("publishAllPublicationsToProjectRepository"))
    finalizedBy(tasks.named("jreleaserDeploy"))
}

tasks.register("deploy") {
    group = "glass"
    dependsOn(tasks.named("clean"))
    dependsOn(tasks.named("spotlessApply"))
    finalizedBy(tasks.named("doDeploy"))
}
