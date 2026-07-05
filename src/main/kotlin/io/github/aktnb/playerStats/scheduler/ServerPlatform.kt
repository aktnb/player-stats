package io.github.aktnb.playerStats.scheduler

object ServerPlatform {

    private val folia = classExists("io.papermc.paper.threadedregions.RegionizedServer")

    fun isFolia(): Boolean = folia

    fun isPaper(): Boolean = classExists("com.destroystokyo.paper.PaperConfig") ||
            classExists("io.papermc.paper.configuration.Configuration")

    private fun classExists(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }
}