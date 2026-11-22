package com.equalizer.app.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    @Test fun contrast_textOnScreen_atLeast4_5() =
        assertTrue(contrastRatio(Tokens.TextPrimary, Tokens.BgScreen) >= 4.5)

    @Test fun contrast_secondaryTextOnScreen_atLeast4_5() =
        assertTrue(contrastRatio(Tokens.TextSecondary, Tokens.BgScreen) >= 4.5)

    @Test fun contrast_textOnSurface_atLeast4_5() =
        assertTrue(contrastRatio(Tokens.TextPrimary, Tokens.BgSurface) >= 4.5)

    @Test fun contrast_secondaryTextOnSurface_atLeast4_5() =
        assertTrue(contrastRatio(Tokens.TextSecondary, Tokens.BgSurface) >= 4.5)

    @Test fun contrast_onPrimaryOverPrimary_atLeast4_5() =
        assertTrue(contrastRatio(Tokens.OnBrand, Tokens.BrandPrimary) >= 4.5)
}
