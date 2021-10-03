package com.github.warriorzz.bot.config

import dev.kord.common.entity.Snowflake
import dev.schlaubi.envconf.environment
import dev.schlaubi.envconf.getEnv

object Config {

    val DISCORD_TOKEN by environment
    val DATABASE_CONNECTION_STRING by environment
    val DATABASE_NAME by environment
    val DEV_ENVIRONMENT by getEnv(default = false) { it.toBoolean() }
    val OWNER_ID by getEnv(default = Snowflake(419146440682766343)) { Snowflake(it) }
    val DEV_GUILD by getEnv { Snowflake(it) }
    val DOCKY_URL by environment
    val DOCKY_IMAGE_NAME by environment

    val MESSAGE_TIMEOUT by getEnv(default = 1500) { it.toLong() }
    val BENCH_SIZE by getEnv(default = 2) { it.toInt() }

    val MUSIC_CLIENT_ID by environment
    val MUSIC_CLIENT_SECRET by environment
    val MUSIC_REDIRECT_URI by environment
}
