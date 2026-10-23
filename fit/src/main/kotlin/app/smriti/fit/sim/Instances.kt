package app.smriti.fit.sim

import app.smriti.fit.Device
import app.smriti.fit.Lang
import app.smriti.fit.Needs
import app.smriti.fit.Screen
import app.smriti.fit.TextLevel
import kotlin.random.Random

/** One benchmark case: a screen, the person's explicit settings and the device. */
data class Instance(val seed: Long, val screen: Screen, val needs: Needs, val device: Device, val deviceClass: String, val realScreen: Boolean)

/**
 * Seeded instance generator. The mix below is an assumption, not a survey of SMRITI users; results
 * are therefore also reported per stratum (language, font scale, device class, content).
 */
object Instances {
    /** Portrait sizes in dp of common Android phones, foldables and tablets. */
    private val phones = listOf(320 to 568, 360 to 640, 360 to 740, 360 to 800, 384 to 854, 393 to 851, 411 to 891, 412 to 915, 430 to 932)
    private val tablets = listOf(600 to 960, 673 to 841, 768 to 1024, 800 to 1280, 834 to 1194, 1024 to 1366)
    private val fontScales = listOf(0.85 to 5, 1.0 to 45, 1.15 to 20, 1.3 to 15, 1.5 to 8, 1.8 to 4, 2.0 to 3)

    private fun <T> Random.weighted(items: List<Pair<T, Int>>): T {
        var r = nextInt(items.sumOf { it.second })
        for ((v, w) in items) { if (r < w) return v; r -= w }
        return items.last().first
    }

    fun make(seed: Long, content: Content, eval: Boolean): Instance {
        val r = Random(seed)
        val (device, cls) = when (r.nextInt(100)) {
            in 0 until 55 -> phones[r.nextInt(phones.size)].let { (w, h) -> if (r.nextInt(100) < 70) Device(w, h) to "phone-portrait" else Device(h, w) to "phone-landscape" }
            in 55 until 80 -> tablets[r.nextInt(tablets.size)].let { (w, h) -> if (r.nextBoolean()) Device(w, h) to "tablet-portrait" else Device(h, w) to "tablet-landscape" }
            else -> Device(300 + r.nextInt(1001), 320 + r.nextInt(981)) to "multi-window"
        }
        val lang = if (r.nextInt(100) < 60) Lang.TAMIL else Lang.ENGLISH
        val needs = Needs(
            language = lang,
            textLevel = r.weighted(listOf(TextLevel.NORMAL to 20, TextLevel.LARGE to 50, TextLevel.LARGER to 30)),
            textIsNeed = r.nextInt(100) < 30,
            minTargetDp = r.weighted(listOf(48 to 60, 56 to 25, 64 to 15)),
            reducedMotion = r.nextInt(100) < 25,
        )
        val dev = device.copy(systemFontScale = r.weighted(fontScales))
        val real = r.nextInt(100) < 40
        val screen = if (real) content.radio(lang) else {
            val pool = content.buttonPool(eval).shuffled(r)
            val n = 2 + r.nextInt(5)
            val titles = content.titlePool(eval)
            val statuses = content.statusPool(eval)
            content.generated(
                "panel_$seed", lang, r.nextInt(4), titles[r.nextInt(titles.size)],
                if (r.nextBoolean()) statuses[r.nextInt(statuses.size)] else null, pool.take(n), r.nextBoolean(),
            )
        }
        return Instance(seed, screen, needs, dev, cls, real)
    }

    val DEV_SEEDS = 1L..2000L
    val EVAL_SEEDS = 3001L..8000L
}
