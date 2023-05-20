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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
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

  private static final int SPINNER_POS_BACK_ZOOM_PAN = 0;
  private static final int SPINNER_POS_COLOR = 1;

  private final Deque<Mandlebrot> mStack = new ArrayDeque<>();
  private int mandlebrotSize;

  private ImageView mandlebrotImageView;
  private ColorTableView colorTableView;
  private TextView mandlebrotTextView;
  private Button backButton;
  private EditText hMin;
  private EditText hMax;
  private EditText hDelta;
  private EditText sMin;
  private EditText sMax;
  private EditText sDelta;
  private EditText vMin;
  private EditText vMax;
  private EditText vDelta;
  private final List<View> views = new ArrayList<>();

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
      hsv[0] = (float) (h - Math.floor(h)) * 360f;
      hsv[1] = s;
      hsv[2] = b;
      return Color.HSVToColor(hsv);
    });

    final LinearLayout mandlebrotPanel = findViewById(R.id.mandlebrotPanel);
    LinearLayout colorTablePanel = findViewById(R.id.colorTablePanel);
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
    LinearLayout colorPanel = findViewById(R.id.colorPanel);
    hMin = findViewById(R.id.hMin);
    Button hMinInc = findViewById(R.id.hMinInc);
    Button hMinDec = findViewById(R.id.hMinDec);
    hMax = findViewById(R.id.hMax);
    Button hMaxInc = findViewById(R.id.hMaxInc);
    Button hMaxDec = findViewById(R.id.hMaxDec);
    hDelta = findViewById(R.id.hDelta);
    Button hDeltaInc = findViewById(R.id.hDeltaInc);
    Button hDeltaDec = findViewById(R.id.hDeltaDec);
    sMin = findViewById(R.id.sMin);
    Button sMinInc = findViewById(R.id.sMinInc);
    Button sMinDec = findViewById(R.id.sMinDec);
    sMax = findViewById(R.id.sMax);
    Button sMaxInc = findViewById(R.id.sMaxInc);
    Button sMaxDec = findViewById(R.id.sMaxDec);
    sDelta = findViewById(R.id.sDelta);
    Button sDeltaInc = findViewById(R.id.sDeltaInc);
    Button sDeltaDec = findViewById(R.id.sDeltaDec);
    vMin = findViewById(R.id.vMin);
    Button vMinInc = findViewById(R.id.vMinInc);
    Button vMinDec = findViewById(R.id.vMinDec);
    vMax = findViewById(R.id.vMax);
    Button vMaxInc = findViewById(R.id.vMaxInc);
    Button vMaxDec = findViewById(R.id.vMaxDec);
    vDelta = findViewById(R.id.vDelta);
    Button vDeltaInc = findViewById(R.id.vDeltaInc);
    Button vDeltaDec = findViewById(R.id.vDeltaDec);

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
        new LayoutParams(mandlebrotSize, mandlebrotSize));

    colorTableView = new ColorTableView(this, colorTable);
    LayoutParams layoutParams = new LayoutParams(colorTable.size(), 50 /* height */);
    layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
    colorTablePanel.addView(colorTableView, layoutParams);

    ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
        R.array.spinner_choices, R.layout.spinner);
    spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
    spinner.setAdapter(spinnerAdapter);
    spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        backPanZoomPanel.setVisibility(pos == SPINNER_POS_BACK_ZOOM_PAN ? View.VISIBLE : View.GONE);
        colorPanel.setVisibility(pos == SPINNER_POS_COLOR ? View.VISIBLE : View.GONE);
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

    hMin.setText("0"); // "180");
    hMin.addTextChangedListener(new TextWatcherAdapter() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        colorControlPanelChanged();
      }
    });
    hMinInc.setOnClickListener(view -> incDec(hMin, 1));
    hMinDec.setOnClickListener(view -> incDec(hMin, -1));
    hMax.setText("720"); // "270");
    hMax.addTextChangedListener(new TextWatcherAdapter() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        colorControlPanelChanged();
      }
    });
    hMaxInc.setOnClickListener(view -> incDec(hMax, 1));
    hMaxDec.setOnClickListener(view -> incDec(hMax, -1));
    hDelta.setText("1");
    hDelta.addTextChangedListener(new TextWatcherAdapter() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        colorControlPanelChanged();
      }
    });
    hDeltaInc.setOnClickListener(view -> incDec(hDelta, 1));
    hDeltaDec.setOnClickListener(view -> incDec(hDelta, -1));
    sMin.setText("70");
    sMin.addTextChangedListener(new TextWatcherAdapter() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        colorControlPanelChanged();
      }
    });
    sMinInc.setOnClickListener(view -> incDec(sMin, 1));
    sMinDec.setOnClickListener(view -> incDec(sMin, -1));
    sMax.setText("100");
    sMax.addTextChangedListener(new TextWatcherAdapter() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        colorControlPanelChanged();
      }
    });
    sMaxInc.setOnClickListener(view -> incDec(sMax, 1));
    sMaxDec.setOnClickListener(view -> incDec(sMax, -1));
    sDelta.setText("0");
    sDelta.addTextChangedListener(new TextWatcherAdapter() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        colorControlPanelChanged();
      }
    });
    sDeltaInc.setOnClickListener(view -> incDec(sDelta, 1));
    sDeltaDec.setOnClickListener(view -> incDec(sDelta, -1));
    vMin.setText("70");
    vMin.addTextChangedListener(new TextWatcherAdapter() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        colorControlPanelChanged();
      }
    });
    vMinInc.setOnClickListener(view -> incDec(vMin, 1));
    vMinDec.setOnClickListener(view -> incDec(vMin, -1));
    vMax.setText("100");
    vMax.addTextChangedListener(new TextWatcherAdapter() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        colorControlPanelChanged();
      }
    });
    vMaxInc.setOnClickListener(view -> incDec(vMax, 1));
    vMaxDec.setOnClickListener(view -> incDec(vMax, -1));
    vDelta.setText("0");
    vDelta.addTextChangedListener(new TextWatcherAdapter() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        colorControlPanelChanged();
      }
    });
    vDeltaInc.setOnClickListener(view -> incDec(vDelta, 1));
    vDeltaDec.setOnClickListener(view -> incDec(vDelta, -1));

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
    views.add(hMin);
    views.add(hMinInc);
    views.add(hMinDec);
    views.add(hMax);
    views.add(hMaxInc);
    views.add(hMaxDec);
    views.add(hDelta);
    views.add(hDeltaInc);
    views.add(hDeltaDec);
    views.add(sMin);
    views.add(sMinInc);
    views.add(sMinDec);
    views.add(sMax);
    views.add(sMaxInc);
    views.add(sMaxDec);
    views.add(sDelta);
    views.add(sDeltaInc);
    views.add(sDeltaDec);
    views.add(vMin);
    views.add(vMinInc);
    views.add(vMinDec);
    views.add(vMax);
    views.add(vMaxInc);
    views.add(vMaxDec);
    views.add(vDelta);
    views.add(vDeltaInc);
    views.add(vDeltaDec);

    fillColorTable();

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
        new ColorTable.Hue(valueOf(hMin), valueOf(hMax), valueOf(hDelta)),
        new ColorTable.Saturation(valueOf(sMin), valueOf(sMax), valueOf(sDelta)),
        new ColorTable.Brightness(valueOf(vMin), valueOf(vMax), valueOf(vDelta)));
  }

  private float valueOf(EditText editText) {
    float value = 0;
    try {
      value = Float.parseFloat(editText.getText().toString());
    } catch (NumberFormatException e) {
    }
    return constrainValue(editText, value);
  }

  private float constrainValue(EditText editText, float value) {
    if (editText == hMin) {
      return constrainValue(value, 0, 360);
    } else if (editText == hMax) {
      return constrainValue(value, 0, 720);
    } else if (editText == hDelta) {
      return constrainValue(value, 0, 360);
    } else if (editText == sMin) {
      return constrainValue(value, 0, 100);
    } else if (editText == sMax) {
      return constrainValue(value, 0, 100);
    } else if (editText == sDelta) {
      return constrainValue(value, 0, 100);
    } else if (editText == vMin) {
      return constrainValue(value, 0, 100);
    } else if (editText == vMax) {
      return constrainValue(value, 0, 100);
    } else if (editText == vDelta) {
      return constrainValue(value, 0, 100);
    }
    throw new AssertionError("editText is not recognized");
  }

  private static float constrainValue(float value, float min, float max) {
    if (value < min) {
      return min;
    } else if (value > max) {
      return max;
    }
    return value;
  }

  private void incDec(EditText editText, float delta) {
    float value = valueOf(editText);
    float newValue = constrainValue(editText, value + delta);
    String newText = Float.toString(newValue);
    if (newText.contains(".")) {
      while (newText.endsWith("0")) {
        newText = newText.substring(0, newText.length() - 1);
      }
      if (newText.endsWith(".")) {
        newText = newText.substring(0, newText.length() - 1);
      }
    }
    if (editText.getText().toString() != newText) {
      editText.setText(newText);
    }
  }

  private void colorControlPanelChanged() {
    fillColorTable();

    Bitmap bitmap = produceImage(mStack.peekLast());
    mandlebrotImageView.setImageBitmap(bitmap);

    colorTableView.invalidate();
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
