package ua.`in`.smartjava.processor.anomaly

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;

class AnomalyRecordProcessorFactory : IRecordProcessorFactory {

    override fun createProcessor(): IRecordProcessor {
        return AnomalyRecordProcessor()
    }
}
