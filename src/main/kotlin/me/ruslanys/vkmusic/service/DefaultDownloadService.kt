package me.ruslanys.vkmusic.service

import me.ruslanys.vkmusic.component.VkClient
import me.ruslanys.vkmusic.domain.Audio
import me.ruslanys.vkmusic.event.DownloadFailEvent
import me.ruslanys.vkmusic.event.DownloadInProgressEvent
import me.ruslanys.vkmusic.event.DownloadSuccessEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executor

@Service
class DefaultDownloadService(
        private val vkClient: VkClient,
        private val publisher: ApplicationEventPublisher,
        @Qualifier("downloadExecutor") private val executor: Executor
) : DownloadService {

    @Async
    override fun download(destination: File, audio: Audio) {
        download(destination, listOf(audio))
    }

    @Async
    override fun download(destination: File, audioList: List<Audio>) {
        log.info("Download [{}]", audioList.joinToString { it.id.toString() })
        vkClient.fetchUrls(audioList)

        if (!destination.exists() || !destination.isDirectory) {
            throw IllegalArgumentException("Destination path is incorrect.")
        }

        for (audio in audioList) {
            executor.execute {
                downloadFile(destination, audio)
            }
        }
    }

    private fun downloadFile(destination: File, audio: Audio) {
        log.info("Download file {}", audio.url)
        publisher.publishEvent(DownloadInProgressEvent(this, audio))

        try {
            val connection = URL(audio.url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val file = File(destination, audio.filename())
            BufferedInputStream(connection.inputStream, 5120).use { input ->
                BufferedOutputStream(FileOutputStream(file), 5120).use { output ->
                    val buff = ByteArray(5120)
                    var len: Int

                    do {
                        len = input.read(buff)
                        if (len > 0) output.write(buff, 0, len)
                    } while (len != -1)
                }
            }

            publisher.publishEvent(DownloadSuccessEvent(this, audio, file))
        } catch (e: Exception) {
            publisher.publishEvent(DownloadFailEvent(this, audio, e))
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DefaultDownloadService::class.java)
    }

}

fun Audio.filename(): String {
    val regex = "[!\"#$%&'()*+,\\-/:;<=>?@\\[\\]^_`{|}~]".toRegex()
    val formattedArtist = artist.trim().replace(regex, "")
    val formattedTitle = title.trim().replace(regex, "")

    return "${formattedArtist.take(10)} - ${formattedTitle.take(20)}.mp3"
}