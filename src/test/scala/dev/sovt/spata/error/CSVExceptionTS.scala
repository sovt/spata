/*
 * Copyright 2020-2025 FINGO sp. z o.o.
 * Copyright 2025 sovt contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.sovt.spata.error

import dev.sovt.spata.Position
import org.scalatest.funsuite.AnyFunSuite

import java.time.format.DateTimeParseException

class CSVExceptionTS extends AnyFunSuite:

  test("CSV structure exception provides useful error message"):
    val error = ParsingErrorCode.UnescapedQuotation
    val ex = new StructureException(error, Position.some(9, 10), Some(25))
    val expected = s"Error occurred at row 9 (line 10) and column 25 while parsing CSV source. ${error.message}"
    assert(ex.getMessage == expected)

  test("CSV data exception provides useful error message"):
    val text = "this is some text which is obviously not a date"
    val cause = new DateTimeParseException("Cannot parse date", text, 0)
    val ex = new DataError(text, Position.some(9, 10), FieldInfo("date"), cause)
    val expected =
      s"Error occurred at row 9 (line 10) while parsing CSV field (name 'date', index ???) having value [this is some text...] to date/time."
    assert(ex.getMessage == expected)
