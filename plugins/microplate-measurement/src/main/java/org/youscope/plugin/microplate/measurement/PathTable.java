/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 OR the GNU Lesser General Public
 * License, Version 2.1 (or at your option any later version).
 *
 * You may choose which license you wish to use when copying, modifying,
 * or distributing this software.
 *
 * Licenses are available at:
 * - Apache 2.0: http://apache.org
 * - LGPL 2.1:   https://gnu.org
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-2.1-or-later
 *
 * Contributors:
 *     Moritz Lang - initial API and implementation
 *     Andreas P. Cuny - add sync between selected and configured positions
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.microplate.measurement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import javax.swing.AbstractCellEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.PositionInformation;
import org.youscope.common.Well;
import org.youscope.common.microplate.MicroplateLayout;
import org.youscope.common.microplate.WellLayout;
import org.youscope.common.microplate.WellWithGroup;
import org.youscope.uielements.DoubleTextField;
import org.youscope.uielements.DynamicPanel;
import org.youscope.uielements.ImageLoadingTools;

/**
 * Table to show and set configured positions.
 *
 * <p>
 * Two editing modes are supported, selected implicitly by the columns passed to
 * the constructor:
 * </p>
 * <ul>
 * <li><b>Microplate mode</b> (a {@link Column#WELL} column is present and a
 * non-null {@link MicroplateLayout} is
 * supplied): manual positions may only be added for wells that actually exist
 * in the plate definition. The
 * X/Y coordinates are derived automatically from the plate layout and are not
 * editable in the add dialog
 * (they remain editable in the fine-configuration wizard). Each added position
 * also carries a well group, so
 * that the selected-wells set and the configured positions stay
 * consistent.</li>
 * <li><b>Custom-position mode</b> (a {@link Column#MAIN_POSITION} column is
 * present and the layout is null):
 * arbitrary X/Y positions may be added freely, as before.</li>
 * </ul>
 *
 * @author Moritz Lang, Andreas P. Cuny
 *
 */
public class PathTable extends JPanel {
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 6131526938094793770L;
	private final JTable pathTable;
	private final PathTableModel pathTableModel;
	private final ArrayList<PositionInformation> positionInformations;
	private final HashMap<PositionInformation, XYAndFocusPosition> configuredPositions;
	private final ArrayList<ActionListener> layoutChangedListeners = new ArrayList<>(1);
	private final YouScopeFrame frame;
	private final Column[] columns;

	// Context needed to keep wells, groups and positions in sync (may be null
	// in custom-position mode).
	private final MicroplateLayout microplateLayout;
	private final TileConfiguration tileConfiguration;
	/**
	 * Authoritative selected-wells set (shared reference with {@code PathPage}).
	 * Manual add/delete operations in
	 * microplate mode keep this set in sync with the configured positions. Never
	 * null; empty in custom mode.
	 */
	private final Set<WellWithGroup> selectedWells;
	/**
	 * Groups a manually added well may be assigned to. Typically the groups already
	 * used in the experiment plus
	 * {@link WellWithGroup.WellGroup#GROUP0}. Never null.
	 */
	private final WellWithGroup.WellGroup[] availableGroups;

	/**
	 * Columns in the path table.
	 */
	public enum Column {
		/**
		 * Position of a custom positions.
		 */
		MAIN_POSITION("Position", String.class, false),
		/**
		 * Well
		 */
		WELL("Well", String.class, false),
		/**
		 * Group of the well.
		 */
		GROUP("Group", String.class, false),
		/**
		 * Tile
		 */
		TILE("Tile", String.class, false),
		/**
		 * X-position in um
		 */
		X("X-Position (um)", Double.class, true),
		/**
		 * y-position in um.
		 */
		Y("Y-Position (um)", Double.class, true),
		/**
		 * focus in um.
		 */
		FOCUS("Focus (um)", Double.class, true);

		private final String name;
		private final Class<?> type;
		private final boolean editable;

		Column(String name, Class<?> type, boolean editable) {
			this.name = name;
			this.type = type;
			this.editable = editable;
		}

		@Override
		public String toString() {
			return name;
		}

		/**
		 * Returns true if the column can be edited.
		 * 
		 * @return true if editable
		 */
		public boolean isEdiable() {
			return editable;
		}

		/**
		 * Returns the class of content stored in this column.
		 * 
		 * @return content type.
		 */
		public Class<?> getType() {
			return type;
		}
	}

	/**
	 * Backwards compatible constructor for the custom-position case (no microplate
	 * layout, no well/group sync).
	 * 
	 * @param configuredPositions Hash map of already configured positions. Can be
	 *                            empty.
	 * @param frame               The containing frame.
	 * @param columns             The columns which should show up in the table.
	 */
	public PathTable(HashMap<PositionInformation, XYAndFocusPosition> configuredPositions, YouScopeFrame frame,
			Column... columns) {
		this(configuredPositions, null, null, null, null, frame, columns);
	}

	/**
	 * Constructor.
	 * 
	 * @param configuredPositions Hash map of already configured positions. Can be
	 *                            empty.
	 * @param microplateLayout    Layout of the microplate, or null for arbitrary
	 *                            (custom) positions.
	 * @param tileConfiguration   Tile configuration, or null if not imaging tiles.
	 * @param selectedWells       Authoritative selected-wells set which is kept in
	 *                            sync with the configured positions.
	 *                            Pass the same set instance the caller persists.
	 *                            May be null in custom-position mode.
	 * @param availableGroups     Groups a manually added well may be assigned to.
	 *                            May be null (then only GROUP0 is offered).
	 * @param frame               The containing frame.
	 * @param columns             The columns which should show up in the table.
	 */
	public PathTable(HashMap<PositionInformation, XYAndFocusPosition> configuredPositions,
			MicroplateLayout microplateLayout, TileConfiguration tileConfiguration, Set<WellWithGroup> selectedWells,
			WellWithGroup.WellGroup[] availableGroups, YouScopeFrame frame, Column... columns) {
		super(new BorderLayout(0, 0));
		this.columns = columns;
		this.frame = frame;
		this.configuredPositions = configuredPositions;
		this.microplateLayout = microplateLayout;
		this.tileConfiguration = tileConfiguration;
		this.selectedWells = selectedWells;
		if (availableGroups != null && availableGroups.length > 0)
			this.availableGroups = availableGroups;
		else
			this.availableGroups = new WellWithGroup.WellGroup[] { WellWithGroup.WellGroup.GROUP0 };
		positionInformations = new ArrayList<>(configuredPositions.keySet());
		Collections.sort(positionInformations);
		this.pathTableModel = new PathTableModel();
		this.pathTable = new JTable(pathTableModel);
		pathTable.setAutoCreateColumnsFromModel(true);
		pathTable.setRowSelectionAllowed(true);
		pathTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		pathTable.setColumnSelectionAllowed(false);
		pathTable.setSurrendersFocusOnKeystroke(true);
		pathTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		pathTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		pathTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		PathTableEditor editor = new PathTableEditor();
		pathTable.setDefaultRenderer(String.class, editor);
		pathTable.setDefaultRenderer(Double.class, editor);
		pathTable.setDefaultEditor(Double.class, editor);
		JScrollPane pathTableScrollPane = new JScrollPane(pathTable);
		pathTableScrollPane.setPreferredSize(new Dimension(250, 70));
		pathTableScrollPane.setMinimumSize(new Dimension(10, 10));
		add(pathTableScrollPane, BorderLayout.CENTER);

		// Up, down, add and remove Buttons
		Icon addButtonIcon = ImageLoadingTools.getResourceIcon("icons/block--plus.png", "Add Position");
		Icon deleteButtonIcon = ImageLoadingTools.getResourceIcon("icons/block--minus.png", "Remove Position");
		JButton newPositionButton;
		if (addButtonIcon == null)
			newPositionButton = new JButton("Add Position");
		else
			newPositionButton = new JButton("Add Position", addButtonIcon);
		newPositionButton.setHorizontalAlignment(SwingConstants.LEFT);
		newPositionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addPosition();
			}
		});
		JButton deletePositionButton;
		if (deleteButtonIcon == null)
			deletePositionButton = new JButton("Delete Position");
		else
			deletePositionButton = new JButton("Delete Position", deleteButtonIcon);
		deletePositionButton.setHorizontalAlignment(SwingConstants.LEFT);
		deletePositionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deletePosition();
			}
		});
		DynamicPanel buttonPanel = new DynamicPanel();
		buttonPanel.add(newPositionButton);
		buttonPanel.add(deletePositionButton);
		buttonPanel.addFillEmpty();
		buttonPanel.setOpaque(true);
		add(buttonPanel, BorderLayout.EAST);
	}

	private boolean hasColumn(Column column) {
		for (Column c : columns) {
			if (c == column)
				return true;
		}
		return false;
	}

	/**
	 * True if positions are bound to plate wells (i.e. a microplate layout is
	 * available).
	 * 
	 * @return true in microplate mode, false in custom-position mode.
	 */
	private boolean isMicroplateMode() {
		return microplateLayout != null && hasColumn(Column.WELL);
	}

	/**
	 * Derives the stage reference (zero) position from an already configured well,
	 * so that the absolute stage
	 * coordinates of a further well can be computed exactly as the
	 * fine-configuration wizard would. Returns
	 * {@code {zeroX, zeroY}}, or {@code {0,0}} if no suitable reference is
	 * available (positions are then
	 * plate-relative and should be refined via the wizard).
	 */
	private double[] referenceZero() {
		if (microplateLayout == null)
			return new double[] { 0, 0 };
		for (PositionInformation posInfo : positionInformations) {
			Well well = posInfo.getWell();
			if (well == null)
				continue;
			WellLayout wellLayout = microplateLayout.getWell(well);
			if (wellLayout == null)
				continue;
			XYAndFocusPosition pos = configuredPositions.get(posInfo);
			if (pos == null)
				continue;
			double zeroX;
			double zeroY;
			if (posInfo.getNumPositions() == 2 && tileConfiguration != null) {
				zeroX = pos.getX() - wellLayout.getX()
						- (posInfo.getPosition(1) + 0.5) * wellLayout.getWidth() / tileConfiguration.getNumTilesX();
				zeroY = pos.getY() - wellLayout.getY()
						- (posInfo.getPosition(0) + 0.5) * wellLayout.getHeight() / tileConfiguration.getNumTilesY();
			} else {
				zeroX = pos.getX() - wellLayout.getX() - wellLayout.getWidth() / 2;
				zeroY = pos.getY() - wellLayout.getY() - wellLayout.getHeight() / 2;
			}
			return new double[] { zeroX, zeroY };
		}
		return new double[] { 0, 0 };
	}

	/**
	 * Computes the absolute stage X/Y of the (well[, tile]) using the plate layout
	 * and the given reference.
	 * 
	 * @return {@code {x, y}}, or null if the well is not part of the layout.
	 */
	private double[] computeXY(Well well, Integer tileY, Integer tileX, double[] zero) {
		WellLayout wellLayout = microplateLayout == null ? null : microplateLayout.getWell(well);
		if (wellLayout == null)
			return null;
		double x;
		double y;
		if (tileY != null && tileX != null && tileConfiguration != null) {
			x = zero[0] + wellLayout.getX() + (tileX + 0.5) * wellLayout.getWidth() / tileConfiguration.getNumTilesX();
			y = zero[1] + wellLayout.getY() + (tileY + 0.5) * wellLayout.getHeight() / tileConfiguration.getNumTilesY();
		} else {
			x = zero[0] + wellLayout.getX() + wellLayout.getWidth() / 2;
			y = zero[1] + wellLayout.getY() + wellLayout.getHeight() / 2;
		}
		return new double[] { x, y };
	}

	/**
	 * Returns the group currently assigned to the given well in the selected-wells
	 * set, or GROUP0 if the well is
	 * not (yet) selected.
	 */
	private WellWithGroup.WellGroup groupOfWell(Well well) {
		if (selectedWells != null) {
			for (WellWithGroup wellWithGroup : selectedWells) {
				if (wellWithGroup.getWell().equals(well))
					return wellWithGroup.getGroup();
			}
		}
		return WellWithGroup.WellGroup.GROUP0;
	}

	/**
	 * Adds or replaces the well/group tuple in the selected-wells set, keeping
	 * selection in sync with a newly
	 * added position. No-op in custom-position mode.
	 */
	private void addOrUpdateSelectedWell(Well well, WellWithGroup.WellGroup group) {
		if (selectedWells == null || well == null)
			return;
		java.util.Iterator<WellWithGroup> iterator = selectedWells.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getWell().equals(well))
				iterator.remove();
		}
		selectedWells.add(new WellWithGroup(well, group));
	}

	/**
	 * Removes the well from the selected-wells set if no configured position
	 * references it anymore. No-op in
	 * custom-position mode.
	 */
	private void removeSelectedWellIfOrphaned(Well well) {
		if (selectedWells == null || well == null)
			return;
		for (PositionInformation posInfo : positionInformations) {
			if (well.equals(posInfo.getWell()))
				return; // still referenced by another position (e.g. another tile).
		}
		java.util.Iterator<WellWithGroup> iterator = selectedWells.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getWell().equals(well))
				iterator.remove();
		}
	}

	private class AddPositionComponent extends DynamicPanel {
		/**
		 * Serial Version UID.
		 */
		private static final long serialVersionUID = -6550278275596583426L;
		private JComboBox<Integer> mainPositionComboBox = new JComboBox<>(new XComboBoxModel());
		private JComboBox<Well> wellComboBox = new JComboBox<>();
		private JComboBox<WellWithGroup.WellGroup> groupComboBox = new JComboBox<>(availableGroups);
		private JComboBox<Integer> tileYComboBox;
		private JComboBox<Integer> tileXComboBox;
		private DoubleTextField xPosTextField = new DoubleTextField();
		private DoubleTextField yPosTextField = new DoubleTextField();
		private DoubleTextField focusTextField = new DoubleTextField();
		private static final int NUM_POS_OPTIONS = 999;
		private final YouScopeFrame frame;

		AddPositionComponent(YouScopeFrame frame) {
			this.frame = frame;
			final boolean microplateMode = isMicroplateMode();

			// Populate the well combo box only with wells that exist in the plate
			// definition.
			if (microplateMode) {
				ArrayList<Well> wells = new ArrayList<>(microplateLayout.getNumWells());
				for (WellLayout wellLayout : microplateLayout)
					wells.add(wellLayout.getWell());
				Collections.sort(wells);
				wellComboBox.setModel(new DefaultComboBoxModel<>(wells.toArray(new Well[wells.size()])));
				wellComboBox.setRenderer(new DefaultListCellRenderer() {
					private static final long serialVersionUID = 1L;

					@Override
					public Component getListCellRendererComponent(JList<?> list, Object value, int index,
							boolean isSelected, boolean cellHasFocus) {
						super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
						if (value instanceof Well)
							setText(((Well) value).getWellName());
						return this;
					}
				});
				groupComboBox.setRenderer(new DefaultListCellRenderer() {
					private static final long serialVersionUID = 1L;

					@Override
					public Component getListCellRendererComponent(JList<?> list, Object value, int index,
							boolean isSelected, boolean cellHasFocus) {
						super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
						if (value instanceof WellWithGroup.WellGroup) {
							setText(((WellWithGroup.WellGroup) value).getName());
							setIcon(((WellWithGroup.WellGroup) value).getIcon());
						}
						return this;
					}
				});
				// In microplate mode the plate coordinates are auto-filled and therefore not
				// editable here.
				xPosTextField.setEditable(false);
				yPosTextField.setEditable(false);
			}

			// Bounded tile combo boxes (only tiles that exist in the tile configuration).
			int numTilesY = tileConfiguration != null ? tileConfiguration.getNumTilesY() : NUM_POS_OPTIONS;
			int numTilesX = tileConfiguration != null ? tileConfiguration.getNumTilesX() : NUM_POS_OPTIONS;
			tileYComboBox = new JComboBox<>(indexModel(numTilesY));
			tileXComboBox = new JComboBox<>(indexModel(numTilesX));

			for (Column column : columns) {
				add(new JLabel(column.toString() + ":"));
				switch (column) {
					case MAIN_POSITION:
						add(mainPositionComboBox);
						break;
					case WELL:
						add(wellComboBox);
						break;
					case GROUP:
						add(groupComboBox);
						break;
					case TILE:
						JPanel tilePanel = new JPanel(new FlowLayout());
						tilePanel.setOpaque(false);
						tilePanel.add(tileYComboBox);
						tilePanel.add(tileXComboBox);
						add(tilePanel);
						break;
					case X:
						add(xPosTextField);
						break;
					case Y:
						add(yPosTextField);
						break;
					case FOCUS:
						add(focusTextField);
						break;
					default:
						break;
				}
			}

			if (microplateMode) {
				// Auto-fill X/Y (and preselect the well's current group) whenever the well or
				// tile changes.
				ActionListener autoFill = new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						updateAutoFilledFields();
					}
				};
				wellComboBox.addActionListener(autoFill);
				tileYComboBox.addActionListener(autoFill);
				tileXComboBox.addActionListener(autoFill);
				updateAutoFilledFields();
			}

			JButton addButton = new JButton("Add Position");
			addButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					addPosition();
				}
			});
			add(addButton);
		}

		private ComboBoxModel<Integer> indexModel(final int size) {
			Integer[] values = new Integer[Math.max(size, 1)];
			for (int i = 0; i < values.length; i++)
				values[i] = i + 1;
			return new DefaultComboBoxModel<>(values);
		}

		private void updateAutoFilledFields() {
			Well well = (Well) wellComboBox.getSelectedItem();
			if (well == null)
				return;
			// Preselect the group the well already belongs to (if any).
			groupComboBox.setSelectedItem(groupOfWell(well));
			Integer tileY = null;
			Integer tileX = null;
			if (hasColumn(Column.TILE) && tileConfiguration != null) {
				tileY = tileYComboBox.getSelectedIndex();
				tileX = tileXComboBox.getSelectedIndex();
			}
			double[] xy = computeXY(well, tileY, tileX, referenceZero());
			if (xy != null) {
				xPosTextField.setValue(xy[0]);
				yPosTextField.setValue(xy[1]);
			}
		}

		void addPosition() {
			PositionInformation positionInformation = getPositionInformation();
			if (positionInformation == null)
				return;
			boolean alreadyExists = false;
			if (positionInformations.contains(positionInformation)) {
				if (JOptionPane.showConfirmDialog(this,
						"Position " + positionInformation.toString() + " already exists. Overwrite?",
						"Position Already Exists", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
					return;
				alreadyExists = true;
			}
			double x = xPosTextField.getValue();
			double y = yPosTextField.getValue();
			double focus = Double.NaN;
			for (Column column : columns) {
				if (column == Column.FOCUS) {
					focus = focusTextField.getValue();
					break;
				}
			}
			configuredPositions.put(positionInformation, new XYAndFocusPosition(x, y, focus));
			if (!alreadyExists) {
				positionInformations.add(positionInformation);
				Collections.sort(positionInformations);
			}

			// Keep the selected-wells set (and its group) in sync with the newly added
			// position.
			if (isMicroplateMode() && positionInformation.getWell() != null) {
				WellWithGroup.WellGroup group = (WellWithGroup.WellGroup) groupComboBox.getSelectedItem();
				if (group == null)
					group = WellWithGroup.WellGroup.GROUP0;
				addOrUpdateSelectedWell(positionInformation.getWell(), group);
			}

			int row = Collections.binarySearch(positionInformations, positionInformation);
			if (row >= 0) {
				pathTableModel.fireTableRowsInserted(row, row);
				pathTable.setRowSelectionInterval(row, row);
				pathTable.scrollRectToVisible(new Rectangle(pathTable.getCellRect(row, 0, true)));
			}
			notifyLayoutChanged();

			frame.setVisible(false);
		}

		PositionInformation getPositionInformation() {
			for (Column column : columns) {
				if (column == Column.MAIN_POSITION) {
					return new PositionInformation(PositionInformation.POSITION_TYPE_MAIN_POSITION,
							mainPositionComboBox.getSelectedIndex());
				}
			}
			Well well = (Well) wellComboBox.getSelectedItem();
			if (well == null)
				return null;
			PositionInformation positionInformation = new PositionInformation(well);
			for (Column column : columns) {
				if (column == Column.TILE) {
					positionInformation = new PositionInformation(positionInformation,
							PositionInformation.POSITION_TYPE_YTILE, tileYComboBox.getSelectedIndex());
					positionInformation = new PositionInformation(positionInformation,
							PositionInformation.POSITION_TYPE_XTILE, tileXComboBox.getSelectedIndex());
					break;
				}
			}
			return positionInformation;
		}

		private class XComboBoxModel implements ComboBoxModel<Integer> {
			private Object selectedItem = 1;

			@Override
			public void addListDataListener(ListDataListener arg0) {
				// not needed, no changes possible.
			}

			@Override
			public Integer getElementAt(int arg0) {
				return arg0 + 1;
			}

			@Override
			public int getSize() {
				return NUM_POS_OPTIONS;
			}

			@Override
			public void removeListDataListener(ListDataListener arg0) {
				// not needed, no changes possible.
			}

			@Override
			public Object getSelectedItem() {
				return selectedItem;
			}

			@Override
			public void setSelectedItem(Object anItem) {
				selectedItem = anItem;
			}

		}
	}

	private void addPosition() {
		YouScopeFrame childFrame = frame.createModalChildFrame();
		childFrame.setTitle("Add Position");
		childFrame.setResizable(false);
		childFrame.setContentPane(new AddPositionComponent(childFrame));
		childFrame.pack();
		childFrame.setVisible(true);
	}

	private void deletePosition() {
		int row = pathTable.getSelectedRow();
		if (row < 0 || row >= positionInformations.size())
			return;
		PositionInformation positionInformation = positionInformations.remove(row);
		configuredPositions.remove(positionInformation);
		pathTableModel.fireTableRowsDeleted(row, row);
		// Keep the selected-wells set in sync: drop the well if no position references
		// it anymore.
		if (isMicroplateMode())
			removeSelectedWellIfOrphaned(positionInformation.getWell());
		notifyLayoutChanged();
	}

	/**
	 * Adds a listener which gets notified if wells/tiles were added or deleted.
	 * Does not get notified if
	 * positions of wells/tiles changed.
	 * 
	 * @param listener Listener to add.
	 */
	public void addLayoutChangedListener(ActionListener listener) {
		layoutChangedListeners.add(listener);
	}

	/**
	 * Removes a previously added listener.
	 * 
	 * @param listener listener to remove.
	 */
	public void removeLayoutChangedListener(ActionListener listener) {
		layoutChangedListeners.remove(listener);
	}

	private void notifyLayoutChanged() {
		for (ActionListener listener : layoutChangedListeners) {
			listener.actionPerformed(new ActionEvent(this, 1234, "Layout Changed"));
		}
	}

	private class PathTableModel extends AbstractTableModel {
		/**
		 * Serial Version UID.
		 */
		private static final long serialVersionUID = -3595541853840543126L;

		PathTableModel() {
			// do nothing.
		}

		@Override
		public String getColumnName(int column) {
			if (column < 0 || column >= columns.length)
				return "Invalid Column";
			return columns[column].toString();
		}

		@Override
		public int getRowCount() {
			return positionInformations.size();
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public Class<?> getColumnClass(int column) {
			if (column < 0 || column >= columns.length)
				return String.class;
			return columns[column].type;
		}

		@Override
		public Object getValueAt(int row, int column) {
			if (row < 0 || row >= positionInformations.size() || column < 0 || column >= columns.length)
				return "Invalid Column";
			PositionInformation positionInformation = positionInformations.get(row);
			if (columns[column] == Column.MAIN_POSITION)
				return positionInformation.getNumPositions() == 1
						&& positionInformation.getPositionType(0) == PositionInformation.POSITION_TYPE_MAIN_POSITION
								? Integer.toString(positionInformation.getPosition(0) + 1)
								: "---";
			else if (columns[column] == Column.WELL)
				return positionInformation.getWell() != null ? positionInformation.getWell().toString() : "---";
			else if (columns[column] == Column.GROUP)
				return positionInformation.getWell() != null ? groupOfWell(positionInformation.getWell()).getName()
						: "---";
			else if (columns[column] == Column.TILE)
				return positionInformation.getNumPositions() == 2
						? Well.getWellName(positionInformation.getPosition(0), positionInformation.getPosition(1))
						: "---";
			else {
				XYAndFocusPosition position = configuredPositions.get(positionInformation);
				if (columns[column] == Column.X)
					return position.getX();
				else if (columns[column] == Column.Y)
					return position.getY();
				else if (columns[column] == Column.FOCUS)
					return position.getFocus();
				return "Invalid Column";
			}
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			if (column < 0 || column >= columns.length)
				return false;
			return columns[column].isEdiable();
		}

		@Override
		public void setValueAt(Object objectValue, int row, int column) {
			if (row < 0 || row >= positionInformations.size() || column < 0 || column >= columns.length)
				return;
			if (!(objectValue instanceof Double))
				return;
			double value = (Double) objectValue;
			PositionInformation positionInformation = positionInformations.get(row);
			XYAndFocusPosition oldPosition = configuredPositions.get(positionInformation);
			XYAndFocusPosition newPosition;
			if (columns[column] == Column.X)
				newPosition = new XYAndFocusPosition(value, oldPosition.getY(), oldPosition.getFocus());
			else if (columns[column] == Column.Y)
				newPosition = new XYAndFocusPosition(oldPosition.getX(), value, oldPosition.getFocus());
			else if (columns[column] == Column.FOCUS)
				newPosition = new XYAndFocusPosition(oldPosition.getX(), oldPosition.getY(), value);
			else
				return;
			configuredPositions.put(positionInformation, newPosition);
			fireTableCellUpdated(row, column);
		}
	}

	private class PathTableEditor extends AbstractCellEditor implements
			TableCellEditor, TableCellRenderer {
		/**
		 * Serial Version UID.
		 */
		private static final long serialVersionUID = -5745423014387039692L;
		private DoubleTextField valueField = new DoubleTextField();
		private final JLabel label = new JLabel("");
		private Color activeBackground;
		private Color activeForeground;
		private Color passiveBackground;
		private Color passiveForeground;

		private Color activeSelectedBackground;
		private Color activeSelectedForeground;
		private Color passiveSelectedBackground;
		private Color passiveSelectedForeground;

		PathTableEditor() {
			label.setOpaque(true);
			label.setBorder(new EmptyBorder(0, 3, 0, 3));
			valueField.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					fireEditingStopped();
				}
			});
			valueField.setBorder(new EmptyBorder(0, 3, 0, 3));

			activeBackground = UIManager.getColor("Table.background");
			if (activeBackground == null)
				activeBackground = getBackground();
			activeForeground = UIManager.getColor("Table.foreground");
			if (activeForeground == null)
				activeForeground = getForeground();
			passiveBackground = activeBackground.darker();
			passiveForeground = activeForeground.darker();

			activeSelectedBackground = activeForeground;
			activeSelectedForeground = activeBackground;
			passiveSelectedBackground = activeSelectedBackground;
			passiveSelectedForeground = activeSelectedForeground.brighter();

			valueField.setBackground(activeSelectedBackground);
			valueField.setForeground(activeSelectedForeground);
			valueField.setCaretColor(activeSelectedForeground);

		}

		@Override
		public Object getCellEditorValue() {
			return valueField.getValue();
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			if (value instanceof Double) {
				valueField.setValue(value);
			} else {
				valueField.setValue(Double.NaN);
			}
			return valueField;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object object,
				boolean isSelected, boolean hasFocus, int row, int column) {
			if (column < 0 || column >= columns.length) {
				label.setText("Invalid Column");
				label.setBackground(Color.RED);
				label.setForeground(Color.BLACK);
				label.setHorizontalAlignment(SwingConstants.LEFT);
				return label;
			}
			label.setText(object.toString());
			if (columns[column].isEdiable()) {
				label.setForeground(isSelected ? activeSelectedForeground : activeForeground);
				label.setBackground(isSelected ? activeSelectedBackground : activeBackground);
				label.setHorizontalAlignment(SwingConstants.LEFT);
			} else {
				label.setForeground(isSelected ? passiveSelectedForeground : passiveForeground);
				label.setBackground(isSelected ? passiveSelectedBackground : passiveBackground);
				label.setHorizontalAlignment(SwingConstants.RIGHT);
			}
			return label;
		}
	}
}