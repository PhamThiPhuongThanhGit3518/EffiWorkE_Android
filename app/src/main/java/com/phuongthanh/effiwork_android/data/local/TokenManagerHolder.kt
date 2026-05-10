package com.phuongthanh.effiwork_android.data.local

/**
 * Temporary holder for TokenManager instance.
 * This is needed because ApiClient is an object (singleton) that gets initialized
 * before Hilt can inject dependencies.
 */
object TokenManagerHolder {
    var tokenManager: TokenManager? = null
}