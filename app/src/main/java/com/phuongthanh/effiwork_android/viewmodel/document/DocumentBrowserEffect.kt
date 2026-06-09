package com.phuongthanh.effiwork_android.viewmodel.document

sealed class DocumentListEffect {
    data class ShowToast(val message: String) : DocumentListEffect()
    data class ShowError(val message: String, val description: String? = null) : DocumentListEffect()
    object FolderCreated : DocumentListEffect()
    object DocumentDeleted : DocumentListEffect()
}
