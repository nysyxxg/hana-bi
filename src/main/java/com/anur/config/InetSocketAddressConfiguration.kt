package com.anur.config

import com.anur.core.util.ChannelManager
import com.anur.config.common.ConfigHelper
import com.anur.config.common.ConfigurationEnum
import com.anur.core.coordinate.model.HanabiNode
import com.anur.exception.ApplicationConfigException

/**
 * Created by Anur IjuoKaruKas on 2019/7/5
 *
 * 网络相关配置，都可以从这里获取
 */
object InetSocketAddressConfiguration : ConfigHelper() {

    private val me: HanabiNode

    init {
        val name = getConfig(ConfigurationEnum.SERVER_NAME) { unChange -> unChange } as String
        if (name == ChannelManager.CoordinateLeaderSign) {
            throw ApplicationConfigException(" 'Leader' 为关键词，节点不能命名为这个")
        }
        me = getNode(name)
    }

    fun getServerElectionPort(): Int {
        return me.electionPort
    }

    fun getServerCoordinatePort(): Int {
        return me.coordinatePort
    }

    fun getServerName(): String {
        return me.serverName
    }

    fun getCluster(): List<HanabiNode> {
        return getConfigSimilar(ConfigurationEnum.CLIENT_ADDR) { pair ->
            val serverName = pair.key
            val split = pair.value
                .split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            HanabiNode(serverName, split[0], Integer.valueOf(split[1]), Integer.valueOf(split[2]))
        } as List<HanabiNode>
    }

    fun getNode(serverName: String?): HanabiNode {
        return getCluster().associateBy { hanabiLegal: HanabiNode -> hanabiLegal.serverName }[serverName] ?: HanabiNode.NOT_EXIST
    }
}
