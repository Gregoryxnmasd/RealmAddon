plugins {
    `java-library`
}

dependencies {
    implementation(project(":common"))
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:5.1.0")
}

java {
    withJavadocJar()
    withSourcesJar()
}
