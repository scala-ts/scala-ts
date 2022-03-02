package io.github.scalats.sbttest

import java.time._

case class CaseClassFoo(
    id: java.util.UUID,
    name: String,
    i: Int,
    flag: Byte,
    score: Short,
    time: Long,
    localDate: LocalDate,
    instant: Instant,
    localDateTime: LocalDateTime,
    offsetDateTime: OffsetDateTime,
    zonedDateTime: ZonedDateTime,
    ts: java.sql.Timestamp,
    tuple2: (String, Int),
    tuple3: (Instant, LocalDate, CaseClassFoo),
    bar: Option[CaseClassBar])
