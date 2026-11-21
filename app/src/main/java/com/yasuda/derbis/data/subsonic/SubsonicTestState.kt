package com.yasuda.derbis.data.subsonic

/**
 * Why a connection test failed, in terms the screen can translate.
 *
 * The server's own error text is kept alongside it: when Navidrome says "Wrong username or
 * password" that is more specific than anything this app could invent, so it wins. The reason is
 * the fallback for the cases where the server said nothing useful.
 */
enum class SubsonicFailure {
    IncompleteForm,
    Rejected,
    Unreachable
}

/**
 * The outcome of pressing "Test" on the server form.
 *
 * [Ok] carries the server's own name and version because that is the cheapest proof that the thing
 * that answered is a music server and not a router login page.
 */
sealed interface SubsonicTestState {
    data object Idle : SubsonicTestState

    data object Testing : SubsonicTestState

    data class Ok(val serverType: String, val version: String) : SubsonicTestState {
        val label: String
            get() = listOf(serverType.ifBlank { "Subsonic" }, version)
                .filter { it.isNotBlank() }
                .joinToString(" ")
    }

    data class Failed(
        val reason: SubsonicFailure,
        val message: String = ""
    ) : SubsonicTestState {
        /** What is worth showing: the server's words if it had any, otherwise nothing. */
        val serverMessage: String get() = message.trim()
    }
}
