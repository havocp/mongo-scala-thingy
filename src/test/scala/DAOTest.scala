import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.MongoCollection
import com.ometer.bson.Implicits._
import com.ometer.bson._
import com.ometer.casbah._
import org.bson.types._
import org.junit.Assert._
import org.junit._
import play.test._

package foo {
    case class Foo(_id : ObjectId, intField : Int, stringField : String)

    object Foo extends CasbahCollectionOperationsWithDefaultId[Foo] {
        override protected lazy val collection : MongoCollection = {
            MongoUtil.collection("foo")
        }

        def customQuery[E : Manifest]() = {
            syncDAO[E].find(BObject("intField" -> 23))
        }
    }
}

class DAOTest extends UnitTest {
    import foo._

    @org.junit.Before
    def setup() {
        MongoUtil.collection("foo").remove(MongoDBObject())
    }

    @Test
    def testSaveAndFindOneCaseClass() {
        val foo = Foo(new ObjectId(), 23, "woohoo")
        Foo.caseClassSyncDAO.save(foo)
        val maybeFound = Foo.caseClassSyncDAO.findOneByID(foo._id)
        assertTrue(maybeFound.isDefined)
        assertEquals(foo, maybeFound.get)
    }

    @Test
    def testCustomQueryReturnsVariousEntityTypes() {
        val foo = Foo(new ObjectId(), 23, "woohoo")
        Foo.caseClassSyncDAO.save(foo)

        val objects = Foo.customQuery[BObject].toIndexedSeq
        assertEquals(1, objects.size)
        assertEquals(BInt32(23), objects(0).get("intField").get)
        assertEquals(BString("woohoo"), objects(0).get("stringField").get)

        val caseClasses = Foo.customQuery[Foo].toIndexedSeq
        assertEquals(1, caseClasses.size)
        val f = caseClasses(0)
        assertEquals(23, f.intField)
        assertEquals("woohoo", f.stringField)
    }
}
