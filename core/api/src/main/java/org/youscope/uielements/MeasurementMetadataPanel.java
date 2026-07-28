/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Moritz Lang - initial API and implementation
 *     Andreas P. Cuny - update API supporting placeholders, default-values and FAIR metadata
 ******************************************************************************/
package org.youscope.uielements;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.youscope.clientinterfaces.MetadataCategoryProvider;
import org.youscope.clientinterfaces.MetadataDefinition;
import org.youscope.clientinterfaces.MetadataDefinitionManager;
import org.youscope.clientinterfaces.PropertyProvider;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.MetadataProperty;
import org.youscope.common.measurement.MeasurementConfiguration;

/**
 * A panel to define and edit the metadata of a
 * {@link MeasurementConfiguration}.
 * <p>
 * If the {@link MetadataDefinitionManager} of the client also implements
 * {@link MetadataCategoryProvider},
 * the properties are grouped by category (i.e. by the sub-folder of
 * {@code configuration/metadata} the
 * corresponding definition is stored in) and each group is introduced by a
 * non-editable header row.
 * Otherwise, the properties are displayed as a flat list as before.
 * </p>
 * <p>
 * Metadata definitions may define a default value
 * ({@link MetadataDefinition#getDefaultValue()}), which is
 * pre-selected respectively pre-filled when the property is added, and a
 * placeholder
 * ({@link MetadataDefinition#getPlaceholder()}), which is displayed as grey
 * hint text while a free-text
 * property is still empty. A placeholder is never stored as a metadata value.
 * </p>
 * 
 * @author Moritz Lang
 *
 */
public class MeasurementMetadataPanel extends JPanel {
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 1608172021539503499L;
	private final MetadataDefinitionManager measurementMetadataProvider;
	/**
	 * Provider of the metadata categories, or null if the metadata provider does
	 * not support categories.
	 */
	private final MetadataCategoryProvider categoryProvider;
	private final PropertyProvider propertyProvider;
	private final ArrayList<MetadataProperty> properties = new ArrayList<>();
	/**
	 * Visual representation of {@link #properties}: category headers, property rows
	 * and the "add new" row.
	 */
	private final ArrayList<TableRow> rows = new ArrayList<>();
	private static final String PROPERTY_LAST_PREFIX = "YouScope.MeasurementProperties.Last.";
	/**
	 * If true (the default), the value the user entered for a property in the
	 * previous measurement takes
	 * precedence over the default value configured in the metadata definition. Set
	 * to false to let the
	 * configured default always win, e.g. in facilities which want to enforce a
	 * starting value.
	 */
	private static final String PROPERTY_PREFER_LAST_VALUE = "YouScope.MeasurementProperties.PreferLastValue";

	private static final int DELETE_COLUMN_IDX = 0;
	private static final int NAME_COLUMN_IDX = 1;
	private static final int VALUE_COLUMN_IDX = 2;

	private static final int ROW_TYPE_CATEGORY = 0;
	private static final int ROW_TYPE_PROPERTY = 1;
	private static final int ROW_TYPE_ADD = 2;

	/**
	 * Hint shown for empty free-text properties which do not define their own
	 * placeholder.
	 */
	private static final String DEFAULT_PLACEHOLDER = "<enter value>";
	/**
	 * Label used for properties whose definition is stored directly in the metadata
	 * root folder, or for
	 * custom properties without definition.
	 */
	private static final String UNCATEGORIZED_LABEL = "Other";

	/**
	 * One row of the table. Either a category header, a property, or the "add new"
	 * row.
	 */
	private static class TableRow {
		final int type;
		/**
		 * Index into {@link MeasurementMetadataPanel#properties}, or -1 for
		 * non-property rows.
		 */
		final int propertyIndex;
		/**
		 * Displayed text of a category header row, or null.
		 */
		final String label;

		TableRow(int type, int propertyIndex, String label) {
			this.type = type;
			this.propertyIndex = propertyIndex;
			this.label = label;
		}
	}

	/**
	 * A text field which paints a grey hint text while it is empty. The hint is
	 * purely visual: it is never
	 * part of the document, thus {@link JTextField#getText()} returns an empty
	 * string as long as the user
	 * has not typed anything.
	 */
	private static class HintTextField extends JTextField {
		/**
		 * Serial Version UID.
		 */
		private static final long serialVersionUID = 3049875923718404522L;
		private String hint = null;

		void setHint(String hint) {
			this.hint = (hint == null || hint.isEmpty()) ? null : hint;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (hint == null || !getText().isEmpty())
				return;
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setFont(getFont());
				g2.setColor(getDisabledTextColor());
				Insets insets = getInsets();
				FontMetrics metrics = g2.getFontMetrics();
				int x = insets.left;
				int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
				g2.drawString(hint, x, y);
			} finally {
				g2.dispose();
			}
		}
	}

	/**
	 * Constructor. Adds all default properties to the list. Same as
	 * {@code MeasurementMetadataPanel(youscopeClient, null)}.
	 * 
	 * @param youscopeClient Reference to the YouScope client object.
	 * @throws IllegalArgumentException Thrown if youscopeClient is null.
	 */
	public MeasurementMetadataPanel(YouScopeClient youscopeClient) throws IllegalArgumentException {
		this(youscopeClient, null);
	}

	/**
	 * Constructor. Adds all provided properties to the list of defined properties,
	 * besides the mandatory properties (see
	 * {@link #setMetadataProperties(Collection)}).
	 * 
	 * @param youscopeClient Reference to the YouScope client object.
	 * @param properties     Properties which should be added, or null to add all
	 *                       default properties.
	 * @throws IllegalArgumentException Thrown if youscopeClient is null.
	 */
	public MeasurementMetadataPanel(YouScopeClient youscopeClient, Collection<MetadataProperty> properties)
			throws IllegalArgumentException {
		super(new BorderLayout());
		if (youscopeClient == null)
			throw new IllegalArgumentException();
		measurementMetadataProvider = youscopeClient.getMeasurementMetadataProvider();
		categoryProvider = (measurementMetadataProvider instanceof MetadataCategoryProvider)
				? (MetadataCategoryProvider) measurementMetadataProvider
				: null;
		propertyProvider = youscopeClient.getPropertyProvider();

		// Initialize with default properties
		setMetadataProperties(properties);

		PropertyTableEditor propertyEditor = new PropertyTableEditor();
		table.setDefaultRenderer(String.class, propertyEditor);
		table.setDefaultEditor(String.class, propertyEditor);
		DeleteTableEditor deleteEditor = new DeleteTableEditor();
		table.setDefaultRenderer(Boolean.class, deleteEditor);
		table.setDefaultEditor(Boolean.class, deleteEditor);
		table.setDragEnabled(false);
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(true);
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(8, 0));
		table.setTableHeader(null);
		table.setAutoCreateColumnsFromModel(true);
		table.setRowSelectionAllowed(false);
		table.setColumnSelectionAllowed(false);
		table.setSurrendersFocusOnKeystroke(true);
		table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		table.setFillsViewportHeight(true);
		tableModel.addTableModelListener(columnAdjuster);

		JScrollPane tableScrollPane = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		tableScrollPane.setPreferredSize(new Dimension(250, 100));
		tableScrollPane.setMinimumSize(new Dimension(100, 75));
		add(tableScrollPane, BorderLayout.CENTER);

		// set width of delete column
		TableColumn deleteColumn = table.getColumnModel().getColumn(DELETE_COLUMN_IDX);
		int deleteColumnWidth = deleteEditor.inactiveAllowed.getPreferredSize().width
				+ table.getIntercellSpacing().width;
		deleteColumn.setPreferredWidth(deleteColumnWidth);
		deleteColumn.setMaxWidth(deleteColumnWidth);
		deleteColumn.setMinWidth(deleteColumnWidth);

		columnAdjuster.tableChanged(new TableModelEvent(tableModel));
	}

	private final TableModelListener columnAdjuster = new TableModelListener() {

		@Override
		public void tableChanged(final TableModelEvent event) {
			Runnable runner = new Runnable() {
				@Override
				public void run() {
					int column = event.getColumn();
					if (column == NAME_COLUMN_IDX || column < 0)
						adjustColumn(NAME_COLUMN_IDX);
				}
			};
			if (SwingUtilities.isEventDispatchThread())
				runner.run();
			else
				SwingUtilities.invokeLater(runner);
		}

		/*
		 * Adjust the width of the specified column in the table
		 */
		private void adjustColumn(final int column) {
			TableColumn tableColumn = table.getColumnModel().getColumn(column);
			if (!tableColumn.getResizable())
				return;
			int width = 0;
			for (int row = 0; row < table.getRowCount(); row++) {
				// Category headers may be long; they must not blow up the property name column.
				if (row < rows.size() && rows.get(row).type == ROW_TYPE_CATEGORY)
					continue;
				TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
				Component c = table.prepareRenderer(cellRenderer, row, column);
				int dataWidth = c.getPreferredSize().width + table.getIntercellSpacing().width;
				width = Math.max(width, dataWidth);
			}

			tableColumn.setPreferredWidth(width);
			tableColumn.setMaxWidth(width);
			tableColumn.setMinWidth(width);
		}
	};

	/**
	 * Returns all defined metadata properties.
	 * 
	 * @return Defined properties.
	 */
	public Collection<MetadataProperty> getMetadataProperties() {
		return new ArrayList<MetadataProperty>(properties);
	}

	/**
	 * Sets the displayed and editable properties. All previous properties are
	 * deleted.
	 * Mandatory properties (i.e. properties for which
	 * {@link MetadataDefinition#getType()} returns
	 * {@link org.youscope.clientinterfaces.MetadataDefinition.Type#MANDATORY})
	 * are always added. If properties are null, also default properties (i.e.
	 * properties for which {@link MetadataDefinition#getType()} returns
	 * {@link org.youscope.clientinterfaces.MetadataDefinition.Type#DEFAULT}) are
	 * added.
	 * 
	 * @param properties Properties which should be displayed, or null to display
	 *                   default properties.
	 */
	public void setMetadataProperties(Collection<MetadataProperty> properties) {
		this.properties.clear();
		// Add default or mandatory properties.
		Collection<MetadataDefinition> defaultProperties;
		if (properties == null)
			defaultProperties = measurementMetadataProvider.getDefaultMetadataDefinitions();
		else
			defaultProperties = measurementMetadataProvider.getMandatoryMetadataDefinitions();
		for (MetadataDefinition defaultProperty : defaultProperties) {
			addProperty(defaultProperty.getName(), null);
		}

		// Add provided properties
		if (properties != null) {
			for (MetadataProperty property : properties) {
				addProperty(property.getName(), property.getValue());
			}
		}

		rebuildRows();
		tableModel.fireTableDataChanged();
	}

	// ---------------------------------------------------------------------------------------------
	// Row model
	// ---------------------------------------------------------------------------------------------

	private String getCategoryOf(MetadataProperty property) {
		if (categoryProvider == null || property == null)
			return MetadataCategoryProvider.UNCATEGORIZED;
		String category = categoryProvider.getMetadataCategory(property.getName());
		return category == null ? MetadataCategoryProvider.UNCATEGORIZED : category;
	}

	private boolean isAddRowVisible() {
		return measurementMetadataProvider.isAllowCustomMetadata()
				|| measurementMetadataProvider.getNumMetadataDefinitions() > properties.size();
	}

	/**
	 * Recreates the visual row structure from the current list of properties. Must
	 * be called whenever
	 * properties are added, removed or renamed.
	 */
	private void rebuildRows() {
		rows.clear();
		if (categoryProvider == null) {
			for (int i = 0; i < properties.size(); i++)
				rows.add(new TableRow(ROW_TYPE_PROPERTY, i, null));
		} else {
			TreeMap<String, ArrayList<Integer>> grouped = new TreeMap<String, ArrayList<Integer>>(
					String.CASE_INSENSITIVE_ORDER);
			ArrayList<Integer> uncategorized = new ArrayList<Integer>();
			for (int i = 0; i < properties.size(); i++) {
				String category = getCategoryOf(properties.get(i));
				if (category.isEmpty()) {
					uncategorized.add(Integer.valueOf(i));
					continue;
				}
				ArrayList<Integer> group = grouped.get(category);
				if (group == null) {
					group = new ArrayList<Integer>();
					grouped.put(category, group);
				}
				group.add(Integer.valueOf(i));
			}
			boolean showHeaders = !grouped.isEmpty();
			for (Map.Entry<String, ArrayList<Integer>> entry : grouped.entrySet()) {
				rows.add(new TableRow(ROW_TYPE_CATEGORY, -1, getCategoryDisplayName(entry.getKey())));
				appendPropertyRows(entry.getValue());
			}
			if (!uncategorized.isEmpty()) {
				if (showHeaders)
					rows.add(new TableRow(ROW_TYPE_CATEGORY, -1, UNCATEGORIZED_LABEL));
				appendPropertyRows(uncategorized);
			}
		}
		if (isAddRowVisible())
			rows.add(new TableRow(ROW_TYPE_ADD, -1, null));
	}

	private void appendPropertyRows(ArrayList<Integer> propertyIndices) {
		Collections.sort(propertyIndices, new Comparator<Integer>() {
			@Override
			public int compare(Integer first, Integer second) {
				return properties.get(first.intValue()).getName()
						.compareToIgnoreCase(properties.get(second.intValue()).getName());
			}
		});
		for (Integer propertyIndex : propertyIndices)
			rows.add(new TableRow(ROW_TYPE_PROPERTY, propertyIndex.intValue(), null));
	}

	/**
	 * Converts a category (i.e. a folder name such as {@code sample-preparation})
	 * into a human readable label.
	 * 
	 * @param category Category to convert.
	 * @return Human readable label.
	 */
	static String getCategoryDisplayName(String category) {
		if (category == null || category.isEmpty())
			return UNCATEGORIZED_LABEL;
		StringBuilder builder = new StringBuilder();
		for (String rawSegment : category.split("/")) {
			String segment = rawSegment.replace('-', ' ').replace('_', ' ').trim();
			if (segment.isEmpty())
				continue;
			if (builder.length() > 0)
				builder.append(" / ");
			builder.append(Character.toUpperCase(segment.charAt(0)));
			if (segment.length() > 1)
				builder.append(segment.substring(1));
		}
		return builder.length() == 0 ? UNCATEGORIZED_LABEL : builder.toString();
	}

	private static String escapeHTML(String text) {
		if (text == null)
			return "";
		StringBuilder builder = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char character = text.charAt(i);
			if (character == '&')
				builder.append("&amp;");
			else if (character == '<')
				builder.append("&lt;");
			else if (character == '>')
				builder.append("&gt;");
			else
				builder.append(character);
		}
		return builder.toString();
	}

	// ---------------------------------------------------------------------------------------------
	// Default values and placeholders
	// ---------------------------------------------------------------------------------------------

	/**
	 * Returns the placeholder which should be displayed for an empty property, i.e.
	 * the placeholder of its
	 * definition or, if none is configured, a generic hint.
	 */
	private String getPlaceholder(String propertyName) {
		MetadataDefinition definition = measurementMetadataProvider.getMetadataDefinition(propertyName);
		if (definition != null) {
			String placeholder = definition.getPlaceholder();
			if (placeholder != null && !placeholder.isEmpty())
				return placeholder;
		}
		return DEFAULT_PLACEHOLDER;
	}

	private boolean isPreferLastValue() {
		return propertyProvider.getProperty(PROPERTY_PREFER_LAST_VALUE, true);
	}

	private static boolean isValueAllowed(String value, MetadataDefinition definition) {
		if (value == null || value.isEmpty())
			return false;
		if (definition == null || definition.isCustomValuesAllowed())
			return true;
		String[] knownValues = definition.getKnownValues();
		if (knownValues == null || knownValues.length == 0)
			return true;
		for (String knownValue : knownValues) {
			if (value.equals(knownValue))
				return true;
		}
		return false;
	}

	/**
	 * Determines the value a property should start with when it is added to the
	 * measurement without an
	 * explicit value.
	 * <p>
	 * The order of preference is: the value the user chose in the previous
	 * measurement, then the default
	 * value configured in the metadata definition, then the first known value of a
	 * drop-down. Setting the
	 * client property {@code YouScope.MeasurementProperties.PreferLastValue} to
	 * false swaps the first two,
	 * i.e. lets the configured default always win. Values which are no longer
	 * allowed by the definition
	 * are skipped.
	 * </p>
	 * 
	 * @return Initial value, or null if the property should start out empty.
	 */
	private String getInitialValue(String propertyName, MetadataDefinition definition) {
		String lastValue = getLastPropertyValue(propertyName);
		if (!isValueAllowed(lastValue, definition))
			lastValue = null;
		String defaultValue = definition == null ? null : definition.getDefaultValue();

		if (isPreferLastValue()) {
			if (lastValue != null)
				return lastValue;
			if (defaultValue != null)
				return defaultValue;
		} else {
			if (defaultValue != null)
				return defaultValue;
			if (lastValue != null)
				return lastValue;
		}

		// No default configured: fall back to the first entry of a drop-down, as
		// before. Free-text
		// properties start out empty and only show their placeholder.
		if (definition != null) {
			String[] knownValues = definition.getKnownValues();
			if (knownValues != null && knownValues.length > 0)
				return knownValues[0];
		}
		return null;
	}

	// ---------------------------------------------------------------------------------------------
	// Property manipulation
	// ---------------------------------------------------------------------------------------------

	private boolean deleteProperty(int index) {
		if (index < 0 || index >= properties.size())
			return false;
		MetadataDefinition propertyDefinition = measurementMetadataProvider
				.getMetadataDefinition(properties.get(index).getName());
		if (propertyDefinition != null && propertyDefinition.getType() == MetadataDefinition.Type.MANDATORY)
			return false;
		properties.remove(index);
		return true;
	}

	private boolean addProperty(String propertyName, String propertyValue) {
		return setProperty(propertyName, propertyValue, -1);
	}

	private boolean setProperty(String propertyName, String propertyValue, int index) {
		if (propertyName == null || propertyName.isEmpty())
			return false;

		// find out if property with same name already exists...
		int lastIndex = -1;
		for (int i = 0; i < properties.size(); i++) {
			if (properties.get(i).getName().equals(propertyName)) {
				lastIndex = i;
				break;
			}
		}
		if (index >= 0 && lastIndex >= 0) {
			// we want to replace a given element with an element which does already exist.
			// Skip!
			return false;
		} else if (index >= 0) {
			// we want to replace a property. Check if we are actually allowed to do so.
			MetadataDefinition oldPropertyDefinition = measurementMetadataProvider
					.getMetadataDefinition(properties.get(index).getName());
			if (oldPropertyDefinition != null && oldPropertyDefinition.getType() == MetadataDefinition.Type.MANDATORY)
				return false;
		}
		// if the value is null and we already have that property in the list, just keep
		// the old one.
		if (propertyValue == null && lastIndex >= 0)
			return false;

		// get the definition for this property
		MetadataDefinition propertyDefinition = measurementMetadataProvider.getMetadataDefinition(propertyName);
		if (propertyDefinition == null) {
			// no definition means custom property. Check if allowed...
			if (!measurementMetadataProvider.isAllowCustomMetadata())
				return false;
			// no definition means no default value either; fall back to the last used
			// value.
			if (propertyValue == null)
				propertyValue = getLastPropertyValue(propertyName);
		} else {
			// determine the starting value: last used value, configured default, or first
			// known value.
			if (propertyValue == null)
				propertyValue = getInitialValue(propertyName, propertyDefinition);

			if (!propertyDefinition.isCustomValuesAllowed()) {
				// We must check if value is in agreement with allowed values.
				boolean allowed = false;
				if (propertyValue != null) {
					for (String allowedValue : propertyDefinition.getKnownValues()) {
						if (propertyValue.equals(allowedValue)) {
							allowed = true;
							break;
						}
					}
				}
				if (!allowed) {
					// if we have an old allowed value already in the list, use the old one...
					if (lastIndex >= 0)
						return false;
					// Since no custom values are allowed, we can be sure that at least one value is
					// known...
					propertyValue = propertyDefinition.getKnownValues()[0];
				}
			}
		}
		if (index >= 0)
			properties.set(index, new MetadataProperty(propertyName, propertyValue == null ? "" : propertyValue));
		else if (lastIndex >= 0)
			properties.set(lastIndex, new MetadataProperty(propertyName, propertyValue == null ? "" : propertyValue));
		else
			properties.add(new MetadataProperty(propertyName, propertyValue == null ? "" : propertyValue));
		if (propertyValue != null && !propertyValue.isEmpty())
			setLastPropertyValue(propertyName, propertyValue);
		return true;
	}

	private String getLastPropertyValue(String propertyName) {
		return propertyProvider.getProperty(PROPERTY_LAST_PREFIX + propertyName, (String) null);
	}

	private void setLastPropertyValue(String propertyName, String propertyValue) {
		propertyProvider.setProperty(PROPERTY_LAST_PREFIX + propertyName, propertyValue);
	}

	/**
	 * Returns all names of metadata definitions which are not yet part of the
	 * property list, sorted by
	 * category and, within a category, by name.
	 */
	private ArrayList<String> getAvailablePropertyNames() {
		HashSet<String> availableProperties = new HashSet<>();
		for (MetadataDefinition property : measurementMetadataProvider.getMetadataDefinitions()) {
			availableProperties.add(property.getName());
		}
		for (MetadataProperty property : properties) {
			availableProperties.remove(property.getName());
		}
		ArrayList<String> result = new ArrayList<String>(availableProperties);
		Collections.sort(result, new Comparator<String>() {
			@Override
			public int compare(String first, String second) {
				if (categoryProvider != null) {
					String firstCategory = categoryProvider.getMetadataCategory(first);
					String secondCategory = categoryProvider.getMetadataCategory(second);
					if (firstCategory == null)
						firstCategory = MetadataCategoryProvider.UNCATEGORIZED;
					if (secondCategory == null)
						secondCategory = MetadataCategoryProvider.UNCATEGORIZED;
					// uncategorized definitions go last.
					if (firstCategory.isEmpty() != secondCategory.isEmpty())
						return firstCategory.isEmpty() ? 1 : -1;
					int categoryComparison = firstCategory.compareToIgnoreCase(secondCategory);
					if (categoryComparison != 0)
						return categoryComparison;
				}
				return first.compareToIgnoreCase(second);
			}
		});
		return result;
	}

	// ---------------------------------------------------------------------------------------------
	// Table model
	// ---------------------------------------------------------------------------------------------

	private final AbstractTableModel tableModel = new AbstractTableModel() {
		/**
		 * Serial Version UID.
		 */
		private static final long serialVersionUID = 4091073520994701669L;

		@Override
		public String getColumnName(int col) {
			if (col == DELETE_COLUMN_IDX)
				return "Delete";
			else if (col == NAME_COLUMN_IDX)
				return "Property";
			else if (col == VALUE_COLUMN_IDX)
				return "Value";
			return "";
		}

		@Override
		public int getRowCount() {
			return rows.size();
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public Class<?> getColumnClass(int col) {
			if (col == DELETE_COLUMN_IDX)
				return Boolean.class;
			return String.class;
		}

		@Override
		public Object getValueAt(int row, int col) {
			if (row < 0 || row >= rows.size())
				return col == DELETE_COLUMN_IDX ? (Object) Boolean.FALSE : (Object) "";
			TableRow tableRow = rows.get(row);
			if (col == DELETE_COLUMN_IDX)
				return Boolean.FALSE;
			if (tableRow.type == ROW_TYPE_CATEGORY)
				return col == NAME_COLUMN_IDX ? tableRow.label : "";
			if (tableRow.type == ROW_TYPE_ADD)
				return col == NAME_COLUMN_IDX ? "<add new>" : "";
			if (col == NAME_COLUMN_IDX)
				return properties.get(tableRow.propertyIndex).getName();
			else if (col == VALUE_COLUMN_IDX)
				return properties.get(tableRow.propertyIndex).getValue();
			return "";
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			if (row < 0 || row >= rows.size())
				return false;
			TableRow tableRow = rows.get(row);
			// Category headers are pure decoration.
			if (tableRow.type == ROW_TYPE_CATEGORY)
				return false;
			if (tableRow.type == ROW_TYPE_ADD)
				return col == NAME_COLUMN_IDX;
			// Value always editable
			if (col == VALUE_COLUMN_IDX)
				return true;
			else if (col == NAME_COLUMN_IDX || col == DELETE_COLUMN_IDX) {
				MetadataDefinition definition = measurementMetadataProvider
						.getMetadataDefinition(properties.get(tableRow.propertyIndex).getName());
				if (definition == null)
					return true;
				return definition.getType() != MetadataDefinition.Type.MANDATORY;
			} else
				return false;
		}

		@Override
		public void setValueAt(Object rawValue, int row, int col) {
			if (rawValue == null)
				return;
			if (row < 0 || row >= rows.size())
				return;
			TableRow tableRow = rows.get(row);
			if (tableRow.type == ROW_TYPE_CATEGORY)
				return;
			if (col == DELETE_COLUMN_IDX) {
				if (tableRow.type != ROW_TYPE_PROPERTY)
					return;
				if (!(rawValue instanceof Boolean) || !((Boolean) rawValue).booleanValue())
					return;
				if (deleteProperty(tableRow.propertyIndex)) {
					rebuildRows();
					fireTableDataChanged();
				}
				return;
			}
			String value = rawValue.toString();
			if (tableRow.type == ROW_TYPE_ADD) {
				// we want to add a value...
				if (col != NAME_COLUMN_IDX)
					return;
				if (addProperty(value, null)) {
					rebuildRows();
					fireTableDataChanged();
				}
				return;
			} else if (col == VALUE_COLUMN_IDX) {
				// we want to change a value...
				addProperty(properties.get(tableRow.propertyIndex).getName(), value);
				fireTableCellUpdated(row, col);
				return;
			} else if (col == NAME_COLUMN_IDX) {
				// replace the old property by the new one, if allowed...
				if (setProperty(value, null, tableRow.propertyIndex)) {
					// The new property may belong to a different category, so the row structure
					// changes.
					rebuildRows();
					fireTableDataChanged();
				}
				return;
			}
		}
	};
	private final JTable table = new JTable(tableModel);

	private class DeleteTableEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
		/**
		 * Serial Version UID.
		 */
		private static final long serialVersionUID = 5717587328458546821L;
		private final JLabel inactiveForbidden = new JLabel("");
		private final JLabel activeForbidden = new JLabel("");
		private final JButton inactiveAllowed;
		private final JButton activeAllowed;
		private final Color normalBackground;
		private boolean lastDecision = false;

		public DeleteTableEditor() {
			Icon deleteIcon = ImageLoadingTools.getResourceIcon("iconsShadowless/cross-script.png", "Delete");
			if (deleteIcon != null) {
				inactiveAllowed = new JButton(deleteIcon);
				activeAllowed = new JButton(deleteIcon);
			} else {
				inactiveAllowed = new JButton("X");
				activeAllowed = new JButton("X");
			}
			inactiveAllowed.setBorderPainted(false);
			activeAllowed.setBorderPainted(false);
			inactiveAllowed.setBorder(null);
			activeAllowed.setBorder(null);
			inactiveAllowed.setOpaque(false);
			activeAllowed.setOpaque(false);
			inactiveAllowed.setContentAreaFilled(false);
			activeAllowed.setContentAreaFilled(false);

			JTextField colorModel = new JTextField();
			normalBackground = colorModel.getBackground();
			activeAllowed.setBackground(normalBackground);
			inactiveAllowed.setBackground(normalBackground);
			activeAllowed.setForeground(colorModel.getForeground());
			inactiveAllowed.setForeground(colorModel.getForeground());
			inactiveForbidden.setOpaque(true);
			activeForbidden.setOpaque(true);
			inactiveForbidden.setBackground(normalBackground);
			activeForbidden.setBackground(normalBackground);

			Dimension dim1 = inactiveForbidden.getPreferredSize();
			Dimension dim2 = activeAllowed.getPreferredSize();
			Dimension dim = new Dimension(Math.max(dim1.width, dim2.width), Math.max(dim1.height, dim2.height));
			inactiveForbidden.setPreferredSize(dim);
			inactiveForbidden.setMaximumSize(dim);
			inactiveForbidden.setMinimumSize(dim);
			activeForbidden.setPreferredSize(dim);
			activeForbidden.setMaximumSize(dim);
			activeForbidden.setMinimumSize(dim);
			inactiveAllowed.setPreferredSize(dim);
			inactiveAllowed.setMaximumSize(dim);
			inactiveAllowed.setMinimumSize(dim);
			activeAllowed.setPreferredSize(dim);
			activeAllowed.setMaximumSize(dim);
			activeAllowed.setMinimumSize(dim);

			activeAllowed.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					lastDecision = true;
					fireEditingStopped();
				}
			});
		}

		@Override
		public Object getCellEditorValue() {
			return lastDecision;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
			lastDecision = false;
			activeAllowed.setSelected(false);
			if (tableModel.isCellEditable(row, col))
				return activeAllowed;
			return activeForbidden;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int col) {
			if (row >= 0 && row < rows.size() && rows.get(row).type == ROW_TYPE_CATEGORY) {
				// Continue the category band across the delete column.
				inactiveForbidden.setBackground(getCategoryBackground(normalBackground));
				return inactiveForbidden;
			}
			inactiveForbidden.setBackground(normalBackground);
			if (tableModel.isCellEditable(row, col))
				return inactiveAllowed;
			return inactiveForbidden;
		}
	}

	/**
	 * Derives a slightly contrasting background used for category header rows, so
	 * that the grouping also
	 * works with dark look-and-feels.
	 */
	private static Color getCategoryBackground(Color base) {
		int brightness = (base.getRed() + base.getGreen() + base.getBlue()) / 3;
		int delta = brightness > 127 ? -18 : 24;
		return new Color(Math.min(255, Math.max(0, base.getRed() + delta)),
				Math.min(255, Math.max(0, base.getGreen() + delta)),
				Math.min(255, Math.max(0, base.getBlue() + delta)));
	}

	/**
	 * Mixes two colors in equal parts, used to derive the grey of the placeholder
	 * text.
	 */
	private static Color blend(Color first, Color second) {
		return new Color((first.getRed() + second.getRed()) / 2, (first.getGreen() + second.getGreen()) / 2,
				(first.getBlue() + second.getBlue()) / 2);
	}

	private class PropertyTableEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
		/**
		 * Serial Version UID.
		 */
		private static final long serialVersionUID = -7924080179258623862L;
		private final JComboBox<String> editComboBox = new JComboBox<String>();
		private final HintTextField editTextField = new HintTextField();
		private final JLabel viewLabel = new JLabel();
		private Object lastEditor = null;
		private final Font plainFont;
		private final Font boldFont;
		private final Color normalBackground;
		private final Color categoryBackground;
		private final Color normalForeground;
		private final Color placeholderForeground;
		/**
		 * Maps property names currently shown in {@link #editComboBox} to their
		 * category. Empty while the
		 * combo box is used to edit a property value.
		 */
		private final HashMap<String, String> comboBoxCategories = new HashMap<String, String>();

		PropertyTableEditor() {
			plainFont = viewLabel.getFont();
			boldFont = plainFont.deriveFont(Font.BOLD);
			viewLabel.setOpaque(true);
			normalBackground = editTextField.getBackground();
			categoryBackground = getCategoryBackground(normalBackground);
			normalForeground = editTextField.getForeground();
			placeholderForeground = blend(normalForeground, normalBackground);
			viewLabel.setBackground(normalBackground);
			viewLabel.setForeground(normalForeground);
			editComboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					fireEditingStopped();
				}
			});
			editComboBox.setRenderer(new DefaultListCellRenderer() {
				/**
				 * Serial Version UID.
				 */
				private static final long serialVersionUID = -2288396166380248126L;

				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index,
						boolean isSelected, boolean cellHasFocus) {
					Component component = super.getListCellRendererComponent(list, value, index, isSelected,
							cellHasFocus);
					if (value != null && component instanceof JLabel && !comboBoxCategories.isEmpty()) {
						String name = value.toString();
						String category = comboBoxCategories.get(name);
						if (category != null && !category.isEmpty()) {
							((JLabel) component)
									.setText("<html>" + escapeHTML(name) + " <font color=\"#909090\">&#8211; "
											+ escapeHTML(getCategoryDisplayName(category)) + "</font></html>");
						}
					}
					return component;
				}
			});
		}

		@Override
		public Object getCellEditorValue() {
			if (lastEditor == null)
				return null;
			else if (lastEditor == editTextField) {
				return editTextField.getText();
			} else if (lastEditor == editComboBox) {
				Object selectedItem = editComboBox.getSelectedItem();
				return selectedItem == null ? null : selectedItem.toString();
			} else
				return null;
		}

		/**
		 * Fills the combo box with the given property names and remembers their
		 * categories, so that the
		 * renderer can annotate them.
		 */
		private void fillWithPropertyNames(Collection<String> propertyNames) {
			comboBoxCategories.clear();
			editComboBox.removeAllItems();
			for (String propertyName : propertyNames) {
				editComboBox.addItem(propertyName);
				if (categoryProvider != null) {
					String category = categoryProvider.getMetadataCategory(propertyName);
					if (category != null && !category.isEmpty())
						comboBoxCategories.put(propertyName, category);
				}
			}
		}

		/**
		 * Prepares the text field as editor, with the given content and grey hint text.
		 */
		private Component prepareTextField(String text, String hint) {
			editTextField.setHint(hint);
			editTextField.setText(text == null ? "" : text);
			lastEditor = editTextField;
			return editTextField;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
			TableRow tableRow = (row >= 0 && row < rows.size()) ? rows.get(row) : null;
			// check if new property
			if (tableRow == null || tableRow.type == ROW_TYPE_ADD) {
				ArrayList<String> availableProperties = getAvailablePropertyNames();
				if (availableProperties.size() > 0) {
					fillWithPropertyNames(availableProperties);
					if (measurementMetadataProvider.isAllowCustomMetadata()) {
						editComboBox.setEditable(true);
						editComboBox.setSelectedItem("");
					} else {
						editComboBox.setEditable(false);
					}
					lastEditor = editComboBox;
					return editComboBox;
				}
				return prepareTextField("", null);
			} else if (col == NAME_COLUMN_IDX) {
				// we want to change a property to another...
				// get all defined properties not yet set
				ArrayList<String> availableProperties = new ArrayList<String>();
				availableProperties.add(properties.get(tableRow.propertyIndex).getName());
				availableProperties.addAll(getAvailablePropertyNames());
				fillWithPropertyNames(availableProperties);
				editComboBox.setSelectedItem(properties.get(tableRow.propertyIndex).getName());
				editComboBox.setEditable(measurementMetadataProvider.isAllowCustomMetadata());
				lastEditor = editComboBox;
				return editComboBox;
			} else if (col == VALUE_COLUMN_IDX) {
				String propertyName = properties.get(tableRow.propertyIndex).getName();
				String propertyValue = properties.get(tableRow.propertyIndex).getValue();
				// we want to change a value
				MetadataDefinition definition = measurementMetadataProvider.getMetadataDefinition(propertyName);
				if (definition == null) {
					// custom property without definition: no placeholder configurable.
					return prepareTextField(propertyValue, null);
				}
				String[] knownValues = definition.getKnownValues();
				if (knownValues.length > 0) {
					// values are not categorized, thus no annotation in the renderer.
					comboBoxCategories.clear();
					editComboBox.removeAllItems();
					for (String knownValue : knownValues) {
						editComboBox.addItem(knownValue);
					}
					editComboBox.setEditable(definition.isCustomValuesAllowed());
					// The value already carries the default; it was applied when the property was
					// added.
					editComboBox.setSelectedItem(propertyValue);
					lastEditor = editComboBox;
					return editComboBox;
				}
				return prepareTextField(propertyValue, definition.getPlaceholder());
			} else {
				return prepareTextField("", null);
			}
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int col) {
			TableRow tableRow = (row >= 0 && row < rows.size()) ? rows.get(row) : null;
			String text = value == null ? "" : value.toString();

			if (tableRow != null && tableRow.type == ROW_TYPE_CATEGORY) {
				viewLabel.setText(col == NAME_COLUMN_IDX ? text : "");
				viewLabel.setFont(boldFont);
				viewLabel.setForeground(normalForeground);
				viewLabel.setBackground(categoryBackground);
				return viewLabel;
			}
			viewLabel.setBackground(normalBackground);
			viewLabel.setForeground(normalForeground);

			boolean isProperty = tableRow != null && tableRow.type == ROW_TYPE_PROPERTY;
			if (text.isEmpty() && col == VALUE_COLUMN_IDX && isProperty) {
				// Empty value: show the placeholder of the definition in grey. It is a hint
				// only and is
				// never stored as a value.
				text = getPlaceholder(properties.get(tableRow.propertyIndex).getName());
				viewLabel.setForeground(placeholderForeground);
			} else if (col == NAME_COLUMN_IDX && isProperty)
				text += ":";
			viewLabel.setText(text);
			if (!isProperty)
				viewLabel.setFont(plainFont);
			else if (col == NAME_COLUMN_IDX) {
				MetadataDefinition definition = measurementMetadataProvider
						.getMetadataDefinition(properties.get(tableRow.propertyIndex).getName());
				if (definition == null || definition.getType() != MetadataDefinition.Type.MANDATORY)
					viewLabel.setFont(plainFont);
				else
					viewLabel.setFont(boldFont);
			} else
				viewLabel.setFont(plainFont);
			return viewLabel;
		}
	}

}
