package com.kirawii.thunderswufe.network

import com.google.gson.annotations.SerializedName // 如果字段名与JSON key不同

/**
 * Data class representing the expected JSON response structure from the API.
 * Fields are nullable to handle potential missing fields in the response.
 */
data class ElectricityResponse(
    // Assuming JSON keys match these field names.
    // If not, use @SerializedName("json_key_name")

    @SerializedName("balance") // Example if JSON key is 'balance'
    val balance: String?,

    @SerializedName("status") // Example
    val status: String?,

    @SerializedName("message") // Example
    val message: String?
    // TODO: Add/remove/modify fields based on the actual API response structure
)