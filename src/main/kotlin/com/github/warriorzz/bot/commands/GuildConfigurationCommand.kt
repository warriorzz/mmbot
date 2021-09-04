package com.github.warriorzz.bot.commands

import com.github.warriorzz.bot.MMBot
import com.github.warriorzz.bot.asTextEmoji
import com.github.warriorzz.bot.checkAnimated
import com.github.warriorzz.bot.crossAnimated
import com.github.warriorzz.bot.model.GuildConfiguration
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.flow.*
import org.litote.kmongo.eq
import kotlin.sequences.count

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
        val chain = ConfigurationChain(this).apply {
            start = {
                title = "Configuration"
                description = "Configure the settings for your guild"
            }

            append {
                type = ConfigurationChain.ConfigurationChainElement.InteractionType.MENU
                startEmbedBuilder = {
                    title = "Mention"
                    description = "Please select a role"
                }
                startActionRowBuilder = listOf {
                    selectMenu("1" + this@apply.id + "-role") {
                        if (roles.size > 25) {
                            var counter = 0
                            roles.map { it.key to it.value }.shuffled().forEach {
                                if (counter == 25) return@forEach
                                option(it.first, it.second.value.toString())
                                counter++
                            }
                        } else {
                            roles.forEach { role ->
                                option(role.key, role.value.value.toString())
                            }
                        }
                    }
                }

                validateMenuInteraction = menu@{
                    if (this.componentId.substring(1) == this@apply.id + "-role") {
                        this.acknowledgePublic().delete()
                        val role = this.values.first().toLong()
                        options["role"] = Snowflake(role)
                        this@append.edit(true) {
                            title = "Success!"
                            description = "${Emojis.checkAnimated.asTextEmoji()} <@&$role> can now create appointments!"
                            color = Color(0, 255, 0)
                        }
                        return@menu true
                    }
                    false
                }
            }

            append {
                type = ConfigurationChain.ConfigurationChainElement.InteractionType.MESSAGE
                startEmbedBuilder = {
                    title = "Mentionable roles"
                    description = "Please mention all roles which should be mentionable for an appointment"
                }

                validateMessage = {
                    val matches = "<@&\\d*>".toRegex().findAll(content).count() != 0
                    val message = this
                    if (!matches) {
                        this@append.edit {
                            title = "Error!"
                            description =
                                "${Emojis.crossAnimated.asTextEmoji()} \"${message.content}\" does not contain any roles!"
                            color = Color(255, 0, 0)
                        }
                    }
                    matches
                }

                executeMessage = message@{
                    options["roles"] = "<@&\\d*>".toRegex().findAll(content).asFlow()
                        .map { this@message.supplier.getRole(this.getGuild().id, Snowflake(it.value.substring(3, 21))) }.map {
                            it.name to it.id
                        }.toList(mutableListOf()).associate { it.first to it.second }
                    val message = this
                    this@append.edit {
                        title = "Success!"
                        description =
                            "${Emojis.checkAnimated.asTextEmoji()} The roles \"${message.content}\" will be mentionable!"
                        color = Color(0, 255, 0)
                    }
                }
            }

            append {
                type = ConfigurationChain.ConfigurationChainElement.InteractionType.MESSAGE
                startEmbedBuilder = {
                    title = "Mentionable channels"
                    description = "Please mention all channels which should be mentionable for an appointment"
                }

                validateMessage = {
                    val matches = "<#\\d*>".toRegex().findAll(content).count() != 0
                    val message = this
                    if (!matches) {
                        this@append.edit {
                            title = "Error!"
                            description =
                                "${Emojis.crossAnimated.asTextEmoji()} \"${message.content}\" does not contain any channels!"
                            color = Color(255, 0, 0)
                        }
                    }
                    matches
                }

                executeMessage = message@{
                    options["channels"] = "<#\\d*>".toRegex().findAll(content).asFlow()
                        .map { this@message.supplier.getChannel(Snowflake(it.value.substring(2, 20))) }.map {
                        it.data.name.value to it.id
                    }.toList(mutableListOf()).associate { it.first to it.second }

                    val message = this
                    this@append.edit {
                        title = "Success!"
                        description =
                            "${Emojis.checkAnimated.asTextEmoji()} The channels \"${message.content}\" will be mentionable!"
                        color = Color(0, 255, 0)
                    }
                }
            }

            append {
                type = ConfigurationChain.ConfigurationChainElement.InteractionType.NONE
                start = {
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
        }
        chain.start(interaction)
        return chain
    }
}
