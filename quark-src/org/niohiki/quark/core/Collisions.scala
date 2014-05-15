package org.niohiki.quark.core

sealed trait CollisionType

sealed trait InternalCollision extends CollisionType
sealed trait SideCollision extends CollisionType
sealed trait CornerCollision extends CollisionType
sealed trait TopCollision extends CollisionType
sealed trait BottomCollision extends CollisionType
sealed trait LeftCollision extends CollisionType
sealed trait RightCollision extends CollisionType

object CentralCollision extends InternalCollision

object TopCentralCollision extends SideCollision with TopCollision
object BottomCentralCollision extends SideCollision with BottomCollision
object LeftCentralCollision extends SideCollision with LeftCollision
object RightCentralCollision extends SideCollision with RightCollision

object TopRightCollision extends CornerCollision with TopCollision with RightCollision
object BottomRightCollision extends CornerCollision with BottomCollision with RightCollision
object TopLeftCollision extends CornerCollision with TopCollision with LeftCollision
object BottomLeftCollision extends CornerCollision with BottomCollision with LeftCollision

