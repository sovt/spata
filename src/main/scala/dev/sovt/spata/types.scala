/*
 * Copyright 2020-2025 FINGO sp. z o.o.
 * Copyright 2025 sovt contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.sovt.spata

import dev.sovt.spata.error.ContentError

/** Convenience type representing result of decoding record data. */
type Decoded[A] = Either[ContentError, A]

/** Convenience type. */
type S2S = PartialFunction[String, String]

/** Convenience type. */
type I2S = PartialFunction[Int, String]
