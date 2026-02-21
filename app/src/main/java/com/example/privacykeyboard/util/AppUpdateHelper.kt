package com.example.privacykeyboard.util

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed

/**
 * Handles Google Play flexible in-app updates.
 *
 * How it works (production):
 *   1. On launch, checks Play Store for a newer version.
 *   2. If found, Play shows a small update sheet (user can accept or dismiss).
 *   3. Update downloads in the background while the user keeps using the keyboard.
 *   4. When download finishes, a "Restart" snackbar appears.
 *   5. User taps Restart → app restarts with the new version applied.
 *
 * How to test (two options):
 *   A) Play Console internal test track:
 *      – Publish version 1 to your internal track, install it on a device.
 *      – Bump versionCode to 2 in build.gradle, publish version 2 to the same track.
 *      – Open the app — the update sheet appears automatically.
 *   B) FakeAppUpdateManager (unit / local test):
 *      – Replace AppUpdateManagerFactory.create(activity) below with
 *        FakeAppUpdateManager(activity) from com.google.android.play:app-update.
 *      – Call fake.setUpdateAvailable(2) then fake.userAcceptsUpdate(),
 *        fake.downloadStarts(), fake.downloadCompletes() to walk through the flow.
 *
 * Lifecycle — call from MainActivity:
 *   onCreate  → register() + checkForUpdate()
 *   onResume  → onResume()
 *   onDestroy → unregister()
 */
class AppUpdateHelper(private val activity: AppCompatActivity) {

    private val manager = AppUpdateManagerFactory.create(activity)

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) showRestartPrompt()
    }

    fun register()   = manager.registerListener(installListener)
    fun unregister() = manager.unregisterListener(installListener)

    /** Checks Play Store and starts the flexible update flow if a newer version exists. */
    fun checkForUpdate() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isFlexibleUpdateAllowed
            ) {
                @Suppress("DEPRECATION")
                manager.startUpdateFlowForResult(
                    info,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    REQUEST_CODE
                )
            }
        }
    }

    /**
     * Call from onResume — if the update downloaded while the app was in the background
     * (e.g. user switched to another app), re-show the install prompt.
     */
    fun onResume() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) showRestartPrompt()
        }
    }

    /** Call from onActivityResult to handle cancellation without crashing. */
    @Suppress("DEPRECATION")
    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == REQUEST_CODE && resultCode != Activity.RESULT_OK) {
            // User dismissed the update sheet — we silently retry on the next launch.
        }
    }

    private fun showRestartPrompt() {
        Snackbar.make(
            activity.findViewById(android.R.id.content),
            "Update downloaded — tap Restart to apply it.",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Restart") {
            manager.completeUpdate()
        }.show()
    }

    companion object {
        const val REQUEST_CODE = 500
    }
}
