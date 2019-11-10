package com.anggrayudi.materialpreference.migration

import android.content.SharedPreferences
import android.os.Handler
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import kotlin.concurrent.thread

interface PreferenceMigration {

    /**
     * Called on background thread. You should not update Views here.
     */
    @WorkerThread
    fun migrate(plan: MigrationPlan, currentVersion: Int)

    /**
     * Called on main thread. You can update UI like showing Toasts and Dialogs here.
     */
    @UiThread
    fun onMigrationCompleted(preferences: SharedPreferences)

    companion object {

        const val DEFAULT_PREFERENCE_VERSION_KEY = "com.anggrayudi.materialpreference.preferenceVersion"

        /**
         * You can use PreferenceMigration if you want to update your SharedPreferences.
         * This method migrates your SharedPreferences in background thread,
         * hence there'is no guarantee that it will be completed before activity creation.
         * In real case, your main activity might not be a settings activity,
         * so you should not worry about this.
         *
         * @param preferences `SharedPreferences` you want to migrate
         * @param newVersion new version of next `SharedPreferences`
         * @param preferenceVersionKey you can change it according to your needs
         */
        fun setupMigration(
                migration: PreferenceMigration,
                preferences: SharedPreferences,
                newVersion: Int,
                preferenceVersionKey: String = DEFAULT_PREFERENCE_VERSION_KEY
        ) {
            val handler = Handler()
            thread {
                val plan = MigrationPlan(preferences, preferenceVersionKey, newVersion)
                migration.migrate(plan, preferences.getInt(preferenceVersionKey, 0))
                plan.updatePreferenceVersion()
                handler.post { migration.onMigrationCompleted(preferences) }
            }
        }
    }
}