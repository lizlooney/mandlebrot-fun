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

import java.awt.Color;

public class ColorTable {
  static abstract class ColorComponent {
    protected int value;
    private final int min;
    private final int max;
    private int delta;

    ColorComponent(int value, int min, int max, int delta) {
      this.value = value;
      this.min = min;
      this.max = max;
      this.delta = delta;
    }

    void next() {
      if (delta != 0) {
        int newValue = value + delta;
        if (newValue < min || newValue > max) {
          delta *= -1;
          newValue = value + delta;
        }
        if (newValue >= min && newValue <= max) {
          value = newValue;
        }
      }
    }
  }

  public static class Hue extends ColorComponent {
    Hue(int value, int min, int max, int delta) {
      super(value, min, max, delta);
    }

    float hue() {
      return value / 360f;
    }
  }

  public static class Saturation extends ColorComponent {
    Saturation(int value, int min, int max, int delta) {
      super(value, min, max, delta);
    }

    float saturation() {
      return value / 100f;
    }
  }

  public static class Brightness extends ColorComponent {
    Brightness(int value, int min, int max, int delta) {
      super(value, min, max, delta);
    }

    float brightness() {
      return value / 100f;
    }
  }

  public static void fill(int[] colorTable, Hue h, Saturation s, Brightness b) {
    for (int i = 0; i < colorTable.length; i++) {
      colorTable[i] = Color.HSBtoRGB(h.hue(), s.saturation(), b.brightness());
      h.next();
      s.next();
      b.next();
    }
  }
}
