/*
 * Copyright 2020-2025 FINGO sp. z o.o.
 * Copyright 2025 sovt contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.sovt.spata

import cats.effect.unsafe.implicits.global
import dev.sovt.spata.PerformanceTH.mwHeader
import dev.sovt.spata.PerformanceTH.renderer
import dev.sovt.spata.PerformanceTH.testMarsWeather
import dev.sovt.spata.PerformanceTH.testRecords
import dev.sovt.spata.Record.ProductOps
import org.scalameter.Bench
import org.scalameter.Gen
import org.scalameter.Key.exec

/* Check performance of parser. */
class CSVRendererPTS extends Bench.LocalTime:

  performance.of("renderer").config(exec.maxWarmupRuns := 1, exec.benchRuns := 3) in:
    measure.method("render_gen") in:
      using(amounts) in: amount =>
        testRecords(amount).through(renderer.render).compile.drain.unsafeRunSync()
    measure.method("convert_and_render_gen") in:
      using(amounts) in: amount =>
        testMarsWeather(amount).map(_.toRecord).through(renderer.render).compile.drain.unsafeRunSync()
    measure.method("convert_and_render_with_header_gen") in:
      using(amounts) in: amount =>
        testMarsWeather(amount).map(_.toRecord).through(renderer.render(mwHeader)).compile.drain.unsafeRunSync()

  private lazy val amounts = Gen.exponential("amount")(1_000, 25_000, 5)
