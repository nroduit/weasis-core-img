/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op.lut;

import java.awt.Color;

/**
 * An enumeration representing various color lookup tables (LUTs). Each LUT is designed for specific
 * color transformations or visual effects and is defined by a name and mapping data for red, green,
 * and blue channels.
 */
public enum ColorLut {
  IMAGE("Default (image)", null),
  FLAG("Flag", createFlagLut()),
  MULTICOLOR("Multi-Color", createMultiColorLut()),
  HUE("Hue", createHueLut()),
  RED("Red", createSingleChannelLut(2)), // Red channel
  GREEN("Green", createSingleChannelLut(1)), // Green channel
  BLUE("Blue", createSingleChannelLut(0)), // Blue channel
  GRAY("Gray", createGrayLut());

  private static final int LUT_SIZE = 256;
  private static final int CHANNEL_COUNT = 3;

  // Channel indices for BGR format
  private static final int BLUE_CHANNEL = 0;
  private static final int GREEN_CHANNEL = 1;
  private static final int RED_CHANNEL = 2;

  private final ByteLut byteLut;

  ColorLut(String name, byte[][] lutTable) {
    this.byteLut = new ByteLut(name, lutTable);
  }

  public String getName() {
    return byteLut.name();
  }

  public ByteLut getByteLut() {
    return byteLut;
  }

  @Override
  public String toString() {
    return byteLut.name();
  }

  // Static factory methods for LUT creation

  private static byte[][] createFlagLut() {
    byte[][] flag = new byte[CHANNEL_COUNT][LUT_SIZE];

    // Flag pattern: Blue, Yellow, Magenta, Black (cycling every 4 values)
    int[] red = {255, 255, 0, 0};
    int[] green = {0, 255, 0, 0};
    int[] blue = {0, 255, 255, 0};

    return fillChannels(flag, red, green, blue);
  }

  private static byte[][] fillChannels(byte[][] flag, int[] red, int[] green, int[] blue) {
    for (int i = 0; i < LUT_SIZE; i++) {
      int patternIndex = i % red.length;
      flag[BLUE_CHANNEL][i] = (byte) blue[patternIndex];
      flag[GREEN_CHANNEL][i] = (byte) green[patternIndex];
      flag[RED_CHANNEL][i] = (byte) red[patternIndex];
    }
    return flag;
  }

  private static byte[][] createMultiColorLut() {
    byte[][] multiColor = new byte[CHANNEL_COUNT][LUT_SIZE];

    // 36-color palette for diverse visualization
    int[] red = {
      255, 0, 255, 0, 255, 128, 64, 255, 0, 128, 236, 189, 250, 154, 221, 255, 128, 255, 0, 128,
      228, 131, 189, 0, 36, 66, 40, 132, 156, 135, 98, 194, 217, 251, 255, 0
    };
    int[] green = {
      3, 255, 245, 0, 0, 0, 128, 128, 0, 0, 83, 228, 202, 172, 160, 128, 128, 200, 187, 88, 93, 209,
      89, 255, 137, 114, 202, 106, 235, 85, 216, 226, 182, 247, 195, 173
    };
    int[] blue = {
      0, 0, 55, 255, 255, 0, 64, 0, 128, 128, 153, 170, 87, 216, 246, 128, 64, 188, 236, 189, 39,
      96, 212, 255, 176, 215, 204, 221, 255, 70, 182, 84, 172, 176, 142, 95
    };
    return fillChannels(multiColor, red, green, blue);
  }

  private static byte[][] createHueLut() {
    byte[][] hue = new byte[CHANNEL_COUNT][LUT_SIZE];

    for (int i = 0; i < LUT_SIZE; i++) {
      Color color = Color.getHSBColor(i / 255f, 1f, 1f);
      hue[BLUE_CHANNEL][i] = (byte) color.getBlue();
      hue[GREEN_CHANNEL][i] = (byte) color.getGreen();
      hue[RED_CHANNEL][i] = (byte) color.getRed();
    }
    return hue;
  }

  /**
   * Creates a single-channel LUT where only one color channel varies from 0-255 while others remain
   * at 0.
   *
   * @param activeChannel the channel to activate (0=Blue, 1=Green, 2=Red)
   * @return the single-channel LUT
   */
  private static byte[][] createSingleChannelLut(int activeChannel) {
    byte[][] singleChannel = new byte[CHANNEL_COUNT][LUT_SIZE];

    for (int i = 0; i < LUT_SIZE; i++) {
      singleChannel[activeChannel][i] = (byte) i;
      // Other channels remain 0 (initialized by default)
    }
    return singleChannel;
  }

  private static byte[][] createGrayLut() {
    byte[][] gray = new byte[CHANNEL_COUNT][LUT_SIZE];

    for (int i = 0; i < LUT_SIZE; i++) {
      byte grayValue = (byte) i;
      gray[BLUE_CHANNEL][i] = grayValue;
      gray[GREEN_CHANNEL][i] = grayValue;
      gray[RED_CHANNEL][i] = grayValue;
    }

    return gray;
  }
}
