package org.niohiki.quark.util

import org.niohiki.quark.core.Spatial

trait SpatialFactory {
  def create(x: Double, y: Double, params: String): Spatial
}