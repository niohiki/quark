package org.niohiki.quark.util

trait Updater {
  private var factor: Double = 1
  private var time = System.nanoTime
  private var delta: Double = 0
  def tick(max: Double) {
    val nextTime = System.nanoTime
    delta = (nextTime - time) * 1.0e-9
    if (delta > max) delta = max
    time = nextTime
  }
  def setLocalTimeFactor(f: Double) = factor = f
  def resetLocalTimeFactor = factor = 1
  def getDelta = Updater.factor * factor * delta
}
object Updater {
  private var factor: Double = 1
  def setGlobalTimeFactor(f: Double) = factor = f
  def resetGlobalTimeFactor = factor = 1
}