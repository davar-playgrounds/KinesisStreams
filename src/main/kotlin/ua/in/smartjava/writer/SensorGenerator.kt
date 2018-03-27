package ua.`in`.smartjava.writer

import ua.`in`.smartjava.data.Event
import ua.`in`.smartjava.data.Sensor
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class SensorGenerator {

    private val random = Random()
    private val id = AtomicLong()

    val randomSensorEvent: Event
        get() {
            val sensor = Sensor(
                    SENSORS_IDS[random.nextInt(SENSORS_IDS.size)],
                    ((5 + random.nextInt(35)).toString()),
                    Timestamp.valueOf(LocalDateTime.now()))
            return Event(id.incrementAndGet(), sensor)
        }

    companion object {

        private val SENSORS_IDS = ArrayList<String>()

        init {
            SENSORS_IDS.add(UUID.randomUUID().toString())
            SENSORS_IDS.add(UUID.randomUUID().toString())
            SENSORS_IDS.add(UUID.randomUUID().toString())
            SENSORS_IDS.add(UUID.randomUUID().toString())
        }

    }
}
