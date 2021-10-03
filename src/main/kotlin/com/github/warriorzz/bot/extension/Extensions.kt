package com.github.warriorzz.bot.extension

import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.Interaction
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(KordPreview::class)
internal suspend fun Interaction.respondHasNoPermission() = respondEphemeral {
    embed {
        title = "No Permission"
        description = "You don't have the permission to perform this interaction!"
    }
}

internal fun randomString(): String {
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..48)
        .map { charPool.random() }
        .joinToString("")
}

fun Long.toTimeString(): String {
    val dateTime =
        Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
    return DateTimeFormatter.ofPattern("HH:mm").format(dateTime)
}
