package com.github.warriorzz.bot.model

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class GuildConfiguration(
    val guildId: Snowflake,
    val appointmentRole: Snowflake,
    val mentionableRoles: Map<String, Snowflake>? = null,
    val mentionableChannels: Map<String, Snowflake>? = null,
)
