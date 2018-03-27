package ua.`in`.smartjava.writer

import com.amazonaws.AmazonClientException
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
import com.amazonaws.services.kinesis.model.PutRecordRequest
import com.amazonaws.services.kinesis.model.ResourceNotFoundException
import org.apache.commons.logging.LogFactory
import ua.`in`.smartjava.data.Event
import ua.`in`.smartjava.utils.ConfigurationUtils
import ua.`in`.smartjava.utils.CredentialUtils
import java.nio.ByteBuffer

private val LOG = LogFactory.getLog(SensorEventWriter::class.java)

fun main(args: Array<String>) {
    val sensorEventWriter = SensorEventWriter()

    // Validate that the stream exists and is active
    sensorEventWriter.validateStream()

    val sensorGenerator = SensorGenerator()

    while (true) {
        val sensorEvent = sensorGenerator.randomSensorEvent
        sensorEventWriter.sendSensorEventData(sensorEvent)
        Thread.sleep(500)
    }
}

class SensorEventWriter {

    private val sensorSerializer = SensorSerializer()
    private val clientBuilder = AmazonKinesisClientBuilder.standard()
    private val credentialUtils = CredentialUtils()
    private val configurationUtils = ConfigurationUtils()
    private val streamName = configurationUtils.awsConfig.streamName

    init {
        clientBuilder.region = configurationUtils.awsConfig.region.name
        clientBuilder.credentials = credentialUtils.credentialsProvider
        clientBuilder.clientConfiguration = configurationUtils.clientConfigWithUserAgent
    }

    private val kinesisClient = clientBuilder.build()

    fun streamDetails() {
        val result = kinesisClient.describeStream(streamName)
        LOG.info(result.streamDescription)
    }

    fun validateStream() {
        try {
            val describeStreamResult = kinesisClient.describeStream(streamName)
            if ("ACTIVE" != describeStreamResult.streamDescription.streamStatus) {
                LOG.error("Stream $streamName is not active. Please wait a few moments and try again.")
                System.exit(1)
            }
        } catch (e: ResourceNotFoundException) {
            LOG.error("Stream $streamName does not exist. Please create it in the console.")
            LOG.error(e)
            System.exit(1)
        } catch (e: Exception) {
            LOG.error("Error found while describing the stream $streamName")
            LOG.error(e)
            System.exit(1)
        }
    }

//    TODO implements this method check for performance in batch
//    fun sendSensorDateInBatch() {
//        PutRecordsRequest
//    }

    fun sendSensorEventData(event: Event) {
        val bytes = sensorSerializer.toJsonAsBytes(event)
        // The bytes could be null if there is an issue with the JSON serialization by the Jackson JSON library.
        if (bytes == null) {
            LOG.warn("Could not get JSON bytes for stock sensor")
            return
        }

        LOG.info("Putting event: ${event}")
        val putRecord = PutRecordRequest()
        putRecord.streamName = streamName

        putRecord.partitionKey = event.sensor.sensorId
        putRecord.data = ByteBuffer.wrap(bytes)

        try {
            kinesisClient.putRecord(putRecord)
        } catch (ex: AmazonClientException) {
            LOG.warn("Error sending record to Amazon Kinesis.", ex)
        }

    }
}
