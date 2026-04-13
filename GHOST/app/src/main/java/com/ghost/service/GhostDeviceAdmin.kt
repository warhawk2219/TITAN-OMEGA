package com.ghost.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class GhostDeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {}
    override fun onDisabled(context: Context, intent: Intent) {}
}
