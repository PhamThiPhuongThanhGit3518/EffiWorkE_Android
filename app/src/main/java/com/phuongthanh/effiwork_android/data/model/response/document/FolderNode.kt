package com.phuongthanh.effiwork_android.data.model.response.document

import com.google.gson.annotations.SerializedName

enum class FolderType(val value: String) {
    PROJECT_SHARED("PROJECT_SHARED"),
    PERSONAL("PERSONAL");

    companion object {
        fun fromString(value: String?): FolderType =
            values().firstOrNull { it.value == value } ?: PERSONAL
    }
}

data class FolderNode(
    val id: String,
    val projectId: String,
    @SerializedName("parentFolderId") val parentFolderId: String? = null,
    val name: String,
    val type: String? = null,
    val folderType: String? = null,
    val ownerId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val children: List<FolderNode> = emptyList()
) {
    fun resolvedFolderType(): FolderType = FolderType.fromString(type ?: folderType)
    fun isPersonal(): Boolean = resolvedFolderType() == FolderType.PERSONAL
    fun isManageable(currentUserId: String): Boolean = isPersonal() && ownerId == currentUserId
}
