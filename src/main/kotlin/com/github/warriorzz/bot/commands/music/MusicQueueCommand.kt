package com.github.warriorzz.bot.commands.music

import com.github.warriorzz.bot.commands.AbstractCommand
import com.github.warriorzz.bot.commands.configuration.ConfigurationChain
import dev.kord.common.annotation.KordPreview
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string

@OptIn(KordPreview::class)
object MusicQueueCommand : AbstractCommand() {
    override val name: String = "queue"
    override val description: String = "Queue music to current music party"
    override var commandBuilder: ChatInputCreateBuilder.() -> Unit = {
        string("name", "The name of the song")
    }

    override suspend fun invoke(interaction: ApplicationCommandInteraction): ConfigurationChain? {

        return null
    }

    override suspend fun invokeButtonReaction(interaction: ButtonInteraction) {}
}
