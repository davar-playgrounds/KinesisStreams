package ua.`in`.smartjava.utils

import com.amazonaws.ClientConfiguration
import com.amazonaws.ClientConfiguration.DEFAULT_USER_AGENT
import com.amazonaws.regions.Region
import com.amazonaws.regions.RegionUtils
import java.util.*

class ConfigurationUtils {

    private val APPLICATION_NAME = "amazon-kinesis-sensors"
    private val VERSION = "1.0.0"

    val clientConfigWithUserAgent: ClientConfiguration
        get() {
            val config = ClientConfiguration()
            val userAgent = "$DEFAULT_USER_AGENT $APPLICATION_NAME/$VERSION";
            config.userAgentPrefix = userAgent
            config.userAgentSuffix = null
            return config
        }

    data class AwsConfig(val appName: String, val streamName: String, val region: Region)

    val awsConfig: AwsConfig
        get() {
            val prop = Properties()
            javaClass.getResourceAsStream("/kinesis.properties").use { prop.load(it) }
            return AwsConfig(
                    prop.getProperty("applicationName"),
                    prop.getProperty("streamName"),
                    RegionUtils.getRegion(prop.getProperty("region")))
        }

    data class InfluxConfig(val databaseURL: String, val dbName: String, val username: String, val password: String, val retentionPolicy: String)

    private val prop = Properties()
    val influxConfig: InfluxConfig
        get() {
            javaClass.getResourceAsStream("/influx.properties").use { prop.load(it) }
            return InfluxConfig(
                    "http://${getEnvOrLoadProps("databaseHost")}:${getEnvOrLoadProps("databasePort")}",
                    getEnvOrLoadProps("dbName"),
                    getEnvOrLoadProps("username"),
                    getEnvOrLoadProps("password"),
                    getEnvOrLoadProps("retentionPolicy"))
        }

    private fun getEnvOrLoadProps(key: String): String {
        return System.getenv(key) ?: prop.getProperty(key)
    }

}
