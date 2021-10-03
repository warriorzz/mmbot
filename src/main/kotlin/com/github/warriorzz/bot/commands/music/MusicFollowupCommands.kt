package com.github.warriorzz.bot.commands.music

import com.github.warriorzz.bot.commands.AbstractCommand
import com.github.warriorzz.bot.commands.configuration.ConfigurationChain
import com.github.warriorzz.bot.music.MusicManager
import dev.kord.common.annotation.KordPreview
import dev.kord.core.entity.interaction.ApplicationCommandInteraction

@OptIn(KordPreview::class)
object MusicFollowupCommands : AbstractCommand() {
    override val name: String = "followup"
    override val description: String = "In case you skipped or stopped your playback, followup the playback of the "

    override suspend fun invoke(interaction: ApplicationCommandInteraction): ConfigurationChain? {
        MusicManager.followup(interaction.user.id)
        return null
    }
}
