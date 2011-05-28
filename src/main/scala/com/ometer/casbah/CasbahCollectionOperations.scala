package com.ometer.casbah

import com.ometer.mongo._
import com.ometer.bson._
import com.mongodb.casbah.MongoCollection
import org.bson.types.ObjectId

abstract class CasbahCollectionOperations[EntityType <: Product : Manifest, CaseClassIdType, BObjectIdType]
    extends CollectionOperations[EntityType, CaseClassIdType, BObjectIdType] {

    /** Implement this field in subclass to attach to a Casbah collection */
    protected val collection : MongoCollection

    /**
     * There probably isn't a reason to override this, but it would modify a query
     * as it went from the case class DAO to the BObject DAO.
     */
    protected val caseClassBObjectQueryComposer : QueryComposer[BObject, BObject] =
        new IdentityQueryComposer()

    /**
     * You would override this if you want to adjust how a BObject is mapped to a
     * case class entity. For example if you need to deal with missing fields or
     * database format changes, you could do that in this composer. Or if you
     * wanted to do a type mapping, say from Int to an enumeration, you could do that
     * here. Many things you might do with an annotation in something like JPA
     * could instead be done by subclassing CaseClassBObjectEntityComposer, in theory.
     */
    protected val caseClassBObjectEntityComposer : EntityComposer[EntityType, BObject] =
        new CaseClassBObjectEntityComposer[EntityType]

    /**
     * You would have to override this if your ID type changes between the case class
     * and BObject layers.
     */
    protected val caseClassBObjectIdComposer : IdComposer[CaseClassIdType, BObjectIdType]

    /* If this isn't lazy, then caseClassBObjectIdComposer is null, I guess because
     * the superclass is initialized prior to the base class.
     */
    private lazy val daoGroup =
        new CaseClassBObjectCasbahDAOGroup[EntityType, CaseClassIdType, BObjectIdType](collection,
            caseClassBObjectQueryComposer,
            caseClassBObjectEntityComposer,
            caseClassBObjectIdComposer)

    final override protected val manifestOfEntityType = manifest[EntityType]

    final override lazy val bobjectSyncDAO = daoGroup.bobjectSyncDAO
    final override lazy val caseClassSyncDAO = daoGroup.caseClassSyncDAO
}

abstract class CasbahCollectionOperationsWithDefaultId[EntityType <: Product : Manifest]
    extends CasbahCollectionOperations[EntityType, ObjectId, ObjectId] {
    override protected val caseClassBObjectIdComposer = new IdentityIdComposer[ObjectId]
}
