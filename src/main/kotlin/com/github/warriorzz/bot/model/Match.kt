package com.github.warriorzz.bot.model

import com.github.warriorzz.bot.MMBot
import com.github.warriorzz.bot.commands.toTimeString
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.x.emoji.Emojis
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.newId

@Serializable
data class Match(
    @Contextual
    val _id: Id<Match> = newId(),
    val requiredPlayers: Int,
    val players: MutableList<Snowflake>,
    val startTime: Long,
    val name: String,
    val specialMatch: String,
    val game: Game,
    val guild: Snowflake?,
    val channel: Snowflake?,
    val message: Snowflake?,
    val chainId: String?,
    val creator: Snowflake,
    val createdStamp: Long
)

@OptIn(KordPreview::class)
suspend fun Match.start() {
    println("Match $name is starting")
    for (it in players) {
        println(it)
        MMBot.kord.getUser(it)?.asMember(guild!!)?.getDmChannel()
            ?.createMessage("${Emojis.clock} Your match \"$name\" starts now!")
    }
    (MMBot.kord.getGuild(guild!!)?.getChannel(channel ?: Snowflake(-1)) as TextChannel).getMessage(message!!).edit {
        components = mutableListOf()
    }
    MMBot.Database.collections.matches.deleteOneById(_id)
}

@OptIn(KordPreview::class)
suspend fun Match.renderMessage(): EmbedBuilder.() -> Unit {
    val user = guild?.let { MMBot.kord.getUser(creator)?.asMember(it) }
    val playerString: String
    var benchString: String? = null
    if (players.size > requiredPlayers) {
        val subPlayers = players.subList(0, 10)
        playerString = subPlayers.joinToString("\r\n") { "- <@${it.value}>" }
        players.removeAll(subPlayers)
        benchString = players.joinToString("\r\n") { "- <@${it.value}>" }
    } else {
        playerString = players.joinToString("\r\n") { "- <@${it.value}>" }
    }
    return {
        this.title = title
        field("${Emojis["\uD83C\uDFAE"]} Game", true) { game.value }
        field("${Emojis.clock} Time", true) { Instant.fromEpochMilliseconds(startTime).toMessageFormat() }
        field(
            "${Emojis.firstPlaceMedal} Special Match",
            true
        ) { specialMatch }
        field {
            name = "Players"
            value = "$playerString \r\n-> ${players.size}/$requiredPlayers"
            inline = false
        }
        if (benchString != null) {
            field {
                name = "Bench"
                value = "$benchString"
                inline = false
            }
        }
        footer = EmbedBuilder.Footer().apply {
            text =
                "Requested by ${user?.displayName ?: "unknown"} at ${
                    createdStamp.toTimeString()
                }"
        }
        author = EmbedBuilder.Author().apply {
            name = this@renderMessage.name
            icon = user?.avatar?.url
        }
        thumbnail = EmbedBuilder.Thumbnail().apply {
            url = game.thumbnailUrl
        }
    }
}

enum class Game(val id: String, val value: String, val thumbnailUrl: String, val specialChoises: List<String>? = null) {
    AMONG_US(
        "amongus",
        "Among Us",
        "https://cdn1.iconfinder.com/data/icons/logos-brands-in-colors/231/among-us-player-red-512.png",
        listOf("With mods")
    ),
    VALORANT(
        "valorant",
        "VALORANT",
        "https://discords.com/_next/image?url=https%3A%2F%2Fcdn.discordapp.com%2Femojis%2F849040109457506305.png%3Fv%3D1&w=64&q=75",
        listOf("Unrated", "Ranked", "Custom Game", "Spike Rush", "Deathmatch", "Special Game")
    ),
    CS_GO(
        "csgo",
        "Counter Strike: Global Offensive",
        "https://cdn.discordapp.com/emojis/871037720359751722.png?v=1",
        listOf("Ranked", "Unranked", "Deathmatch")
    ),
    MINECRAFT(
        "minecraft",
        "Minecraft",
        "https://cdn.discordapp.com/emojis/871041246066520165.png?v=1",
        listOf("TTT", "Survival Server", "Modpack", "Skywars", "Bedwars", "Other")
    ),
    PUBG(
        "pubg",
        "Player Unknown's Battleground",
        "https://images-wixmp-ed30a86b8c4ca887773594c2.wixmp.com/f/2890910c-866d-4a01-af6a-2067e1209024/dccvgi0-a3e8c60b-5914-4b83-86eb-f4ad3f9bb19e.png?token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1cm46YXBwOjdlMGQxODg5ODIyNjQzNzNhNWYwZDQxNWVhMGQyNmUwIiwiaXNzIjoidXJuOmFwcDo3ZTBkMTg4OTgyMjY0MzczYTVmMGQ0MTVlYTBkMjZlMCIsIm9iaiI6W1t7InBhdGgiOiJcL2ZcLzI4OTA5MTBjLTg2NmQtNGEwMS1hZjZhLTIwNjdlMTIwOTAyNFwvZGNjdmdpMC1hM2U4YzYwYi01OTE0LTRiODMtODZlYi1mNGFkM2Y5YmIxOWUucG5nIn1dXSwiYXVkIjpbInVybjpzZXJ2aWNlOmZpbGUuZG93bmxvYWQiXX0.F1gdO0Rh3suMnith_2t1ClJvlxeQdJk8AuCldKPw4HU",
        listOf("Custom")
    ),
    FALL_GUYS(
        "fallguys",
        "Fall Guys",
        "https://cdn2.steamgriddb.com/file/sgdb-cdn/logo/8c2b7c5f9176bd359d4d42f4e9f9f15d.png"
    ),
    RAFT(
        "raft",
        "Raft",
        "https://w0.pngwave.com/png/90/746/logo-raft-game-the-raft-logo-png-clip-art.png"
    ),
    ARK(
        "ark",
        "ARK: Survival Evolved",
        "https://clipground.com/images/ark-survival-logo.png"
    ),
    UNRAILED(
        "unrailed",
        "Unrailed!",
        "https://styles.redditmedia.com/t5_243odp/styles/communityIcon_0r7gu9rr7n141.png?width=256&s=417660d639857027a7c1282b4f4151842611bd8f"
    ),
    FISHING_PLANET(
        "fishingplanet",
        "Fishing Planet",
        "https://hry-nej.cz/14143-tm_thickbox_default/the-fisherman-fishing-planet-pc-steam-simulator-hra-na-pc.jpg"
    ),
    FORTNITE(
        "fortnite",
        "Fortnite - Battle Royale",
        "https://yt3.ggpht.com/a/AGF-l79liGxAtE9VHK_q9vN6CzhUhAB78R57i_KN=s900-mo-c-c0xffffffff-rj-k-no"
    ),
    APEX_LEGENDS(
        "apexlegends",
        "Apex Legends",
        "https://logodownload.org/wp-content/uploads/2019/02/apex-legends-logo-1.png"
    ),
    PALADINS(
        "paladins",
        "Paladins",
        "https://clipground.com/images/paladins-icon-clipart-8.jpg"
    ),
    OVERWATCH(
        "overwatch",
        "Overwatch",
        "https://us.forums.blizzard.com/en/overwatch/plugins/discourse-blizzard-themes/images/icons/overwatch-social.jpg"
    ),
    OSU(
        "osu",
        "osu!",
        "https://upload.wikimedia.org/wikipedia/commons/d/d3/Osu!Logo_(2015).png"
    ),
    ROCKET_LEAGUE(
        "rocketleague",
        "Rocket League",
        "https://www.pikpng.com/pngl/m/26-261661_rocketleague-rocket-league-icon-png-clipart.png",
        listOf("Custom")
    ),
    SPECIAL_EVENT(
        "specialevent",
        "Special Events",
        "https://i.ytimg.com/vi/XyX59LtClXE/maxresdefault.jpg"
    ),
    LOL(
        "lol",
        "League of Legends",
        "https://yt3.ggpht.com/-AEerXPqHm3M/AAAAAAAAAAI/AAAAAAAAAAA/S8WpkwxItLQ/s900-c-k-no-mo-rj-c0xffffff/photo.jpg"
    );

    fun fromId(string: String) = values().first { it.id == string.trim() }
}

fun Instant.toMessageFormat() = "<t:$epochSeconds:R>"
