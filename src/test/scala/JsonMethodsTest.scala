import com.mongodb.casbah.commons.MongoDBObject
import com.ometer.bson.Implicits._
import com.ometer.bson._
import com.ometer.casbah._
import com.ometer.mongo.JsonMethods
import com.ometer.ClassAnalysis
import org.bson.types._
import org.junit.Assert._
import org.junit._
import play.test._

package restdemo {
    case class Foo(_id : ObjectId, aString : String, anInt : Int)

    object Foo
        extends CasbahCollectionOperationsWithDefaultId[Foo]
        with JsonMethods[Foo] {
        override lazy val collection = MongoUtil.collection("restdemofoo")
        override val jsonAnalysis = new ClassAnalysis(classOf[Foo])
        override def jsonDAO = bobjectSyncDAO
        override def createQueryForAllObjects = BObject() // this would be dangerous in a non-test

        // This object inherits a complete DAO for BObject and for the Foo case class,
        // plus CRUD methods that accept/return JSON strings. In this file we're
        // testing the CRUD methods.
    }
}

class JsonMethodsTest extends UnitTest {
    import restdemo._

    @org.junit.Before
    def setup() {
        Foo.collection.remove(MongoDBObject())
    }

    @Test
    def putAndGetWorks() : Unit = {
        // create an object
        val createdJson = Foo.createJson("""{ "aString" : "hello", "anInt" : 76 }""")
        // parse what we created
        val bobjectCreated = Foo.parseJson(createdJson)
        assertEquals("hello", bobjectCreated.get("aString").get.unwrapped)
        assertEquals(76, bobjectCreated.get("anInt").get.unwrapped)
        val createdIdString = bobjectCreated.get("_id").get.unwrapped.toString

        // get the object
        val gotJsonOption = Foo.readJson(Some(createdIdString))
        assertTrue(gotJsonOption.isDefined)
        // parse what we got
        val bobjectGot = Foo.parseJson(gotJsonOption.get)
        assertEquals(createdIdString, bobjectGot.get("_id").get.unwrapped.toString)
        assertEquals(bobjectCreated, bobjectGot)
        assertEquals("hello", bobjectGot.get("aString").get.unwrapped)
        assertEquals(76, bobjectGot.get("anInt").get.unwrapped)

        // update the object with the ID in the path only, not in JSON
        val modifiedJson = Foo.updateJson(createdIdString, """{ "aString" : "hello world", "anInt" : 57 }""")
        val gotModifiedJsonOption = Foo.readJson(Some(createdIdString))
        val bobjectModified = Foo.parseJson(gotModifiedJsonOption.get)
        assertEquals(createdIdString, bobjectModified.get("_id").get.unwrapped.toString)
        assertEquals("hello world", bobjectModified.get("aString").get.unwrapped)
        assertEquals(57, bobjectModified.get("anInt").get.unwrapped)

        // update the object with redundant ID in the JSON
        val modifiedJson2 = Foo.updateJson(createdIdString, """{ "_id" : """" +
            createdIdString + """", "aString" : "hello world 2", "anInt" : 23 }""")
        val gotModifiedJsonOption2 = Foo.readJson(Some(createdIdString))
        val bobjectModified2 = Foo.parseJson(gotModifiedJsonOption2.get)
        assertEquals(createdIdString, bobjectModified2.get("_id").get.unwrapped.toString)
        assertEquals("hello world 2", bobjectModified2.get("aString").get.unwrapped)
        assertEquals(23, bobjectModified2.get("anInt").get.unwrapped)
    }

    @Test
    def deleteWorks() : Unit = {
        // create an object
        val createdJson = Foo.createJson("""{ "aString" : "hello", "anInt" : 76 }""")
        // parse what we created
        val bobjectCreated = Foo.parseJson(createdJson)
        assertEquals("hello", bobjectCreated.get("aString").get.unwrapped)
        assertEquals(76, bobjectCreated.get("anInt").get.unwrapped)
        val createdIdString = bobjectCreated.get("_id").get.unwrapped.toString

        // get the object
        val gotJsonOption = Foo.readJson(Some(createdIdString))
        assertTrue(gotJsonOption.isDefined)
        // parse what we got
        val bobjectGot = Foo.parseJson(gotJsonOption.get)
        assertEquals(createdIdString, bobjectGot.get("_id").get.unwrapped.toString)
        assertEquals(bobjectCreated, bobjectGot)
        assertEquals("hello", bobjectGot.get("aString").get.unwrapped)
        assertEquals(76, bobjectGot.get("anInt").get.unwrapped)

        // delete the object
        Foo.deleteJson(createdIdString)

        // fail to get the object
        val gotAfterDeleteJsonOption = Foo.readJson(Some(createdIdString))
        assertFalse("object is gone", gotAfterDeleteJsonOption.isDefined)
    }

    @Test
    def readAllObjectsWorks() : Unit = {
        // create some objects
        Foo.createJson("""{ "aString" : "hello", "anInt" : 76 }""")
        Foo.createJson("""{ "aString" : "hello2", "anInt" : 77 }""")
        Foo.createJson("""{ "aString" : "hello3", "anInt" : 78 }""")

        // read all
        val allJsonOption = Foo.readJson(None)
        assertTrue(allJsonOption.isDefined)
        val objects = Foo.parseJsonArray(allJsonOption.get)
        assertEquals(3, objects.size)
        val strings = objects.map(_ match {
            case obj : BObject => obj.get("aString").get.unwrapped.asInstanceOf[String]
            case _ => throw new Exception("not an object")
        })
        assertEquals(List("hello", "hello2", "hello3"), strings.sorted.toList)
    }
}
