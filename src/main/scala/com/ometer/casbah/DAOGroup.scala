package com.ometer.casbah

import com.ometer.mongo._
import com.ometer.ClassAnalysis
import com.ometer.bson.BsonAST.BObject
import com.mongodb.DBObject
import com.mongodb.casbah.MongoCollection
import org.bson.types.ObjectId

/**
 * A DAOGroup exposes the entire chain of DAO conversions; you can "tap in" and use the
 * nicest DAO at whatever level is convenient for whatever you're doing. You can also
 * override any of the conversions as the data makes its way up from MongoDB.
 *
 * The long-term idea is to get rid of the Casbah part, and the DAOGroup will
 * have a 2x2 of DAO flavors: sync vs. async, and BObject vs. case entity.
 *
 * Rather than a bunch of annotations specifying how to go from MongoDB to
 * the case entity, there's a theory here that you can override the
 * "composer" objects and do things like validation or dealing with legacy
 * object formats in there.
 */
abstract class CaseClassBObjectCasbahDAOGroup[EntityType <: Product : Manifest, CaseClassIdType, BObjectIdType] {
    /* this is not a type parameter because we don't want people to transform ID
     * type between BObject and Casbah; transformations should be done on
     * the case-class-to-bobject layer because we want to keep that layer.
     */
    final private type CasbahIdType = BObjectIdType

    /** Implement this field in subclass to attach the DAOGroup to a collection */
    protected val collection : MongoCollection

    /* Let's not allow changing the BObject-to-Casbah mapping since we want to
     * get rid of Casbah's DBObject. That's why these are private.
     */
    private lazy val bobjectCasbahQueryComposer : QueryComposer[BObject, DBObject] =
        new BObjectCasbahQueryComposer()
    private lazy val bobjectCasbahEntityComposer : EntityComposer[BObject, DBObject] =
        new BObjectCasbahEntityComposer()
    private lazy val bobjectCasbahIdComposer : IdComposer[BObjectIdType, CasbahIdType] =
        new IdentityIdComposer()

    /**
     * There probably isn't a reason to override this, but it would modify a query
     * as it went from the case class DAO to the BObject DAO.
     */
    protected lazy val caseClassBObjectQueryComposer : QueryComposer[BObject, BObject] =
        new IdentityQueryComposer()

    /**
     * You would override this if you want to adjust how a BObject is mapped to a
     * case class entity. For example if you need to deal with missing fields or
     * database format changes, you could do that in this composer. Or if you
     * wanted to do a type mapping, say from Int to an enumeration, you could do that
     * here. Many things you might do with an annotation in something like JPA
     * could instead be done by subclassing CaseClassBObjectEntityComposer, in theory.
     */
    protected lazy val caseClassBObjectEntityComposer : EntityComposer[EntityType, BObject] =
        new CaseClassBObjectEntityComposer[EntityType]()

    /**
     * You would have to override this if your ID type changes between the case class
     * and BObject layers.
     */
    protected val caseClassBObjectIdComposer : IdComposer[CaseClassIdType, BObjectIdType]

    /**
     *  This is the "raw" Casbah DAO, if you need to work with a DBObject for some reason.
     *  This is best avoided because the hope is that Hammersmith would allow us to
     *  eliminate this layer. In fact, we'll make this private...
     */
    private lazy val casbahSyncDAO : CasbahSyncDAO[CasbahIdType] = {
        val outerCollection = collection
        new CasbahSyncDAO[CasbahIdType] {
            override val collection = outerCollection
        }
    }

    /**
     *  This DAO works with a traversable immutable BSON tree (BObject), which is probably
     *  the best representation if you want to convert to JSON. You can also use
     *  the unwrappedAsJava field on BObject to get a Java map, which may work
     *  well with your HTML template system. This is intended to be the "raw"
     *  format that we'd build off the wire using Hammersmith, rather than DBObject,
     *  because it's easier to work with and immutable.
     */
    lazy val bobjectSyncDAO : BObjectSyncDAO[BObjectIdType] = {
        new BObjectCasbahSyncDAO[BObjectIdType, CasbahIdType] {
            override val backend = casbahSyncDAO
            override val queryComposer = bobjectCasbahQueryComposer
            override val entityComposer = bobjectCasbahEntityComposer
            override val idComposer = bobjectCasbahIdComposer
        }
    }

    /**
     *  This DAO works with a specified case class, for typesafe access to fields
     *  from within Scala code. You also know that all the fields are present
     *  if the case class was successfully constructed.
     */
    lazy val caseClassSyncDAO : CaseClassSyncDAO[BObject, EntityType, CaseClassIdType] = {
        new CaseClassBObjectSyncDAO[EntityType, CaseClassIdType, BObjectIdType] {
            override val backend = bobjectSyncDAO
            override val queryComposer = caseClassBObjectQueryComposer
            override val entityComposer = caseClassBObjectEntityComposer
            override val idComposer = caseClassBObjectIdComposer
        }
    }
}

/**
 * A case-class-on-bobject-on-casbah DAO group with no identity transformations, using ObjectId for ids.
 */
abstract class DefaultCaseClassBObjectCasbahDAOGroup[EntityType <: Product : Manifest]
    extends CaseClassBObjectCasbahDAOGroup[EntityType, ObjectId, ObjectId] {
    override val caseClassBObjectIdComposer : IdComposer[ObjectId, ObjectId] = new IdentityIdComposer()

    /**
     * This lets you write a function that generically works for either the case class or
     * BObject results.
     */
    def syncDAO[E : Manifest] : SyncDAO[BObject, E, ObjectId] = {
        manifest[E] match {
            case m if m == manifest[BObject] =>
                bobjectSyncDAO.asInstanceOf[SyncDAO[BObject, E, ObjectId]]
            case m if m == manifest[EntityType] =>
                caseClassSyncDAO.asInstanceOf[SyncDAO[BObject, E, ObjectId]]
            case _ =>
                throw new IllegalArgumentException("Missing type param on syncDAO[T]? add the [T]? No DAO returns type: " + manifest[E])
        }
    }
}

/**
 * A DAO group that adds yet another layer, an "application object" that need not be
 * a case class. You have to manually implement conversions of this object to/from
 * the case class.
 */
abstract class AppObjectDAOGroup[AppObjectType, CaseEntityType <: Product : Manifest, CaseClassIdType, BObjectIdType]
    extends CaseClassBObjectCasbahDAOGroup[CaseEntityType, CaseClassIdType, BObjectIdType] {

    // we don't allow overriding this for now
    final private type AppObjectIdType = CaseClassIdType

    protected val appObjectCaseClassEntityComposer : EntityComposer[AppObjectType, CaseEntityType]

    lazy val appObjectSyncDAO : SyncDAO[BObject, AppObjectType, CaseClassIdType] = {
        new ComposedSyncDAO[BObject, AppObjectType, AppObjectIdType, BObject, CaseEntityType, CaseClassIdType] {
            override val backend = caseClassSyncDAO
            override val queryComposer = new IdentityQueryComposer[BObject]
            override val entityComposer = appObjectCaseClassEntityComposer
            override val idComposer = new IdentityIdComposer[AppObjectIdType]
        }
    }
}

/**
 * An app object DAO group with no identity transformations, using ObjectId for ids.
 */
abstract class DefaultAppObjectDAOGroup[AppObjectType, CaseEntityType <: Product : Manifest]
    extends AppObjectDAOGroup[AppObjectType, CaseEntityType, ObjectId, ObjectId] {
    override val caseClassBObjectIdComposer : IdComposer[ObjectId, ObjectId] = new IdentityIdComposer()
}
