package app.calma.core.common

/** Language preference stored in DataStore. */
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    PERSIAN("fa");

    companion object {
        fun fromTag(tag: String?): AppLanguage = when (tag?.lowercase()) {
            "en" -> ENGLISH
            "fa" -> PERSIAN
            else -> SYSTEM
        }
    }
}

/** Theme preference stored in DataStore. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): ThemeMode = when (name?.lowercase()) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}

/** Focus timer mode. */
enum class FocusMode {
    POMODORO,
    CUSTOM;

    companion object {
        fun fromName(name: String?): FocusMode =
            if (name?.uppercase() == CUSTOM.name) CUSTOM else POMODORO
    }
}

/** Which part of the focus session is currently counting down. */
enum class FocusPhase {
    FOCUS,
    BREAK;

    companion object {
        fun fromName(name: String?): FocusPhase =
            if (name?.uppercase() == BREAK.name) BREAK else FOCUS
    }
}

/** Persisted timer run state, used to restore a session after process death. */
enum class FocusRunState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED;

    companion object {
        fun fromName(name: String?): FocusRunState =
            entries.firstOrNull { it.name == name } ?: IDLE
    }
}

/** Breathing phase. Order matches the sequence used by every built-in pattern. */
enum class BreathPhase {
    INHALE,
    HOLD_AFTER_INHALE,
    EXHALE,
    HOLD_AFTER_EXHALE;

    companion object {
        fun fromName(name: String?): BreathPhase =
            entries.firstOrNull { it.name == name } ?: INHALE
    }
}

/** Running state of a breathing session. */
enum class BreathRunState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED;

    companion object {
        fun fromName(name: String?): BreathRunState =
            entries.firstOrNull { it.name == name } ?: IDLE
    }
}

/** Granularity selector for the statistics charts. */
enum class StatsRange(val days: Int) {
    WEEK(7),
    MONTH(30);

    companion object {
        fun fromName(name: String?): StatsRange =
            if (name?.uppercase() == MONTH.name) MONTH else WEEK
    }
}