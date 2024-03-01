/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.data;

import java.io.File;
import java.util.Objects;
import org.weasis.opencv.op.ImageProcessor;

public record FileRawImage(File file) {

  public static final int HEADER_LENGTH = 46;

  public FileRawImage(File file) {
    this.file = Objects.requireNonNull(file);
  }

  public ImageCV read() {
    return ImageProcessor.readImageWithCvException(file, null);
  }

  public boolean write(PlanarImage mat) {
    return ImageProcessor.writeImage(mat.toMat(), file);
  }
}
