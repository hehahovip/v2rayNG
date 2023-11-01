package com.v2ray.ang.util

import android.util.Log
import com.v2ray.ang.AppConfig
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.math.BigInteger
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

    private const val downloadUrl = " http://103.84.110.38:3088/"
    const val DOWNLOAD_FILE_SUFFIX = "/nodes"

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

    fun doCipher(plainText: ByteArray): ByteArray? {
        // TODO: MAC地址标准为大写，由于测试文件使用小写mac地址，这里传入小写mac地址， 正式使用时，改用大写
        return doCipher(plainText, messMacAddr(getMacAddress().toLowerCase()), "D")
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
            Log.e("SIE", "Cipher failed: ", e)
        }
        return cipherText
    }

    fun downloadNodes(): ByteArray{
        val result =  URL(downloadUrl + generateDownloadID()).readText(Charset.defaultCharset()).toByteArray()
        return result
    }

    fun downloadToFile(path:String) : Boolean {
        try {
            val connection = URL(downloadUrl + generateDownloadID()).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val file = File(path + DOWNLOAD_FILE_SUFFIX)
            if(file.exists()) file.delete()
            connection.inputStream.buffered().copyTo(file.outputStream())
        } catch(e: Exception) {
            Log.e("SIE", e.message, e)
            return false
        }
        return true
    }

    fun generateDownloadID() : String {
        val mac = getMacAddress()
        var result = ""

        if(mac != null){
            val md = MessageDigest.getInstance("MD5")
            result = BigInteger(1, md.digest(mac.toByteArray())).toString(16).padStart(32, '0')
        }

        return result
    }

    fun readSoftAPName() : String {
        var apName = ""
        val FileName = "/data/misc/wifi/softap.conf"

        try {
//            apName = File(FileName).readText(Charsets.UTF_8).trim()
            var list = shellExec("cat $FileName");
            if(list.size >= 3) {
                apName = String(list[1].toByteArray())
                Log.e(AppConfig.ANG_PACKAGE, apName)
            }

        } catch (e : Exception) {
            Log.e("TAG", e.message.toString())
        }

        return apName.trim()
    }

    fun askRootPermission() {
        Runtime.getRuntime().exec("su -c echo")
    }

    fun shellExec(cmd : String) : List<String> {

        try {
            //Process中封装了返回的结果和执行错误的结果
//            val mProcess = mRuntime.exec(arrayOf("su -c", cmd))
            val mProcess = Runtime.getRuntime().exec("su -c $cmd")
            val mReader = BufferedReader(InputStreamReader(mProcess.inputStream, Charsets.UTF_8))
//            val mRespBuff = StringBuffer()
//            val buff = CharArray(1024)
//
//            var ch = 0
//            while (mReader.read(buff).also { ch = it } != -1) {
//                mRespBuff.append(buff, 0, ch)
//            }

            val list = mReader.readLines()

            mReader.close()
            return list
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.message?.let { Log.d(AppConfig.ANG_PACKAGE, it) }
        }
        return emptyList()
    }
}