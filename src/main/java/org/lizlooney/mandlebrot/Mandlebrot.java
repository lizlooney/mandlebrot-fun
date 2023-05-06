/*
 * Copyright 2023 Liz Looney
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lizlooney.mandlebrot;

public class Mandlebrot {
  public static final int MAX_VALUE = 1000;

  private final String s;
  private final int sizeInPixels;
  private final double aMin;
  private final double bMin;
  private final double size;
  private final int[][] values;

  public Mandlebrot(int sizeInPixels, double aCenter, double bCenter, double size) {
    this.s = "Center: (" + formatDouble(aCenter) + ", " + formatDouble(bCenter) + ") width/height: " + formatDouble(size);

    this.sizeInPixels = sizeInPixels;
    aMin = aCenter - size / 2;
    bMin = bCenter - size / 2;
    this.size = size;
    values = new int[sizeInPixels][sizeInPixels];

    calculatePixelValues();
  }

  private static String formatDouble(double d) {
    String s = String.format("%f", d);
    if (s.contains(".")) {
      while (s.endsWith("0")) {
        s = s.substring(0, s.length() - 1);
      }
      if (s.endsWith(".")) {
        s = s.substring(0, s.length() - 1);
      }
    }
    return s;
  }

  public Mandlebrot panZoom(int x, int y, double zoomFactor) {
    double cA = aMin + size * x / sizeInPixels;
    double cB = bMin + size * y / sizeInPixels;
    return new Mandlebrot(sizeInPixels, cA, cB, size * zoomFactor);
  }

  public String toString() {
    return s;
  }

  private void calculatePixelValues() {
    for (int y = 0; y < sizeInPixels; y++) {
      for (int x = 0; x < sizeInPixels; x++) {
        values[y][x] = calculatePixelValue(x, y);
      }
    }
  }

  private int calculatePixelValue(int x, int y) {
    double cA = aMin + size * x / sizeInPixels;
    double cB = bMin + size * y / sizeInPixels;
    return calculateValue(cA, cB);
  }

  private int calculateValue(double cA, double cB) {
    double zA = cA;
    double zB = cB;
    for (int i = 0; i <= MAX_VALUE; i++) {
      double zAzA = zA * zA;
      double zBzB = zB * zB;
      if (zAzA + zBzB >= 4) {
        return i;
      }
      double nextA = zAzA - zBzB + cA;
      zB = 2 * zA * zB + cB;
      zA = nextA;
    }
    return Integer.MAX_VALUE;
  }

  public interface Visitor {
    void visit(int x, int y, int value);
  }

  public void accept(Visitor visitor) {
    for (int y = 0; y < sizeInPixels; y++) {
      for (int x = 0; x < sizeInPixels; x++) {
        visitor.visit(x, y, values[y][x]);
      }
    }
  }
}
