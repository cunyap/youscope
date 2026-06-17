/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Moritz Lang - initial API and implementation
 *     Andreas P. Cuny - Extended: absolute XY and Z position input with stage limit validation.
 ******************************************************************************/
package org.youscope.plugin.positioncontrol;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Point2D;
import java.text.ParseException;
import java.util.Formatter;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.youscope.addon.AddonException;
import org.youscope.addon.tool.ToolAddonUIAdapter;
import org.youscope.addon.tool.ToolMetadata;
import org.youscope.addon.tool.ToolMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrameListener;
import org.youscope.common.microscope.Device;
import org.youscope.common.microscope.DeviceType;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DoubleTextField;
import org.youscope.uielements.ImageLoadingTools;
import org.youscope.uielements.StandardFormats;

/**
 * Stage and focus position control tool.
 *
 * <p>
 * Extended with absolute-position input fields for XY and Z, and
 * optional stage-limit validation. When the microscope exposes min/max
 * limits for a stage axis (via {@code Device.getPropertyRange}) the user
 * input is checked before sending a movement command and the field is
 * coloured red if out of range.
 */
class PositionControl extends ToolAddonUIAdapter implements Runnable, YouScopeFrameListener {

    private final String xPositionText = "Current x position: ";
    private final String yPositionText = "Current y position: ";
    private final String focusPositionText = "Current focus position: ";

    private JLabel xPositionField = new JLabel(xPositionText + "unknown");
    private JLabel yPositionField = new JLabel(yPositionText + "unknown");
    private JLabel focusPositionField = new JLabel(focusPositionText + "unknown");

    private static final double INITIAL_MOVE_STEP_SIZE = 10;
    private static final double INITIAL_FOCUS_STEP_SIZE = 10;

    private DoubleTextField moveStepField = new DoubleTextField(INITIAL_MOVE_STEP_SIZE);
    private JSlider moveStepSlider = new JSlider(0, 50, 10);
    private DoubleTextField focusingStepField = new DoubleTextField(INITIAL_FOCUS_STEP_SIZE);
    private JSlider focusingStepSlider = new JSlider(0, 40, 10);

    /** Target X position field (um). */
    private final DoubleTextField goToXField = new DoubleTextField(0.0);
    /** Target Y position field (um). */
    private final DoubleTextField goToYField = new DoubleTextField(0.0);
    /** "Go to XY" button. */
    private final JButton goToXYButton = new JButton("Go to XY");
    /** Feedback label for XY absolute move. */
    private final JLabel goToXYFeedback = new JLabel(" ");

    /** Target Z (focus) position field (um). */
    private final DoubleTextField goToZField = new DoubleTextField(0.0);
    /** "Go to Z" button. */
    private final JButton goToZButton = new JButton("Go to Z");
    /** Feedback label for Z absolute move. */
    private final JLabel goToZFeedback = new JLabel(" ");

    // Stage limits (populated at startup if available)
    private double stageXMin = Double.NEGATIVE_INFINITY;
    private double stageXMax = Double.POSITIVE_INFINITY;
    private double stageYMin = Double.NEGATIVE_INFINITY;
    private double stageYMax = Double.POSITIVE_INFINITY;
    private double focusMin = Double.NEGATIVE_INFINITY;
    private double focusMax = Double.POSITIVE_INFINITY;

    private JComboBox<String> focusDevicesField = new JComboBox<String>();
    private volatile boolean isChangingMove = false;
    private volatile boolean isChangingFocus = false;

    private static final Color COLOR_OK = null; // default
    private static final Color COLOR_ERR = new Color(255, 200, 200);

    public static final String TYPE_IDENTIFIER = "YouScope.YouScopePositionControl";

    static ToolMetadata getMetadata() {
        return new ToolMetadataAdapter(TYPE_IDENTIFIER, "Stage and Focus Position", new String[0],
                "Allows to directly change stage and focus position.",
                "icons/marker.png");
    }

    public PositionControl(YouScopeClient client, YouScopeServer server) throws AddonException {
        super(getMetadata(), client, server);
    }

    @Override
    public java.awt.Component createUI() {
        setMaximizable(false);
        setResizable(true);
        setTitle("Stage and Focus Position");

        moveStepSlider.setValue((int) toSliderUnits(INITIAL_MOVE_STEP_SIZE));
        focusingStepSlider.setValue((int) toSliderUnits(INITIAL_FOCUS_STEP_SIZE));

        GridBagConstraints newLineConstr = StandardFormats.getNewLineConstraint();
        GridBagConstraints bottomConstr = StandardFormats.getBottomContstraint();

        Icon southIcon = ImageLoadingTools.getResourceIcon("bonus/icons-24/arrow-270.png", "S");
        Icon westIcon = ImageLoadingTools.getResourceIcon("bonus/icons-24/arrow-180.png", "W");
        Icon northIcon = ImageLoadingTools.getResourceIcon("bonus/icons-24/arrow-090.png", "N");
        Icon eastIcon = ImageLoadingTools.getResourceIcon("bonus/icons-24/arrow.png", "E");
        Icon focusInIcon = ImageLoadingTools.getResourceIcon("icons/arrow-step-out.png", "+");
        Icon focusOutIcon = ImageLoadingTools.getResourceIcon("icons/arrow-step.png", "-");

        // Stage position panel
        GridBagLayout posLayout = new GridBagLayout();
        JPanel posPanel = new JPanel(posLayout);
        posPanel.setBorder(new TitledBorder(new EtchedBorder(), "Stage Position"));

        StandardFormats.addGridBagElement(xPositionField, posLayout, newLineConstr, posPanel);
        StandardFormats.addGridBagElement(yPositionField, posLayout, newLineConstr, posPanel);

        // Step size
        StandardFormats.addGridBagElement(new JLabel("Move Step Size:"), posLayout, newLineConstr, posPanel);
        wireSliderAndField(moveStepSlider, moveStepField,
                new BooleanGetter() {
                    @Override
                    public boolean get() {
                        return isChangingMove;
                    }
                },
                new BooleanSetter() {
                    @Override
                    public void set(boolean v) {
                        isChangingMove = v;
                    }
                });
        JPanel moveStepPanel = new JPanel(new BorderLayout());
        moveStepPanel.add(moveStepField, BorderLayout.CENTER);
        moveStepPanel.add(new JLabel("um"), BorderLayout.EAST);
        StandardFormats.addGridBagElement(moveStepSlider, posLayout, newLineConstr, posPanel);
        StandardFormats.addGridBagElement(moveStepPanel, posLayout, newLineConstr, posPanel);

        // Arrow buttons
        StandardFormats.addGridBagElement(new JLabel("Move Stage:"), posLayout, newLineConstr, posPanel);
        JPanel arrows = new JPanel(new GridLayout(3, 3));
        arrows.add(new JPanel());
        arrows.add(makeArrow(northIcon, "^", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRelativePosition(0, -moveStepField.getValue());
            }
        }));
        arrows.add(new JPanel());
        arrows.add(makeArrow(westIcon, "<", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRelativePosition(-moveStepField.getValue(), 0);
            }
        }));
        arrows.add(new JPanel());
        arrows.add(makeArrow(eastIcon, ">", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRelativePosition(+moveStepField.getValue(), 0);
            }
        }));
        arrows.add(new JPanel());
        arrows.add(makeArrow(southIcon, "v", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRelativePosition(0, +moveStepField.getValue());
            }
        }));
        arrows.add(new JPanel());
        GridBagConstraints arrowConstr = new GridBagConstraints();
        arrowConstr.gridwidth = GridBagConstraints.REMAINDER;
        arrowConstr.anchor = GridBagConstraints.NORTHWEST;
        arrowConstr.gridx = 0;
        arrowConstr.weightx = 0;
        StandardFormats.addGridBagElement(arrows, posLayout, arrowConstr, posPanel);

        // Absolute XY input
        StandardFormats.addGridBagElement(
                new JLabel("Go to absolute XY position:"), posLayout, newLineConstr, posPanel);

        JPanel goXYRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        goXYRow.add(new JLabel("X:"));
        goToXField.setColumns(8);
        goXYRow.add(goToXField);
        goXYRow.add(new JLabel("um   Y:"));
        goToYField.setColumns(8);
        goXYRow.add(goToYField);
        goXYRow.add(new JLabel("um"));
        StandardFormats.addGridBagElement(goXYRow, posLayout, newLineConstr, posPanel);
        StandardFormats.addGridBagElement(goToXYButton, posLayout, newLineConstr, posPanel);
        StandardFormats.addGridBagElement(goToXYFeedback, posLayout, newLineConstr, posPanel);

        // Validate and wire XY
        goToXField.addFocusListener(makeValidateXY());
        goToYField.addFocusListener(makeValidateXY());
        goToXYButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveToAbsoluteXY();
            }
        });

        StandardFormats.addGridBagElement(new JPanel(), posLayout, bottomConstr, posPanel);

        // Focus panel
        GridBagLayout focusLayout = new GridBagLayout();
        JPanel focusPanel = new JPanel(focusLayout);
        focusPanel.setBorder(new TitledBorder(new EtchedBorder(), "Focus Position"));

        StandardFormats.addGridBagElement(focusPositionField, focusLayout, newLineConstr, focusPanel);
        StandardFormats.addGridBagElement(new JLabel("Focus Device:"), focusLayout, newLineConstr, focusPanel);
        StandardFormats.addGridBagElement(focusDevicesField, focusLayout, newLineConstr, focusPanel);

        StandardFormats.addGridBagElement(new JLabel("Focus Step Size:"), focusLayout, newLineConstr, focusPanel);
        wireSliderAndField(focusingStepSlider, focusingStepField,
                new BooleanGetter() {
                    @Override
                    public boolean get() {
                        return isChangingFocus;
                    }
                },
                new BooleanSetter() {
                    @Override
                    public void set(boolean v) {
                        isChangingFocus = v;
                    }
                });
        JPanel focusStepPanel = new JPanel(new BorderLayout());
        focusStepPanel.add(focusingStepField, BorderLayout.CENTER);
        focusStepPanel.add(new JLabel("um"), BorderLayout.EAST);
        StandardFormats.addGridBagElement(focusingStepSlider, focusLayout, newLineConstr, focusPanel);
        StandardFormats.addGridBagElement(focusStepPanel, focusLayout, newLineConstr, focusPanel);

        // Focus +/- buttons
        StandardFormats.addGridBagElement(new JLabel("Move Focus:"), focusLayout, newLineConstr, focusPanel);
        JPanel focusBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        focusBtns.add(makeArrow(focusInIcon, "+", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeFocus(+focusingStepField.getValue());
            }
        }));
        focusBtns.add(makeArrow(focusOutIcon, "-", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeFocus(-focusingStepField.getValue());
            }
        }));
        StandardFormats.addGridBagElement(focusBtns, focusLayout, newLineConstr, focusPanel);

        // Absolute Z input
        StandardFormats.addGridBagElement(
                new JLabel("Go to absolute Z position:"), focusLayout, newLineConstr, focusPanel);
        JPanel goZRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        goZRow.add(new JLabel("Z:"));
        goToZField.setColumns(8);
        goZRow.add(goToZField);
        goZRow.add(new JLabel("um"));
        StandardFormats.addGridBagElement(goZRow, focusLayout, newLineConstr, focusPanel);
        StandardFormats.addGridBagElement(goToZButton, focusLayout, newLineConstr, focusPanel);
        StandardFormats.addGridBagElement(goToZFeedback, focusLayout, newLineConstr, focusPanel);

        goToZField.addFocusListener(makeValidateZ());
        goToZButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveToAbsoluteZ();
            }
        });

        StandardFormats.addGridBagElement(new JPanel(), focusLayout, bottomConstr, focusPanel);

        // Load data and start polling
        loadFocusDevices();
        loadStageLimits();

        getContainingFrame().addFrameListener(this);
        if (getContainingFrame().isVisible())
            new Thread(this, "Current Position Poller").start();

        JPanel content = new JPanel(new BorderLayout());
        content.add(posPanel, BorderLayout.WEST);
        content.add(focusPanel, BorderLayout.EAST);
        return content;
    }

    private interface BooleanGetter {
        boolean get();
    }

    private interface BooleanSetter {
        void set(boolean v);
    }

    private void wireSliderAndField(final JSlider slider, final DoubleTextField field,
            final BooleanGetter guard, final BooleanSetter setter) {
        slider.setPaintTicks(true);
        slider.setSnapToTicks(true);
        slider.setMinorTickSpacing(1);
        slider.setMajorTickSpacing(10);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (guard.get())
                    return;
                setter.set(true);
                double raw = fromSliderUnits(slider.getValue());
                double nice = raw > 2 ? Math.round(raw) : Math.round(10.0 * raw) / 10.0;
                field.setValue(nice);
                setter.set(false);
            }
        });
        field.setMinimalValue(0.0);
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commit();
            }

            private void commit() {
                if (guard.get())
                    return;
                try {
                    field.commitEdit();
                } catch (ParseException ignored) {
                }
                setter.set(true);
                slider.setValue((int) toSliderUnits(field.getValue()));
                setter.set(false);
            }
        };
        FocusListener fl = new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (guard.get())
                    return;
                try {
                    field.commitEdit();
                } catch (ParseException ignored) {
                }
                setter.set(true);
                slider.setValue((int) toSliderUnits(field.getValue()));
                setter.set(false);
            }
        };
        field.addActionListener(al);
        field.addFocusListener(fl);
    }

    // Absolute XY movement
    private void moveToAbsoluteXY() {
        final double x = goToXField.getValue();
        final double y = goToYField.getValue();

        String err = validateXY(x, y);
        if (err != null) {
            setFeedback(goToXYFeedback, err, true);
            return;
        }
        goToXYButton.setEnabled(false);
        setFeedback(goToXYFeedback, "Moving?", false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getMicroscope().getStageDevice().setPosition(x, y);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setFeedback(goToXYFeedback, "Moved to (" + fmt(x) + ", " + fmt(y) + ") um", false);
                            goToXYButton.setEnabled(true);
                        }
                    });
                } catch (final Exception ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setFeedback(goToXYFeedback, "Error: " + ex.getMessage(), true);
                            goToXYButton.setEnabled(true);
                        }
                    });
                    sendErrorMessage("Could not move stage to absolute position.", ex);
                }
            }
        }, "AbsoluteXYMover").start();
    }

    /** Returns a validation error string, or null if valid. */
    private String validateXY(double x, double y) {
        if (x < stageXMin || x > stageXMax)
            return String.format("X=%.2f out of range [%.2f, %.2f] um", x, stageXMin, stageXMax);
        if (y < stageYMin || y > stageYMax)
            return String.format("Y=%.2f out of range [%.2f, %.2f] um", y, stageYMin, stageYMax);
        return null;
    }

    private FocusListener makeValidateXY() {
        return new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                boolean xOk = goToXField.getValue() >= stageXMin && goToXField.getValue() <= stageXMax;
                boolean yOk = goToYField.getValue() >= stageYMin && goToYField.getValue() <= stageYMax;
                goToXField.setBackground(xOk ? null : COLOR_ERR);
                goToYField.setBackground(yOk ? null : COLOR_ERR);
                goToXYButton.setEnabled(xOk && yOk);
                if (!xOk || !yOk)
                    setFeedback(goToXYFeedback, "Value out of valid range", true);
                else
                    setFeedback(goToXYFeedback, " ", false);
            }
        };
    }

    // Absolute Z movement
    private void moveToAbsoluteZ() {
        if (focusDevicesField.getItemCount() == 0) {
            setFeedback(goToZFeedback, "No focus device available.", true);
            return;
        }
        final double z = goToZField.getValue();
        String err = validateZ(z);
        if (err != null) {
            setFeedback(goToZFeedback, err, true);
            return;
        }
        final String focusDev = focusDevicesField.getSelectedItem().toString();
        goToZButton.setEnabled(false);
        setFeedback(goToZFeedback, "Moving?", false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getMicroscope().getFocusDevice(focusDev).setFocusPosition(z);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setFeedback(goToZFeedback, "Moved to Z=" + fmt(z) + " um", false);
                            goToZButton.setEnabled(true);
                        }
                    });
                } catch (final Exception ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setFeedback(goToZFeedback, "Error: " + ex.getMessage(), true);
                            goToZButton.setEnabled(true);
                        }
                    });
                    sendErrorMessage("Could not move focus to absolute position.", ex);
                }
            }
        }, "AbsoluteZMover").start();
    }

    private String validateZ(double z) {
        if (z < focusMin || z > focusMax)
            return String.format("Z=%.2f out of range [%.2f, %.2f] um", z, focusMin, focusMax);
        return null;
    }

    private FocusListener makeValidateZ() {
        return new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                boolean ok = goToZField.getValue() >= focusMin && goToZField.getValue() <= focusMax;
                goToZField.setBackground(ok ? null : COLOR_ERR);
                goToZButton.setEnabled(ok);
                setFeedback(goToZFeedback, ok ? " " : "Value out of valid range", !ok);
            }
        };
    }

    // Stage limit loading
    /**
     * Attempts to read stage and focus axis limits from the microscope.
     * Many microscopes/drivers do not expose limits; failures are silently
     * ignored and the fields remain unbounded.
     */
    private void loadStageLimits() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // XY stage
                try {
                    Device stage = getMicroscope().getStageDevice();
                    // Try standard MicroManager property names
                    tryLoadLimit(stage, "MinX", new DoubleConsumer() {
                        @Override
                        public void accept(double v) {
                            stageXMin = v;
                        }
                    });
                    tryLoadLimit(stage, "MaxX", new DoubleConsumer() {
                        @Override
                        public void accept(double v) {
                            stageXMax = v;
                        }
                    });
                    tryLoadLimit(stage, "MinY", new DoubleConsumer() {
                        @Override
                        public void accept(double v) {
                            stageYMin = v;
                        }
                    });
                    tryLoadLimit(stage, "MaxY", new DoubleConsumer() {
                        @Override
                        public void accept(double v) {
                            stageYMax = v;
                        }
                    });
                } catch (Exception ignored) {
                    /* no stage or no limits */ }

                // Focus device
                try {
                    if (focusDevicesField.getItemCount() > 0) {
                        Device focus = getMicroscope().getFocusDevice(
                                focusDevicesField.getItemAt(0));
                        tryLoadLimit(focus, "Min", new DoubleConsumer() {
                            @Override
                            public void accept(double v) {
                                focusMin = v;
                            }
                        });
                        tryLoadLimit(focus, "Max", new DoubleConsumer() {
                            @Override
                            public void accept(double v) {
                                focusMax = v;
                            }
                        });
                    }
                } catch (Exception ignored) {
                }
            }
        }, "StageLimitLoader").start();
    }

    private interface DoubleConsumer {
        void accept(double v);
    }

    private void tryLoadLimit(Device device, String propName, DoubleConsumer setter) {
        try {
            String val = device.getProperty(propName).getValue();
            setter.accept(Double.parseDouble(val));
        } catch (Exception ignored) {
        }
    }

    // Position polling (Java 7 compatible)
    @Override
    public void run() {
        while (getContainingFrame().isVisible()) {
            try {
                Point2D.Double pos = getMicroscope().getStageDevice().getPosition();
                try (Formatter f = new Formatter()) {
                    xPositionField.setText(xPositionText + f.format("%2.2f um", pos.x));
                }
                try (Formatter f = new Formatter()) {
                    yPositionField.setText(yPositionText + f.format("%2.2f um", pos.y));
                }
                if (focusDevicesField.getItemCount() > 0) {
                    double fp = getMicroscope().getFocusDevice(
                            focusDevicesField.getSelectedItem().toString()).getFocusPosition();
                    try (Formatter f = new Formatter()) {
                        focusPositionField.setText(focusPositionText + f.format("%2.2f um", fp));
                    }
                } else {
                    focusPositionField.setText(focusPositionText + "unknown");
                }
            } catch (Exception e) {
                sendErrorMessage("Could not obtain current microscope position. Stopping.", e);
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public void frameClosed() {
    }

    @Override
    public void frameOpened() {
        new Thread(this, "Current Position Poller").start();
    }

    // Relative movement helpers (unchanged)
    private void setRelativePosition(double dx, double dy) {
        try {
            getMicroscope().getStageDevice().setRelativePosition(dx, dy);
        } catch (Exception e) {
            sendErrorMessage("Could not set stage position.", e);
        }
    }

    private void changeFocus(double delta) {
        try {
            if (focusDevicesField.getItemCount() > 0)
                getMicroscope().getFocusDevice(
                        focusDevicesField.getSelectedItem().toString())
                        .setRelativeFocusPosition(delta);
        } catch (Exception e) {
            sendErrorMessage("Could not set focus position.", e);
        }
    }

    private void loadFocusDevices() {
        String[] devs = null;
        try {
            Device[] devices = getMicroscope().getFocusDevices();
            devs = new String[devices.length];
            for (int i = 0; i < devices.length; i++)
                devs[i] = devices[i].getDeviceID();
        } catch (Exception e) {
            sendErrorMessage("Could not obtain focus device names.", e);
        }
        focusDevicesField.removeAllItems();
        if (devs != null)
            for (String d : devs)
                focusDevicesField.addItem(d);
    }

    // Small helpers
    private static double toSliderUnits(double v) {
        return 10.0 * Math.log10(10.0 * v);
    }

    private static double fromSliderUnits(double s) {
        return 0.1 * Math.pow(10.0, s / 10.0);
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static void setFeedback(JLabel label, String msg, boolean error) {
        label.setText(msg);
        label.setForeground(error ? Color.RED : Color.DARK_GRAY);
    }

    private static JButton makeArrow(Icon icon, String fallback, ActionListener al) {
        JButton btn = icon == null ? new JButton(fallback) : new JButton(icon);
        btn.setMargin(new Insets(1, 1, 1, 1));
        btn.addActionListener(al);
        return btn;
    }
}