package com.phuongthanh.effiwork_android.viewmodel.chatbot

sealed class ChatbotEffect {
    object ScrollToBottom : ChatbotEffect()
    data class ShowError(val message: String) : ChatbotEffect()
    object ResetCompleted : ChatbotEffect()
}
