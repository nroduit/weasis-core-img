/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op.tile;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.weasis.core.util.annotations.Generated;

/** Not an API. This class is under development and can be changed or removed at any moment. */
@Generated
public class TiledAlgorithm {
  private final int mTileSize;
  private final int mPadding;
  private final int mBorderType;

  TiledAlgorithm(int tileSize, int padding, int borderType) {
    this.mTileSize = tileSize;
    this.mPadding = padding;
    this.mBorderType = borderType;
  }

  void process(Mat sourceImage, Mat resultImage) {
    if (sourceImage.rows() != resultImage.rows() || sourceImage.cols() != resultImage.cols()) {
      throw new IllegalStateException("");
    }

    final int rows =
        (sourceImage.rows() / mTileSize) + (sourceImage.rows() % mTileSize != 0 ? 1 : 0);
    final int cols =
        (sourceImage.cols() / mTileSize) + (sourceImage.cols() % mTileSize != 0 ? 1 : 0);

    Mat tileInput = new Mat();
    Mat tileOutput = new Mat();

    for (int rowTile = 0; rowTile < rows; rowTile++) {
      for (int colTile = 0; colTile < cols; colTile++) {
        Rect srcTile =
            new Rect(
                colTile * mTileSize - mPadding,
                rowTile * mTileSize - mPadding,
                mTileSize + 2 * mPadding,
                mTileSize + 2 * mPadding);
        Rect dstTile = new Rect(colTile * mTileSize, rowTile * mTileSize, mTileSize, mTileSize);

        copySourceTile(sourceImage, tileInput, srcTile);
        processTileImpl(tileInput, tileOutput);
        copyTileToResultImage(tileOutput, resultImage, dstTile);
      }
    }
  }

  private void copyTileToResultImage(Mat tileOutput, Mat resultImage, Rect dstTile) {
    Rect srcTile = new Rect(mPadding, mPadding, mTileSize, mTileSize);

    int x = dstTile.x;
    int y = dstTile.y;

    if (x >= resultImage.cols()) {
      dstTile.width -= x - resultImage.cols();
      srcTile.width -= x - resultImage.cols();
    }

    if (y >= resultImage.rows()) {
      dstTile.height -= y - resultImage.rows();
      srcTile.height -= y - resultImage.rows();
    }

    Mat tileView = tileOutput.submat(srcTile);
    Mat dstView = resultImage.submat(dstTile);

    assert (tileView.rows() == dstView.rows());
    assert (tileView.cols() == dstView.cols());

    tileView.copyTo(dstView);
  }

  private void processTileImpl(Mat tileInput, Mat tileOutput) {
    // TODO Auto-generated method stub

  }

  private void copySourceTile(Mat sourceImage, Mat tileInput, Rect tile) {
    TiledProcessor.copyTileFromSource(sourceImage, tileInput, tile, mBorderType, tile.x, tile.y);
  }
}
