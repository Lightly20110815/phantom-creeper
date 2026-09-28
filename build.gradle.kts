plugins {
    id("dev.kikugie.loom-back-compat")
}

val mcVersion = sc.current.version
val mc = sc.current.parsed

/** 该游戏版本要求的 Java 版本 */
val javaTarget = when {
    mc >= "26.1" -> 25
    mc >= "1.20.5" -> 21
    else -> 17
}

version = "${property("mod_version")}+$mcVersion"
group = property("maven_group") as String
base.archivesName = property("archives_base_name") as String

loom {
    splitEnvironmentSourceSets()

    mods {
        create("phantomcreeper") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }

    runConfigs.all {
        runDir = "../../run/$mcVersion"
    }
}

// 各版本不同的资源（格式或路径在版本间有变化）
sourceSets.main {
    resources.srcDir(rootDir.resolve(if (mc >= "1.21") "src/versioned/loot_table_1.21" else "src/versioned/loot_tables_1.20"))
    resources.srcDir(rootDir.resolve(if (mc >= "1.21.5") "src/versioned/spawn_egg_textured" else "src/versioned/spawn_egg_tinted"))
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api")}")
}

java {
    withSourcesJar()
    // 1.20.x 用 JDK 21 编译出 Java 17 字节码即可；26.x 需要 JDK 25
    toolchain.languageVersion = JavaLanguageVersion.of(if (javaTarget >= 25) 25 else 21)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = javaTarget
    options.encoding = "UTF-8"
}

// runClient / runServer 使用与编译相同的 JDK
tasks.withType<net.fabricmc.loom.task.RunGameTask>().configureEach {
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
}

val resourceProps = mapOf(
    "version" to project.version.toString(),
    "minecraft" to project.property("minecraft_dependency").toString(),
    "loader" to project.property("loader_dependency").toString(),
    "java" to javaTarget.toString(),
)

tasks.processResources {
    val props = resourceProps
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}
