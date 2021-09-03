package com.github.warriorzz.bot.commands

import com.github.warriorzz.bot.MMBot
import com.github.warriorzz.bot.asTextEmoji
import com.github.warriorzz.bot.checkAnimated
import com.github.warriorzz.bot.config.Config
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.EphemeralInteractionResponseBehavior
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.Message
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.CommandInteraction
import dev.kord.core.entity.interaction.Interaction
import dev.kord.core.entity.interaction.SelectMenuInteraction
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

@KordPreview
abstract class AbstractCommand {
    abstract val name: String
    abstract val description: String
    protected open var mustBeOwner: Boolean = false
    protected open var permission: Permission? = null
    protected open var buttonPrefix: String = ""
    val messageInteractionChainList = HashMap<Pair<Snowflake, Snowflake>, (suspend MessageCreateEvent.() -> Unit)?>()
    val buttonInteractionChainList = HashMap<Pair<Snowflake, Snowflake>, (suspend ButtonInteraction.() -> Unit)?>()
    val menuInteractionChainList = HashMap<Pair<Snowflake, Snowflake>, (suspend SelectMenuInteraction.() -> Unit)?>()

    val chainList = HashMap<Snowflake, ConfigurationChain?>()
    private lateinit var kord: Kord

    abstract suspend fun invoke(interaction: CommandInteraction): ConfigurationChain?
    protected open suspend fun invokeButtonReaction(interaction: ButtonInteraction) {}

    open suspend fun register(kord: Kord) {
        this.kord = kord
        if (Config.DEV_ENVIRONMENT) {
            kord.createGuildApplicationCommands(
                Config.DEV_GUILD
            ) {
                input(name, description) {}
            }
        } else {
            kord.createGlobalApplicationCommands {
                input(name, description) {}
            }
        }
        kord.events.buffer(Channel.UNLIMITED).filterIsInstance<InteractionCreateEvent>()
            .filter { it.interaction is CommandInteraction && (it.interaction as CommandInteraction).command.rootName == name }
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
                    val chain = invoke(it.interaction as CommandInteraction)
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
                    println("")
                    println("MESSAGE BLOCK")
                    println("")
                    exception.printStackTrace()
                    println("")
                    println("MESSAGE BLOCK")
                    println("")
                }
            }.launchIn(kord)
    }
}

@KordPreview
class ConfigurationChain(private val command: AbstractCommand) {
    private val _list = ArrayList<ConfigurationChainElement>()
    val options = HashMap<String, Any>()
    val id = randomString()
    private var ephemeralResponse: EphemeralInteractionResponseBehavior? = null
    var start: EmbedBuilder.() -> Unit = {}
    internal var exited = false

    fun append(builder: ConfigurationChainElement.() -> Unit) {
        _list.add(ConfigurationChainElement(this).apply(builder))
    }

    suspend fun start(interaction: CommandInteraction) {
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

@OptIn(KordPreview::class)
internal suspend fun Interaction.respondHasNoPermission() = respondEphemeral {
    embed {
        title = "NO PERMISSION"
    }
}

private fun randomString(): String {
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..48)
        .map { charPool.random() }
        .joinToString("")
}

fun Long.toDateString(): String {
    val dateTime =
        Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
    return DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy").format(dateTime)
}

fun Long.toTimeString(): String {
    val dateTime =
        Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
    return DateTimeFormatter.ofPattern("HH:mm").format(dateTime)
}
