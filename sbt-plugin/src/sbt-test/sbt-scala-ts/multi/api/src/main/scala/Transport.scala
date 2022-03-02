package io.github.scalats.sbttest

import common.NamedFeature

sealed trait Transport {
  def name: String
}

case class TrainLine(
    name: String,
    startStationId: String,
    endStationId: String,
    feature: NamedFeature)
    extends Transport

case class BusLine(
    id: Int,
    name: String,
    stopIds: Seq[String])
    extends Transport
