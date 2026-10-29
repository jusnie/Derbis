package com.yasuda.derbis.ui

internal fun shouldShowArtistError(hasError: Boolean, hasProfile: Boolean): Boolean {
    return hasError || !hasProfile
}
