package com.sned.worldcuppredictor.util

import java.security.MessageDigest

object SecurityUtils {

    fun hashPin(pin: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(pin.toByteArray())

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}