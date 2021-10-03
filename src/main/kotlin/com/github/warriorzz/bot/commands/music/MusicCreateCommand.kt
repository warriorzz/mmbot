package com.github.warriorzz.bot.commands.music

import com.github.warriorzz.bot.commands.AbstractCommand
import com.github.warriorzz.bot.commands.configuration.ConfigurationChain
import com.github.warriorzz.bot.config.Config
import com.github.warriorzz.bot.model.MusicParty
import com.github.warriorzz.bot.model.render
import com.github.warriorzz.bot.music.MusicManager
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed

@OptIn(KordPreview::class)
object MusicCreateCommand : AbstractCommand() {
    override val name: String = "musicparty"
    override val description: String = "Start a new listening party!"

    override suspend fun invoke(interaction: ApplicationCommandInteraction): ConfigurationChain? {
        val chain = ConfigurationChain(this).apply {
            start = {
                title = "Music Party"
                description = "Some shit right there shut up"
            }

            string {
                title = "Title"
                description = "Please type the name of the music party!"
                key = "title"
            }

            message {
                val party = MusicParty(name = options["title"] as String, creator = this@MusicCreateCommand.chainList.entries.first { it.value?.id == this@apply.id }.key, channel = Snowflake(this@apply.channelId), guild = Snowflake(this@apply.guildId))
                val message = interaction.channel.createMessage {
                    content = ""
                    embed(party.render())
                    actionRow {
                        linkButton(url = "https://accounts.spotify.com/authorize?client_id=${Config.MUSIC_CLIENT_ID}&response_type=code&show_dialog=true&redirect_uri=${Config.MUSIC_REDIRECT_URI}&state=${party.id}&scope=user-modify-playback-state%20user-read-private") {
                            label = "Join"
                        }
                    }
                }
                MusicManager.currentMusicParties.add(party.copy(message = message.id))
            }
        }
        chain.start(interaction)
        return null
    }
}
