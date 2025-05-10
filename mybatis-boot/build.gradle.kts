dependencies {
    api(rootProject)

    compileOnly(rootProject.project(":${rootProject.name}-jedis-boot"))
    api(libs.mybatis)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
