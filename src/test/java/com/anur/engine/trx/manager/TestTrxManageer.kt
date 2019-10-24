package com.anur.engine.trx.manager

import java.lang.StringBuilder


/**
 * Created by Anur IjuoKaruKas on 2019/10/22
 */
fun main() {
    val currentTimeMillis = System.currentTimeMillis()

    for (i in 0 until 10000L) {
        TrxManager.acquireTrx(i)
    }
    println(TrxManager.minTrx())

    for (i in 0 until 9998L) {
        TrxManager.releaseTrx(i)
    }
    println(TrxManager.minTrx())
    println(TrxManager.minTrx() - (0))

    println(System.currentTimeMillis() - currentTimeMillis)
}


fun toBinaryStr(long: Long) {
//    println(toBinaryStrIter(long, 63, StringBuilder()).toString())
}

fun toBinaryStrIter(long: Long, index: Int, appender: StringBuilder): StringBuilder {
    if (index == -1) {
        return appender
    } else {
        var mask = 1L shl index
        if (mask and long == mask) {
            appender.append("1")
        } else {
            appender.append("0")
        }
        return toBinaryStrIter(long, index - 1, appender)
    }
}