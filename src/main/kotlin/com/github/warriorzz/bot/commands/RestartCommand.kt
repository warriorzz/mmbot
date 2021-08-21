package com.github.warriorzz.bot.commands

import com.github.warriorzz.bot.MMBot
import com.github.warriorzz.bot.config.Config
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.CommandInteraction
import dev.kord.rest.builder.message.create.embed

@OptIn(KordPreview::class)
object RestartCommand : AbstractCommand() {
    override val name: String = "restart"
    override val description: String = "Restarts the bot."

    override suspend fun invoke(interaction: CommandInteraction) : ConfigurationChain? {
        if (interaction.user.id != Config.OWNER_ID) {
            interaction.respondHasNoPermission()
            return null
        }
        val appointmentCreateChains = AppointmentCreateCommand.chainList.isEmpty() || !AppointmentCreateCommand.chainList.map { it.value != null }.reduce { acc, b -> acc || b }
        val guildConfigurationChain = GuildConfigurationCommand.chainList.isEmpty() || !GuildConfigurationCommand.chainList.map { it.value != null }.reduce { acc, b -> acc || b }
        if (appointmentCreateChains && guildConfigurationChain) {
            interaction.respondEphemeral {
                embed {
                    title = "Restarting..."
                    description = "Bot is restarting."
                    color = Color(0, 255, 0)
                }
            }
            MMBot.restart()
        } else {
            interaction.respondEphemeral {
                embed {
                    title = "Can't restart!"
                    description = "There are currently some configurations going on."
                    color = Color(255, 0, 0)
                }
            }
        }
        return null
    }
}
