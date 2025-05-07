dependencies {
    compileOnly(libs.log4j.api)
    compileOnly(libs.slf4j.api)
    compileOnly(java(project, "tools"))

    internal(libs.asm)
    internal(libs.snakeyaml)
    internal(libs.hikariCP)
    internal(libs.mybatis)
    internal(libs.jedis)
    internal(libs.byte.buddy)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
