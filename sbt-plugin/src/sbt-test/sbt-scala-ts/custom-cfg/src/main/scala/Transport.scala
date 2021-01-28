package io.github.scalats.sbttest

sealed trait Transport {
  def name: String
}

case class TrainLine(
    name: String,
    startStationId: String,
    endStationId: String,
    serviceDays: Set[WeekDay.Value])
    extends Transport

case class BusLine(
    id: Int,
    name: String,
    stopIds: Seq[String])
    extends Transport
