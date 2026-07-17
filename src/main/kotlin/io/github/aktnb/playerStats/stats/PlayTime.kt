package io.github.aktnb.playerStats.stats

/**
 * 累計プレイ時間の表示単位。表示文言(単位ラベル)は [io.github.aktnb.playerStats.i18n.Messages] 側で
 * 閲覧者の言語ごとに解決するため、ここでは単位を区別するためだけのプレーンな識別子に留める。
 */
enum class PlayTimeUnit {
    DAY,
    HOUR,
    MINUTE,
}

/** [PlayTime.displayUnits] が返す、表示すべき単位とその値の組。 */
data class PlayTimeUnitValue(val unit: PlayTimeUnit, val value: Long)

/**
 * 累計プレイ時間を日・時・分に分解して保持する値オブジェクト。
 *
 * `Statistic.PLAY_ONE_MINUTE` はその名に反して実際の値はtick単位(20 tick = 1秒)であるため、
 * tickから日時分への変換は本クラスの [fromTicks] に集約し、Bukkit非依存の純粋なロジックとして
 * JUnitテスト対象にしている(既存の [io.github.aktnb.playerStats.i18n.LanguageResolver] と同じ思想)。
 * 単位の日本語/英語ラベル付けは言語依存のため [io.github.aktnb.playerStats.i18n.Messages] に委ね、
 * 本クラスは「どの単位をいくつ表示するか」という言語非依存の判断までを担う。
 */
data class PlayTime(val days: Long, val hours: Long, val minutes: Long) {
    companion object {
        private const val TICKS_PER_MINUTE = 20L * 60L
        private const val MINUTES_PER_HOUR = 60L
        private const val HOURS_PER_DAY = 24L

        /**
         * tick値(20 tick = 1秒)を日・時・分に分解する。秒未満は切り捨てる。
         * 負値(実運用では発生しないが理論上のガード)は全て0として扱う。
         */
        fun fromTicks(ticks: Long): PlayTime {
            if (ticks <= 0L) return PlayTime(0L, 0L, 0L)
            val totalMinutes = ticks / TICKS_PER_MINUTE
            val minutes = totalMinutes % MINUTES_PER_HOUR
            val totalHours = totalMinutes / MINUTES_PER_HOUR
            val hours = totalHours % HOURS_PER_DAY
            val days = totalHours / HOURS_PER_DAY
            return PlayTime(days = days, hours = hours, minutes = minutes)
        }
    }

    /**
     * 表示すべき単位トークン列を返す。値が0の単位は省略し(上位・末尾を問わず)、
     * すべて0の場合のみ「分」を0として1つだけ返す(最低1単位は必ず表示する要件のため)。
     *
     * 例: 45分 → [MINUTE=45] / 3時間20分 → [HOUR=3, MINUTE=20] /
     *     2日5時間0分 → [DAY=2, HOUR=5](0分を省略) / すべて0 → [MINUTE=0]。
     */
    fun displayUnits(): List<PlayTimeUnitValue> {
        val units = buildList {
            if (days > 0L) add(PlayTimeUnitValue(PlayTimeUnit.DAY, days))
            if (hours > 0L) add(PlayTimeUnitValue(PlayTimeUnit.HOUR, hours))
            if (minutes > 0L) add(PlayTimeUnitValue(PlayTimeUnit.MINUTE, minutes))
        }
        return units.ifEmpty { listOf(PlayTimeUnitValue(PlayTimeUnit.MINUTE, 0L)) }
    }
}
