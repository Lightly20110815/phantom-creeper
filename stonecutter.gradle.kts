plugins {
    id("dev.kikugie.stonecutter")
}

// 当前在 src/ 里直接编辑的版本；用 “Set active project to <版本>” 任务切换
stonecutter active "1.20.4"

stonecutter parameters {
    replacements {
        // 1.21.11 起 Mojang 把 ResourceLocation 改名为 Identifier
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}
