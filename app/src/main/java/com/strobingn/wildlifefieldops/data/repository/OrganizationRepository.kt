package com.strobingn.wildlifefieldops.data.repository

import com.strobingn.wildlifefieldops.data.remote.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class Organization(
    val id: String,
    val name: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
private data class CreateOrganizationArgs(
    @SerialName("organization_name") val organizationName: String
)

@Singleton
class OrganizationRepository @Inject constructor(
    private val supabaseService: SupabaseService
) {
    suspend fun listForCurrentUser(): Result<List<Organization>> = runCatching {
        val client = requireNotNull(supabaseService.client) { "Supabase is not configured" }
        client.from("organizations")
            .select()
            .decodeList<Organization>()
            .sortedBy { it.name.lowercase() }
    }

    suspend fun create(name: String): Result<Organization> = runCatching {
        val cleanName = name.trim()
        require(cleanName.length >= 2) { "Organization name must contain at least 2 characters" }
        val client = requireNotNull(supabaseService.client) { "Supabase is not configured" }
        client.rpc(
            function = "create_organization",
            parameters = CreateOrganizationArgs(cleanName)
        ).decodeSingle<Organization>()
    }
}
