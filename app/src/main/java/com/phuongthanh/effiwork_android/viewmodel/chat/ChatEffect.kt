package com.phuongthanh.effiwork_android.viewmodel.chat

import com.phuongthanh.effiwork_android.data.model.response.chat.ChatMessageResponse

sealed class ChatEffect {
    data class ShowToast(val message: String) : ChatEffect()
    data class MessageSent(val message: ChatMessageResponse) : ChatEffect()
    object ScrollToBottom : ChatEffect()
    data class NewMessageReceived(val message: ChatMessageResponse) : ChatEffect()
}