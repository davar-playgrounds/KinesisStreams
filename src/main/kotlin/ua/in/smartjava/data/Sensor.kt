package ua.`in`.smartjava.data

import java.sql.Timestamp

data class Sensor(
        val sensorId: String,
        val data: String,
        val localDateTime: Timestamp
)
