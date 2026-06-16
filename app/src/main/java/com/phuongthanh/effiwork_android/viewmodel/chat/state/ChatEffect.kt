package com.phuongthanh.effiwork_android.viewmodel.chat.state

import com.phuongthanh.effiwork_android.data.model.response.chat.ChatMessageResponse

sealed class ChatEffect {
    data class ShowToast(val message: String) : ChatEffect()
    data class MessageSent(val message: ChatMessageResponse) : ChatEffect()
    object ScrollToBottom : ChatEffect()
    data class NewMessageReceived(val message: ChatMessageResponse) : ChatEffect()
    data class GroupNameUpdated(val newName: String) : ChatEffect()
    object LeftGroup : ChatEffect()
}