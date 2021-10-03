package com.github.warriorzz.bot.commands.configuration

import com.github.warriorzz.bot.asTextEmoji
import com.github.warriorzz.bot.checkAnimated
import com.github.warriorzz.bot.commands.AbstractCommand
import com.github.warriorzz.bot.config.Config
import com.github.warriorzz.bot.extension.randomString
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.EphemeralInteractionResponseBehavior
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.Message
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.SelectMenuInteraction
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.delay

@KordPreview
class ConfigurationChain(private val command: AbstractCommand) {
    private val _list = ArrayList<ConfigurationChainElement>()
    val options = HashMap<String, Any>()
    val id = randomString()
    private var ephemeralResponse: EphemeralInteractionResponseBehavior? = null
    var start: EmbedBuilder.() -> Unit = {}
    internal var exited = false
    var channelId: Long = -1L
    var guildId: Long = -1L

    fun append(builder: ConfigurationChainElement.() -> Unit) {
        _list.add(ConfigurationChainElement(this).apply(builder))
    }

    fun role(builder: RoleConfigurationBuilder.() -> Unit) {
        _list.add(RoleConfigurationBuilder().apply(builder).build(this))
    }

    fun roles(range: IntRange, builder: RoleConfigurationBuilder.() -> Unit) {
        _list.add(RoleConfigurationBuilder(range, "${Emojis.checkAnimated.asTextEmoji()} %VALUES% will be mentioned!").apply(builder).build(this))
    }

    fun channel(builder: ChannelConfigurationBuilder.() -> Unit) {
        _list.add(ChannelConfigurationBuilder().apply(builder).build(this))
    }

    fun channels(range: IntRange, builder: ChannelConfigurationBuilder.() -> Unit) {
        _list.add(ChannelConfigurationBuilder(range).apply(builder).build(this))
    }

    fun string(builder: StringConfigurationBuilder.() -> Unit) {
        _list.add(StringConfigurationBuilder().apply(builder).build(this))
    }

    fun int(builder: IntConfigurationBuilder.() -> Unit) {
        _list.add(IntConfigurationBuilder().apply(builder).build(this))
    }

    fun menu(builder: MenuConfigurationBuilder.() -> Unit) {
        _list.add(MenuConfigurationBuilder().apply(builder).build(this))
    }

    fun timestamp(builder: TimestampConfigurationBuilder.() -> Unit) {
        _list.add(TimestampConfigurationBuilder().apply(builder).build(this))
    }

    fun message(builder: suspend EphemeralInteractionResponseBehavior.() -> Unit) {
        _list.add(MessageConfigurationBuilder(builder).build(this))
    }

    suspend fun start(interaction: ApplicationCommandInteraction) {
        channelId = interaction.data.channelId.value
        guildId = interaction.data.guildId.value?.value ?: -1L
        command.chainList[interaction.user.id] = this
        ephemeralResponse = interaction.respondEphemeral {
            embed(start)
        }
        execute(interaction.user.id, interaction.channelId)
    }

    suspend fun exit() {
        ephemeralResponse?.edit {
            embed {
                title = "Exiting"
                description = "Configuration exited."
            }
            components = mutableListOf()
        }
        _list.clear()
        exited = true
    }

    private suspend fun execute(userId: Snowflake, channelId: Snowflake) {
        delay(Config.MESSAGE_TIMEOUT)
        val next = _list.firstOrNull()
        if (next == null || exited) {
            command.chainList[userId] = null
            if (!exited) ephemeralResponse?.edit {
                embed {
                    title = "Ended!"
                    description = "${Emojis.checkAnimated.asTextEmoji()} The configuration ended successful!"
                    color = Color(0, 255, 0)
                }
                components = mutableListOf()
            }
            command.buttonInteractionChainList[Pair(userId, channelId)] = null
            command.messageInteractionChainList[Pair(userId, channelId)] = null
            command.menuInteractionChainList[Pair(userId, channelId)] = null
            return
        }
        next.start()
        when (next.type) {
            ConfigurationChainElement.InteractionType.MESSAGE -> {
                command.buttonInteractionChainList[Pair(userId, channelId)] = null
                command.menuInteractionChainList[Pair(userId, channelId)] = null
                command.messageInteractionChainList[Pair(
                    userId,
                    channelId
                )] = {
                    this.message.delete()
                    if (next.validate(this.message)) {
                        next.execute(this.message)
                        _list.remove(next)
                        execute(userId, channelId)
                    }
                }
            }
            ConfigurationChainElement.InteractionType.BUTTON -> {
                command.messageInteractionChainList[Pair(userId, channelId)] = null
                command.menuInteractionChainList[Pair(userId, channelId)] = null
                command.buttonInteractionChainList[Pair(
                    userId,
                    channelId
                )] = {
                    if (next.validate(this)) {
                        next.execute(this)
                        _list.remove(next)
                        execute(userId, channelId)
                    }
                }
            }
            ConfigurationChainElement.InteractionType.MENU -> {
                command.buttonInteractionChainList[Pair(userId, channelId)] = null
                command.messageInteractionChainList[Pair(userId, channelId)] = null
                command.menuInteractionChainList[Pair(
                    userId,
                    channelId
                )] = {
                    if (next.validate(this)) {
                        next.execute(this)
                        _list.remove(next)
                        execute(userId, channelId)
                    }
                }
            }
            else -> {
                _list.remove(next)
                execute(userId, channelId)
            }
        }
    }

    open class ConfigurationChainElement(private val chain: ConfigurationChain) {
        var type = InteractionType.NONE
        var start: suspend EphemeralInteractionResponseBehavior.() -> Unit = {}
        var startEmbedBuilder: (EmbedBuilder.() -> Unit)? = null
        var startActionRowBuilder: List<(ActionRowBuilder.() -> Unit)> = listOf()

        var validateMessage: suspend Message.() -> Boolean = { true }
        var validateButtonInteraction: suspend ButtonInteraction.() -> Boolean = { true }
        var validateMenuInteraction: suspend SelectMenuInteraction.() -> Boolean = { true }

        var executeMessage: suspend Message.() -> Unit = {}
        var executeButtonInteraction: suspend ButtonInteraction.() -> Unit = {}
        var executeMenuInteraction: suspend SelectMenuInteraction.() -> Unit = {}

        suspend fun start() {
            chain.ephemeralResponse?.let { start.invoke(it) }
            if (startEmbedBuilder != null) {
                chain.ephemeralResponse?.edit {
                    startEmbedBuilder?.let { embed(it) }
                    if (startActionRowBuilder.isNotEmpty()) startActionRowBuilder.forEach {
                        actionRow(it)
                    } else components =
                        mutableListOf()
                }
            }
        }

        suspend fun edit(clearComponents: Boolean? = null, builder: EmbedBuilder.() -> Unit) {
            chain.ephemeralResponse?.edit {
                embed(builder)
                if (clearComponents == true) components = mutableListOf()
            }
        }

        suspend fun validate(message: Message): Boolean = validateMessage.invoke(message)
        suspend fun validate(interaction: ButtonInteraction): Boolean = validateButtonInteraction.invoke(interaction)
        suspend fun validate(interaction: SelectMenuInteraction): Boolean = validateMenuInteraction.invoke(interaction)

        open suspend fun execute(message: Message) = executeMessage.invoke(message)
        open suspend fun execute(interaction: ButtonInteraction) = executeButtonInteraction.invoke(interaction)
        open suspend fun execute(interaction: SelectMenuInteraction) = executeMenuInteraction.invoke(interaction)

        enum class InteractionType {
            BUTTON,
            MESSAGE,
            MENU,
            NONE
        }
    }
}
