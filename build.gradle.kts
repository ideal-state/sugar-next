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
version = "0.1.0"

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

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

publishing {
    repositories {
        project(project)
    }
    publications {
        main {
            pom {
                description.set("Coffee(Java) with sugar is sweeter.")
                url.set("https://github.com/ideal-state/sugar-next")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/ideal-state/sugar-next")
                    connection.set("scm:git:https://github.com/ideal-state/sugar-next.git")
                    developerConnection.set("scm:git:https://github.com/ideal-state/sugar-next.git")
                }
                developers {
                    developer {
                        id.set("ideal-state")
                        name.set("ideal-state")
                        email.set("support@idealstate.team")
                    }
                }
            }
        }
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
