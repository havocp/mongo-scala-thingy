package com.ometer.mongo

import com.ometer.bson.BsonAST._
import com.mongodb.WriteResult

/**
 * A DAO parameterized to work with BObject
 */
abstract trait BObjectSyncDAO[IdType] extends SyncDAO[BObject, BObject, IdType] {

}

/** A BObject DAO that backends to another DAO */
abstract trait BObjectComposedSyncDAO[OuterIdType, InnerQueryType, InnerEntityType, InnerIdType]
    extends BObjectSyncDAO[OuterIdType]
    with ComposedSyncDAO[BObject, BObject, OuterIdType, InnerQueryType, InnerEntityType, InnerIdType] {
}
