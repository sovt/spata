/*
 * Copyright 2020-2025 FINGO sp. z o.o.
 * Copyright 2025 sovt contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.sovt.spata

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import dev.sovt.spata.PerformanceTH.MarsWeather
import dev.sovt.spata.PerformanceTH.input
import dev.sovt.spata.PerformanceTH.parser
import dev.sovt.spata.PerformanceTH.testSource
import dev.sovt.spata.io.Reader
import org.scalameter.Bench
import org.scalameter.Gen
import org.scalameter.Key.exec

/* Check performance of parser.
 * It would be good to have regression for it but ScalaMeter somehow refused to work in this mode.
 */
object CSVParserPTS extends Bench.LocalTime:

  performance.of("parser").config(exec.maxWarmupRuns := 1, exec.benchRuns := 3) in:
    measure.method("parse_gen") in:
      using(amounts) in: amount =>
        testSource(amount).through(parser.parse).compile.drain.unsafeRunSync()
    measure.method("parse_and_convert_gen") in:
      using(amounts) in: amount =>
        testSource(amount)
          .through(parser.parse)
          .map(_.to[(Double, Double, Double, Double, Double, Double, Double, Double, Double, String)])
          .compile
          .drain
          .unsafeRunSync()
    measure.method("parse_and_convert_file") in:
      using(Gen.unit("file")) in: _ =>
        Reader[IO]
          .read(input)
          .through(parser.parse)
          .map(_.to[MarsWeather])
          .compile
          .drain
          .unsafeRunSync()

  private lazy val amounts = Gen.exponential("amount")(1_000, 25_000, 5)
