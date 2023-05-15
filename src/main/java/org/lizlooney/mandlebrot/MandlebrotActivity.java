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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.NumberPicker;
import android.widget.Spinner;
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

  private static final int BACK_ZOOM_PAN = 0;
  private static final int HUE = 1;
  private static final int SATURATION = 2;
  private static final int VALUE = 3;

  private final Deque<Mandlebrot> mStack = new ArrayDeque<>();
  private int mandlebrotSize;

  private ImageView mandlebrotImageView;
  private TextView mandlebrotTextView;
  private Button backButton;
  private NumberPicker hStart;
  private NumberPicker hMin;
  private NumberPicker hMax;
  private NumberPicker hDelta;
  private NumberPicker sStart;
  private NumberPicker sMin;
  private NumberPicker sMax;
  private NumberPicker sDelta;
  private NumberPicker vStart;
  private NumberPicker vMin;
  private NumberPicker vMax;
  private NumberPicker vDelta;
  private final List<View> views = new ArrayList<>();
  private final List<NumberPicker> colorControlWidgets = new ArrayList<>();

  private ColorTable colorTable;

  static {
    System.loadLibrary("android_app");
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.mandlebrot_activity);

    mandlebrotSize = getSizeForMandlebrot();

    final float[] hsv = new float[3];
    colorTable = new ColorTable(Mandlebrot.MAX_VALUE, (h, s, b) -> {
      hsv[0] = (float) h * 360f;
      hsv[1] = (float) s;
      hsv[2] = (float) b;
      return Color.HSVToColor(hsv);
    });

    final LinearLayout mandlebrotPanel = findViewById(R.id.mandlebrotPanel);
    mandlebrotTextView = findViewById(R.id.mandlebrotTextView);
    Spinner spinner = findViewById(R.id.spinner);
    LinearLayout backPanZoomPanel = findViewById(R.id.backPanZoomPanel);
    backButton = findViewById(R.id.back);
    Button zoomOutButton = findViewById(R.id.zoomOut);
    Button zoomInButton = findViewById(R.id.zoomIn);
    Button upLeftButton = findViewById(R.id.upLeft);
    Button upButton = findViewById(R.id.up);
    Button upRightButton = findViewById(R.id.upRight);
    Button leftButton = findViewById(R.id.left);
    Button rightButton = findViewById(R.id.right);
    Button downLeftButton = findViewById(R.id.downLeft);
    Button downButton = findViewById(R.id.down);
    Button downRightButton = findViewById(R.id.downRight);
    LinearLayout huePanel = findViewById(R.id.huePanel);
    hStart = findViewById(R.id.hStart);
    hMin = findViewById(R.id.hMin);
    hMax = findViewById(R.id.hMax);
    hDelta = findViewById(R.id.hDelta);
    LinearLayout saturationPanel = findViewById(R.id.saturationPanel);
    sStart = findViewById(R.id.sStart);
    sMin = findViewById(R.id.sMin);
    sMax = findViewById(R.id.sMax);
    sDelta = findViewById(R.id.sDelta);
    LinearLayout valuePanel = findViewById(R.id.valuePanel);
    vStart = findViewById(R.id.vStart);
    vMin = findViewById(R.id.vMin);
    vMax = findViewById(R.id.vMax);
    vDelta = findViewById(R.id.vDelta);

    final GestureDetectorCompat gestureDetector = new GestureDetectorCompat(this, new SimpleOnGestureListener() {
      @Override
      public boolean onDoubleTapEvent(MotionEvent event) {
        if (mandlebrotImageView.isEnabled()) {
          panZoom((int) event.getX(), (int) event.getY(), ZOOM_IN);
        }
        return true;
      }
    });
    mandlebrotImageView = new ImageView(this) {
      @Override
      public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
          return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          mandlebrotPanel.requestDisallowInterceptTouchEvent(true);
        }
        return true;
      }
    };
    mandlebrotPanel.addView(mandlebrotImageView,
        new LayoutParams(mandlebrotSize, mandlebrotSize, 0f));

    ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
        R.array.spinner_choices, R.layout.spinner);
    spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
    spinner.setAdapter(spinnerAdapter);
    spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        backPanZoomPanel.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
        huePanel.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
        saturationPanel.setVisibility(pos == 2 ? View.VISIBLE : View.GONE);
        valuePanel.setVisibility(pos == 3 ? View.VISIBLE : View.GONE);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {}
    });

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

    hStart.setMinValue(0);
    hStart.setMaxValue(360);
    hStart.setValue(0);
    hStart.setOnValueChangedListener((p, o, v) -> colorControlPanelChanged(true));
    hMin.setMinValue(0);
    hMin.setMaxValue(360);
    hMin.setValue(0);
    hMin.setOnValueChangedListener((p, o, v) -> colorControlPanelChanged(true));
    hMax.setMinValue(0);
    hMax.setMaxValue(720);
    hMax.setValue(360);
    hMax.setOnValueChangedListener((p, o, v) -> colorControlPanelChanged(true));
    hDelta.setMinValue(0);
    hDelta.setMaxValue(360);
    hDelta.setValue(1);
    hDelta.setOnValueChangedListener((p, o, v) -> colorControlPanelChanged(false));
    sStart.setMinValue(0);
    sStart.setMaxValue(100);
    sStart.setValue(70);
    sStart.setOnValueChangedListener((p, o, v) -> colorControlPanelChanged(true));
    sMin.setMinValue(0);
    sMin.setMaxValue(100);
    sMax.setValue(0);
    sMin.setOnValueChangedListener((p, o, v) -> colorControlPanelChanged(true));
    sMax.setMinValue(0);
    sMax.setMaxValue(100);
    sMax.setValue(100);
    sMax.setOnValueChangedListener((p, o, v) -> colorControlPanelChanged(true));
    sDelta.setMinValue(0);
    sDelta.setMaxValue(100);
    sDelta.setValue(0);
    sDelta.setOnValueChangedListener((p, o, v) -> colorControlPanelChanged(false));
    vStart.setMinValue(0);
    vStart.setMaxValue(100);
    vStart.setValue(70);
    vStart.setOnValueChangedListener((p, o, v) -> colorControlPanelChanged(true));
    vMin.setMinValue(0);
    vMin.setMaxValue(100);
    vMax.setValue(0);
    vMin.setOnValueChangedListener((p, o, v) -> colorControlPanelChanged(true));
    vMax.setMinValue(0);
    vMax.setMaxValue(100);
    vMax.setValue(100);
    vMax.setOnValueChangedListener((p, o, v) -> colorControlPanelChanged(true));
    vDelta.setMinValue(0);
    vDelta.setMaxValue(100);
    vDelta.setValue(0);
    vDelta.setOnValueChangedListener((p, o, v) -> colorControlPanelChanged(false));

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
    views.add(hStart);
    views.add(hMin);
    views.add(hMax);
    views.add(hDelta);
    views.add(sStart);
    views.add(sMin);
    views.add(sMax);
    views.add(sDelta);
    views.add(vStart);
    views.add(vMin);
    views.add(vMax);
    views.add(vDelta);
    colorControlWidgets.add(hStart);
    colorControlWidgets.add(hMin);
    colorControlWidgets.add(hMax);
    colorControlWidgets.add(hDelta);
    colorControlWidgets.add(sStart);
    colorControlWidgets.add(sMin);
    colorControlWidgets.add(sMax);
    colorControlWidgets.add(sDelta);
    colorControlWidgets.add(vStart);
    colorControlWidgets.add(vMin);
    colorControlWidgets.add(vMax);
    colorControlWidgets.add(vDelta);

    fillColorTable();
    updateColorControlWidgets();

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
        new ColorTable.Hue(hStart.getValue(), hMin.getValue(), hMax.getValue(), hDelta.getValue()),
        new ColorTable.Saturation(sStart.getValue(), sMin.getValue(), sMax.getValue(), sDelta.getValue()),
        new ColorTable.Brightness(vStart.getValue(), vMin.getValue(), vMax.getValue(), vDelta.getValue()));
  }

  private void colorControlPanelChanged(boolean updateColorControlWidgets) {
    if (updateColorControlWidgets) {
      updateColorControlWidgets();
    }
    colorControlPanelChanged();
  }

  private void updateColorControlWidgets() {
    float[] hsv = new float[3];
    for (NumberPicker p : colorControlWidgets) {
      if (p == hStart || p == hMin || p == hMax) {
        hsv[0] = p.getValue();
        hsv[1] = 0.7f;
        hsv[2] = 0.7f;
        p.setBackgroundColor(Color.HSVToColor(hsv));
      } else if (p == sStart || p == sMin || p == sMax) {
        hsv[0] = hStart.getValue();
        hsv[1] = p.getValue() / 100f;
        hsv[2] = 0.7f;
        p.setBackgroundColor(Color.HSVToColor(hsv));
      } else if (p == vStart || p == vMin || p == vMax) {
        hsv[0] = hStart.getValue();
        hsv[1] = 0.7f;
        hsv[2] = p.getValue() / 100f;
        p.setBackgroundColor(Color.HSVToColor(hsv));
      }
    }
  }

  private void colorControlPanelChanged() {
    fillColorTable();

    Bitmap bitmap = produceImage(mStack.peekLast());
    mandlebrotImageView.setImageBitmap(bitmap);
  }

  private Bitmap produceImage(Mandlebrot m) {
    final Bitmap bitmap = Bitmap.createBitmap(mandlebrotSize, mandlebrotSize, Bitmap.Config.ARGB_8888);
    m.accept((x, y, value) -> {
      bitmap.setPixel(x, y, 0xFF000000 | colorTable.valueToColor(value));
    });
    return bitmap;
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
    Mandlebrot mandlebrot = mStack.peekLast();
    mandlebrotTextView.setText(mandlebrot.toString());

    Bitmap bitmap = produceImage(mandlebrot);
    mandlebrotImageView.setImageBitmap(bitmap);

    if (toast != null) {
      toast.cancel();
    }
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
