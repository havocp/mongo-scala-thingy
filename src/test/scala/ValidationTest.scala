import com.ometer.bson.Implicits._
import com.ometer.bson._
import com.ometer.ClassAnalysis
import org.bson.types.{ BSONTimestamp, ObjectId, Binary }
import org.joda.time.{ DateTimeZone, DateTime }
import org.junit.Assert._
import org.junit._
import play.test._

package bar {
    case class LotsOfTypes(anInt : Int,
        aLong : Long,
        aDouble : Double,
        aBoolean : Boolean,
        aString : String,
        aDateTime : DateTime,
        aTimestamp : BSONTimestamp,
        anObjectId : ObjectId,
        aBinary : Binary,
        aMap : Map[String, Int],
        aSeq : List[Int]);

    case class RecursiveCollections(aMapWithLists : Map[String, List[Int]],
        aListWithMaps : List[Map[String, Int]],
        aListOfListOfList : List[List[List[Int]]])
}

class ValidationTest extends UnitTest {
    import bar._

    @org.junit.Before
    def setup() {
    }

    private val someDateTime = new DateTime(1996, 7, 10, 16, 50, 00, 00, DateTimeZone.UTC)

    private def lotsOfTypesAsBObject() = {
        BObject("anInt" -> 42,
            "aLong" -> BInt64(43),
            "aDouble" -> 3.14,
            "aBoolean" -> true,
            "aString" -> "lazy dog",
            "aDateTime" -> someDateTime,
            "aTimestamp" -> new BSONTimestamp((someDateTime.getMillis / 1000).toInt, 1),
            "anObjectId" -> new ObjectId("4dbf8ea93364e3bd9745723c"),
            "aBinary" -> new Binary(BsonSubtype.GENERAL.code, new Array[Byte](10)),
            "aMap" -> Map[String, Int]("a" -> 20, "b" -> 21),
            "aSeq" -> List(1, 2, 3, 4))
    }

    @Test
    def validateSuccessfullyLotsOfTypes() : Unit = {
        val bobj = lotsOfTypesAsBObject()
        val asJsonString = """
{
  "anInt":42,
  "aLong":43,
  "aDouble":3.14,
  "aBoolean":true,
  "aString":"lazy dog",
  "aDateTime":837017400000,
  "aTimestamp":837017400001,
  "anObjectId":"4dbf8ea93364e3bd9745723c",
  "aBinary":"AAAAAAAAAAAAAA==\r\n",
  "aMap":{
    "a":20,
    "b":21
  },
  "aSeq":[1,2,3,4]
}
"""
        val analysis = new ClassAnalysis(classOf[LotsOfTypes])
        val result = BValue.parseJson(asJsonString, analysis)

        assertTrue(result.isInstanceOf[BObject])
        // item-by-item equality check is easier to debug; relies
        // on bobject keeping its sort order of course
        for ((expected, found) <- bobj zip result.asInstanceOf[BObject]) {
            assertEquals(expected, found)
        }
        assertEquals(bobj, result)
    }

    @Test
    def validateRecursiveCollections() : Unit = {
        val analysis = new ClassAnalysis(classOf[RecursiveCollections])
        val validObj = BObject("aMapWithLists" -> BObject("a" -> BArray(List(1, 2)), "b" -> BArray(List(3, 4))),
            "aListWithMaps" -> BArray(BObject("a" -> 1, "b" -> 2), BObject("c" -> 3, "d" -> 4)),
            "aListOfListOfList" -> BArray(BArray(BArray(1, 2, 3), BArray(4, 5, 6)),
                BArray(BArray(BArray(7, 8, 9)), BArray(BArray(10, 11, 12))),
                BArray()))
        val result = BValue.parseJson(validObj.toJson(), analysis)
        assertEquals(validObj, result)
    }

    @Test
    def failToValidateWithMissingField() : Unit = {
        val bobj = lotsOfTypesAsBObject()
        val analysis = new ClassAnalysis(classOf[LotsOfTypes])
        var failure : JsonValidationException = null
        try {
            val withMissingField = bobj.toJValue() - "anInt"
            val result = BValue.fromJValue(withMissingField, analysis)
        } catch {
            case e : JsonValidationException =>
                failure = e
        }
        assertNotNull("got validation exception", failure)
        assertTrue("proper exception message", failure.getMessage().contains("should be in the parsed object"))
        assertTrue("exception message contains field name", failure.getMessage().contains("anInt"))
    }

    @Test
    def failToValidateWithBadFieldType() : Unit = {
        val bobj = lotsOfTypesAsBObject()
        val analysis = new ClassAnalysis(classOf[LotsOfTypes])
        var failure : JsonValidationException = null
        try {
            val withBadField = bobj.toJValue() + ("anInt" -> BString("foo"))
            val result = BValue.fromJValue(withBadField, analysis)
        } catch {
            case e : JsonValidationException =>
                failure = e
        }
        assertNotNull("got validation exception", failure)
        assertTrue("proper exception message", failure.getMessage().contains("Expecting"))
        assertTrue("exception message contains field name", failure.getMessage().contains("anInt"))
    }

    @Test
    def failToValidateWithNullField() : Unit = {
        val bobj = lotsOfTypesAsBObject()
        val analysis = new ClassAnalysis(classOf[LotsOfTypes])
        var failure : JsonValidationException = null
        try {
            val withNullField = bobj.toJValue() + ("anInt" -> BNull)
            val result = BValue.fromJValue(withNullField, analysis)
        } catch {
            case e : JsonValidationException =>
                failure = e
        }
        assertNotNull("got validation exception", failure)
        assertTrue("proper exception message", failure.getMessage().contains("Expecting"))
        assertTrue("exception message contains field name", failure.getMessage().contains("anInt"))
    }
}
