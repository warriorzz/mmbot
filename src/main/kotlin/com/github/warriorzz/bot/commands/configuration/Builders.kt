package com.github.warriorzz.bot.commands.configuration

import com.github.warriorzz.bot.asTextEmoji
import com.github.warriorzz.bot.checkAnimated
import com.github.warriorzz.bot.crossAnimated
import com.github.warriorzz.bot.model.DiscordTimestampStyle
import com.github.warriorzz.bot.model.toMessageFormat
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.EphemeralInteractionResponseBehavior
import dev.kord.core.entity.Message
import dev.kord.x.emoji.Emojis
import kotlinx.datetime.Instant
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

@KordPreview
class TimestampConfigurationBuilder : ConfigurationChainElementBuilder() {
    var editedDescription: String = description ?: "%TIME%"
    var errorDescription: String = description ?: "Error! %TIME%"

    override fun build(chain: ConfigurationChain) = ConfigurationChain.ConfigurationChainElement(chain).apply {
        type = ConfigurationChain.ConfigurationChainElement.InteractionType.BUTTON
        startEmbedBuilder = {
            title = "Time"
            chain.options[key] = LocalDateTime.now(Clock.systemUTC()).toInstant(ZoneOffset.ofHours(0))?.epochSecond
                ?: System.currentTimeMillis()
            description = this@TimestampConfigurationBuilder.description?.replace("%TIME%", Instant.fromEpochSeconds(
                chain.options[key] as Long
            ).toMessageFormat(DiscordTimestampStyle.ShortDateTime))
        }
        startActionRowBuilder = listOf({
            interactionButton(ButtonStyle.Danger, "${chain.id}-2") {
                label = "- 5m"
            }
            interactionButton(ButtonStyle.Danger, "${chain.id}-3") {
                label = "- 1m"
            }
            interactionButton(ButtonStyle.Success, "${chain.id}-0") {
                emoji = Emojis.checkAnimated
            }
            interactionButton(ButtonStyle.Danger, "${chain.id}-4") {
                label = "+ 1m"
            }
            interactionButton(ButtonStyle.Danger, "${chain.id}-5") {
                label = "+ 5m"
            }
        }, {
            interactionButton(ButtonStyle.Danger, "${chain.id}-6") {
                label = "- 1h"
            }
            interactionButton(ButtonStyle.Danger, "${chain.id}-7") {
                label = "- 15m"
            }
            interactionButton(ButtonStyle.Success, "${chain.id}-1") {
                emoji = Emojis.checkAnimated
            }
            interactionButton(ButtonStyle.Danger, "${chain.id}-8") {
                label = "+ 15m"
            }
            interactionButton(ButtonStyle.Danger, "${chain.id}-9") {
                label = "+ 1h"
            }
        })

        validateButtonInteraction = validateButtonInteraction@{
            if (!this.componentId.startsWith(chain.id, ignoreCase = true)) return@validateButtonInteraction false
            when (this.componentId.last().digitToInt()) {
                in 0..1 -> {
                    this.acknowledgePublic().delete()
                    if ((chain.options[key] as Long) <= LocalDateTime.now(Clock.systemUTC())
                            .toInstant(ZoneOffset.ofHours(0)).epochSecond
                    ) {
                        edit {
                            title = "Error!"
                            description = this@TimestampConfigurationBuilder.errorDescription.replace("%TIME%", Instant.fromEpochSeconds(
                                chain.options[key] as Long
                            ).toMessageFormat(DiscordTimestampStyle.ShortDateTime))
                            color = Color(255, 0, 0)
                        }
                        return@validateButtonInteraction false
                    }
                    return@validateButtonInteraction true
                }
                2 -> {
                    chain.options[key] = chain.options[key] as Long - 5L * 60L
                }
                3 -> {
                    chain.options[key] = chain.options[key] as Long - 1L * 60L
                }
                4 -> {
                    chain.options[key] = chain.options[key] as Long + 1L * 60L
                }
                5 -> {
                    chain.options[key] = chain.options[key] as Long + 5L * 60L
                }
                6 -> {
                    chain.options[key] = chain.options[key] as Long - 60L * 60L
                }
                7 -> {
                    chain.options[key] = chain.options[key] as Long - 15L * 60L
                }
                8 -> {
                    chain.options[key] = chain.options[key] as Long + 15L * 60L
                }
                9 -> {
                    chain.options[key] = chain.options[key] as Long + 60L * 60L
                }
            }
            this.acknowledgeEphemeralDeferredMessageUpdate()
            edit {
                title = "Time"
                description = this@TimestampConfigurationBuilder.editedDescription.replace("%TIME%", Instant.fromEpochSeconds(
                    chain.options[key] as Long
                ).toMessageFormat(DiscordTimestampStyle.ShortDateTime))
                color = Color(120, 120, 120)
            }
            false
        }

        executeButtonInteraction = {
            edit(true) {
                title = "Success!"
                description =
                    "${Emojis.checkAnimated.asTextEmoji()} ${
                        Instant.fromEpochSeconds(chain.options[key] as Long)
                            .toMessageFormat(DiscordTimestampStyle.ShortDateTime)
                    } was selected!"
                color = Color(0, 255, 0)
            }
        }
    }
}

@KordPreview
class RoleConfigurationBuilder(private val count: IntRange = 1..1, private val successDescription: String? = null) : MenuConfigurationBuilder(count) {
    var options: Map<Snowflake, String>? = null
    override var choices: () -> MutableMap<String, String> = { mutableMapOf() }
    override var _successDescription = successDescription ?: "${Emojis.checkAnimated.asTextEmoji()} %VALUE% will be mentioned!"

    override fun build(chain: ConfigurationChain): ConfigurationChain.ConfigurationChainElement {
        element = ConfigurationChain.ConfigurationChainElement(chain).apply {
            choices = { options?.map {
                it.value to it.key.value.toString()
            }?.toMap()?.toMutableMap() ?: mutableMapOf() }
        }
        return super.build(chain)
    }
}

@OptIn(KordPreview::class)
class ChannelConfigurationBuilder(private val count: IntRange = 1..1, private val successDescription: String? = null) : MenuConfigurationBuilder(count) {
    var options: Map<Snowflake, String>? = null
    override var choices: () -> MutableMap<String, String> = { mutableMapOf() }
    override var _successDescription = successDescription ?: "${Emojis.checkAnimated.asTextEmoji()} %VALUE% will be mentioned!"

    override fun build(chain: ConfigurationChain): ConfigurationChain.ConfigurationChainElement {
        element = ConfigurationChain.ConfigurationChainElement(chain).apply {
            choices = { options?.map {
                it.value to it.key.value.toString()
            }?.toMap()?.toMutableMap() ?: mutableMapOf() }
        }
        return super.build(chain)
    }
}

@OptIn(KordPreview::class)
class StringConfigurationBuilder : ConfigurationChainElementBuilder() {
    var validation: suspend Message.() -> Boolean = { true }

    override fun build(chain: ConfigurationChain) = ConfigurationChain.ConfigurationChainElement(chain).apply {
        type = ConfigurationChain.ConfigurationChainElement.InteractionType.MESSAGE
        startEmbedBuilder = {
            title = this@StringConfigurationBuilder.title
            description = this@StringConfigurationBuilder.description
        }

        validateMessage = validation

        executeMessage = {
            chain.options[key] = this.content
            val message = this
            edit {
                title = "Success!"
                description =
                    "${Emojis.checkAnimated.asTextEmoji()} \"${message.content}\" was accepted."
                color = Color(0, 255, 0)
            }
        }
    }
}

@KordPreview
class IntConfigurationBuilder : ConfigurationChainElementBuilder() {
    var range: IntRange = -1000..1000

    override fun build(chain: ConfigurationChain) = ConfigurationChain.ConfigurationChainElement(chain).apply {
        type = ConfigurationChain.ConfigurationChainElement.InteractionType.MESSAGE
        startEmbedBuilder = {
            title = this@IntConfigurationBuilder.title
            description = this@IntConfigurationBuilder.description
        }

        validateMessage = {
            val matches = this.content.matches("\\d*".toRegex()) && (this.content.toIntOrNull() ?: -1) > 0 && this.content.toInt() in range
            val message = this
            if (!matches) {
                edit {
                    title = "Error!"
                    description =
                        "${Emojis.crossAnimated.asTextEmoji()} \"${message.content}\" is not a valid amount!"
                    color = Color(255, 0, 0)
                }
            }
            matches
        }

        executeMessage = {
            chain.options[key] = this.content.toInt()
            val message = this
            edit {
                title = "Success!"
                description =
                    "${Emojis.checkAnimated.asTextEmoji()} ${message.content} was selected!"
                color = Color(0, 255, 0)
            }
        }
    }
}

@KordPreview
open class MenuConfigurationBuilder(private val range: IntRange = 1..1) : ConfigurationChainElementBuilder() {
    open var choices: () -> MutableMap<String, String> = { mutableMapOf() }
    protected open var _successDescription = "${Emojis.checkAnimated.asTextEmoji()} %KEY% was selected!"
    protected open var snowflakeMessageFormattingCharacter = ""

    override fun build(chain: ConfigurationChain) = (element ?: ConfigurationChain.ConfigurationChainElement(chain)).apply {
        type = ConfigurationChain.ConfigurationChainElement.InteractionType.MENU
        startEmbedBuilder = {
            title = this@MenuConfigurationBuilder.title
            description = this@MenuConfigurationBuilder.description
        }
        startActionRowBuilder = listOf {
            selectMenu(chain.id) {
                allowedValues = range
                choices.invoke().onEach {
                    option(it.key, it.value) {}
                }
            }
        }

        validateMenuInteraction = {
            this.componentId == chain.id
        }

        executeMenuInteraction = {
            if (this.componentId == chain.id) {
                this.acknowledgePublic().delete()
                val choice = choices.invoke().filter { this.values.contains(it.value) }
                chain.options[key] = choice.values.map { Snowflake(it) }
                edit(true) {
                    title = "Success!"
                    description = _successDescription
                        .replace("%KEY%", choice.asIterable().first().key)
                        .replace("%VALUE%", choice.asIterable().first().value)
                        .replace("%KEYS%",
                            choice.asIterable()
                                .joinToString(separator = " ") { it.key })
                        .replace("%VALUES%",
                            choice.asIterable()
                                .joinToString(separator = "> <$snowflakeMessageFormattingCharacter", prefix = "<$snowflakeMessageFormattingCharacter", postfix = ">") { it.value })
                    color = Color(0, 255, 0)
                }
            }
        }
    }
}

@KordPreview
class MessageConfigurationBuilder(val message: suspend EphemeralInteractionResponseBehavior.() -> Unit) : ConfigurationChainElementBuilder() {

    override fun build(chain: ConfigurationChain) = ConfigurationChain.ConfigurationChainElement(chain).apply {
        type = ConfigurationChain.ConfigurationChainElement.InteractionType.NONE
        start = message
    }
}

@OptIn(KordPreview::class)
abstract class ConfigurationChainElementBuilder {
    var description: String? = null
    var title: String? = null
    var key: String = ""
    var element: ConfigurationChain.ConfigurationChainElement? = null

    abstract fun build(chain: ConfigurationChain): ConfigurationChain.ConfigurationChainElement
}
