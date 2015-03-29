/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package test.util.mongodb

import de.flapdoodle.embed.mongo.distribution.{Feature, IFeatureAwareVersion}

sealed trait MongoVersion extends IFeatureAwareVersion {
  def specificVersion: String

  def features: Set[Feature]

  def enabled(feature: Feature): Boolean = features.contains(feature)

  def asInDownloadPath(): String = specificVersion

  override def toString = s"MongoVersion($specificVersion)"
}

object MongoVersion {

  object V2_6_8 extends MongoVersion {
    lazy val specificVersion = "2.6.8"
    lazy val features = Set(Feature.SYNC_DELAY)

  }

  object V3_0_0 extends MongoVersion {
    lazy val specificVersion = "3.0.0"
    lazy val features = Set(Feature.SYNC_DELAY)
  }

}