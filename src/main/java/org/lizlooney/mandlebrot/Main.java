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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main {
  public static final int SIZE = 1000;
  private static final int PAN_CENTER = SIZE / 2;
  private static final int PAN_UP = SIZE / 10;
  private static final int PAN_DOWN = SIZE * 9 / 10;
  private static final int PAN_LEFT = SIZE / 10;
  private static final int PAN_RIGHT = SIZE * 9 / 10;
  private static final double ZOOM_IN = 0.25;
  private static final double ZOOM_OUT = 4;

  private final JFrame frame = new JFrame("Mandlebrot");
  private final JButton backButton = new JButton("Back");
  private final JButton upLeftButton = new JButton("\u2196");
  private final JButton upButton = new JButton("\u2191");
  private final JButton upRightButton = new JButton("\u2197");
  private final JButton leftButton = new JButton("\u2190");
  private final JButton rightButton = new JButton("\u2192");
  private final JButton downLeftButton = new JButton("\u2199");
  private final JButton downButton = new JButton("\u2193");
  private final JButton downRightButton = new JButton("\u2198");
  private final JButton zoomOutButton = new JButton(new String(Character.toChars(0x1f50d)) + "-");
  private final JButton zoomInButton = new JButton(new String(Character.toChars(0x1f50d)) + "+");
  private final JSpinner hStart = new JSpinner(new SpinnerNumberModel(0, 0, 360, 1));
  private final JSpinner hMin = new JSpinner(new SpinnerNumberModel(0, 0, 360, 1));
  private final JSpinner hMax = new JSpinner(new SpinnerNumberModel(360, 0, 720, 1));
  private final JSpinner hDelta = new JSpinner(new SpinnerNumberModel(1, 0, 360, 1));
  private final JSpinner sStart = new JSpinner(new SpinnerNumberModel(70, 0, 100, 1));
  private final JSpinner sMin = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
  private final JSpinner sMax = new JSpinner(new SpinnerNumberModel(100, 0, 100, 1));
  private final JSpinner sDelta = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
  private final JSpinner bStart = new JSpinner(new SpinnerNumberModel(70, 0, 100, 1));
  private final JSpinner bMin = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
  private final JSpinner bMax = new JSpinner(new SpinnerNumberModel(100, 0, 100, 1));
  private final JSpinner bDelta = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
  private final JPanel mandlebrotPanel = new MandlebrotPanel();
  private final JLabel mandlebrotLabel = new JLabel();
  private final JButton saveFileButton = new JButton("Save image file");
  private final ColorTable colorTable;
  private final Deque<Mandlebrot> mStack = new ArrayDeque<>();
  private RenderedImage renderedImage;
  private final List<JComponent> components = new ArrayList<>();

  Main() {
    colorTable = new ColorTable(Mandlebrot.MAX_VALUE, (h, s, b) -> Color.HSBtoRGB((float) h, (float) s, (float) b));
    fillColorTable();
    mStack.addLast(new Mandlebrot(SIZE, 0, 0, 4));

    components.add(backButton);
    components.add(upLeftButton);
    components.add(upButton);
    components.add(upRightButton);
    components.add(leftButton);
    components.add(rightButton);
    components.add(downLeftButton);
    components.add(downButton);
    components.add(downRightButton);
    components.add(zoomOutButton);
    components.add(zoomInButton);
    components.add(hStart);
    components.add(hMin);
    components.add(hMax);
    components.add(hDelta);
    components.add(sStart);
    components.add(sMin);
    components.add(sMax);
    components.add(sDelta);
    components.add(bStart);
    components.add(bMin);
    components.add(bMax);
    components.add(bDelta);
    components.add(mandlebrotPanel);
    components.add(saveFileButton);
  }

  private void addListeners() {
    backButton.addActionListener(event -> {
      if (mStack.size() > 1) {
        mStack.removeLast();
        onMandlebrotChanged(true);
      }
    });
    upLeftButton.addActionListener(event -> pan(PAN_LEFT, PAN_UP));
    upButton.addActionListener(event -> pan(PAN_CENTER, PAN_UP));
    upRightButton.addActionListener(event -> pan(PAN_RIGHT, PAN_UP));
    leftButton.addActionListener(event -> pan(PAN_LEFT, PAN_CENTER));
    rightButton.addActionListener(event -> pan(PAN_RIGHT, PAN_CENTER));
    downLeftButton.addActionListener(event -> pan(PAN_LEFT, PAN_DOWN));
    downButton.addActionListener(event -> pan(PAN_CENTER, PAN_DOWN));
    downRightButton.addActionListener(event -> pan(PAN_RIGHT, PAN_DOWN));
    zoomOutButton.addActionListener(event -> zoom(ZOOM_OUT));
    zoomInButton.addActionListener(event -> zoom(ZOOM_IN));

    hStart.addChangeListener(event -> colorControlPanelChanged());
    hMin.addChangeListener(event -> colorControlPanelChanged());
    hMax.addChangeListener(event -> colorControlPanelChanged());
    hDelta.addChangeListener(event -> colorControlPanelChanged());
    sStart.addChangeListener(event -> colorControlPanelChanged());
    sMin.addChangeListener(event -> colorControlPanelChanged());
    sMax.addChangeListener(event -> colorControlPanelChanged());
    sDelta.addChangeListener(event -> colorControlPanelChanged());
    bStart.addChangeListener(event -> colorControlPanelChanged());
    bMin.addChangeListener(event -> colorControlPanelChanged());
    bMax.addChangeListener(event -> colorControlPanelChanged());
    bDelta.addChangeListener(event -> colorControlPanelChanged());

    mandlebrotPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        if (!mandlebrotPanel.isEnabled()) {
          return;
        }
        if (event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
          new PanZoomWorker(event.getX(), event.getY(), ZOOM_IN).execute();
        }
      }
    });

    saveFileButton.addActionListener(event -> {
      if (renderedImage != null) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif"));
        int state = chooser.showSaveDialog(frame);
        if (state == JFileChooser.APPROVE_OPTION) {
          File file = chooser.getSelectedFile();
          String name = file.getName();
          String formatName = "JPEG";
          if (name.endsWith(".png")) {
            formatName = "PNG";
          } else if (name.endsWith(".gif")) {
            formatName = "GIF";
          }
          try {
            ImageIO.write(renderedImage, formatName, file);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    });
  }

  private void pan(int x, int y) {
    new PanZoomWorker(x, y, 1.0).execute();
  }

  private void zoom(double zoomFactor) {
    new PanZoomWorker(PAN_CENTER, PAN_CENTER, zoomFactor).execute();
  }

  private void colorControlPanelChanged() {
    fillColorTable();
    mandlebrotPanel.repaint(0L, 0, 0, SIZE, SIZE);
  }

  class PanZoomWorker extends SwingWorker<Mandlebrot, Object> {
    private final int x;
    private final int y;
    private final double zoomFactor;
    private final Mandlebrot mBeforeZoom;
    private final List<JComponent> disabledComponents;

    PanZoomWorker(int x, int y, double zoomFactor) {
      this.x = x;
      this.y = y;
      this.zoomFactor = zoomFactor;
      mBeforeZoom = mStack.peekLast();
      disabledComponents = disableUI();
    }

    @Override
    public Mandlebrot doInBackground() {
      return mBeforeZoom.panZoom(x, y, zoomFactor);
    }

    @Override
    protected void done() {
      enableUI(disabledComponents);
      try {
        mStack.addLast(get());
        onMandlebrotChanged(true);
      } catch (ExecutionException | InterruptedException e) {
        e.printStackTrace();
      }
    }

    private List<JComponent> disableUI() {
      frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      List<JComponent> disabledComponents = new ArrayList<>();
      for (JComponent component : components) {
        if (component.isEnabled()) {
          component.setEnabled(false);
          disabledComponents.add(component);
        }
      }
      return disabledComponents;
    }

    private void enableUI(List<JComponent> disabledComponents) {
      for (JComponent component  : disabledComponents) {
        component.setEnabled(true);
      }
      frame.setCursor(Cursor.getDefaultCursor());
    }
  }

  private void onMandlebrotChanged(boolean repaint) {
    backButton.setEnabled(mStack.size() > 1);
    mandlebrotLabel.setText(mStack.peekLast().toString());
    if (repaint) {
      mandlebrotPanel.repaint(0L, 0, 0, SIZE, SIZE);
    }
  }

  private void show() {
    JPanel navigationPanel = createNavigationPanel();
    JPanel colorControlPanel = createColorControlPanel();

    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    frame.setLayout(gridbag);
    // Back button
    c.gridwidth = 1;
    gridbag.setConstraints(backButton, c);
    frame.add(backButton);
    // Navigation panel
    gridbag.setConstraints(navigationPanel, c);
    frame.add(navigationPanel);
    // Color control panel
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(colorControlPanel, c);
    frame.add(colorControlPanel);
    // Mandlebrot panel
    c.fill = GridBagConstraints.BOTH;
    mandlebrotPanel.setPreferredSize(new Dimension(SIZE, SIZE));
    gridbag.setConstraints(mandlebrotPanel, c);
    frame.add(mandlebrotPanel);
    // Mandlebrot label
    c.fill = GridBagConstraints.HORIZONTAL;
    mandlebrotLabel.setHorizontalAlignment(SwingConstants.CENTER);
    gridbag.setConstraints(mandlebrotLabel, c);
    frame.add(mandlebrotLabel);
    // Save file button
    c.fill = GridBagConstraints.NONE;
    gridbag.setConstraints(saveFileButton, c);
    frame.add(saveFileButton);

    onMandlebrotChanged(false);

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(1020, 1250);
    frame.setVisible(true);
  }

  private JPanel createNavigationPanel() {
    JPanel navigationPanel = new JPanel();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    navigationPanel.setLayout(gridbag);
    c.gridwidth = 1;
    gridbag.setConstraints(upLeftButton, c);
    navigationPanel.add(upLeftButton);
    gridbag.setConstraints(upButton, c);
    navigationPanel.add(upButton);
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(upRightButton, c);
    navigationPanel.add(upRightButton);
    c.gridwidth = 1;
    gridbag.setConstraints(leftButton, c);
    navigationPanel.add(leftButton);
    JLabel spacer = new JLabel();
    gridbag.setConstraints(spacer, c);
    navigationPanel.add(spacer);
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(rightButton, c);
    navigationPanel.add(rightButton);
    c.gridwidth = 1;
    gridbag.setConstraints(downLeftButton, c);
    navigationPanel.add(downLeftButton);
    gridbag.setConstraints(downButton, c);
    navigationPanel.add(downButton);
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(downRightButton, c);
    navigationPanel.add(downRightButton);

    c.gridwidth = 1;
    gridbag.setConstraints(zoomOutButton, c);
    navigationPanel.add(zoomOutButton);
    gridbag.setConstraints(zoomInButton, c);
    navigationPanel.add(zoomInButton);

    return navigationPanel;
  }

  private JPanel createColorControlPanel() {
    JPanel colorControlPanel = new JPanel();
    colorControlPanel.setBorder(new TitledBorder("Color Control Panel"));
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    colorControlPanel.setLayout(gridbag);
    c.gridwidth = 1;
    JLabel label = new JLabel("");
    gridbag.setConstraints(label, c);
    colorControlPanel.add(label);
    label = new JLabel("Start");
    gridbag.setConstraints(label, c);
    colorControlPanel.add(label);
    label = new JLabel("Min");
    gridbag.setConstraints(label, c);
    colorControlPanel.add(label);
    label = new JLabel("Max");
    gridbag.setConstraints(label, c);
    colorControlPanel.add(label);
    label = new JLabel("Delta");
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(label, c);
    colorControlPanel.add(label);

    c.gridwidth = 1;
    label = new JLabel("Hue");
    gridbag.setConstraints(label, c);
    colorControlPanel.add(label);
    // Hue initial value
    gridbag.setConstraints(hStart, c);
    colorControlPanel.add(hStart);
    // Hue minimum
    gridbag.setConstraints(hMin, c);
    colorControlPanel.add(hMin);
    // Hue maximum
    gridbag.setConstraints(hMax, c);
    colorControlPanel.add(hMax);
    // Hue delta
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(hDelta, c);
    colorControlPanel.add(hDelta);

    c.gridwidth = 1;
    label = new JLabel("Saturation");
    gridbag.setConstraints(label, c);
    colorControlPanel.add(label);
    // Saturation initial value
    gridbag.setConstraints(sStart, c);
    colorControlPanel.add(sStart);
    // Saturation minimum
    gridbag.setConstraints(sMin, c);
    colorControlPanel.add(sMin);
    // Saturation maximum
    gridbag.setConstraints(sMax, c);
    colorControlPanel.add(sMax);
    // Saturation delta
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(sDelta, c);
    colorControlPanel.add(sDelta);

    c.gridwidth = 1;
    label = new JLabel("Brightness");
    gridbag.setConstraints(label, c);
    colorControlPanel.add(label);
    // Brightness initial value
    gridbag.setConstraints(bStart, c);
    colorControlPanel.add(bStart);
    // Brightness minimum
    gridbag.setConstraints(bMin, c);
    colorControlPanel.add(bMin);
    // Brightness maximum
    gridbag.setConstraints(bMax, c);
    colorControlPanel.add(bMax);
    // Brightness delta
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(bDelta, c);
    colorControlPanel.add(bDelta);

    return colorControlPanel;
  }

  private void fillColorTable() {
    colorTable.fill(
        new ColorTable.Hue(valueOf(hStart), valueOf(hMin), valueOf(hMax), valueOf(hDelta)),
        new ColorTable.Saturation(valueOf(sStart), valueOf(sMin), valueOf(sMax), valueOf(sDelta)),
        new ColorTable.Brightness(valueOf(bStart), valueOf(bMin), valueOf(bMax), valueOf(bDelta)));
  }

  private static int valueOf(JSpinner spinner) {
    return ((Integer) spinner.getValue()).intValue();
  }

  class MandlebrotPanel extends JPanel {
    @Override
    public void paint(Graphics g) {
      Graphics2D g2d = (Graphics2D) g;
      renderedImage = produceImage(mStack.peekLast());
      g2d.drawRenderedImage(renderedImage, new AffineTransform());
    }

    public RenderedImage produceImage(Mandlebrot m) {
      final BufferedImage bi = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
      m.accept((x, y, value) -> {
        bi.setRGB(x, y, colorTable.valueToColor(value));
      });
      return bi;
    }
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        Main main = new Main();
        main.addListeners();
        main.show();
      }
    });
  }
}
