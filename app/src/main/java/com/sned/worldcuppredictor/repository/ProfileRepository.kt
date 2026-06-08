package com.sned.worldcuppredictor.repository

import com.sned.worldcuppredictor.api.SupabaseClient
import com.sned.worldcuppredictor.model.Profile
import com.sned.worldcuppredictor.util.SecurityUtils
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class ProfileRepository {

    suspend fun usernameExists(username: String): Boolean {
        val result = SupabaseClient.client
            .from("profiles")
            .select(
                Columns.list("username")
            ) {
                filter {
                    eq("username", username)
                }
            }
            .decodeList<Profile>()

        return result.isNotEmpty()
    }

    suspend fun createProfile(
        username: String,
        pin: String
    ): Profile {
        val profile = Profile(
            username = username,
            pinHash = SecurityUtils.hashPin(pin)
        )

        return SupabaseClient.client
            .from("profiles")
            .insert(profile) {
                select()
            }
            .decodeSingle<Profile>()
    }

    suspend fun getProfileByUsername(username: String): Profile? {
        return SupabaseClient.client
            .from("profiles")
            .select {
                filter {
                    eq("username", username)
                }
            }
            .decodeList<Profile>()
            .firstOrNull()
    }

    suspend fun getAllProfiles(): List<Profile> {
        return SupabaseClient.client
            .from("profiles")
            .select()
            .decodeList<Profile>()
    }

    suspend fun verifyLogin(
        username: String,
        pin: String
    ): Profile? {

        val profile = getProfileByUsername(username)
            ?: return null

        val enteredHash = SecurityUtils.hashPin(pin)

        return if (profile.pinHash == enteredHash) {
            profile
        } else {
            null
        }
    }
}