package com.mono.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.mono.launcher.R

/** A font the user can pick. [googleName] null = the phone's system font. */
data class FontChoice(val id: String, val name: String, val googleName: String?)

/**
 * Curated Google Fonts that suit a calm, text-first launcher. Downloaded on demand through
 * Google Play services' font provider (no INTERNET permission, cached system-wide); if a font
 * can't be fetched, text falls back to the system font.
 */
object FontChoices {
    val all = listOf(
        FontChoice("system", "System", null),
        FontChoice("inter", "Inter", "Inter"),
        FontChoice("manrope", "Manrope", "Manrope"),
        FontChoice("dm_sans", "DM Sans", "DM Sans"),
        FontChoice("outfit", "Outfit", "Outfit"),
        FontChoice("sora", "Sora", "Sora"),
        FontChoice("space_grotesk", "Space Grotesk", "Space Grotesk"),
        FontChoice("plex", "IBM Plex Sans", "IBM Plex Sans"),
        FontChoice("lexend", "Lexend", "Lexend"),
        FontChoice("poppins", "Poppins", "Poppins"),
        FontChoice("jetbrains", "JetBrains Mono", "JetBrains Mono"),
        FontChoice("fraunces", "Fraunces", "Fraunces"),
        FontChoice("playfair", "Playfair", "Playfair Display"),
    )

    fun byId(id: String): FontChoice = all.firstOrNull { it.id == id } ?: all.first()

    private val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

    private val families = HashMap<String, FontFamily>()

    /** Light, Regular and Medium — the three weights Mono uses. */
    fun family(choice: FontChoice): FontFamily {
        val name = choice.googleName ?: return FontFamily.Default
        return families.getOrPut(choice.id) {
            val gf = GoogleFont(name)
            FontFamily(
                Font(googleFont = gf, fontProvider = provider, weight = FontWeight.Light),
                Font(googleFont = gf, fontProvider = provider, weight = FontWeight.Normal),
                Font(googleFont = gf, fontProvider = provider, weight = FontWeight.Medium),
                Font(googleFont = gf, fontProvider = provider, weight = FontWeight.SemiBold),
                Font(googleFont = gf, fontProvider = provider, weight = FontWeight.Bold),
            )
        }
    }
}

/** Material typography with every style set to [family]. */
fun typographyFor(family: FontFamily): Typography {
    val t = Typography()
    if (family == FontFamily.Default) return t
    return Typography(
        displayLarge = t.displayLarge.copy(fontFamily = family),
        displayMedium = t.displayMedium.copy(fontFamily = family),
        displaySmall = t.displaySmall.copy(fontFamily = family),
        headlineLarge = t.headlineLarge.copy(fontFamily = family),
        headlineMedium = t.headlineMedium.copy(fontFamily = family),
        headlineSmall = t.headlineSmall.copy(fontFamily = family),
        titleLarge = t.titleLarge.copy(fontFamily = family),
        titleMedium = t.titleMedium.copy(fontFamily = family),
        titleSmall = t.titleSmall.copy(fontFamily = family),
        bodyLarge = t.bodyLarge.copy(fontFamily = family),
        bodyMedium = t.bodyMedium.copy(fontFamily = family),
        bodySmall = t.bodySmall.copy(fontFamily = family),
        labelLarge = t.labelLarge.copy(fontFamily = family),
        labelMedium = t.labelMedium.copy(fontFamily = family),
        labelSmall = t.labelSmall.copy(fontFamily = family),
    )
}
