package com.github.warriorzz.bot

import com.github.warriorzz.bot.commands.AppointmentCreateCommand
import com.github.warriorzz.bot.commands.GuildConfigurationCommand
import com.github.warriorzz.bot.commands.RedeployCommand
import com.github.warriorzz.bot.commands.RestartCommand
import com.github.warriorzz.bot.config.Config
import com.github.warriorzz.bot.model.GuildConfiguration
import com.github.warriorzz.bot.model.Match
import com.github.warriorzz.bot.model.start
import de.nycode.docky.client.DockyClient
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.x.emoji.Emojis
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.*
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import kotlin.coroutines.CoroutineContext

object MMBot : CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    internal lateinit var kord: Kord
    private val dockyClient = DockyClient(CIO, Config.DOCKY_URL)

    @OptIn(KordPreview::class)
    suspend operator fun invoke() {
        Database()
        kord = Kord(Config.DISCORD_TOKEN)

        GuildConfigurationCommand.register(kord)
        AppointmentCreateCommand.register(kord)
        RedeployCommand.register(kord)
        RestartCommand.register(kord)

        kord.on<ReadyEvent> {
            Database.collections.matches.find().consumeEach { match ->
                if (System.currentTimeMillis() > match.startTime) {
                    Database.collections.matches.deleteOneById(match._id)
                } else {
                    launch {
                        delay(match.startTime - System.currentTimeMillis())
                        match.start()
                    }
                }
            }
        }

        kord.login {
            status = PresenceStatus.DoNotDisturb
            listening("EZ4ENCE - The Verkkars")
        }
    }

    suspend fun redeploy() {
        kord.logout()
        Database.disconnect()
        val containers = dockyClient.getContainers()
        dockyClient.redeployContainer(containers.first { it.image == Config.DOCKY_IMAGE_NAME })
    }

    suspend fun restart() {
        kord.logout()
        Database.disconnect()
    }

    object Database {
        private val mongoClient = KMongo.createClient(Config.DATABASE_CONNECTION_STRING).coroutine
        private val database = mongoClient.getDatabase(Config.DATABASE_NAME)
        val collections: Collections = Collections(database.getCollection(), database.getCollection())

        operator fun invoke() {}

        fun disconnect() = mongoClient.close()
    }
}

data class Collections(
    val matches: CoroutineCollection<Match>,
    val guildConfigurations: CoroutineCollection<GuildConfiguration>
)

val Emojis.checkAnimated: DiscordPartialEmoji
    get() = DiscordPartialEmoji(Snowflake(878347026063061053), "check", animated = OptionalBoolean.Value(true))

val Emojis.crossAnimated: DiscordPartialEmoji
    get() = DiscordPartialEmoji(Snowflake(878347057046364240), "cross", animated = OptionalBoolean.Value(true))

fun DiscordPartialEmoji.asTextEmoji() = "<${if (animated.orElse(false)) "a" else ""}:$name:${id?.value}>"
