package com.github.warriorzz.bot.model

import com.github.warriorzz.bot.MMBot
import com.github.warriorzz.bot.extension.randomString
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.embed
import io.github.warriorzz.ktify.model.Track
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.newId

@Serializable
data class MusicParty(
    @Contextual
    val _id: Id<MusicParty> = newId(),
    val id: String = randomString(),
    val name: String,
    val listeners: MutableList<MusicListener> = mutableListOf(),
    val queue: MutableList<Track> = mutableListOf(),
    var currentlyPlaying: Track? = null,
    val currentTrackStartTime: Long? = null,
    val creator: MusicListener,
    val message: Snowflake? = null,
    val channel: Snowflake,
    val guild: Snowflake
)

typealias MusicListener = Snowflake

suspend fun MusicParty.updateMessage() {
    val channel = MMBot.kord.getGuild(guild)?.getChannel(channel) as TextChannel
    if (message != null) {
        channel.getMessage(message).edit {
            embed(render())
        }
    }
}

fun MusicParty.render(): EmbedBuilder.() -> Unit = {
    title = name
    thumbnail {
        url = "https://www.sydneybrouwer.nl/wp-content/uploads/2019/07/listen-on-spotify-logo.png"
    }
    field {
        name = "Currently Playing"
        value = currentlyPlaying?.name ?: "Nothing"
        inline = true
    }
    field {
        name = "Creator"
        value = "<@${creator.value}>"
        inline = true
    }
    field {
        name = "Listeners"
        value = listeners.joinToString(separator = "\r\n - ") { "<@${it.value}>" } + "\n -> ${listeners.size} Listeners"
        inline = false
    }
    field {
        name = "Queue"
        value = if (queue.size == 0) "Empty" else queue.map { it.name + " - " + it.artists.joinToString { ", " } }.joinToString { "\r\n" }
        inline = true
    }
}
