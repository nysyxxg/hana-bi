package com.anur.engine.trx.manager

import com.anur.core.lock.rentrant.ReentrantReadWriteLocker
import com.anur.engine.trx.lock.TrxFreeQueuedSynchronizer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.function.Supplier


/**
 * Created by Anur IjuoKaruKas on 2019/10/15
 *
 * 事务管理器
 */
object TrxManager {

    private const val interval = 64L

    private val logger: Logger = LoggerFactory.getLogger(TrxFreeQueuedSynchronizer::class.java)

    private val locker = ReentrantReadWriteLocker()

    private var nowTrx: Long = -1L
//            Long.MIN_VALUE

    private val waterHolder = TreeMap<Long, TrxSegment>(kotlin.Comparator { o1, o2 -> o1.compareTo(o2) })

    /**
     * 申请一个递增的事务id
     */
    fun allocate(): Long {
        val trx = locker.writeLockSupplierCompel(Supplier {
            val trx = nowTrx + 1
            nowTrx = trx

            val index = trx / interval

            // 将事务扔进水位
            if (!waterHolder.contains(index)) waterHolder[index] = TrxSegment(trx)
            waterHolder[index]!!.acquire(trx)

            return@Supplier trx
        })

        return trx
    }

    /**
     * 释放一个事务
     */
    fun releaseTrx(anyElse: Long) {
        locker.writeLocker() {
            val index = anyElse / interval

            when (val trxSegment = waterHolder.get(index)) {
                null -> logger.error("重复释放事务？？？？？？？？？？？？？？？？？？？？？")
                else -> {
                    trxSegment.release(anyElse)
                    if (trxSegment.trxBitMap == 0L && waterHolder.higherEntry(index) != null) {
                        waterHolder.remove(index)
                    }
                }
            }
        }
    }

    /**
     * 获取的最小的有效的事务
     */
    fun minTrx(): Long {
        return locker.readLockSupplierCompel(Supplier {
            return@Supplier waterHolder.firstEntry()?.value?.minTrxId ?: nowTrx
        })
    }


    /**
     * 为了避免事务太多，列表太大，故采用分段
     */
    class TrxSegment(anyElse: Long) {

        @Volatile
        var trxBitMap: Long = 0

        var minTrxId = anyElse

        val start: Long = anyElse / interval

        fun acquire(trxId: Long) {
            val index = ((interval - 1) and trxId).toInt()
            val mask = 1L.shl(index)
            trxBitMap = trxBitMap or mask

            longToByteStr(trxBitMap)

            if (trxId < minTrxId) {
                minTrxId = trxId
            }
        }

        fun release(trxId: Long) {
            val index = ((interval - 1) and trxId).toInt()
            val mask = 1L.shl(index)
            trxBitMap = mask.inv() and trxBitMap

            longToByteStr(trxBitMap)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TrxSegment

            if (start != other.start) return false

            return true
        }

        override fun hashCode(): Int {
            return start.hashCode()
        }
    }
}

fun longToByteStr(l: Long) {
    val allZero = "000000000000000000000000000000000000000000000000000000000000000000000000"
    val toString = device2Then(l, StringBuilder()).toString()
    println(allZero.substring(toString.length) + toString)
}

fun device2Then(l: Long, sb: StringBuilder): StringBuilder {
    val result = l / 2
    val nogoru = l - result * 2

    if (result != 0L) {
        device2Then(result, sb)
    }

    sb.append(nogoru)
    return sb
}

fun main() {
    for (i in 0 until 65) {
        TrxManager.allocate()
    }
    println(TrxManager.minTrx())

    for (i in 0 until 65) {
        TrxManager.releaseTrx(i.toLong())
    }
    println(TrxManager.minTrx())
}