package com.github.warriorzz.bot.commands

import com.github.warriorzz.bot.MMBot
import com.github.warriorzz.bot.asTextEmoji
import com.github.warriorzz.bot.checkAnimated
import com.github.warriorzz.bot.model.GuildConfiguration
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.entity.interaction.CommandInteraction
import dev.kord.rest.builder.message.modify.embed
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.litote.kmongo.eq

@OptIn(KordPreview::class)
object GuildConfigurationCommand : AbstractCommand() {
    override val name: String = "configure"
    override val description: String = "Configure the settings for your guild"
    override var mustBeOwner: Boolean = true
    override var buttonPrefix: String = name

    override suspend fun invoke(interaction: CommandInteraction) {
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
                startActionRowBuilder = {
                    selectMenu("1"+ this@apply.id + "-role") {
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

                executeMenuInteraction = {
                    if (this.componentId.substring(1) == this@apply.id + "-role") {
                        val acknowledged = this.acknowledgeEphemeral()
                        val role = this.values.first().toLong()
                        val configuration =
                            MMBot.Database.collections.guildConfigurations.findOne(GuildConfiguration::guildId eq this.data.guildId.value)
                        if (configuration != null) {
                            MMBot.Database.collections.guildConfigurations.updateOne(
                                GuildConfiguration::guildId eq configuration.guildId,
                                GuildConfiguration(configuration.guildId, Snowflake(role))
                            )
                        } else {
                            MMBot.Database.collections.guildConfigurations.insertOne(
                                GuildConfiguration(
                                    this.data.guildId.value!!,
                                    Snowflake(role)
                                )
                            )
                        }
                        acknowledged.edit {
                            embed {
                                title = "Success!"
                                description = "${Emojis.checkAnimated.asTextEmoji()} <@&$role> can now create appointments!"
                                color = Color(0, 255, 0)
                            }
                            components = mutableListOf()
                        }
                    }
                }
            }
        }
        chain.start(interaction)
    }
}
