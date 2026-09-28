pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // 自动下载缺少的 JDK（各游戏版本需要的 Java 版本不同）
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    // 多版本：一份源码，用 //? 注释标出各版本的差异，每个版本单独构建出一个 jar
    id("dev.kikugie.stonecutter") version "0.9.8"
    // 按游戏版本自动选择 Loom 插件（≤1.21.11 需要重映射，26.x 起游戏不再混淆）
    id("dev.kikugie.loom-back-compat") version "0.4.3"
}

rootProject.name = "phantom-creeper"

stonecutter {
    create(rootProject) {
        versions("1.20.1", "1.20.4", "1.21.1", "1.21.11", "26.3")
        vcsVersion = "1.20.4"
    }
}
