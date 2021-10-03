package com.github.warriorzz.bot.commands

import com.github.warriorzz.bot.MMBot
import com.github.warriorzz.bot.asTextEmoji
import com.github.warriorzz.bot.checkAnimated
import com.github.warriorzz.bot.commands.configuration.ConfigurationChain
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

            menu {
                title = "Game"
                description = "Please provide a game to play."
                key = "game"
                choices = {
                    val map = mutableMapOf<String, String>()
                    Game.values().onEach {
                        map[it.value] = it.id
                    }
                    map
                }
            }

            string {
                title = "Title"
                description = "Please provide a title for the appointment."
                key = "title"
            }

            timestamp {
                title = "Time"
                description = "Please provide the time of the appointment. Current selected time: %TIME%"
                editedDescription = "Current selected time: %TIME%"
                errorDescription = "Error! %TIME% is too early!"
                key = "time"
            }

            int {
                title = "Players"
                description = "Please provide the amount of required players for the appointment."
                key = "players"
            }

            menu {
                title = "Special Match"
                description = "Is this match a special match?"
                key = "special"
                choices = {
                    val map = mutableMapOf("None" to "no special match")
                    Game.AMONG_US.fromId(this@apply.options["game"] as String).specialChoises?.forEach { choice ->
                        map[choice] = choice
                    }
                    map
                }
            }

            role {
                title = "Mention"
                description = "Please select a role to mention."
                key = "role"
                choices = {
                    roles.map { it.key to it.value.toString() }.shuffled().subList(0, 24).toMap().toMutableMap()
                }
            }

            channel {
                title = "Channel"
                description = "Please select a channel to send the appointment message to."
                key = "channel"
                choices = {
                    val map = mutableMapOf<String, String>()
                    var counter = 0
                    channels.forEach {
                        if (counter == 26) return@forEach
                        map[it.key] = it.value.value.toString()
                        counter++
                    }
                    map
                }
            }

            message {
                val date = options["time"] as Long
                val game = Game.APEX_LEGENDS.fromId(options["game"] as String)

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
