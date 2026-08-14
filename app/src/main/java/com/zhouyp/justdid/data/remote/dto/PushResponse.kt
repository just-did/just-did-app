package com.zhouyp.justdid.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PushResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("updated_index") val updatedIndex: List<UpdatedIndexDto>?
)

data class UpdatedIndexDto(
    @SerializedName("year") val year: Int,
    @SerializedName("month") val month: Int,
    @SerializedName("day") val day: Int,
    @SerializedName("path") val path: String?,
    @SerializedName("file_size") val fileSize: Long
)
