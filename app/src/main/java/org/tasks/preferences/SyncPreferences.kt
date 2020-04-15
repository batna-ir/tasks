package org.tasks.preferences

import android.view.MenuItem
import org.tasks.R
import org.tasks.injection.ActivityComponent
import org.tasks.preferences.fragments.Synchronization

class SyncPreferences : BasePreferences() {

    override fun getRootTitle() = R.string.synchronization

    override fun getRootPreference() = Synchronization()

    override fun inject(component: ActivityComponent) = component.inject(this)
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}