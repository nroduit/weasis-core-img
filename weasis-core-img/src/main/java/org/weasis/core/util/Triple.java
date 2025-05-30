/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

/**
 * A generic record Triple that holds three values: first, second, and third. This class is designed
 * to encapsulate three objects of potentially different types, providing a simple tuple-like
 * structure.
 *
 * @param <K> the type of the first element in the triple
 * @param <V> the type of the second element in the triple
 * @param <T> the type of the third element in the triple
 * @param first the first value in the triple
 * @param second the second value in the triple
 * @param third the third value in the triple
 */
public record Triple<K, V, T>(K first, V second, T third) {}
