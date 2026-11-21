package com.focus.feature.blocker

/** What happens when a listed app is opened during a focus. */
enum class BlockMode {
    /** A heads-up notification reminds the user they are in a focus. */
    WARN,

    /** A full-screen block page is shown over the app (needs "display over other apps"). */
    BLOCK,
}

data class BlockerSettings(
    val enabled: Boolean = false,
    val mode: BlockMode = BlockMode.WARN,
    val packages: Set<String> = emptySet(),
) {
    companion object {
        const val MAX_PACKAGES = 200

        private val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")

        /** Only well-formed package names, bounded in length and count. */
        fun cleanPackages(input: Collection<String>): Set<String> =
            input.asSequence().filter { it.length <= 255 && PACKAGE_NAME.matches(it) }.take(MAX_PACKAGES).toSet()
    }
}
