package com.strobingn.wildlifefieldops.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TenantContext @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences("tenant", Context.MODE_PRIVATE)

    val organizationId: String?
        get() = preferences.getString("organization_id", null)?.takeIf { it.isNotBlank() }

    val organizationName: String?
        get() = preferences.getString("organization_name", null)?.takeIf { it.isNotBlank() }

    fun requireOrganizationId(): String = requireNotNull(organizationId) {
        "No organization selected. Sign in and select an organization before syncing."
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
