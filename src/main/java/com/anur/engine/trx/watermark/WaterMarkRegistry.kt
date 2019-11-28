package com.anur.engine.trx.watermark

import com.anur.exception.TransactionNotActivateException


/**
 * Created by Anur IjuoKaruKas on 2019/11/28
 *
 * 存放水位快照的地方，没别的用途
 *
 * TODO 超时销毁机制，用这个类来控制比如事务超时等等
 */
object WaterMarkRegistry {
    val registry = mutableMapOf<Long, WaterMarker>()

    fun register(trxId: Long, waterMarker: WaterMarker) {
        registry[trxId] = waterMarker
    }

    fun findOut(trxId: Long): WaterMarker {
        val waterMarker = registry[trxId]
        if (waterMarker == null) {
            throw TransactionNotActivateException()
        } else {
            return waterMarker
        }
    }
}