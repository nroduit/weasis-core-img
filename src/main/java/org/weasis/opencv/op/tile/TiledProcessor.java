/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.weasis.opencv.op.tile;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class TiledProcessor {

  public Mat blur(Mat input, int numberOfTimes) {
    Mat sourceImage;
    Mat destImage = input.clone();
    for (int i = 0; i < numberOfTimes; i++) {
      sourceImage = destImage.clone();
      // Imgproc.blur(sourceImage, destImage, new Size(3.0, 3.0));
      process(sourceImage, destImage, 256);
    }
    return destImage;
  }

  public void process(Mat sourceImage, Mat resultImage, int tileSize) {

    if (sourceImage.rows() != resultImage.rows() || sourceImage.cols() != resultImage.cols()) {
      throw new IllegalStateException("");
    }

    final int rowTiles =
        (sourceImage.rows() / tileSize) + (sourceImage.rows() % tileSize != 0 ? 1 : 0);
    final int colTiles =
        (sourceImage.cols() / tileSize) + (sourceImage.cols() % tileSize != 0 ? 1 : 0);

    Mat tileInput = new Mat(tileSize, tileSize, sourceImage.type());
    Mat tileOutput = new Mat(tileSize, tileSize, sourceImage.type());

    int boderType = Core.BORDER_DEFAULT;
    int mPadding = 3;

    for (int rowTile = 0; rowTile < rowTiles; rowTile++) {
      for (int colTile = 0; colTile < colTiles; colTile++) {
        Rect srcTile =
            new Rect(
                colTile * tileSize - mPadding,
                rowTile * tileSize - mPadding,
                tileSize + 2 * mPadding,
                tileSize + 2 * mPadding);
        Rect dstTile = new Rect(colTile * tileSize, rowTile * tileSize, tileSize, tileSize);
        copyTileFromSource(sourceImage, tileInput, srcTile, boderType);
        processTileImpl(tileInput, tileOutput);
        copyTileToResultImage(
            tileOutput, resultImage, new Rect(mPadding, mPadding, tileSize, tileSize), dstTile);
      }
    }
  }

  private void copyTileToResultImage(Mat tileOutput, Mat resultImage, Rect srcTile, Rect dstTile) {
    Point br = dstTile.br();

    if (br.x >= resultImage.cols()) {
      dstTile.width -= br.x - resultImage.cols();
      srcTile.width -= br.x - resultImage.cols();
    }

    if (br.y >= resultImage.rows()) {
      dstTile.height -= br.y - resultImage.rows();
      srcTile.height -= br.y - resultImage.rows();
    }

    Mat tileView = tileOutput.submat(srcTile);
    Mat dstView = resultImage.submat(dstTile);

    assert (tileView.rows() == dstView.rows());
    assert (tileView.cols() == dstView.cols());

    tileView.copyTo(dstView);
  }

  private void processTileImpl(Mat tileInput, Mat tileOutput) {
    Imgproc.blur(tileInput, tileOutput, new Size(7.0, 7.0));
  }

  private void copyTileFromSource(Mat sourceImage, Mat tileInput, Rect tile, int mBorderType) {
    Point tl = tile.tl();
    Point br = tile.br();

    Point tloffset = new Point();
    Point broffset = new Point();

    // Take care of border cases
    if (tile.x < 0) {
      tloffset.x = -tile.x;
      tile.x = 0;
    }

    if (tile.y < 0) {
      tloffset.y = -tile.y;
      tile.y = 0;
    }

    if (br.x >= sourceImage.cols()) {
      broffset.x = br.x - sourceImage.cols() + 1;
      tile.width -= broffset.x;
    }

    if (br.y >= sourceImage.rows()) {
      broffset.y = br.y - sourceImage.rows() + 1;
      tile.height -= broffset.y;
    }

    // If any of the tile sides exceed source image boundary we must use copyMakeBorder to make
    // proper paddings
    // for this side
    if (tloffset.x > 0 || tloffset.y > 0 || broffset.x > 0 || broffset.y > 0) {
      Rect paddedTile = new Rect(tile.tl(), tile.br());
      assert (paddedTile.x >= 0);
      assert (paddedTile.y >= 0);
      assert (paddedTile.br().x < sourceImage.cols());
      assert (paddedTile.br().y < sourceImage.rows());

      Core.copyMakeBorder(
          sourceImage.submat(paddedTile),
          tileInput,
          (int) tloffset.y,
          (int) broffset.y,
          (int) tloffset.x,
          (int) broffset.x,
          mBorderType);
    } else {
      // Entire tile (with paddings lies inside image and it's safe to just take a region:
      sourceImage.submat(tile).copyTo(tileInput);
    }
  }
}
