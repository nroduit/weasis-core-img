/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import java.io.IOException;
import org.weasis.core.util.annotations.Generated;

/**
 * A custom exception class that represents errors occurring in input or output operations with
 * streams. This exception extends the {@code IOException}, providing additional constructors to
 * specify detailed messages or causes.
 */
@Generated
public class StreamIOException extends IOException {

  public StreamIOException() {
    super();
  }

  public StreamIOException(String message, Throwable cause) {
    super(message, cause);
  }

  public StreamIOException(String message) {
    super(message);
  }

  public StreamIOException(Throwable cause) {
    super(cause);
  }
}
