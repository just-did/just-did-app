package com.zhouyp.justdid.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface ConnectionRepository {
    val isConnected: StateFlow<Boolean>

    suspend fun connectToMaster(url: String): Boolean

    fun startPolling()
    fun stopPolling()
}
