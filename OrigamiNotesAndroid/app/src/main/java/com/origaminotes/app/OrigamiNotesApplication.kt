package com.origaminotes.app

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.origaminotes.app.data.AppContainer
import com.origaminotes.app.data.AppDataContainer
import com.origaminotes.app.widget.refreshWidgets

val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class OrigamiNotesApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this, onNotesChanged = { refreshWidgets(this) })
    }
}
