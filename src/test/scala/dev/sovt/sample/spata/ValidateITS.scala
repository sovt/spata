/*
 * Copyright 2020-2025 FINGO sp. z o.o.
 * Copyright 2025 sovt contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.sovt.sample.spata

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import dev.sovt.spata.CSVConfig
import dev.sovt.spata.io.Reader
import dev.sovt.spata.schema.CSVSchema
import dev.sovt.spata.schema.validator.FiniteValidator
import fs2.Stream
import org.scalatest.funsuite.AnyFunSuite
import org.slf4j.LoggerFactory

import java.time.LocalDate

/* Sample which validates CSV and provides typed records. */
class ValidateITS extends AnyFunSuite:

  private val logger = LoggerFactory.getLogger(this.getClass) // regular, impure logger

  private def getSource() = SampleTH.sourceFromResource(SampleTH.dataFile)

  test("spata allows data validation and conversion to case classes in type-safe manner"):
    case class DayTempVar(date: LocalDate, tempVar: Double)
    val parser = CSVConfig().mapHeader("terrestrial_date" -> "date").stripSpaces.parser[IO] // parser with IO effect
    val schema = CSVSchema()
      .add[LocalDate]("date")
      .add[Double]("min_temp", FiniteValidator()) // NaN is not accepted
      .add[Double]("max_temp", FiniteValidator())
    val stream = Stream
      .bracket(IO(getSource()))(source => IO(source.close())) // ensure resource cleanup
      .through(Reader.plain[IO].by)
      .through(parser.parse) // get stream of CSV records
      .through(schema.validate) // validate against schema, get stream of Validated
      .map:
        _.bimap( // map over invalid and valid part of Validated
          invalidRecord => logger.warn(invalidRecord.toString),
          typedRecord => DayTempVar(typedRecord("date"), typedRecord("max_temp") - typedRecord("min_temp"))
        )
      .map(_.toOption) // get only valid DayTempVar out of Validated (this and next line)
      .unNone
      .filter(_.date.getYear == 2016) // filter data for specific year
      .handleErrorWith(ex => fail(ex.getMessage)) // fail test on any stream error
    val result = stream.compile.toList.unsafeRunSync()
    assert(result.length > 300 && result.length < 400)
    assert(result.forall(_.date.getYear == 2016))
