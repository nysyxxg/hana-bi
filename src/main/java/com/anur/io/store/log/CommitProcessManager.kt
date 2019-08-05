package com.anur.io.store.log

import com.anur.config.LogConfigHelper
import com.anur.core.elect.model.GenerationAndOffset
import com.anur.core.lock.ReentrantReadWriteLocker
import com.anur.io.store.prelog.ByteBufPreLogManager
import org.slf4j.LoggerFactory
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Created by Anur IjuoKaruKas on 2019/7/15
 */
object CommitProcessManager : ReentrantReadWriteLocker() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val dir = File(LogConfigHelper.getBaseDir()!!)

    private val offsetFile = File(LogConfigHelper.getBaseDir(), "commitOffset.temp")

    private val mmap: MappedByteBuffer

    private var commitGAO: GenerationAndOffset? = null

    init {
        dir.mkdir()
        offsetFile.createNewFile()
        val raf = RandomAccessFile(offsetFile, "rw")
        raf.setLength((8 + 8).toLong())

        mmap = raf.channel.map(FileChannel.MapMode.READ_WRITE, 0, (8 + 8).toLong())

        load()
        discardInvalidMsg()
    }

    fun discardInvalidMsg() {
        if (commitGAO != null && commitGAO != GenerationAndOffset.INVALID) {
            logger.info("检测到本节点曾是 leader 节点，需摒弃部分未 Commit 的消息")
            LogManager.discardAfter(commitGAO!!)
            ByteBufPreLogManager.cover(commitGAO!!)
            cover(GenerationAndOffset.INVALID)
        }
    }


    fun load(): GenerationAndOffset {
        writeLocker {
            if (commitGAO == null) {
                val gen = mmap.long
                val offset = mmap.long
                commitGAO = GenerationAndOffset(gen, offset)
                mmap.rewind()
            }
        }
        return commitGAO!!
    }

    fun cover(GAO: GenerationAndOffset) {
        writeLocker {
            mmap.putLong(GAO.generation)
            mmap.putLong(GAO.offset)
            mmap.rewind()
            mmap.force()
            commitGAO = null
        }
    }
}