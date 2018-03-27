package ua.`in`.smartjava.processor.metics

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import org.apache.commons.logging.LogFactory
import ua.`in`.smartjava.utils.ConfigurationUtils
import ua.`in`.smartjava.utils.CredentialUtils
import java.util.*

private val LOGGER = LogFactory.getLog(MetricsRecordProcessor::class.java)

fun main(args: Array<String>) {

    val credentialUtils = CredentialUtils()
    val configurationUtils = ConfigurationUtils()

    val (appName, streamName, region) = configurationUtils.awsConfig
    val credProvider = credentialUtils.credentialsProvider

    val workerId = UUID.randomUUID().toString()
    val kclConfig = KinesisClientLibConfiguration("$appName.Metrics", streamName, credProvider, workerId)
            .withRegionName(region.name)
            .withCommonClientConfig(configurationUtils.clientConfigWithUserAgent)

    val recordProcessorFactory = MetricsRecordProcessorFactory(configurationUtils.influxConfig)
    val worker = Worker.Builder()
            .recordProcessorFactory(recordProcessorFactory)
            .config(kclConfig)
            .build()

    var exitCode = 0

    try {
        worker.run()
    } catch (t: Throwable) {
        LOGGER.error("Caught throwable while processing data.", t)
        exitCode = 1
    }

    System.exit(exitCode)
}
