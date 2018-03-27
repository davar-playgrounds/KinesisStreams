package ua.`in`.smartjava.processor.metics

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput
import com.amazonaws.services.kinesis.model.Record
import org.apache.commons.logging.LogFactory
import org.influxdb.BatchOptions
import org.influxdb.InfluxDB
import org.influxdb.InfluxDBFactory
import org.influxdb.dto.BatchPoints
import org.influxdb.dto.Point
import ua.`in`.smartjava.data.Event
import ua.`in`.smartjava.utils.ConfigurationUtils
import ua.`in`.smartjava.writer.SensorSerializer
import java.util.concurrent.TimeUnit

class MetricsRecordProcessor : IRecordProcessor {

    private val influxDB: InfluxDB
    private val influxConfig: ConfigurationUtils.InfluxConfig

    constructor(influxConfig: ConfigurationUtils.InfluxConfig) {
        this.influxConfig = influxConfig
        influxDB = InfluxDBFactory.connect(influxConfig.databaseURL, influxConfig.username, influxConfig.password)
        influxDB.setLogLevel(InfluxDB.LogLevel.BASIC)
//        influxDB.createDatabase("dbdemo");
//        influxDB.createRetentionPolicy(
//                "defaultPolicy", "dbdemo", "30d", 1, true);
        influxDB.enableBatch(BatchOptions.DEFAULTS.actions(100).flushDuration(100))
        influxDB.setDatabase(influxConfig.dbName)
        influxDB.setRetentionPolicy(influxConfig.retentionPolicy)
        checkInflux()
    }

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
//        processRecordsInput.records.forEach { processRecord(it) }
        writeInBatch(processRecordsInput)

        // Checkpoint once every checkpoint interval
        if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
            checkpoint(processRecordsInput.checkpointer)
            nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS
        }
    }

    private fun writeInBatch(processRecordsInput: ProcessRecordsInput) {
        //Batching
        val batchPoints = BatchPoints
                .database(this.influxConfig.dbName)
                .tag("async", "true")
                .retentionPolicy(influxConfig.retentionPolicy)
                .consistency(InfluxDB.ConsistencyLevel.ALL)
                .build()

        processRecordsInput.records
                .map { record -> sensorSerializer.fromJsonAsBytes(record.data.array()) }
                .map { event ->
                    Point.measurement("cpu")
                            .time(event.sensor.localDateTime.time, TimeUnit.MILLISECONDS)
                            .addField("name", event.sensor.sensorId)
                            .addField("value", event.sensor.data.toFloat())
                            .build()
                }
                .forEach({ batchPoints.point(it) })

        influxDB.write(batchPoints)
    }

    private fun checkInflux() {
        val pong = influxDB.ping()
        LOGGER.info(pong)
    }

    private fun processRecord(record: Record) {
        val event = sensorSerializer.fromJsonAsBytes(record.data.array())
        if (event == null) {
            LOGGER.warn("Skipping record. Unable to parse record into StockTrade. Partition Key: ${record.partitionKey}")
            return
        }
        LOGGER.info(event.toString())
        writeToInflux(event)
    }

    private fun writeToInflux(event: Event) {
        val point: Point
                = Point.measurement("cpu")
                .time(event.sensor.localDateTime.time, TimeUnit.MILLISECONDS)
                .addField("name", event.sensor.sensorId)
                .addField("value", event.sensor.data.toFloat())
                .build()
        influxDB.write(point)
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
        private val LOGGER = LogFactory.getLog(MetricsRecordProcessor::class.java)
        // Reporting interval
        private val REPORTING_INTERVAL_MILLIS = 60000L // 1 minute
        // Checkpoint interval
        private val CHECKPOINT_INTERVAL_MILLIS = 60000L // 1 minute
    }

}