/*
 * Copyright 2020-2025 FINGO sp. z o.o.
 * Copyright 2025 sovt contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.sovt.spata.converter

import dev.sovt.spata.Decoded
import dev.sovt.spata.Record

/** Dispatcher type class for converting a record to a target type.
  *
  * Delegates to [[ToTuple]], [[ToNamedTuple]] or [[ToProduct]] depending on the target type.
  *
  * @tparam T type of target entity.
  */
trait RecordDecoder[T]:

  /** Converts record to target type.
    *
    * @param r record to be converted.
    * @return either converted entity or an error.
    */
  def decode(r: Record): Decoded[T]

/** `RecordDecoder` companion object with given instance for tuples (high priority).
  *
  * Tuples are both named tuples and products, so placing the tuple instance here
  * ensures the compiler selects it before the lower-priority alternatives.
  */
object RecordDecoder extends RecordDecoderNT, RecordDecoderCC:

  /** Given instance for standard tuples. */
  given forTuple[T <: Tuple: ToTuple]: RecordDecoder[T] with
    def decode(r: Record) = summon[ToTuple[T]].decode(r)

/** Low-priority given instance for named tuples. */
trait RecordDecoderNT:

  /** Given instance for named tuples. */
  given forNamedTuple[T <: NamedTuple.AnyNamedTuple: ToNamedTuple]: RecordDecoder[T] with
    def decode(r: Record) = summon[ToNamedTuple[T]].decode(r)

/** Low-priority given instance for case classes. */
trait RecordDecoderCC:

  /** Given instance for case classes (products). */
  given forCaseClass[P <: Product: ToProduct]: RecordDecoder[P] with
    def decode(r: Record) = summon[ToProduct[P]].decode(r)
