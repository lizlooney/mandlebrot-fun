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
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.GestureDetectorCompat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class MandlebrotActivity extends Activity {
  private static final boolean USE_NATIVE_CODE = true;
  private static final int NUM_THREADS = 16;
  private static final double ZOOM_OUT = 4;
  private static final double ZOOM_IN = 1 / ZOOM_OUT;

  private final Deque<Mandlebrot> mStack = new ArrayDeque<>();
  private ColorTable colorTable;
  private int mandlebrotSize;

  private ImageView mandlebrotImageView;
  private TextView mandlebrotLabel;
  private Button backButton;
  private Button zoomOutButton;
  private Button zoomInButton;
  private Button upLeftButton;
  private Button upButton;
  private Button upRightButton;
  private Button leftButton;
  private Button rightButton;
  private Button downLeftButton;
  private Button downButton;
  private Button downRightButton;

  private final List<View> views = new ArrayList<>();

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

    mandlebrotSize = getSizeForMandlebrot();

    final LinearLayout mandlebrotImageViewParent = findViewById(R.id.mandlebrotImageViewParent);
    mandlebrotLabel = findViewById(R.id.mandlebrotLabel);
    backButton = findViewById(R.id.back);
    zoomOutButton = findViewById(R.id.zoomOut);
    zoomInButton = findViewById(R.id.zoomIn);
    upLeftButton = findViewById(R.id.upLeft);
    upButton = findViewById(R.id.up);
    upRightButton = findViewById(R.id.upRight);
    leftButton = findViewById(R.id.left);
    rightButton = findViewById(R.id.right);
    downLeftButton = findViewById(R.id.downLeft);
    downButton = findViewById(R.id.down);
    downRightButton = findViewById(R.id.downRight);

    final GestureDetectorCompat gestureDetector = new GestureDetectorCompat(this, new SimpleOnGestureListener() {
      @Override
      public void onLongPress(MotionEvent event) {
        panZoom((int) event.getX(), (int) event.getY(), ZOOM_IN);
      }
      @Override
      public boolean onDoubleTap(MotionEvent event) {
        panZoom((int) event.getX(), (int) event.getY(), ZOOM_IN);
        return true;
      }
    });
    gestureDetector.setIsLongpressEnabled(true);
    mandlebrotImageView = new ImageView(this) {
      @Override
      public boolean onTouchEvent(MotionEvent event) {
        if (! mandlebrotImageView.isEnabled()) {
          return false;
        }
        if (gestureDetector.onTouchEvent(event)) {
          return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          mandlebrotImageViewParent.requestDisallowInterceptTouchEvent(true);
        }
        return true;
      }
    };
    mandlebrotImageViewParent.addView(mandlebrotImageView,
        new LayoutParams(mandlebrotSize, mandlebrotSize, 0f));

    backButton.setOnClickListener(view -> {
      if (mStack.size() > 1) {
        mStack.removeLast();
        onMandlebrotChanged(null);
      }
    });
    zoomOutButton.setOnClickListener(view -> zoom(ZOOM_OUT));
    zoomInButton.setOnClickListener(view -> zoom(ZOOM_IN));
    upLeftButton.setOnClickListener(view -> pan(panLeft(), panUp()));
    upButton.setOnClickListener(view -> pan(panCenter(), panUp()));
    upRightButton.setOnClickListener(view -> pan(panRight(), panUp()));
    leftButton.setOnClickListener(view -> pan(panLeft(), panCenter()));
    rightButton.setOnClickListener(view -> pan(panRight(), panCenter()));
    downLeftButton.setOnClickListener(view -> pan(panLeft(), panDown()));
    downButton.setOnClickListener(view -> pan(panCenter(), panDown()));
    downRightButton.setOnClickListener(view -> pan(panRight(), panDown()));

    views.add(mandlebrotImageView);
    views.add(backButton);
    views.add(zoomOutButton);
    views.add(zoomInButton);
    views.add(upLeftButton);
    views.add(upButton);
    views.add(upRightButton);
    views.add(leftButton);
    views.add(rightButton);
    views.add(downLeftButton);
    views.add(downButton);
    views.add(downRightButton);

    final Toast toast = Toast.makeText(MandlebrotActivity.this, "Calculating...", Toast.LENGTH_LONG);
    toast.show();

    final List<View> disabledViews = disableUI();
    new Thread(() -> {
      Mandlebrot mandlebrot = new Mandlebrot(USE_NATIVE_CODE, NUM_THREADS,
          mandlebrotSize, 0, 0, 4);
      runOnUiThread(() -> {
        enableUI(disabledViews);
        mStack.addLast(mandlebrot);
        onMandlebrotChanged(toast);
      });
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

  private List<View> disableUI() {
    List<View> disabledViews = new ArrayList<>();
    for (View view : views) {
      if (view.isEnabled()) {
        view.setEnabled(false);
        disabledViews.add(view);
      }
    }
    return disabledViews;
  }

  private void enableUI(List<View> disabledViews) {
    for (View view  : disabledViews) {
      view.setEnabled(true);
    }
  }

  private void onMandlebrotChanged(final Toast toast) {
    backButton.setEnabled(mStack.size() > 1);
    mandlebrotLabel.setText(mStack.peekLast().toString());

    final Bitmap bitmap = Bitmap.createBitmap(mandlebrotSize, mandlebrotSize, Bitmap.Config.ARGB_8888);
    mStack.peekLast().accept((x, y, value) -> {
      bitmap.setPixel(x, y, 0xFF000000 | colorTable.valueToColor(value));
    });

    if (toast != null) {
      toast.cancel();
    }
    mandlebrotImageView.setImageBitmap(bitmap);
  }

  private void zoom(double zoomFactor) {
    panZoom(panCenter(), panCenter(), zoomFactor);
  }

  private int panCenter() {
    return mandlebrotSize / 2;
  }

  private int panUp() {
    return mandlebrotSize / 10;
  }

  private int panDown() {
    return mandlebrotSize * 9 / 10;
  }

  private int panLeft() {
    return mandlebrotSize / 10;
  }

  private int panRight() {
    return mandlebrotSize * 9 / 10;
  }

  private void pan(int x, int y) {
    panZoom(x, y, 1.0);
  }

  private void panZoom(final int x, final int y, final double zoomFactor) {
    Toast toast = Toast.makeText(MandlebrotActivity.this, "Calculating...", Toast.LENGTH_LONG);
    toast.show();
    final List<View> disabledViews = disableUI();
    new Thread(() -> {
      Mandlebrot mandlebrot = mStack.peekLast().panZoom(x, y, zoomFactor);
      runOnUiThread(() -> {
        enableUI(disabledViews);
        mStack.addLast(mandlebrot);
        onMandlebrotChanged(toast);
      });
    }).start();
  }
}
