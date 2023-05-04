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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.JButton;
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
  private final JFrame frame = new JFrame("Mandlebrot");
  private final JButton backButton = new JButton("Back");
  private final JSpinner hStart = new JSpinner(new SpinnerNumberModel(240, 0, 360, 1));
  private final JSpinner hMin = new JSpinner(new SpinnerNumberModel(0, 0, 360, 1));
  private final JSpinner hMax = new JSpinner(new SpinnerNumberModel(360, 0, 360, 1));
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
  private final int[] colorTable;
  private final Deque<Mandlebrot> mStack = new ArrayDeque<>();;
  private RenderedImage renderedImage;


  Main() {
    colorTable = new int[Mandlebrot.MAX_VALUE];
    fillColorTable();
    mStack.addLast(new Mandlebrot(0, 0, 4));
  }

  private void addListeners() {
    ChangeListener spinnerChangeListener = new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        fillColorTable();
        mandlebrotPanel.repaint(0L, 0, 0, 1000, 1000);
      }
    };
    hStart.addChangeListener(spinnerChangeListener);
    hMin.addChangeListener(spinnerChangeListener);
    hMax.addChangeListener(spinnerChangeListener);
    hDelta.addChangeListener(spinnerChangeListener);
    sStart.addChangeListener(spinnerChangeListener);
    sMin.addChangeListener(spinnerChangeListener);
    sMax.addChangeListener(spinnerChangeListener);
    sDelta.addChangeListener(spinnerChangeListener);
    bStart.addChangeListener(spinnerChangeListener);
    bMin.addChangeListener(spinnerChangeListener);
    bMax.addChangeListener(spinnerChangeListener);
    bDelta.addChangeListener(spinnerChangeListener);

    mandlebrotPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          ZoomWorker worker = new ZoomWorker(mStack.peekLast(), e.getX(), e.getY());
          worker.execute();
        }
      }
    });

    backButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        if (mStack.size() > 1) {
          mStack.removeLast();
          onMandlebrotChanged();
        }
      }
    });

    saveFileButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
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
      }
    });
  }

  class ZoomWorker extends SwingWorker<Mandlebrot, Object> {
    private final Mandlebrot mBeforeZoom;
    private final int x;
    private final int y;

    ZoomWorker(Mandlebrot mBeforeZoom, int x, int y) {
      this.mBeforeZoom = mBeforeZoom;
      this.x = x;
      this.y = y;
      mandlebrotPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    @Override
    public Mandlebrot doInBackground() {
      return mBeforeZoom.zoom(x, y);
    }

    @Override
    protected void done() {
      try {
        mStack.addLast(get());
        onMandlebrotChanged();
      } catch (ExecutionException | InterruptedException e) {
        e.printStackTrace();
      } finally {
        mandlebrotPanel.setCursor(Cursor.getDefaultCursor());
      }
    }
  }

  private void onMandlebrotChanged() {
    mandlebrotLabel.setText(mStack.peekLast().toString());
    mandlebrotPanel.repaint(0L, 0, 0, 1000, 1000);
  }

  private void show() {
    JPanel colorControlPanel = new JPanel();
    colorControlPanel.setBorder(new TitledBorder("Color Control Panel"));
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    colorControlPanel.setLayout(gridbag);
    c.gridwidth = 1;
    c.gridheight = 1;
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

    gridbag = new GridBagLayout();
    c = new GridBagConstraints();
    frame.setLayout(gridbag);
    // Back button
    c.gridwidth = 1;
    gridbag.setConstraints(backButton, c);
    frame.add(backButton);
    // Color control panel
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(colorControlPanel, c);
    frame.add(colorControlPanel);
    // Mandlebrot panel
    c.fill = GridBagConstraints.BOTH;
    mandlebrotPanel.setPreferredSize(new Dimension(1000, 1000));
    gridbag.setConstraints(mandlebrotPanel, c);
    frame.add(mandlebrotPanel);
    // Mandlebrot label
    c.fill = GridBagConstraints.HORIZONTAL;
    mandlebrotLabel.setHorizontalAlignment(SwingConstants.CENTER);
    mandlebrotLabel.setText(mStack.peekLast().toString());
    gridbag.setConstraints(mandlebrotLabel, c);
    frame.add(mandlebrotLabel);
    // Save file button
    c.fill = GridBagConstraints.NONE;
    gridbag.setConstraints(saveFileButton, c);
    frame.add(saveFileButton);

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(1020, 1250);
    frame.setVisible(true);
  }

  private void fillColorTable() {
    ColorTable.fill(colorTable,
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
      renderedImage = mStack.peekLast().produceImage(colorTable);
      g2d.drawRenderedImage(renderedImage, new AffineTransform());
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
