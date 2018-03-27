package ua.`in`.smartjava.processor.anomaly

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker

import org.apache.commons.logging.LogFactory
import ua.`in`.smartjava.utils.ConfigurationUtils
import ua.`in`.smartjava.utils.CredentialUtils
import java.util.*

private val LOGGER = LogFactory.getLog(AnomalyRecordProcessor::class.java)

fun main(args: Array<String>) {

    val credentialUtils = CredentialUtils()
    val configurationUtils = ConfigurationUtils()

    val (appName, streamName, region) = configurationUtils.awsConfig

    val credProvider = credentialUtils.credentialsProvider

    val workerId = UUID.randomUUID().toString()
    val kclConfig = KinesisClientLibConfiguration("$appName.Anomaly", streamName, credProvider, workerId)
            .withRegionName(region.name)
            .withCommonClientConfig(configurationUtils.clientConfigWithUserAgent)

    val recordProcessorFactory = AnomalyRecordProcessorFactory()
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
