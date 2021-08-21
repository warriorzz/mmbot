package com.github.warriorzz.bot.model

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class GuildConfiguration(
    val guildId: Snowflake,
    val role: Snowflake,
)
