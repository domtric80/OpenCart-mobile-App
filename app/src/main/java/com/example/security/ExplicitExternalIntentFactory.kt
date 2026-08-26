package com.example.security

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager

/**
 * Resolves an external handler without attaching sensitive data, then creates a new Intent whose
 * destination is explicit before customer data is added.
 */
object ExplicitExternalIntentFactory {

    fun dial(context: Context, cleanPhone: String): Intent? {
        val component = resolveHandler(context, Intent.ACTION_DIAL, "tel") ?: return null
        return buildDialIntent(component, cleanPhone)
    }

    fun email(context: Context, cleanEmail: String, subject: String): Intent? {
        val component = resolveHandler(context, Intent.ACTION_SENDTO, "mailto") ?: return null
        return buildEmailIntent(component, cleanEmail, subject)
    }

    private fun resolveHandler(context: Context, action: String, scheme: String): ComponentName? {
        val probe = Intent(action, Uri.parse("$scheme:"))
        val resolved = context.packageManager.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)
            ?: return null
        val activityInfo = resolved.activityInfo ?: return null
        if (!activityInfo.enabled || !activityInfo.exported) return null

        return ComponentName(activityInfo.packageName, activityInfo.name)
    }

    internal fun buildDialIntent(component: ComponentName, cleanPhone: String): Intent =
        Intent(Intent.ACTION_DIAL).apply {
            this.component = component
            setPackage(component.packageName)
            data = Uri.fromParts("tel", cleanPhone, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    internal fun buildEmailIntent(
        component: ComponentName,
        cleanEmail: String,
        subject: String
    ): Intent = Intent(Intent.ACTION_SENDTO).apply {
        this.component = component
        setPackage(component.packageName)
        data = Uri.fromParts("mailto", cleanEmail, null)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}
