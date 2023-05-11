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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;
import java.util.ArrayDeque;
import java.util.Deque;

public final class MandlebrotActivity extends Activity {
  private static final boolean USE_NATIVE_CODE = false;
  private static final int NUM_THREADS = 16;

  private final Deque<Mandlebrot> mStack = new ArrayDeque<>();
  private ColorTable colorTable;
  private ImageView mandlebrotImageView;
  private int mandlebrotSize;

  static {
    System.loadLibrary("android_app");
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.mandlebrot_activity);

    final float[] hsv = new float[3];
    colorTable = new ColorTable(Mandlebrot.MAX_VALUE, (h, s, b) -> {
      hsv[0] = (float) h * 360f;
      hsv[1] = (float) s;
      hsv[2] = (float) b;
      return Color.HSVToColor(hsv);
    });
    fillColorTable();
    mandlebrotImageView = findViewById(R.id.mandlebrotImageView);
    mandlebrotSize = getSizeForMandlebrot();

    new Thread(() -> {
      mStack.addLast(new Mandlebrot(USE_NATIVE_CODE, NUM_THREADS,
          mandlebrotSize, 0, 0, 4));
      Bitmap bitmap = produceBitmap(mStack.peekLast());
      setMandlebrotImage(bitmap);
    }).start();
  }

  private void fillColorTable() {
    colorTable.fill(
        new ColorTable.Hue(0, 0, 360, 1),
        new ColorTable.Saturation(70, 0, 100, 0),
        new ColorTable.Brightness(70, 0, 100, 0));
  }

  private int getSizeForMandlebrot() {
    WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    return Math.min(size.x, size.y);
  }

  private Bitmap produceBitmap(Mandlebrot m) {
    Bitmap bitmap = Bitmap.createBitmap(mandlebrotSize, mandlebrotSize, Bitmap.Config.ARGB_8888);
    m.accept((x, y, value) -> {
      bitmap.setPixel(x, y, colorTable.valueToColor(value));
    });
    return bitmap;
  }

  private void setMandlebrotImage(Bitmap bitmap) {
    runOnUiThread(() -> mandlebrotImageView.setImageBitmap(bitmap));
  }
}
