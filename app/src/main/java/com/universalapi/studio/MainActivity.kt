package com.universalapi.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.universalapi.studio.data.AppRepository
import com.universalapi.studio.ui.MainChatScreen
import com.universalapi.studio.ui.theme.UniversalApiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = AppRepository(applicationContext)

        setContent {
            UniversalApiTheme {
                val profiles = remember { repository.loadProfiles() }
                val activeProfileId = remember { repository.getActiveProfileId(profiles) }
                MainChatScreen(
                    repository = repository,
                    initialProfiles = profiles,
                    initialActiveProfileId = activeProfileId,
                )
            }
        }
    }
}
