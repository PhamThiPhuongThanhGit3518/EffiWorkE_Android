package com.phuongthanh.effiwork_android.viewmodel.document

sealed class DocumentBrowserEffect {
    data class ShowToast(val message: String) : DocumentBrowserEffect()
    data class ShowError(val message: String, val description: String? = null) : DocumentBrowserEffect()
    object FolderCreated : DocumentBrowserEffect()
    object DocumentDeleted : DocumentBrowserEffect()
}
