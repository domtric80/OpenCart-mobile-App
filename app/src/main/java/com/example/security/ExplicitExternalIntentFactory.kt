package com.example.security

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.content.pm.PackageManager

/**
 * Resolves an external handler without attaching sensitive data, then creates a new Intent whose
 * destination is explicit before customer data is added.
 */
object ExplicitExternalIntentFactory {

    fun dial(context: Context, cleanPhone: String): Boolean {
        val component = resolveHandler(context, Intent.ACTION_DIAL, "tel") ?: return false
        val intent = Intent()
        intent.setClassName(component.packageName, component.className)
        intent.action = Intent.ACTION_DIAL
        intent.data = Uri.fromParts("tel", cleanPhone, null)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun email(context: Context, cleanEmail: String, subject: String): Boolean {
        val component = resolveHandler(context, Intent.ACTION_SENDTO, "mailto") ?: return false
        val intent = Intent()
        intent.setClassName(component.packageName, component.className)
        intent.action = Intent.ACTION_SENDTO
        intent.data = Uri.fromParts("mailto", cleanEmail, null)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun resolveHandler(context: Context, action: String, scheme: String): ComponentName? {
        val probe = Intent(action, Uri.parse("$scheme:"))
        val resolved = context.packageManager.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)
            ?: return null
        val activityInfo = resolved.activityInfo ?: return null
        if (!activityInfo.enabled || !activityInfo.exported) return null

        return ComponentName(activityInfo.packageName, activityInfo.name)
    }

    internal fun buildDialIntent(component: ComponentName, cleanPhone: String): Intent {
        val intent = Intent()
        intent.setClassName(component.packageName, component.className)
        intent.action = Intent.ACTION_DIAL
        intent.data = Uri.fromParts("tel", cleanPhone, null)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }

    internal fun buildEmailIntent(
        component: ComponentName,
        cleanEmail: String,
        subject: String
    ): Intent {
        val intent = Intent()
        intent.setClassName(component.packageName, component.className)
        intent.action = Intent.ACTION_SENDTO
        intent.data = Uri.fromParts("mailto", cleanEmail, null)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }
}
