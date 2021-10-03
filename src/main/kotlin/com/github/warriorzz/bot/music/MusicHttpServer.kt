package com.github.warriorzz.bot.music

import com.github.warriorzz.bot.config.Config
import dev.kord.common.entity.Snowflake
import freemarker.cache.ClassTemplateLoader
import io.github.warriorzz.ktify.Ktify
import io.github.warriorzz.ktify.KtifyBuilder
import io.github.warriorzz.ktify.user.getCurrentProfile
import io.ktor.application.*
import io.ktor.freemarker.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*

fun startMusicServer() = embeddedServer(CIO) {
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    routing {
        get("join") {
            val token = context.parameters["code"]
            val state = context.parameters["state"]
            if (token == null || state == null) {
                call.respond(
                    FreeMarkerContent(
                        "error.ftl",
                        mapOf("error" to ErrorOptions("Your token isn't valid! Please try again."))
                    )
                )
                return@get
            }
            call.respond(FreeMarkerContent("join.ftl", mapOf("options" to JoinPartyOptions(token, state))))
        }
        get("joinparty") {
            val token = context.parameters["token"]
            val dcid = context.parameters["dcid"]
            val state = context.parameters["state"]
            if (token == null || dcid == null || state == null) {
                call.respond(
                    FreeMarkerContent(
                        "error.ftl",
                        mapOf("error" to ErrorOptions("Your token or discord id isn't valid! Please try again."))
                    )
                )
                return@get
            }
            val ktify: Ktify
            try {
                ktify =
                    KtifyBuilder(
                        Config.MUSIC_CLIENT_ID,
                        Config.MUSIC_CLIENT_SECRET,
                        token,
                        Config.MUSIC_REDIRECT_URI
                    ).build()
            } catch (_: Exception) {
                call.respond(
                    FreeMarkerContent(
                        "error.ftl",
                        mapOf("error" to ErrorOptions("There was an error with your Spotify Account! Make sure you have a Premium Account and didn't change the token!"))
                    )
                )
                return@get
            }
            if (ktify.getCurrentProfile().product != "premium") {
                call.respond(
                    FreeMarkerContent(
                        "error.ftl",
                        mapOf("error" to ErrorOptions("There was an error with your Spotify Account! Make sure you have a Premium Account and didn't change the token!"))
                    )
                )
                return@get
            }
            MusicManager.currentInstances[Snowflake(dcid)] = ktify
            MusicManager.join(Snowflake(dcid), state)
            call.respond(
                FreeMarkerContent(
                    "success.ftl",
                    mapOf("success" to SuccessOptions("You may now close this window."))
                )
            )
        }
    }
}.start(false)

data class JoinPartyOptions(val token: String, val state: String)
data class ErrorOptions(val message: String)
data class SuccessOptions(val message: String)
