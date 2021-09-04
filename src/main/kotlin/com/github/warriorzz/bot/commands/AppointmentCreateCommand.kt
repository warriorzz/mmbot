package com.github.warriorzz.bot.commands

import com.github.warriorzz.bot.MMBot
import com.github.warriorzz.bot.asTextEmoji
import com.github.warriorzz.bot.checkAnimated
import com.github.warriorzz.bot.config.Config
import com.github.warriorzz.bot.crossAnimated
import com.github.warriorzz.bot.model.*
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.litote.kmongo.eq
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

@OptIn(KordPreview::class)
object AppointmentCreateCommand : AbstractCommand() {
    override val name: String = "appointment"
    override val description: String = "Create a new appointment for a game"
    override var buttonPrefix: String = name
    override var mustBeOwner: Boolean = false

    override suspend fun invoke(interaction: ApplicationCommandInteraction): ConfigurationChain? {
        val guildConfiguration =
            MMBot.Database.collections.guildConfigurations.findOne(GuildConfiguration::guildId eq interaction.data.guildId.asOptional.value)
        if (!interaction.user.asMember(interaction.data.guildId.value ?: Snowflake(-1)).roleIds.contains(
                guildConfiguration?.appointmentRole
            )
        ) {
            interaction.respondEphemeral {
                embed {
                    title = "NO PERMISSION"
                }
            }
            return null
        }

        val roles =
            MMBot.Database.collections.guildConfigurations.findOne(GuildConfiguration::guildId eq interaction.data.guildId.value)?.mentionableRoles
                ?: MMBot.kord.getGuild(interaction.data.guildId.value!!)?.roles?.map {
                    it.name to it.id
                }?.toList(mutableListOf())?.associate { it.first to it.second } as Map<String, Snowflake>

        val channels =
            MMBot.Database.collections.guildConfigurations.findOne(GuildConfiguration::guildId eq interaction.data.guildId.value)?.mentionableChannels
                ?: MMBot.kord.getGuild(interaction.data.guildId.value!!)?.channels?.filterIsInstance<TextChannel>()
                    ?.map {
                        it.name to it.id
                    }?.toList(mutableListOf())?.associate { it.first to it.second } as Map<String, Snowflake>

        val chain = ConfigurationChain(this).apply {
            start = {
                title = "New game?"
                description = "Creating a new appointment."
                color = Color(0, 0, 255)
            }

            append {
                type = ConfigurationChain.ConfigurationChainElement.InteractionType.MENU
                startEmbedBuilder = {
                    title = "Game"
                    description = "Please provide a game to play."
                }
                startActionRowBuilder = listOf {
                    selectMenu(this@apply.id) {
                        Game.values().onEach {
                            option(it.value, it.id) {}
                        }
                    }
                }

                validateMenuInteraction = {
                    this.componentId == this@apply.id
                }

                executeMenuInteraction = {
                    if (this.componentId == this@apply.id) {
                        this.acknowledgePublic().delete()
                        val game = Game.AMONG_US.fromId(this.values.first())
                        options["game"] = game
                        this@append.edit(true) {
                            title = "Success!"
                            description = "${Emojis.checkAnimated.asTextEmoji()} ${game.value} will be played!"
                            color = Color(0, 255, 0)
                        }
                    }
                }
            }

            append {
                type = ConfigurationChain.ConfigurationChainElement.InteractionType.MESSAGE
                startEmbedBuilder = {
                    title = "Title"
                    description = "Please provide a title for the appointment."
                }

                executeMessage = {
                    options["title"] = this.content
                    val message = this
                    this@append.edit {
                        title = "Success!"
                        description =
                            "${Emojis.checkAnimated.asTextEmoji()} \"${message.content}\" was accepted as title."
                        color = Color(0, 255, 0)
                    }
                }
            }

            append {
                type = ConfigurationChain.ConfigurationChainElement.InteractionType.BUTTON
                startEmbedBuilder = {
                    title = "Time"
                    options["time"] = LocalDateTime.now(Clock.systemUTC()).toInstant(ZoneOffset.ofHours(0))?.epochSecond
                        ?: System.currentTimeMillis()
                    description =
                        "Please provide the time of the appointment. Current selected time: ${
                            Instant.fromEpochSeconds(
                                options["time"] as Long
                            ).toMessageFormat(DiscordTimestampStyle.ShortDateTime)
                        }"
                }
                startActionRowBuilder = listOf({
                    interactionButton(ButtonStyle.Danger, "$id-2") {
                        label = "- 5m"
                    }
                    interactionButton(ButtonStyle.Danger, "$id-3") {
                        label = "- 1m"
                    }
                    interactionButton(ButtonStyle.Success, "$id-0") {
                        emoji = Emojis.checkAnimated
                    }
                    interactionButton(ButtonStyle.Danger, "$id-4") {
                        label = "+ 1m"
                    }
                    interactionButton(ButtonStyle.Danger, "$id-5") {
                        label = "+ 5m"
                    }
                }, {
                    interactionButton(ButtonStyle.Danger, "$id-6") {
                        label = "- 1h"
                    }
                    interactionButton(ButtonStyle.Danger, "$id-7") {
                        label = "- 15m"
                    }
                    interactionButton(ButtonStyle.Success, "$id-1") {
                        emoji = Emojis.checkAnimated
                    }
                    interactionButton(ButtonStyle.Danger, "$id-8") {
                        label = "+ 15m"
                    }
                    interactionButton(ButtonStyle.Danger, "$id-9") {
                        label = "+ 1h"
                    }
                })

                validateButtonInteraction = validateButtonInteraction@{
                    if (!this.componentId.startsWith(this@apply.id)) return@validateButtonInteraction false
                    when (this.componentId.last().digitToInt()) {
                        in 0..1 -> {
                            this.acknowledgePublic().delete()
                            if ((options["time"] as Long) <= LocalDateTime.now(Clock.systemUTC())
                                    .toInstant(ZoneOffset.ofHours(0)).epochSecond
                            ) {
                                this@append.edit {
                                    title = "Error!"
                                    description =
                                        "${Emojis.crossAnimated.asTextEmoji()} ${
                                            Instant.fromEpochSeconds(options["time"] as Long)
                                                .toMessageFormat(DiscordTimestampStyle.ShortDateTime)
                                        } is too early!"
                                    color = Color(255, 0, 0)
                                }
                                return@validateButtonInteraction false
                            }
                            return@validateButtonInteraction true
                        }
                        2 -> {
                            options["time"] = options["time"] as Long - 5L * 60L
                        }
                        3 -> {
                            options["time"] = options["time"] as Long - 1L * 60L
                        }
                        4 -> {
                            options["time"] = options["time"] as Long + 1L * 60L
                        }
                        5 -> {
                            options["time"] = options["time"] as Long + 5L * 60L
                        }
                        6 -> {
                            options["time"] = options["time"] as Long - 60L * 60L
                        }
                        7 -> {
                            options["time"] = options["time"] as Long - 15L * 60L
                        }
                        8 -> {
                            options["time"] = options["time"] as Long + 15L * 60L
                        }
                        9 -> {
                            options["time"] = options["time"] as Long + 60L * 60L
                        }
                    }
                    this.acknowledgePublic().delete()
                    this@append.edit {
                        title = "Time"
                        description = "Current selected time: ${
                            Instant.fromEpochSeconds(options["time"] as Long)
                                .toMessageFormat(DiscordTimestampStyle.ShortDateTime)
                        }"
                        color = Color(120, 120, 120)
                    }
                    false
                }

                executeButtonInteraction = {
                    this@append.edit(true) {
                        title = "Success!"
                        description =
                            "${Emojis.checkAnimated.asTextEmoji()} ${
                                Instant.fromEpochSeconds(options["time"] as Long)
                                    .toMessageFormat(DiscordTimestampStyle.ShortDateTime)
                            } was selected!"
                        color = Color(0, 255, 0)
                    }
                }
            }

            append {
                type = ConfigurationChain.ConfigurationChainElement.InteractionType.MESSAGE
                startEmbedBuilder = {
                    title = "Players"
                    description = "Please provide the amount of required players for the appointment."
                }

                validateMessage = {
                    val matches = this.content.matches("\\d*".toRegex()) && (this.content.toIntOrNull() ?: -1) > 0
                    val message = this
                    if (!matches) {
                        this@append.edit {
                            title = "Error!"
                            description =
                                "${Emojis.crossAnimated.asTextEmoji()} \"${message.content}\" is not a valid amount!"
                            color = Color(255, 0, 0)
                        }
                    }
                    matches
                }

                executeMessage = {
                    options["players"] = this.content.toInt()
                    val message = this
                    this@append.edit {
                        title = "Success!"
                        description =
                            "${Emojis.checkAnimated.asTextEmoji()} ${message.content} players will be counted in!"
                        color = Color(0, 255, 0)
                    }
                }
            }

            append {
                type = ConfigurationChain.ConfigurationChainElement.InteractionType.MENU
                startEmbedBuilder = {
                    title = "Special Match"
                    description = "Is this match a special match?"
                }

                startActionRowBuilder = listOf {
                    selectMenu(this@apply.id) {
                        option("None", "no special match")
                        (this@apply.options["game"] as Game).specialChoises?.forEach { choice ->
                            option(choice, choice)
                        }
                    }
                }

                validateMenuInteraction = {
                    this.componentId == this@apply.id
                }

                executeMenuInteraction = {
                    if (this.componentId == this@apply.id) {
                        this.acknowledgePublic().delete()
                        val specialMatch = this.values.first()
                        options["special"] = specialMatch
                        this@append.edit(true) {
                            title = "Success!"
                            description = "${Emojis.checkAnimated.asTextEmoji()} This game will be $specialMatch."
                            color = Color(0, 255, 0)
                        }
                    }
                }
            }

            append {
                type = ConfigurationChain.ConfigurationChainElement.InteractionType.MENU
                startEmbedBuilder = {
                    title = "Mention"
                    description = "Please select a role to mention."
                }
                startActionRowBuilder = listOf {
                    selectMenu(this@apply.id + "-role") {
                        option("none", "-1")
                        var counter = 0
                        roles.forEach {
                            if (counter == 25) return@forEach
                            option(it.key, it.value.value.toString())
                            counter++
                        }
                    }
                }

                validateMenuInteraction = {
                    this.componentId == this@apply.id + "-role"
                }

                executeMenuInteraction = {
                    if (this.componentId == this@apply.id + "-role") {
                        this.acknowledgePublic().delete()
                        val role = this.values.first().toLong()
                        options["role"] = role
                        this@append.edit(true) {
                            title = "Success!"
                            description = "${Emojis.checkAnimated.asTextEmoji()} <@&$role> will be mentioned!"
                            color = Color(0, 255, 0)
                        }
                    }
                }
            }

            append {
                type = ConfigurationChain.ConfigurationChainElement.InteractionType.MENU
                startEmbedBuilder = {
                    title = "Channel"
                    description = "Please select a channel to send the appointment message to."
                }
                startActionRowBuilder = listOf {
                    selectMenu(this@apply.id + "-channel") {
                        if (!channels.values.contains(Snowflake(this@apply.channelId))) option("This channel", "${this@apply.channelId}")
                        channels.forEach { channel ->
                            option(channel.key, channel.value.value.toString())
                        }
                    }
                }

                validateMenuInteraction = {
                    this.componentId == this@apply.id + "-channel"
                }

                executeMenuInteraction = {
                    if (this.componentId == this@apply.id + "-channel") {
                        this.acknowledgePublic().delete()
                        val channel = this.values.first().toLong()
                        options["channel"] = channel
                        this@append.edit(true) {
                            title = "Success!"
                            description = "${Emojis.checkAnimated.asTextEmoji()} <#$channel> will be mentioned!"
                            color = Color(0, 255, 0)
                        }
                    }
                }
            }

            append {
                start = {
                    val date = options["time"] as Long
                    val game = options["game"] as Game

                    val specialMatch = options["special"] as String
                    val players = options["players"] as Int
                    val title = options["title"] as String
                    val roleId = options["role"] as Long
                    val channelId = options["channel"] as Long

                    val match = Match(
                        requiredPlayers = players,
                        players = mutableListOf(),
                        startTime = date,
                        name = title,
                        specialMatch = specialMatch,
                        game = game,
                        guild = interaction.data.guildId.value,
                        channel = Snowflake(channelId),
                        message = null,
                        chainId = this@apply.id,
                        creator = interaction.user.id,
                        createdStamp = System.currentTimeMillis()
                    )
                    MMBot.Database.collections.matches.insertOne(match)

                    val message = (MMBot.kord.getGuild(interaction.data.guildId.value!!)
                        ?.getChannel(Snowflake(channelId)) as TextChannel).createMessage {
                        content = if (roleId == -1L) "@all" else "<@&$roleId>"
                        embed(match.renderMessage())
                        actionRow {
                            interactionButton(ButtonStyle.Primary, "add-$id") {
                                label = "Join"
                            }
                            interactionButton(ButtonStyle.Primary, "rem-$id") {
                                label = "Leave"
                            }
                            interactionButton(ButtonStyle.Danger, "can-$id") {
                                label = "Cancel"
                            }
                        }
                    }
                    MMBot.Database.collections.matches.updateOne(
                        Match::_id eq match._id,
                        Match(
                            match._id,
                            match.requiredPlayers,
                            match.players,
                            match.startTime,
                            match.name,
                            match.specialMatch,
                            match.game,
                            match.guild,
                            match.channel,
                            message.id,
                            match.chainId,
                            match.creator,
                            match.createdStamp
                        )
                    )
                    MMBot.launch {
                        delay(
                            match.startTime * 1000 - LocalDateTime.now(Clock.systemUTC())
                                .toInstant(ZoneOffset.ofHours(0)).epochSecond * 1000
                        )
                        MMBot.Database.collections.matches.findOneById(match._id)?.start()
                    }
                }
            }
        }
        chain.start(interaction)
        return chain
    }

    override suspend fun invokeButtonReaction(interaction: ButtonInteraction) {
        val match = MMBot.Database.collections.matches.findOne(Match::chainId eq interaction.componentId.substring(4))
        match?.let { actMatch ->
            val acknowledged = interaction.acknowledgeEphemeral()
            when (interaction.componentId.subSequence(0, 3)) {
                "add" -> if (!actMatch.players.contains(interaction.user.id)) {
                    if (actMatch.players.size == actMatch.requiredPlayers + Config.BENCH_SIZE) {
                        acknowledged.edit {
                            embed {
                                title = "Error!"
                                description =
                                    "${Emojis.crossAnimated.asTextEmoji()} There are already enough players registered!"
                                color = Color(255, 0, 0)
                            }
                        }
                        return@let
                    }
                    val newPlayers = ArrayList<Snowflake>()
                    newPlayers.add(interaction.user.id)
                    newPlayers.addAll(actMatch.players)

                    val newMatch = Match(
                        message = actMatch.message,
                        specialMatch = actMatch.specialMatch,
                        startTime = actMatch.startTime,
                        players = newPlayers,
                        requiredPlayers = actMatch.requiredPlayers,
                        _id = actMatch._id,
                        chainId = actMatch.chainId,
                        channel = actMatch.channel,
                        game = actMatch.game,
                        guild = actMatch.guild,
                        name = actMatch.name,
                        creator = actMatch.creator,
                        createdStamp = actMatch.createdStamp
                    )
                    MMBot.Database.collections.matches.updateOne(
                        Match::_id eq actMatch._id, newMatch
                    )

                    val guild = MMBot.kord.getGuild(actMatch.guild!!)
                    (guild?.getChannel(actMatch.channel!!) as TextChannel).getMessage(actMatch.message!!).edit {
                        embed(newMatch.renderMessage())
                    }
                    acknowledged.edit {
                        embed {
                            title = "Joined!"
                            description =
                                "${Emojis.checkAnimated.asTextEmoji()} You are now counted in for ${actMatch.name}."
                            color = Color(0, 255, 0)
                        }
                    }
                } else {
                    acknowledged.edit {
                        embed {
                            title = "Error!"
                            description =
                                "${Emojis.crossAnimated.asTextEmoji()} You are already registered for this appointment!"
                            color = Color(255, 0, 0)
                        }
                    }
                }

                "rem" -> if (actMatch.players.contains(interaction.user.id)) {
                    val newPlayers = ArrayList<Snowflake>()
                    newPlayers.addAll(actMatch.players)
                    newPlayers.remove(interaction.user.id)

                    val newMatch = Match(
                        message = actMatch.message,
                        specialMatch = actMatch.specialMatch,
                        startTime = actMatch.startTime,
                        players = newPlayers,
                        requiredPlayers = actMatch.requiredPlayers,
                        _id = actMatch._id,
                        chainId = actMatch.chainId,
                        channel = actMatch.channel,
                        game = actMatch.game,
                        guild = actMatch.guild,
                        name = actMatch.name,
                        creator = actMatch.creator,
                        createdStamp = actMatch.createdStamp
                    )
                    MMBot.Database.collections.matches.updateOne(
                        Match::_id eq actMatch._id, newMatch
                    )

                    val guild = MMBot.kord.getGuild(actMatch.guild!!)
                    (guild?.getChannel(actMatch.channel!!) as TextChannel).getMessage(actMatch.message!!).edit {
                        embed(newMatch.renderMessage())
                    }
                    acknowledged.edit {
                        embed {
                            title = "Left!"
                            description = "You left ${actMatch.name}."
                            color = Color(0, 255, 0)
                        }
                    }
                } else {
                    acknowledged.edit {
                        embed {
                            title = "Error!"
                            description =
                                "${Emojis.crossAnimated.asTextEmoji()} You aren't registered for this appointment!"
                            color = Color(255, 0, 0)
                        }
                    }
                }
                "can" -> {
                    if (actMatch.creator != interaction.user.id) {
                        acknowledged.edit {
                            embed {
                                title = "Error!"
                                description =
                                    "${Emojis.crossAnimated.asTextEmoji()} You haven't created this appointment, so you can't cancel it!"
                                color = Color(255, 0, 0)
                            }
                        }
                        return
                    }
                    val guild = MMBot.kord.getGuild(actMatch.guild!!)
                    (guild?.getChannel(actMatch.channel!!) as TextChannel).getMessage(actMatch.message!!).delete()
                    MMBot.Database.collections.matches.deleteOneById(actMatch._id)
                    acknowledged.edit {
                        embed {
                            title = "Cancelled!"
                            description = "You cancelled ${actMatch.name}."
                            color = Color(0, 255, 0)
                        }
                    }
                }
                else -> {
                    acknowledged.edit {
                        embed {
                            title = "${Emojis.crossAnimated.asTextEmoji()} Error!"
                            color = Color(255, 0, 0)
                        }
                    }
                }
            }
        }
    }
}
