/*
 * Copyright 2020-2025 FINGO sp. z o.o.
 * Copyright 2025 sovt contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.sovt.spata.util

/* Gets short class identifier. Used e.g. for error codes. */
private[spata] def classLabel(obj: AnyRef): String =
  def getSimpleName(cls: Class[?]): String =
    Option(cls)
      .map(c => if c.getSimpleName.nonEmpty then c.getSimpleName else getSimpleName(c.getEnclosingClass))
      .getOrElse("?")
  val name = getSimpleName(obj.getClass)
  val head = name.take(1).toLowerCase
  val tail = name.takeWhile(_ != '$').tail
  head + tail
