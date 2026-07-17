package com.strobingn.wildlifefieldops.data.repository

import com.strobingn.wildlifefieldops.data.remote.SupabaseService
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class OrganizationMember(
    @SerialName("organization_id") val organizationId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class OrganizationInvitation(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    val email: String,
    val role: String,
    @SerialName("invited_by") val invitedBy: String,
    @SerialName("accepted_by") val acceptedBy: String? = null,
    @SerialName("accepted_at") val acceptedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
private data class InviteMemberArgs(
    @SerialName("target_organization_id") val organizationId: String,
    @SerialName("target_email") val email: String,
    @SerialName("target_role") val role: String
)

@Serializable
private data class AcceptInvitationArgs(
    @SerialName("invitation_id") val invitationId: String
)

@Serializable
private data class UpdateMemberRoleArgs(
    @SerialName("target_organization_id") val organizationId: String,
    @SerialName("target_user_id") val userId: String,
    @SerialName("target_role") val role: String
)

@Serializable
private data class RemoveMemberArgs(
    @SerialName("target_organization_id") val organizationId: String,
    @SerialName("target_user_id") val userId: String
)

@Singleton
class OrganizationTeamRepository @Inject constructor(
    private val supabaseService: SupabaseService,
    private val tenantContext: TenantContext
) {
    private val supportedRoles = setOf("admin", "dispatcher", "technician", "member")

    suspend fun listMembers(): Result<List<OrganizationMember>> = runCatching {
        val organizationId = tenantContext.requireOrganizationId()
        val postgrest = requireNotNull(supabaseService.postgrest) { "Supabase is not configured" }
        postgrest["organization_members"]
            .select {
                filter { eq("organization_id", organizationId) }
            }
            .decodeList<OrganizationMember>()
            .sortedWith(compareBy<OrganizationMember> { roleOrder(it.role) }.thenBy { it.userId })
    }

    suspend fun listInvitations(): Result<List<OrganizationInvitation>> = runCatching {
        val organizationId = tenantContext.requireOrganizationId()
        val postgrest = requireNotNull(supabaseService.postgrest) { "Supabase is not configured" }
        postgrest["organization_invitations"]
            .select {
                filter { eq("organization_id", organizationId) }
            }
            .decodeList<OrganizationInvitation>()
            .sortedByDescending { it.createdAt.orEmpty() }
    }

    suspend fun invite(email: String, role: String): Result<OrganizationInvitation> = runCatching {
        val cleanEmail = email.trim().lowercase()
        require(cleanEmail.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
            "Enter a valid email address"
        }
        require(role in supportedRoles) { "Unsupported organization role" }
        val postgrest = requireNotNull(supabaseService.postgrest) { "Supabase is not configured" }
        postgrest.rpc(
            function = "invite_organization_member",
            parameters = InviteMemberArgs(
                organizationId = tenantContext.requireOrganizationId(),
                email = cleanEmail,
                role = role
            )
        ).decodeSingle<OrganizationInvitation>()
    }

    suspend fun acceptInvitation(invitationId: String): Result<OrganizationMember> = runCatching {
        require(invitationId.isNotBlank()) { "Invitation ID is required" }
        val postgrest = requireNotNull(supabaseService.postgrest) { "Supabase is not configured" }
        postgrest.rpc(
            function = "accept_organization_invitation",
            parameters = AcceptInvitationArgs(invitationId)
        ).decodeSingle<OrganizationMember>()
    }

    suspend fun updateRole(userId: String, role: String): Result<OrganizationMember> = runCatching {
        require(userId.isNotBlank()) { "User ID is required" }
        require(role in supportedRoles) { "Unsupported organization role" }
        val postgrest = requireNotNull(supabaseService.postgrest) { "Supabase is not configured" }
        postgrest.rpc(
            function = "update_organization_member_role",
            parameters = UpdateMemberRoleArgs(
                organizationId = tenantContext.requireOrganizationId(),
                userId = userId,
                role = role
            )
        ).decodeSingle<OrganizationMember>()
    }

    suspend fun remove(userId: String): Result<Boolean> = runCatching {
        require(userId.isNotBlank()) { "User ID is required" }
        val postgrest = requireNotNull(supabaseService.postgrest) { "Supabase is not configured" }
        postgrest.rpc(
            function = "remove_organization_member",
            parameters = RemoveMemberArgs(
                organizationId = tenantContext.requireOrganizationId(),
                userId = userId
            )
        ).decodeSingle<Boolean>()
    }

    private fun roleOrder(role: String): Int = when (role) {
        "owner" -> 0
        "admin" -> 1
        "dispatcher" -> 2
        "technician" -> 3
        else -> 4
    }
}
