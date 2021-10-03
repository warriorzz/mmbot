package com.github.warriorzz.bot.music

import com.github.warriorzz.bot.model.MusicParty
import com.github.warriorzz.bot.model.updateMessage
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import io.github.warriorzz.ktify.Ktify
import io.github.warriorzz.ktify.model.Track
import io.ktor.http.*
import kotlinx.datetime.Clock

object MusicManager {
    val currentInstances: MutableMap<Snowflake, Ktify> = mutableMapOf()
    val currentMusicParties: MutableList<MusicParty> = mutableListOf()

    operator fun invoke(kord: Kord) {
        startMusicServer()
    }

    suspend fun followup(userId: Snowflake): Boolean {
        val party = currentMusicParties.first { it.listeners.contains(userId) }
        party.currentlyPlaying?.let {
            (Clock.System.now().toEpochMilliseconds() - party.currentTrackStartTime!!).let {
                if (it > 0) {
                    return currentInstances[userId]?.player?.seekToPosition(it.toInt()) == HttpStatusCode.NoContent
                }
            }
        }
        return false
    }

    suspend fun join(userId: Snowflake, queueId: String): Boolean {
        val party = currentMusicParties.firstOrNull { it.id == queueId } ?: return false
        party.listeners.add(party.listeners.size, userId)
        party.updateMessage()
        return false
    }

    fun queue(userId: Snowflake, track: Track) {
    }

    suspend fun skipToNext(party: MusicParty) {
        if (party.queue.isEmpty()) return
        party.currentlyPlaying = party.queue.first()
        party.queue.removeAt(0)
        party.listeners.onEach {
            currentInstances[it]?.player?.addItemToQueue(party.currentlyPlaying?.uri!!)
            currentInstances[it]?.player?.skipToNextTrack()
        }
    }

    fun leave(userId: Snowflake) {
        currentMusicParties.firstOrNull { it.listeners.contains(userId) }?.listeners?.remove(userId)
        currentInstances.remove(userId)
    }
}
