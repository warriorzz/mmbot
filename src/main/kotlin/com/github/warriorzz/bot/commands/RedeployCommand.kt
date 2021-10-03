package com.github.warriorzz.bot.commands

import com.github.warriorzz.bot.MMBot
import com.github.warriorzz.bot.commands.configuration.ConfigurationChain
import com.github.warriorzz.bot.config.Config
import com.github.warriorzz.bot.extension.respondHasNoPermission
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.rest.builder.message.create.embed

@OptIn(KordPreview::class)
object RedeployCommand : AbstractCommand() {
    override val name: String = "redeploy"
    override val description: String = "Redeploys the bot."

    override suspend fun invoke(interaction: ApplicationCommandInteraction): ConfigurationChain? {
        if (interaction.user.id != Config.OWNER_ID) {
            interaction.respondHasNoPermission()
            return null
        }
        val appointmentCreateChains =
            AppointmentCreateCommand.chainList.isEmpty() || !AppointmentCreateCommand.chainList.map { it.value != null }
                .reduce { acc, b -> acc || b }
        val guildConfigurationChain =
            GuildConfigurationCommand.chainList.isEmpty() || !GuildConfigurationCommand.chainList.map { it.value != null }
                .reduce { acc, b -> acc || b }
        if (appointmentCreateChains && guildConfigurationChain) {
            interaction.respondEphemeral {
                embed {
                    title = "Redeploying..."
                    description = "Bot is redeploying."
                    color = Color(0, 255, 0)
                }
            }
            MMBot.redeploy()
        } else {
            interaction.respondEphemeral {
                embed {
                    title = "Can't redeploy!"
                    description = "There are currently some configurations going on."
                    color = Color(255, 0, 0)
                }
            }
        }
        return null
    }
}
