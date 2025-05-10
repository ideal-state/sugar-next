glass {
    application {
        agent {
            val mainClass = "team.idealstate.minecraft.next.common.agent.AgentUtils"
            premain.set(mainClass)
            agentmain.set(mainClass)
            canRedefineClasses.set(true)
            canRetransformClasses.set(true)
            canSetNativeMethodPrefix.set(true)
        }
    }
}

dependencies {
    compileOnly(libs.log4j.api)
    compileOnly(libs.slf4j.api)
    compileOnly(java(project, "tools"))

    internal(libs.asm)
    internal(libs.snakeyaml)
    internal(libs.byte.buddy)
    internal(libs.maven.resolver.supplier)

    api(libs.hikariCP)
    api(libs.mybatis)
    api(libs.jedis)
    runtimeOnly(libs.h2)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
