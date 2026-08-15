package com.zhouyp.justdid.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FetchIndexResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("index") val index: List<UpdatedIndexDto>?
)
