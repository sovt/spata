/*
 * Copyright 2020-2025 FINGO sp. z o.o.
 * Copyright 2025 sovt contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.sovt.spata.converter

import dev.sovt.spata.Decoded
import dev.sovt.spata.Record
import dev.sovt.spata.text.StringParser

import scala.compiletime.constValueTuple
import scala.compiletime.erasedValue
import scala.compiletime.summonInline
import scala.deriving.Mirror

/** Converter from a record to a tuple.
  *
  * This trait defines behavior to be implemented by concrete, given converters.
  *
  * @tparam T type of target tuple
  */
trait ToTuple[T <: Tuple]:

  /** Converts record to tuple.
    *
    * @param r record to be converted.
    * @return either converted tuple or an exception.
    */
  def decode(r: Record): Decoded[T] = decodeAt(r, 0)

  /* Converts record to tuple starting at given index. */
  private[converter] def decodeAt(r: Record, pos: Int): Decoded[T]

/** Converter from a record to a named tuple.
  *
  * This trait defines behavior to be implemented by concrete, given converters.
  *
  * @tparam T type of target named tuple
  */
trait ToNamedTuple[T <: NamedTuple.AnyNamedTuple]:

  /** Converts record to named tuple.
    *
    * @param r record to be converted.
    * @return either converted named tuple or an exception.
    */
  def decode(r: Record): Decoded[T]

/** Converter from a record to a product (case class).
  *
  * This trait defines behavior to be implemented by concrete, given converters.
  *
  * @tparam P type of target entity.
  */
trait ToProduct[P <: Product]:

  /** Converts record to product (case class).
    *
    * @param r record to be converted.
    * @return either converted tuple or an exception.
    */
  def decode(r: Record): Decoded[P]

/** `ToTuple` companion object with given instances of record to tuple converter. */
object ToTuple:

  /** Given instance for converter to empty tuple. */
  given toEmpty: ToTuple[EmptyTuple] with
    private[converter] def decodeAt(r: Record, pos: Int): Decoded[EmptyTuple] = Right(EmptyTuple)

  /** Given instance for recursive converter to tuple cons. */
  given toCons[H: StringParser, T <: Tuple: ToTuple]: ToTuple[H *: T] with
    private[converter] def decodeAt(r: Record, pos: Int): Decoded[H *: T] =
      for // loop ends with reduced tuple, so no guard for growing position is required (wrong index returns error)
        h <- r.get(pos)
        t <- summon[ToTuple[T]].decodeAt(r, pos + 1)
      yield h *: t

object ToNamedTuple extends ToLabeled:

  /** Given instance for converter from record product (case class). */
  inline given toNamedTuple[T <: NamedTuple.AnyNamedTuple]: ToNamedTuple[T] =
    val labels = getLabels[NamedTupleDecomposition.Names[T]]
    val types = getTypes[NamedTupleDecomposition.DropNames[T]]
    decodeNamedTuple(labels.zip(types))

  private def decodeNamedTuple[T <: NamedTuple.AnyNamedTuple](
    elements: List[(String, StringParser[?])]
  ): ToNamedTuple[T] =
    (r: Record) =>
      decodeLabeled(r, elements): vals =>
        NamedTuple(Tuple.fromArray(vals.toArray)).asInstanceOf[T]

/** `ToProduct` companion object with given instance of record to product converter. */
object ToProduct extends ToLabeled:

  /** Given instance for converter from record product (case class). */
  inline given toProduct[P <: Product](using m: Mirror.ProductOf[P]): ToProduct[P] =
    // val labels = getLabels[m.MirroredElemLabels]
    val labels = getLabels[m.MirroredElemLabels]
    val types = getTypes[m.MirroredElemTypes]
    decodeProduct(m, labels.zip(types))

  /* Assemble product from record using field names and typed parsers retrieved from `Mirror`. */
  private def decodeProduct[T <: Product](
    p: Mirror.ProductOf[T],
    elements: List[(String, StringParser[?])]
  ): ToProduct[T] =
    (r: Record) =>
      decodeLabeled(r, elements): vals =>
        p.fromProduct(Tuple.fromArray(vals.toArray))

/* Shared helper methods for named tuples and case classes conversion. */
private[converter] trait ToLabeled:

  /* Get tuple values as list of string.
   * Used to get names of fields of case class or named tuple (provided by `Mirror` or `NamedTupleDecomposition`).
   */
  protected inline def getLabels[T <: Tuple]: List[String] =
    constValueTuple[T].toList.map(_.toString)

  /* Get tuple types as list of string parsers.
   * Used to get types of fields of case class or named tuple (provided by `Mirror` or `NamedTupleDecomposition`).
   */
  protected inline def getTypes[T <: Tuple]: List[StringParser[?]] = inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts) => summonInline[StringParser[t]] :: getTypes[ts]

  /* Assemble case class / named tuple from record using field names and typed parsers.
   * They are retrieved from `Mirror` for case class and `NamedTupleDecomposition` for named tuples.
   */
  protected def decodeLabeled[T](record: Record, elements: List[(String, StringParser[?])])(
    instantiate: List[?] => T
  ): Decoded[T] =
    val instance = for (label, parser) <- elements yield record.get(label)(using parser) // parsed values
    val (left, right) = instance.partitionMap(identity) // split into errors (left) and correct values (right)
    left.headOption.toLeft(instantiate(right)) // retrun error, if any, or instantiate T from values
