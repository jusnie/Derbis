package com.yasuda.derbis.domain

enum class DerbisFontPreset(
    val displayName: String,
    val googleFontName: String?
) {
    Outfit("Outfit", "Outfit"),
    Inter("Inter", "Inter"),
    Manrope("Manrope", "Manrope"),
    Poppins("Poppins", "Poppins"),
    Montserrat("Montserrat", "Montserrat"),
    Nunito("Nunito", "Nunito"),
    Roboto("Roboto", "Roboto"),
    System("System", null);

    companion object {
        fun from(value: String): DerbisFontPreset {
            val clean = value.trim()
            return entries.firstOrNull { preset ->
                preset.name.equals(clean, ignoreCase = true) ||
                    preset.displayName.equals(clean, ignoreCase = true) ||
                    preset.googleFontName?.equals(clean, ignoreCase = true) == true
            } ?: Outfit
        }
    }
}
