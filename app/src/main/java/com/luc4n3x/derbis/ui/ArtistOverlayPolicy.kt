package com.luc4n3x.derbis.ui

internal fun shouldShowArtistError(hasError: Boolean, hasProfile: Boolean): Boolean {
    return hasError || !hasProfile
}
