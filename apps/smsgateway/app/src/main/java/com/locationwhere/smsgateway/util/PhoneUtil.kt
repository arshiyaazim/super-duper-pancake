package com.locationwhere.smsgateway.util

object PhoneUtil {
    fun maskPhoneNumber(phone: String?): String {
        if (phone == null || phone.length < 7) return phone ?: "Unknown"
        return phone.substring(0, 5) + "***" + phone.substring(phone.length - 3)
    }

    fun normalizeNumber(phone: String?): String {
        if (phone == null) return ""
        // Remove spaces, dashes, and non-digit characters
        var clean = phone.replace("[^0-9]".toRegex(), "")
        // Remove Bangladesh country code prefix "880" → prepend "0"
        if (clean.startsWith("880") && clean.length > 3) {
            clean = "0" + clean.substring(3)
        }
        return clean
    }
}
