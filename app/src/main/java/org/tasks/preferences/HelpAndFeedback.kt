package org.tasks.preferences

import android.view.MenuItem
import org.tasks.R
import org.tasks.injection.ActivityComponent
import org.tasks.preferences.fragments.HelpAndFeedback

class HelpAndFeedback : BasePreferences() {

    override fun getRootTitle() = R.string.help_and_feedback

    override fun getRootPreference() = HelpAndFeedback()

    override fun inject(component: ActivityComponent) {
        component.inject(this)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}