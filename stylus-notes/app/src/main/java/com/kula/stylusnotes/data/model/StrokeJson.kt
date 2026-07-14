package com.kula.stylusnotes.data.model

import com.kula.stylusnotes.core.model.StrokePoint
import org.json.JSONArray
import org.json.JSONObject

object StrokeJson {
    fun encodePoints(points: List<StrokePoint>): String {
        val array = JSONArray()
        for (p in points) {
            val obj = JSONObject()
            obj.put("x", p.x.toDouble())
            obj.put("y", p.y.toDouble())
            obj.put("pressure", p.pressure.toDouble())
            obj.put("tilt", p.tiltRadians.toDouble())
            obj.put("orientation", p.orientationRadians.toDouble())
            obj.put("t", p.timestampMs)
            array.put(obj)
        }
        return array.toString()
    }

    fun decodePoints(json: String): List<StrokePoint> {
        if (json.isBlank()) return emptyList()
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            StrokePoint(
                x = obj.getDouble("x").toFloat(),
                y = obj.getDouble("y").toFloat(),
                pressure = obj.getDouble("pressure").toFloat(),
                tiltRadians = obj.optDouble("tilt", 0.0).toFloat(),
                orientationRadians = obj.optDouble("orientation", 0.0).toFloat(),
                timestampMs = obj.getLong("t")
            )
        }
    }
}
