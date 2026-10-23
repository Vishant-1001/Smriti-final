package app.smriti.scope

/**
 * SCOPE v2.1 research engine: domain contracts (see SCOPE_V2_RESEARCH_ANNEX_v2.1 §6).
 *
 * EXPERIMENTAL and NON-OPERATIVE. Nothing in the SMRITI app calls this module; the app module does
 * not depend on it. It never chooses activities, never changes gameplay, and never learns from
 * performance, speed, hints, exits or inferred state.
 */
object ScopeFlags {
    /** `scope.v2.enabled`. Off by default; the engine refuses to recommend or ask while this is false. */
    const val ENABLED_BY_DEFAULT = false
}

/** Person-selected activity. Plain string ID so the engine stays independent of the app. */
@JvmInline
value class ActivityId(val value: String)

@JvmInline
value class VariantId(val value: String)

/** Explicitly selected or confirmed by the person for this session. Never inferred. */
enum class SocialMode { SOLO, TOGETHER }

/**
 * The only context the model may use: the activity the person opened, and Solo/Together if the
 * person explicitly chose it (null = unknown; never inferred). Deliberately has no orientation,
 * screen size, density, device type, time, mood, ability or performance fields.
 */
data class ContextSnapshot(
    val activityId: ActivityId,
    val socialMode: SocialMode?,
)

enum class InstructionModality { VISUAL, AUDIO_SUPPORTED }

/**
 * Immutable feature snapshot of one curated, approved presentation variant. Continuous features
 * are on a reviewed 0..1 scale; null means "no metadata" and contributes nothing (never guessed).
 * `interactionOpenness` is deferred from the v1 model (annex §2.1) and is ignored by the learner.
 */
data class VariantFeatures(
    val variantId: VariantId,
    val activityId: ActivityId,
    val contentVersion: String,
    val featureSchemaVersion: String,
    val visualDensity: Double?,
    val motionIntensity: Double?,
    val narrationLength: Double?,
    val instructionModality: InstructionModality?,
    val interactionOpenness: Double? = null,
    /** Curated, reviewed and enabled. Unapproved variants are never candidates. */
    val approved: Boolean = true,
)

/** The four explicit answers to "Which feels nicer for you?". */
sealed interface ExplicitPreferenceResponse {
    data object PreferLeft : ExplicitPreferenceResponse
    data object PreferRight : ExplicitPreferenceResponse
    data object BothFine : ExplicitPreferenceResponse
    data object Skip : ExplicitPreferenceResponse
}

/** One explicit comparison answer with immutable snapshots, so replay never depends on today's catalogue. */
data class PairwisePreferenceEvent(
    val eventId: String,
    val profileId: String,
    val sessionId: String,
    val context: ContextSnapshot,
    val leftFeaturesSnapshot: VariantFeatures,
    val rightFeaturesSnapshot: VariantFeatures,
    val response: ExplicitPreferenceResponse,
    val consentVersion: String,
    val contentVersion: String,
    val modelVersion: String,
    val createdAtEpochMs: Long,
) {
    val activityId: ActivityId get() = context.activityId
    val leftVariantId: VariantId get() = leftFeaturesSnapshot.variantId
    val rightVariantId: VariantId get() = rightFeaturesSnapshot.variantId
}

/** Versioned schema of approved presentation variants (the content manifest). */
data class VariantSchema(
    val featureSchemaVersion: String,
    val contentVersion: String,
    /** Fixed activity order: defines the activity-residual parameter layout. */
    val activities: List<ActivityId>,
    val variants: List<VariantFeatures>,
    /** The fixed/manual variant used when SCOPE is off or abstains. */
    val manualDefaults: Map<ActivityId, VariantId>,
) {
    init {
        require(variants.map { it.variantId }.toSet().size == variants.size) { "variant IDs must be unique" }
        require(variants.all { it.featureSchemaVersion == featureSchemaVersion && it.contentVersion == contentVersion }) {
            "every variant must carry the schema's feature and content versions"
        }
        require(variants.all { it.activityId in activities }) { "every variant must belong to a declared activity" }
        require(activities.all { a -> manualDefaults[a]?.let { d -> variants.any { it.variantId == d && it.activityId == a } } == true }) {
            "every activity needs a manual default that is one of its own variants"
        }
    }

    fun variantsFor(activity: ActivityId) = variants.filter { it.activityId == activity }
}

/** Immediate control commands. They always win, and are never preference labels. */
enum class ControlCommand { HELP_NOW, SKIP_THIS, DO_IT_FOR_ME, UNDO, HOME, STOP }

/**
 * Separate from the preference model: "Would you like to do this activity again?". Updates only
 * this per-activity acceptability tally; it never touches feature weights.
 */
class ActivityAcceptability {
    private val yes = sortedMapOf<String, Int>()
    private val no = sortedMapOf<String, Int>()

    fun record(activity: ActivityId, again: Boolean?) {
        when (again) {
            true -> yes[activity.value] = (yes[activity.value] ?: 0) + 1
            false -> no[activity.value] = (no[activity.value] ?: 0) + 1
            null -> Unit // not answered: nothing recorded
        }
    }

    fun counts(activity: ActivityId): Pair<Int, Int> = (yes[activity.value] ?: 0) to (no[activity.value] ?: 0)
}
