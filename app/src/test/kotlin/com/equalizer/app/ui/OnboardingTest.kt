package com.equalizer.app.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.equalizer.app.engine.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OnboardingTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun settings() = Settings(PreferenceDataStoreFactory.create { tmp.newFolder().resolve("s.preferences_pb") })

    @Test fun onboarding_isNotDoneByDefault_andPersistsWhenDone() = runTest {
        val s = settings()
        assertFalse(s.onboardingDone.first())
        s.setOnboardingDone(true)
        assertTrue(s.onboardingDone.first())
    }

    @Test fun steps_haveFourPagesEachWithTitleAndBody() {
        assertEquals(4, ONBOARDING_STEPS.size)
        ONBOARDING_STEPS.forEach {
            assertTrue(it.title.isNotBlank())
            assertTrue(it.body.isNotBlank())
        }
    }
}

class BalanceSettingsTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun balance_defaultsToCenter_andPersists() = runTest {
        val s = Settings(PreferenceDataStoreFactory.create { tmp.newFolder().resolve("s.preferences_pb") })
        assertEquals(0f, s.balance.first(), 0.0001f)
        s.setBalance(-0.4f)
        assertEquals(-0.4f, s.balance.first(), 0.0001f)
    }
}
