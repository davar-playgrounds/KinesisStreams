package ua.`in`.smartjava.writer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import ua.`in`.smartjava.data.Event

class SensorSerializer {
    private val JSON: ObjectMapper = ObjectMapper().registerModule(KotlinModule())

    init {
        JSON.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun toJsonAsBytes(event: Event): ByteArray {
        return JSON.writeValueAsBytes(event)
    }

    fun fromJsonAsBytes(bytes: ByteArray): Event {
        return JSON.readValue(bytes, Event::class.java)
    }
}
