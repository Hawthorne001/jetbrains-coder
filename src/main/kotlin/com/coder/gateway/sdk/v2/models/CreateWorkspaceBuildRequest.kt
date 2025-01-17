package com.coder.gateway.sdk.v2.models

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class CreateWorkspaceBuildRequest(
    @SerializedName("template_version_id") val templateVersionID: UUID?,
    @SerializedName("transition") val transition: WorkspaceTransition,
    @SerializedName("dry_run") val dryRun: Boolean?,
    @SerializedName("state") val provisionerState: Array<Byte>?,
    // Orphan may be set for the Destroy transition.
    @SerializedName("orphan") val orphan: Boolean?,
    @SerializedName("parameter_values") val parameterValues: Array<CreateParameterRequest>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CreateWorkspaceBuildRequest

        if (templateVersionID != other.templateVersionID) return false
        if (transition != other.transition) return false
        if (dryRun != other.dryRun) return false
        if (provisionerState != null) {
            if (other.provisionerState == null) return false
            if (!provisionerState.contentEquals(other.provisionerState)) return false
        } else if (other.provisionerState != null) return false
        if (orphan != other.orphan) return false
        if (parameterValues != null) {
            if (other.parameterValues == null) return false
            if (!parameterValues.contentEquals(other.parameterValues)) return false
        } else if (other.parameterValues != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = templateVersionID?.hashCode() ?: 0
        result = 31 * result + transition.hashCode()
        result = 31 * result + (dryRun?.hashCode() ?: 0)
        result = 31 * result + (provisionerState?.contentHashCode() ?: 0)
        result = 31 * result + (orphan?.hashCode() ?: 0)
        result = 31 * result + (parameterValues?.contentHashCode() ?: 0)
        return result
    }
}
