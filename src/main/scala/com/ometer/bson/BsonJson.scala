package com.ometer.bson

import BsonAST._
import java.io.Reader
import net.liftweb.{ json => lift }

private[bson] object BsonJson {
    private[this] def toLift(value : JValue) : lift.JValue = {
        value match {
            case v : JObject =>
                lift.JObject(v.map({ kv => lift.JField(kv._1, toLift(kv._2)) }).toList)
            case v : JArray =>
                lift.JArray(v.value.map({ elem => toLift(elem) }))
            case v : BBoolean =>
                lift.JBool(v.value)
            case v : BInt32 =>
                lift.JInt(v.value)
            case v : BInt64 =>
                lift.JInt(v.value)
            case v : BDouble =>
                lift.JDouble(v.value)
            case v : BString =>
                lift.JString(v.value)
            case BNull =>
                lift.JNull
        }
    }

    private[this] def fromLift(liftValue : lift.JValue) : JValue = {
        liftValue match {
            case lift.JObject(fields) =>
                JObject(fields.map({ field => (field.name, fromLift(field.value)) }))
            case lift.JArray(values) =>
                JArray(values.map(fromLift(_)))
            case lift.JField(name, value) =>
                throw new IllegalStateException("either JField was a toplevel from lift-json or this function is buggy")
            case lift.JInt(i) =>
                if (i.isValidInt) BInt32(i.intValue) else BInt64(i.longValue)
            case lift.JBool(b) =>
                BBoolean(b)
            case lift.JDouble(d) =>
                BDouble(d)
            case lift.JString(s) =>
                BString(s)
            case lift.JNull =>
                BNull
            case lift.JNothing =>
                throw new IllegalStateException("not sure what to do with lift's JNothing here, shouldn't happen")
        }
    }

    def toJson(value : BValue, flavor : JsonFlavor = JsonFlavor.CLEAN) : String = {
        require(value != null)
        lift.Printer.compact(lift.render(toLift(value.toJValue(flavor))))
    }

    def toPrettyJson(value : BValue, flavor : JsonFlavor = JsonFlavor.CLEAN) : String = {
        require(value != null)
        lift.Printer.pretty(lift.render(toLift(value.toJValue(flavor))))
    }

    def fromJson(json : String, flavor : JsonFlavor = JsonFlavor.CLEAN) : JValue = {
        require(flavor == JsonFlavor.CLEAN) // other flavors not supported for now
        fromLift(lift.parse(json))
    }

    def fromJson(json : Reader) : JValue = fromJson(json, JsonFlavor.CLEAN)

    def fromJson(json : Reader, flavor : JsonFlavor) : JValue = {
        require(flavor == JsonFlavor.CLEAN) // other flavors not supported for now
        fromLift(lift.JsonParser.parse(json))
    }
}
