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

public class ColorTable {
  private final int[] table;
  private final ColorUtils colorUtils;

  public ColorTable(int size, ColorUtils colorUtils) {
    table = new int[size];
    this.colorUtils = colorUtils;
  }

  public int size() {
    return table.length;
  }

  public void fill(Hue h, Saturation s, Brightness b) {
    for (int i = 0; i < table.length; i++) {
      table[i] = colorUtils.colorComponentsToRGB(h.hue(), s.saturation(), b.brightness());
      float oldValue = h.value;
      h.next();
      s.next();
      b.next();
    }
  }

  public int valueToColor(int value) {
    if (value == 0) {
      return 0xFFFFFF;
    }
    if (value > table.length) {
      return 0;
    }

    return table[value - 1];
  }

  interface ColorUtils {
    int colorComponentsToRGB(float h, float s, float b);
  }

  static abstract class ColorComponent {
    private final float min;
    private final float max;
    private final float delta;
    protected float value;
    protected int direction;

    ColorComponent(float min, float max, float delta) {
      this.min = min;
      this.max = max;
      this.delta = delta;
      value = min;
      direction = 1;
    }

    void next() {
      if (delta != 0) {
        float oldValue = value;
        float newValue = value + delta * direction;
        if (direction == 1 && newValue > max) {
          direction = -1;
          newValue = value + delta * direction;
        } else if (direction == -1 && newValue < min) {
          direction = 1;
          newValue = value + delta * direction;
        }
        if (newValue >= min && newValue <= max) {
          value = newValue;
        }
      }
    }
  }

  public static class Hue extends ColorComponent {
    Hue(float min, float max, float delta) {
      super(min, max, delta);
    }

    float hue() {
      return value / 360f;
    }
  }

  public static class Saturation extends ColorComponent {
    Saturation(float min, float max, float delta) {
      super(min, max, delta);
    }

    float saturation() {
      return value / 100f;
    }
  }

  public static class Brightness extends ColorComponent {
    Brightness(float min, float max, float delta) {
      super(min, max, delta);
    }

    float brightness() {
      return value / 100f;
    }
  }
}
