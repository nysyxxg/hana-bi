package com.anur.engine.storage.core

import com.anur.engine.api.constant.StorageTypeConst

/**
 * Created by Anur IjuoKaruKas on 2019/10/11
 *
 * 存储在内存中的展现形式
 */
class HanabiEntry(var storageType: StorageTypeConst, var value: Any, var operateType: OperateType) {
    companion object {
        /**
         * DISABLE 代表这个值被删掉了
         */
        enum class OperateType(val b: Byte) {
            ENABLE(0),
            DISABLE(1)
        }
    }

    override fun toString(): String {
        return "HanabiEntry(StorageType=$storageType, value=$value, operateType=$operateType)"
    }
}