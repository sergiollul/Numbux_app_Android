package com.example.numbux.control

object BlockManager {
    private val blockedApps = mutableSetOf<String>()
    private val temporarilyAllowed = mutableSetOf<String>()

    fun isAppBlocked(packageName: String): Boolean {
        return blockedApps.contains(packageName) && !temporarilyAllowed.contains(packageName)
    }

    fun blockApp(packageName: String) {
        blockedApps.add(packageName)
    }

    fun unblockApp(packageName: String) {
        blockedApps.remove(packageName)
        temporarilyAllowed.remove(packageName)
    }

    fun allowTemporarily(packageName: String) {
        temporarilyAllowed.add(packageName)
    }
}

