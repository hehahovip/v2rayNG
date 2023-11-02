package com.dd.sie.dto

data class SubscriptionItem(
        var remarks: String = "",
        var url: String = "",
        var enabled: Boolean = true,
        val addedTime: Long = System.currentTimeMillis()) {
}
