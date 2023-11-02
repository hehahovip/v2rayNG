package com.dd.sie.dto

import com.dd.sie.AppConfig.TAG_AGENT
import com.dd.sie.AppConfig.TAG_BLOCKED
import com.dd.sie.AppConfig.TAG_DIRECT
import com.dd.sie.util.Utils

data class ServerConfig(
    val configVersion: Int = 3,
    val configType: com.dd.sie.dto.EConfigType,
    var subscriptionId: String = "",
    val addedTime: Long = System.currentTimeMillis(),
    var remarks: String = "",
    val outboundBean: V2rayConfig.OutboundBean? = null,
    var fullConfig: V2rayConfig? = null
) {
    companion object {
        fun create(configType: com.dd.sie.dto.EConfigType): ServerConfig {
            when(configType) {
                com.dd.sie.dto.EConfigType.VMESS, com.dd.sie.dto.EConfigType.VLESS ->
                    return ServerConfig(
                            configType = configType,
                            outboundBean = V2rayConfig.OutboundBean(
                                    protocol = configType.name.lowercase(),
                                    settings = V2rayConfig.OutboundBean.OutSettingsBean(
                                            vnext = listOf(V2rayConfig.OutboundBean.OutSettingsBean.VnextBean(
                                                    users = listOf(V2rayConfig.OutboundBean.OutSettingsBean.VnextBean.UsersBean())))),
                                    streamSettings = V2rayConfig.OutboundBean.StreamSettingsBean()))
                com.dd.sie.dto.EConfigType.CUSTOM, com.dd.sie.dto.EConfigType.WIREGUARD ->
                    return ServerConfig(configType = configType)
                com.dd.sie.dto.EConfigType.SHADOWSOCKS, com.dd.sie.dto.EConfigType.SOCKS, com.dd.sie.dto.EConfigType.TROJAN ->
                    return ServerConfig(
                            configType = configType,
                            outboundBean = V2rayConfig.OutboundBean(
                                    protocol = configType.name.lowercase(),
                                    settings = V2rayConfig.OutboundBean.OutSettingsBean(
                                            servers = listOf(V2rayConfig.OutboundBean.OutSettingsBean.ServersBean())),
                                    streamSettings = V2rayConfig.OutboundBean.StreamSettingsBean()))
            }
        }
    }

    fun getProxyOutbound(): V2rayConfig.OutboundBean? {
        if (configType != com.dd.sie.dto.EConfigType.CUSTOM) {
            return outboundBean
        }
        return fullConfig?.getProxyOutbound()
    }

    fun getAllOutboundTags(): MutableList<String> {
        if (configType != com.dd.sie.dto.EConfigType.CUSTOM) {
            return mutableListOf(TAG_AGENT, TAG_DIRECT, TAG_BLOCKED)
        }
        fullConfig?.let { config ->
            return config.outbounds.map { it.tag }.toMutableList()
        }
        return mutableListOf()
    }

    fun getV2rayPointDomainAndPort(): String {
        val address = getProxyOutbound()?.getServerAddress().orEmpty()
        val port = getProxyOutbound()?.getServerPort()
        return if (Utils.isIpv6Address(address)) {
            String.format("[%s]:%s", address, port)
        } else {
            String.format("%s:%s", address, port)
        }
    }
}
