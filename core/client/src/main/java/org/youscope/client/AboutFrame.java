/*******************************************************************************
 * Copyright © 2026 ETH Zürich
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
 *     Andreas P. Cuny - update copyright and maintainer
 ******************************************************************************/
package org.youscope.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.YouScopeVersion;
import org.youscope.uielements.ImageLoadingTools;

class AboutFrame {
    /**
     * URL of the image displayed on top of every frame.
     */
    private static final String BACKGROUND_URL = "org/youscope/client/images/splash.jpg";

    /**
     * System property that can be used to point to the directory containing the
     * "&lt;jar name&gt;-LICENSE.txt" files. If not set, a number of default
     * locations is searched.
     */
    private static final String LICENSE_DIR_PROPERTY = "youscope.licenses.dir";

    /**
     * Suffix of the license files. A file "foo-1.2.3-LICENSE.txt" belongs to
     * "foo-1.2.3.jar".
     */
    private static final String LICENSE_SUFFIX = "-LICENSE.txt";

    /**
     * Jars starting with this prefix (case-insensitive) are part of YouScope itself
     * and not listed as third party.
     */
    private static final String OWN_JAR_PREFIX = "youscope";

    /**
     * Text shown for a library without license file.
     */
    private static final String MISSING_LICENSE = "(license file missing)";

    /**
     * Maximal number of bytes of a license file displayed in the text area.
     */
    private static final int MAX_DISPLAY_BYTES = 400 * 1024;

    AboutFrame(final YouScopeFrame frame) {
        frame.setTitle("YouScope " + YouScopeVersion.getFullVersion());
        frame.setResizable(false);
        frame.setClosable(true);
        frame.setMaximizable(false);
        frame.setMargins(0, 0, 0, 0);

        final Dimension totalSize = new Dimension(600, 361);

        final Image backgroundImage = ImageLoadingTools.getResourceImage(BACKGROUND_URL, "Logo");
        final JPanel splashPane = new JPanel(null) {

            @Override
            public Dimension getPreferredSize() {
                return totalSize;
            }

            /**
             * Serial Version UID
             */
            private static final long serialVersionUID = -1054038275505649084L;

            @Override
            protected void paintComponent(Graphics g) {

                super.paintComponent(g);
                if (backgroundImage != null)
                    g.drawImage(backgroundImage, 0, 0, this);
            }

        };
        splashPane.setOpaque(true);

        JTextPane description = new JTextPane() {
            /**
             * Serial Version UID.
             */
            private static final long serialVersionUID = -4666535620525040910L;

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(1.0f, 1.0f, 1.0f, 0.5f));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }

        };

        description.setContentType("text/html");
        description.setText("<html><p style=\"margin:0px;padding:0px;font-weight:bold;color: #000000;\">"
                + "YouScope " + YouScopeVersion.getFullVersion() + "<br />"
                + "Created by Moritz Lang, Andreas P. Cuny and contributers.<br /><br />"
                + "(c) Copyright Moritz Lang, Andreas P. Cuny, contributors and others 2009-2026.<br/>"
                + "All rights reserved.<br /><br />"
                + "If you have remarks or questions,<br />"
                + "visit: <a href=\"http://www.youscope.org\">www.youscope.org</a><br />"
                + "or contact: <a href=\"mailto:contact@youscope.org\">contact@youscope.org</a>"
                + "</p></html>");

        description.setEditable(false);
        description.setOpaque(false);
        splashPane.add(description);
        Dimension preferredSize = description.getPreferredSize();
        description.setBounds((totalSize.width - preferredSize.width) / 2,
                (totalSize.height - preferredSize.height) / 2, preferredSize.width, preferredSize.height);

        JButton okButton = new JButton("Close");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setVisible(false);
            }
        });
        okButton.setOpaque(false);
        splashPane.add(okButton);
        preferredSize = okButton.getPreferredSize();
        okButton.setBounds(totalSize.width - preferredSize.width - 5, totalSize.height - preferredSize.height - 5,
                preferredSize.width, preferredSize.height);

        // Third party software table

        final LibraryTableModel tableModel = new LibraryTableModel();
        final JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setColumnWidth(table, 0, 260);
        setColumnWidth(table, 1, 90);
        setColumnWidth(table, 2, 230);
        table.getColumnModel().getColumn(2).setCellRenderer(new LicenseCellRenderer());

        final JTextArea licenseText = new JTextArea();
        licenseText.setEditable(false);
        licenseText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        licenseText.setLineWrap(false);
        licenseText.setText("Select a library to display its license.");

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                int viewRow = table.getSelectedRow();
                if (viewRow < 0)
                    return;
                LibraryEntry entry = tableModel.getEntry(table.convertRowIndexToModel(viewRow));
                if (entry.licenseFile == null) {
                    licenseText.setText("No license file (" + entry.baseName + LICENSE_SUFFIX
                            + ") was found for this library.");
                } else {
                    licenseText.setText(readLicenseFile(entry.licenseFile));
                }
                licenseText.setCaretPosition(0);
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(totalSize.width, 170));
        JScrollPane textScroll = new JScrollPane(licenseText);
        textScroll.setPreferredSize(new Dimension(totalSize.width, 110));

        final JLabel header = new JLabel("Third-party software used by YouScope (loading...)");
        header.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        header.setFont(header.getFont().deriveFont(Font.BOLD));

        JPanel libraryPane = new JPanel(new BorderLayout());
        libraryPane.add(header, BorderLayout.NORTH);
        JPanel centerPane = new JPanel(new BorderLayout());
        centerPane.add(tableScroll, BorderLayout.CENTER);
        centerPane.add(textScroll, BorderLayout.SOUTH);
        libraryPane.add(centerPane, BorderLayout.CENTER);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(splashPane, BorderLayout.NORTH);
        contentPane.add(libraryPane, BorderLayout.CENTER);

        // Load the library list in the background, license files are read from disk.
        new SwingWorker<List<LibraryEntry>, Void>() {
            @Override
            protected List<LibraryEntry> doInBackground() {
                return collectLibraries();
            }

            @Override
            protected void done() {
                try {
                    List<LibraryEntry> entries = get();
                    tableModel.setEntries(entries);
                    int missing = 0;
                    for (LibraryEntry entry : entries) {
                        if (entry.licenseFile == null)
                            missing++;
                    }
                    header.setText("Third-party software used by YouScope (" + entries.size() + " libraries"
                            + (missing > 0 ? ", " + missing + " without license file" : "") + ")");
                } catch (Exception e) {
                    header.setText("Third-party software used by YouScope (could not be determined)");
                }
            }
        }.execute();

        frame.setContentPane(contentPane);
        frame.pack();
    }

    private static void setColumnWidth(JTable table, int column, int width) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        tableColumn.setPreferredWidth(width);
    }

    /**
     * One third party library (a jar and/or its license file).
     */
    private static class LibraryEntry {
        final String baseName;
        final String library;
        final String version;
        final String license;
        final File licenseFile;

        LibraryEntry(String baseName, File licenseFile) {
            this.baseName = baseName;
            this.licenseFile = licenseFile;

            // "name-1.2.3" -> name / 1.2.3 (first '-' that is followed by a digit)
            Matcher matcher = Pattern.compile("^(.+?)-(\\d.*)$").matcher(baseName);
            if (matcher.matches()) {
                this.library = matcher.group(1);
                this.version = matcher.group(2);
            } else {
                this.library = baseName;
                this.version = "";
            }

            this.license = licenseFile == null ? MISSING_LICENSE : detectLicense(readHead(licenseFile));
        }
    }

    private static class LibraryTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 4425781346095203442L;

        private static final String[] COLUMNS = { "Library", "Version", "License" };

        private List<LibraryEntry> entries = new ArrayList<LibraryEntry>();

        void setEntries(List<LibraryEntry> entries) {
            this.entries = entries;
            fireTableDataChanged();
        }

        LibraryEntry getEntry(int row) {
            return entries.get(row);
        }

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            LibraryEntry entry = entries.get(row);
            switch (column) {
                case 0:
                    return entry.library;
                case 1:
                    return entry.version;
                default:
                    return entry.license;
            }
        }
    }

    private static class LicenseCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = -2931646002385573814L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            if (!isSelected) {
                component.setForeground(MISSING_LICENSE.equals(value) ? new Color(170, 0, 0) : table.getForeground());
            }
            return component;
        }
    }

    /**
     * Collects all third party jars (from the class path and the library
     * directories) and the license files
     * belonging to them. License files without jar are listed as well.
     */
    private static List<LibraryEntry> collectLibraries() {
        // base name (jar file name without ".jar") -> license file (or null)
        Map<String, File> libraries = new TreeMap<String, File>(String.CASE_INSENSITIVE_ORDER);

        // Jars on the class path.
        String classPath = System.getProperty("java.class.path", "");
        for (String element : classPath.split(File.pathSeparator)) {
            addJar(libraries, new File(element));
        }

        List<File> baseDirs = getBaseDirectories();

        // Jars in typical library directories.
        for (File baseDir : baseDirs) {
            for (String libDirName : new String[] { "libs", "lib" }) {
                File[] jars = new File(baseDir, libDirName).listFiles();
                if (jars != null) {
                    for (File jar : jars)
                        addJar(libraries, jar);
                }
            }
        }

        // License files.
        Set<File> licenseDirs = new LinkedHashSet<File>();
        String configured = System.getProperty(LICENSE_DIR_PROPERTY);
        if (configured != null && configured.length() > 0)
            licenseDirs.add(new File(configured));
        for (File baseDir : baseDirs) {
            licenseDirs.add(new File(baseDir, "licenses"));
            licenseDirs.add(new File(baseDir, "licences"));
        }

        Map<String, File> licenseFiles = new TreeMap<String, File>(String.CASE_INSENSITIVE_ORDER);
        for (File licenseDir : licenseDirs) {
            File[] files = licenseDir.listFiles();
            if (files == null)
                continue;
            for (File file : files) {
                String name = file.getName();
                if (file.isFile() && name.toLowerCase(Locale.ROOT).endsWith(LICENSE_SUFFIX.toLowerCase(Locale.ROOT))) {
                    String baseName = name.substring(0, name.length() - LICENSE_SUFFIX.length());
                    if (!isOwnLibrary(baseName) && !licenseFiles.containsKey(baseName))
                        licenseFiles.put(baseName, file);
                }
            }
        }

        // Merge.
        Set<String> allNames = new LinkedHashSet<String>(libraries.keySet());
        allNames.addAll(licenseFiles.keySet());

        List<LibraryEntry> entries = new ArrayList<LibraryEntry>();
        for (String name : allNames) {
            entries.add(new LibraryEntry(name, licenseFiles.get(name)));
        }

        Collections.sort(entries, new Comparator<LibraryEntry>() {
            @Override
            public int compare(LibraryEntry a, LibraryEntry b) {
                return a.baseName.compareToIgnoreCase(b.baseName);
            }
        });
        return entries;
    }

    private static void addJar(Map<String, File> libraries, File file) {
        String name = file.getName();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".jar"))
            return;
        String baseName = name.substring(0, name.length() - 4);
        if (isOwnLibrary(baseName))
            return;
        if (!libraries.containsKey(baseName))
            libraries.put(baseName, null);
    }

    private static boolean isOwnLibrary(String baseName) {
        return baseName.toLowerCase(Locale.ROOT).startsWith(OWN_JAR_PREFIX);
    }

    /**
     * Directories relative to which "libs" and "licenses" are searched: working
     * directory, its parent, and the
     * directory (and parent) of the location this class was loaded from.
     */
    private static List<File> getBaseDirectories() {
        Set<File> dirs = new LinkedHashSet<File>();
        File workingDir = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        dirs.add(workingDir);
        if (workingDir.getParentFile() != null)
            dirs.add(workingDir.getParentFile());

        try {
            File codeSource = new File(AboutFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File dir = codeSource.isDirectory() ? codeSource : codeSource.getParentFile();
            for (int i = 0; i < 3 && dir != null; i++) {
                dirs.add(dir);
                dir = dir.getParentFile();
            }
        } catch (URISyntaxException e) {
            // ignore, use only working directory
        } catch (RuntimeException e) {
            // no code source available (e.g. security manager), use only working directory
        }
        return new ArrayList<File>(dirs);
    }

    // Reading and classifying license files

    private static String readHead(File file) {
        return readText(file, 8 * 1024);
    }

    private static String readLicenseFile(File file) {
        return readText(file, MAX_DISPLAY_BYTES);
    }

    private static String readText(File file, int maxBytes) {
        try {
            byte[] all = Files.readAllBytes(file.toPath());
            int length = Math.min(all.length, maxBytes);
            int start = 0;
            if (length >= 3 && (all[0] & 0xFF) == 0xEF && (all[1] & 0xFF) == 0xBB && (all[2] & 0xFF) == 0xBF)
                start = 3;
            // The license files are written as UTF-8; invalid bytes are replaced.
            return new String(all, start, length - start, Charset.forName("UTF-8"));
        } catch (IOException e) {
            return "Could not read " + file.getAbsolutePath() + ": " + e.getMessage();
        }
    }

    /**
     * Determines the license type from the beginning of the license text. Java port
     * of the classification used by
     * the script which generates the license files.
     */
    static String detectLicense(String content) {
        if (content == null || content.trim().length() == 0)
            return "Unknown";

        // Files with several licenses start with a "====" separator line.
        if (content.trim().startsWith("======"))
            return "Multiple licenses (see text)";

        String text = content.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        String head = text.substring(0, Math.min(2500, text.length()));

        // GPL texts mention the LGPL and vice versa: the title which appears first
        // wins.
        int lgpl = indexOfFirst(head, "gnu lesser general public license", "gnu library general public license");
        int gpl = head.indexOf("gnu general public license");
        if (lgpl >= 0 && (gpl < 0 || lgpl < gpl)) {
            if (head.contains("version 3"))
                return "LGPL-3.0";
            if (head.contains("version 2.1"))
                return "LGPL-2.1";
            return "LGPL-2.0";
        }
        if (gpl >= 0) {
            if (Pattern.compile("classpath.{0,40}exception").matcher(text).find())
                return "GPL-2.0 with Classpath Exception";
            if (head.contains("version 3"))
                return "GPL-3.0";
            return "GPL-2.0";
        }
        if (Pattern.compile("apache (software )?license.{0,40}version 1\\.1").matcher(head).find())
            return "Apache-1.1";
        if (Pattern.compile("apache license.{0,40}version 2\\.0").matcher(head).find() || head.contains("apache-2.0"))
            return "Apache-2.0";
        if (head.contains("eclipse public license"))
            return head.contains("2.0") ? "EPL-2.0" : "EPL-1.0";
        if (head.contains("mozilla public license"))
            return head.contains("2.0") ? "MPL-2.0" : "MPL-1.1";
        if (head.contains("common development and distribution license"))
            return head.contains("1.1") ? "CDDL-1.1" : "CDDL-1.0";
        if (head.contains("universal permissive license"))
            return "UPL-1.0";
        if (head.contains("unicode license v3"))
            return "Unicode-3.0";
        if (head.contains("unicode"))
            return "Unicode";
        if (text.contains("permission is hereby granted, free of charge"))
            return "MIT";
        if (text.contains("redistribution and use in source and binary forms"))
            return text.contains("neither the name") ? "BSD-3-Clause" : "BSD-2-Clause";
        if (text.contains("permission to use, copy, modify, and/or distribute"))
            return "ISC";
        if (text.contains("free and unencumbered software"))
            return "Unlicense";
        if (text.contains("altered source versions must be plainly marked"))
            return "Zlib";
        if (head.contains("creative commons"))
            return "Creative Commons";
        if (text.contains("public domain"))
            return "Public Domain";
        return "Unknown";
    }

    private static int indexOfFirst(String text, String... needles) {
        int best = -1;
        for (String needle : needles) {
            int index = text.indexOf(needle);
            if (index >= 0 && (best < 0 || index < best))
                best = index;
        }
        return best;
    }
}