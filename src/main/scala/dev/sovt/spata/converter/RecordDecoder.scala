/*
 * Copyright 2020-2025 FINGO sp. z o.o.
 * Copyright 2025 sovt contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.sovt.spata.converter

import dev.sovt.spata.Decoded
import dev.sovt.spata.Record

// The "Dispatcher" type class
trait RecordDecoder[T]:
  def decode(r: Record): Decoded[T]

object RecordDecoder extends RecordDecoderNT, RecordDecoderCC:
  // High priority: standard tuples - put in the companion object so it's checked first
  // (Tuples are both named tuples and products, so compiler needs a hint which method to call for them)
  given forTuple[T <: Tuple: ToTuple]: RecordDecoder[T] with
    def decode(r: Record) = summon[ToTuple[T]].decode(r)

trait RecordDecoderNT:
  // Low priority: named tuples
  given forNamedTuple[T <: NamedTuple.AnyNamedTuple: ToNamedTuple]: RecordDecoder[T] with
    def decode(r: Record) = summon[ToNamedTuple[T]].decode(r)

trait RecordDecoderCC:
  // Low priority: case classes (products)
  given forCaseClass[P <: Product: ToProduct]: RecordDecoder[P] with
    def decode(r: Record) = summon[ToProduct[P]].decode(r)
