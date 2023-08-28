package com.v2ray.ang.util

import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL
import java.nio.charset.Charset
import java.security.Key
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Collections
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

object SIEUtils {

    val downloadUrl = "http://103.84.110.38:3088/2d73ede939df7d337b28f499db1d335c"

    fun messMacAddr(mac: String): String {
        if (mac.length != 12+5) {
            return ""
        } else {
            // 首尾排除
            val exclude = 2
            val stringList: List<String> = mac.split(":")
            val stringBuilder = java.lang.StringBuilder()
            val len = stringList.size - exclude
            stringBuilder.append(stringList[stringList.size - 1])
            for (i in len / exclude downTo 1) {
                stringBuilder.append(stringList[i])
                stringBuilder.append(stringList[i + exclude])
            }
            stringBuilder.append(stringList[0])
            return stringBuilder.toString()
        }

    }

    fun getMacAddress(): String {
        try {
            val all: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in all) {
                if (!networkInterface.name.equals("wlan0", true)) continue
                val macBytes = networkInterface.hardwareAddress ?: return ""
                val res1 = StringBuilder()
                for (b in macBytes) {
                    res1.append(String.format("%02X:", b))
                }
                if (res1.isNotEmpty()) {
                    res1.deleteCharAt(res1.length - 1)
                }
                return res1.toString()
            }
        } catch (ex: java.lang.Exception) {
            Log.e("TAG", "getMacAddress failed: ", ex)
        }
        return ""
    }

    fun doCipher(plainText: ByteArray, messedMAC: String, operation: String): ByteArray? {
        var cipherText: ByteArray? = null
        var des: Cipher
        var sRandom = SecureRandom()
        val md = MessageDigest.getInstance("SHA-256")
        try {
            var desSpecKey = DESKeySpec(md.digest(messedMAC.toByteArray()))
            var secretKeyFactory = SecretKeyFactory.getInstance("DES")
            des = Cipher.getInstance("DES/ECB/pkcs5padding")
            var key : Key? = secretKeyFactory.generateSecret(desSpecKey)

            if (operation == "E") {
                des.init(Cipher.ENCRYPT_MODE, key, sRandom)
                cipherText = des.doFinal(plainText)

            } else {
                des.init(Cipher.DECRYPT_MODE, key, sRandom)
                cipherText = des.doFinal(plainText)

            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cipherText
    }

    fun downloadNodes(): String{
        val result =  URL(downloadUrl).readText(Charset.defaultCharset())
        return result
    }

    fun downloadToFile(path:String) : Boolean {
        try {
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val file = File(path + "/nodes")
            if(file.exists()) file.delete()
            connection.inputStream.buffered().copyTo(file.outputStream())
        } catch(e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }
}