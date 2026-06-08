package com.sned.worldcuppredictor.api

import com.sned.worldcuppredictor.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLIC_KEY
    ) {
        install(Postgrest)
    }
}