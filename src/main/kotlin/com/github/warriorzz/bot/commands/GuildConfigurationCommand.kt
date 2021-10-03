package com.github.warriorzz.bot.commands

import com.github.warriorzz.bot.MMBot
import com.github.warriorzz.bot.commands.configuration.ConfigurationChain
import com.github.warriorzz.bot.model.GuildConfiguration
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import kotlinx.coroutines.flow.*
import org.litote.kmongo.eq

@OptIn(KordPreview::class)
object GuildConfigurationCommand : AbstractCommand() {
    override val name: String = "configure"
    override val description: String = "Configure the settings for your guild"
    override var mustBeOwner: Boolean = true
    override var buttonPrefix: String = name

    override suspend fun invoke(interaction: ApplicationCommandInteraction): ConfigurationChain {
        val roles = HashMap<String, Snowflake>()
        MMBot.kord.getGuild(interaction.data.guildId.value!!)?.roles?.map {
            it.name to it.id
        }?.collect { roles[it.first] = it.second }
        val channels = HashMap<String, Snowflake>()
        MMBot.kord.getGuild(interaction.data.guildId.value!!)?.channels?.map {
            it.name to it.id
        }?.collect { channels[it.first] = it.second }
        val chain = ConfigurationChain(this).apply {
            start = {
                title = "Configuration"
                description = "Configure the settings for your guild"
            }

            role {
                title = "Appointment Role"
                description = "Please select the role which should be able to create appointments"
                choices = {
                    println("invoked")
                    roles.map { it.key to it.value.toString() }.shuffled().subList(0, 24).toMap().toMutableMap()
                }
            }

            roles(1..20) {
                title = "Mentionable roles"
                description = "Please mention all roles which should be mentionable for an appointment"
                choices = {
                    roles.map { it.key to it.value.toString() }.shuffled().subList(0, 24).toMap().toMutableMap()
                }
            }

            channels(1..20) {
                title = "Mentionable channels"
                description = "Please mention all channels which should be mentionable for an appointment"
                choices = {
                    val choices = mutableMapOf<String, String>()
                    var counter = 0
                    channels.map { it.key to it.value }.shuffled().forEach {
                        if (counter == 25) return@forEach
                        choices[it.first] = it.second.value.toString()
                        counter++
                    }
                    choices
                }
            }

            message {
                val role = options["role"] as Snowflake
                val mentionableRoles = options["roles"] as Map<*, *>
                val mentionableChannels = options["channels"] as Map<*, *>
                val configuration =
                    MMBot.Database.collections.guildConfigurations.findOne(GuildConfiguration::guildId eq interaction.data.guildId.value)
                if (configuration != null) {
                    MMBot.Database.collections.guildConfigurations.updateOne(
                        GuildConfiguration::guildId eq configuration.guildId,
                        GuildConfiguration(
                            configuration.guildId,
                            role,
                            mentionableRoles.filter { it.key is String && it.value is Snowflake }
                                .map { it.key as String to it.value as Snowflake }
                                .associate { it.first to it.second },
                            mentionableChannels.filter { it.key is String && it.value is Snowflake }
                                .map { it.key as String to it.value as Snowflake }
                                .associate { it.first to it.second },
                        )
                    )
                } else {
                    MMBot.Database.collections.guildConfigurations.insertOne(
                        GuildConfiguration(
                            interaction.data.guildId.value!!,
                            role,
                            mentionableRoles.filter { it.key is String && it.value is Snowflake }
                                .map { it.key as String to it.value as Snowflake }
                                .associate { it.first to it.second },
                            mentionableChannels.filter { it.key is String && it.value is Snowflake }
                                .map { it.key as String to it.value as Snowflake }
                                .associate { it.first to it.second },
                        )
                    )
                }
            }
        }
        chain.start(interaction)
        return chain
    }
}
