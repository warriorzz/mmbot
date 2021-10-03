package com.github.warriorzz.bot.commands

import com.github.warriorzz.bot.MMBot
import com.github.warriorzz.bot.commands.configuration.ConfigurationChain
import com.github.warriorzz.bot.extension.respondHasNoPermission
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.SelectMenuInteraction
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@KordPreview
abstract class AbstractCommand {
    abstract val name: String
    abstract val description: String
    protected open var mustBeOwner: Boolean = false
    protected open var permission: Permission? = null
    protected open var buttonPrefix: String = ""
    protected open var commandBuilder: ChatInputCreateBuilder.() -> Unit = {}
    val messageInteractionChainList = HashMap<Pair<Snowflake, Snowflake>, (suspend MessageCreateEvent.() -> Unit)?>()
    val buttonInteractionChainList = HashMap<Pair<Snowflake, Snowflake>, (suspend ButtonInteraction.() -> Unit)?>()
    val menuInteractionChainList = HashMap<Pair<Snowflake, Snowflake>, (suspend SelectMenuInteraction.() -> Unit)?>()

    val chainList = HashMap<Snowflake, ConfigurationChain?>()
    private lateinit var kord: Kord

    abstract suspend fun invoke(interaction: ApplicationCommandInteraction): ConfigurationChain?
    protected open suspend fun invokeButtonReaction(interaction: ButtonInteraction) {}

    open suspend fun register(kord: Kord) {
        this.kord = kord
        MMBot.commandList[name to description] = commandBuilder
        kord.events.buffer(Channel.UNLIMITED).filterIsInstance<InteractionCreateEvent>()
            .filter { it.interaction is ApplicationCommandInteraction && (it.interaction as ApplicationCommandInteraction).name == name }
            .onEach {
                try {
                    if (mustBeOwner && it.interaction.user.asMemberOrNull(it.interaction.data.guildId.orElse(Snowflake(-1L)))
                            ?.isOwner() == false
                    ) {
                        it.interaction.respondHasNoPermission()
                        return@onEach
                    }
                    if (permission != null && it.interaction.user.asMemberOrNull(
                            it.interaction.data.guildId.orElse(
                                Snowflake(-1L)
                            )
                        )?.getPermissions()?.contains(permission!!) == false
                    ) {
                        it.interaction.respondHasNoPermission()
                        return@onEach
                    }
                    val chain = invoke(it.interaction as ApplicationCommandInteraction)
                    MMBot.launch {
                        delay(15L * 60 * 1000)
                        if (chainList[it.interaction.user.id]?.id == chain?.id) {
                            chain?.exited = true
                            val userId = it.interaction.user.id
                            val channelId = it.interaction.channelId
                            buttonInteractionChainList[Pair(userId, channelId)] = null
                            messageInteractionChainList[Pair(userId, channelId)] = null
                            menuInteractionChainList[Pair(userId, channelId)] = null
                        }
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }.launchIn(kord)

        kord.events.buffer(Channel.UNLIMITED).filterIsInstance<ButtonInteractionCreateEvent>()
            .onEach {
                try {
                    invokeButtonReaction(it.interaction)
                    buttonInteractionChainList[Pair(
                        it.interaction.user.id,
                        it.interaction.channelId
                    )]?.invoke(it.interaction)
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }.launchIn(kord)

        kord.events.buffer(Channel.UNLIMITED).filterIsInstance<SelectMenuInteractionCreateEvent>()
            .onEach {
                try {
                    menuInteractionChainList[Pair(
                        it.interaction.user.id,
                        it.interaction.channelId
                    )]?.invoke(it.interaction)
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }.launchIn(kord)

        kord.events.buffer(Channel.UNLIMITED).filterIsInstance<MessageCreateEvent>()
            .filter { this.chainList.contains(it.member?.id ?: Snowflake(-1)) }
            .onEach {
                try {
                    if (it.message.content.startsWith("exit") || it.message.content.startsWith("quit")) {
                        it.member?.id?.let { it1 ->
                            chainList[it1]?.exit()
                        }
                        it.message.delete()
                        return@onEach
                    } else {
                        messageInteractionChainList[Pair(it.member?.id ?: Snowflake(-1), it.message.channelId)]?.invoke(
                            it
                        )
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }.launchIn(kord)
    }
}
