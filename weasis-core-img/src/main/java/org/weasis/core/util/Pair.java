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
 * A generic record Pair that holds two values: first and second. This class is designed to
 * encapsulate two objects of potentially different types, providing a simple tuple-like structure.
 *
 * @param <K> the type of the first element in the pair
 * @param <V> the type of the second element in the pair
 * @param first the first value in the pair
 * @param second the second value in the pair
 */
public record Pair<K, V>(K first, V second) {}
