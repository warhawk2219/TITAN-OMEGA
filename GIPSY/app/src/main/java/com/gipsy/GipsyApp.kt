// ================================================================
// GipsyApplication.kt
// ================================================================
package com.gipsy

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GipsyApplication : Application()

// ================================================================
// MainActivity.kt
// ================================================================
package com.gipsy.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gipsy.service.GipsyService
import com.gipsy.ui.screens.ChatScreen
import com.gipsy.ui.screens.SettingsScreen
import com.gipsy.ui.theme.GipsyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start GIPSY service
        startForegroundService(Intent(this, GipsyService::class.java))

        setContent {
            GipsyTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "chat") {
                    composable("chat") {
                        ChatScreen(
                            onSettingsClick = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

// ================================================================
// BootReceiver.kt
// ================================================================
package com.gipsy.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gipsy.service.GipsyService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            context.startForegroundService(Intent(context, GipsyService::class.java))
        }
    }
}

// ================================================================
// AppModule.kt (Hilt DI)
// ================================================================
package com.gipsy.di

import android.content.Context
import androidx.room.Room
import com.gipsy.bridge.BridgeManager
import com.gipsy.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GipsyDatabase {
        return Room.databaseBuilder(
            context,
            GipsyDatabase::class.java,
            GipsyDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideMessageDao(db: GipsyDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideMemoryDao(db: GipsyDatabase): MemoryDao = db.memoryDao()

    @Provides
    @Singleton
    fun provideSessionDao(db: GipsyDatabase): SessionDao = db.sessionDao()

    @Provides
    @Singleton
    fun provideProtocolLogDao(db: GipsyDatabase): ProtocolLogDao = db.protocolLogDao()
}
