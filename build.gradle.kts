import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.model.Active
import team.idealstate.glass.context.util.Extensions

plugins {
    glass(JAVA) apply false
    glass(PUBLISHING) apply false
    glass(SIGNING) apply false
    spotless(GRADLE) apply false
    spotless(JAVA) apply false
    alias(libs.plugins.jreleaser) apply false
}

group = "team.idealstate.minecraft.next"
version = "0.1.0-SNAPSHOT"

subprojects {
    if (!buildFile.exists()) {
        return@subprojects
    }
    apply {
        glass(JAVA)
        glass(PUBLISHING)
        glass(SIGNING)
        spotless(GRADLE)
        spotless(JAVA)
        plugin(rootProject.libs.plugins.jreleaser.get().pluginId)
    }

    group = rootProject.group
    version = rootProject.version

    Extensions.java(this).apply {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
            vendor.set(JvmVendorSpec.AZUL)
        }
    }

    Extensions.glass(this).apply {
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

    Extensions.publishing(this).apply {
        repositories {
            project(project)
        }
    }

    val jreleaser = extensions.getByName("jreleaser") as JReleaserExtension
    jreleaser.apply {
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
}
