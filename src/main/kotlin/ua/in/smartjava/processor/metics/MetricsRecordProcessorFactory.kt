package ua.`in`.smartjava.processor.metics

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory
import ua.`in`.smartjava.utils.ConfigurationUtils.InfluxConfig

class MetricsRecordProcessorFactory(private val influxConfig: InfluxConfig) : IRecordProcessorFactory {

    override fun createProcessor(): IRecordProcessor {
        return MetricsRecordProcessor(influxConfig)
    }
}
