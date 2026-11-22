package com.universalapi.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.universalapi.studio.data.AppRepository
import com.universalapi.studio.i18n.AppStrings
import com.universalapi.studio.i18n.LocalAppLanguage
import com.universalapi.studio.i18n.LocalAppStrings
import com.universalapi.studio.ui.MainChatScreen
import com.universalapi.studio.ui.WelcomeScreen
import com.universalapi.studio.ui.theme.UniversalApiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = AppRepository(applicationContext)
        val initialLang = repository.loadLanguage()
        val initialWelcomeDone = repository.hasCompletedWelcome()

        setContent {
            UniversalApiTheme {
                var currentLanguage by remember { mutableStateOf(initialLang) }
                var welcomeDone by remember { mutableStateOf(initialWelcomeDone) }

                val strings = remember(currentLanguage) { AppStrings.forLanguage(currentLanguage) }

                CompositionLocalProvider(
                    LocalAppLanguage provides currentLanguage,
                    LocalAppStrings provides strings,
                ) {
                    if (!welcomeDone) {
                        WelcomeScreen(
                            initialLanguage = currentLanguage,
                            onLanguagePreview = { picked ->
                                currentLanguage = picked
                            },
                            onContinue = { chosen ->
                                currentLanguage = chosen
                                repository.saveLanguage(chosen)
                                repository.setCompletedWelcome(true)
                                welcomeDone = true
                            },
                        )
                    } else {
                        val profiles = remember { repository.loadProfiles() }
                        val activeProfileId = remember { repository.getActiveProfileId(profiles) }
                        MainChatScreen(
                            repository = repository,
                            initialProfiles = profiles,
                            initialActiveProfileId = activeProfileId,
                            onChangeLanguage = { lang ->
                                currentLanguage = lang
                                repository.saveLanguage(lang)
                            },
                        )
                    }
                }
            }
        }
    }
}
