package ua.`in`.smartjava.processor.anomaly

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput
import com.amazonaws.services.kinesis.model.Record
import org.apache.commons.logging.LogFactory
import ua.`in`.smartjava.data.Event
import ua.`in`.smartjava.writer.SensorSerializer

class AnomalyRecordProcessor : IRecordProcessor {

    private lateinit var kinesisShardId: String
    private var nextReportingTimeInMillis: Long = 0
    private var nextCheckpointTimeInMillis: Long = 0
    private val sensorSerializer = SensorSerializer()

    override fun initialize(initializationInput: InitializationInput) {
        val shardId = initializationInput.shardId
        LOGGER.info("Initializing record processor for shard: $shardId")
        this.kinesisShardId = shardId
        nextReportingTimeInMillis = System.currentTimeMillis() + REPORTING_INTERVAL_MILLIS
        nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS
    }

    override fun processRecords(processRecordsInput: ProcessRecordsInput) {
        processRecordsInput.records.forEach { processRecord(it) }
        // Checkpoint once every checkpoint interval
        if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
            checkpoint(processRecordsInput.checkpointer)
            nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS
        }
    }

    private fun processRecord(record: Record) {
        val event = sensorSerializer.fromJsonAsBytes(record.data.array())
        if (event == null) {
            LOGGER.warn("Skipping record. Unable to parse record into StockTrade. Partition Key: ${record.partitionKey}")
            return
        }
        LOGGER.info(event.toString())
        anomalyDetection(event, { event -> event.sensor.data.toInt() >= 39 })
    }

    private fun anomalyDetection(event: Event, predicate: (Event) -> Boolean) {
        if (event.let(predicate)) {
            LOGGER.error("""
            ***
            Anomaly detected for sensor ${event.eventId}
            ***
            """)
        }
    }

    override fun shutdown(shutdownInput: ShutdownInput) {
        LOGGER.info("Shutting down record processor for shard: $kinesisShardId !!!")
        if (shutdownInput.shutdownReason == ShutdownReason.TERMINATE) {
            checkpoint(shutdownInput.checkpointer)
        }
    }

    private fun checkpoint(checkpointer: IRecordProcessorCheckpointer) {
        LOGGER.info("Checkpointing shard $kinesisShardId !!!")
        try {
            checkpointer.checkpoint()
        } catch (se: ShutdownException) {
            // Ignore checkpoint if the processor instance has been shutdown (fail over).
            LOGGER.info("Caught shutdown exception, skipping checkpoint.", se)
        } catch (e: ThrottlingException) {
            // Skip checkpoint when throttled. In practice, consider a backoff and retry policy.
            LOGGER.error("Caught throttling exception, skipping checkpoint.", e)
        } catch (e: InvalidStateException) {
            // This indicates an issue with the DynamoDB table (check for table, provisioned IOPS).
            LOGGER.error("Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.", e)
        }
    }

    companion object {
        private val LOGGER = LogFactory.getLog(AnomalyRecordProcessor::class.java)
        // Reporting interval
        private val REPORTING_INTERVAL_MILLIS = 60000L // 1 minute
        // Checkpoint interval
        private val CHECKPOINT_INTERVAL_MILLIS = 60000L // 1 minute
    }

}
