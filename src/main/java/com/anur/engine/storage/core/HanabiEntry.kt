package com.anur.engine.storage.core

import com.anur.engine.api.common.constant.StorageTypeConst

/**
 * Created by Anur IjuoKaruKas on 2019/10/11
 *
 * 存储在内存中的展现形式
 */
class HanabiEntry(var StorageType: StorageTypeConst, var value: Any, var operateType: OperateType) {
    companion object {
        enum class OperateType(val b: Byte) {
            INSERT(1),
            UPDATE(2),
            DELETE(3),
        }
    }
}