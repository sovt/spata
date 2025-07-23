/*
 * Copyright 2020-2025 FINGO sp. z o.o.
 * Copyright 2025 sovt contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.sovt.sample.spata

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import dev.sovt.spata.CSVParser
import dev.sovt.spata.CSVRenderer
import dev.sovt.spata.io.Reader
import dev.sovt.spata.io.Writer
import fs2.Stream
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.Files
import scala.io.Codec

// Sample from readme (fahrenheit to celsius conversion), in form of test
class SampleITS extends AnyFunSuite:

  test("spata allows data conversion"):
    val fahrenheitCSV = SampleTH.getTempFile.toPath
    val celsiusCSV = SampleTH.getTempFile.toPath

    val converter: Stream[IO, Unit] =
      given codec: Codec = Codec.UTF8
      def fahrenheitToCelsius(f: Double): Double = (f - 32.0) * (5.0 / 9.0)
      Reader[IO]
        .read(fahrenheitCSV)
        .through(CSVParser[IO].parse)
        .filter(r => r("temp").exists(!_.isBlank))
        .map(_.altered("temp")(fahrenheitToCelsius))
        .rethrow
        .through(CSVRenderer[IO].render)
        .through(Writer[IO].write(celsiusCSV))

    Files.writeString(fahrenheitCSV, fahrenheitData)
    converter.compile.drain.unsafeRunSync()
    val output = Files.readString(celsiusCSV)
    Files.delete(fahrenheitCSV)
    Files.delete(celsiusCSV)
    assert(output == celsiusData)

  private lazy val fahrenheitData =
    """date,temp
      |2020-01-05,123
      |2020-01-05,123.9
      |2020-01-05,123.1
      |2020-01-05,124
      |2020-01-05,125""".stripMargin

  private lazy val celsiusData =
    """date,temp
      |2020-01-05,50.55555555555556
      |2020-01-05,51.055555555555564
      |2020-01-05,50.61111111111111
      |2020-01-05,51.111111111111114
      |2020-01-05,51.66666666666667""".stripMargin
