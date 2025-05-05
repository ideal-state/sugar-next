dependencies {
    compileOnly(libs.log4j.api)
    compileOnly(libs.slf4j.api)
    compileOnly(java(project, "tools"))

    internal(libs.asm)
    internal(libs.jackson.databind)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}