/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
package org.youscope.plugin.cameditor.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.youscope.common.Well;
import org.youscope.plugin.cameditor.model.CameLayoutModel;
import org.youscope.plugin.cameditor.model.EditableWell;
import org.youscope.plugin.custommicroplates.WellShape;

/**
 * The interactive editing surface: a zoom/pan image backdrop with wells drawn on top in scene (pixel)
 * coordinates. Supports adding wells by dragging, selecting/moving/resizing them, deleting them,
 * toggling their active state, an optional snap-to grid, and a measure tool used to calibrate the
 * µm/px scale from a known distance.
 *
 * @author Andreas P. Cuny
 */
public class CameCanvas extends JPanel
{
	private static final long serialVersionUID = 1L;

	/** Interaction modes. */
	public enum Mode { SELECT, ADD, MEASURE }

	/** Listener for the surrounding UI. */
	public interface Listener
	{
		void selectionChanged(EditableWell selected);
		/** Called when the measure tool has captured a pixel distance the user should assign a length to. */
		void distanceMeasured(double pixelDistance);
		void modelChanged();
	}

	private BufferedImage backdrop;
	private final CameLayoutModel model;
	private Mode mode = Mode.SELECT;

	// view transform: screen = scene*zoom + offset
	private double zoom = 1.0;
	private double offsetX = 0;
	private double offsetY = 0;

	// grid
	private boolean showGrid = true;
	private boolean snapToGrid = false;
	private double gridSpacingScene = 50; // in scene pixels

	// interaction state
	private EditableWell selected;
	// additional wells selected for batch operations (e.g. Select-All then Delete); may be empty
	private final Set<EditableWell> multiSelection = new LinkedHashSet<EditableWell>();
	private Point2D.Double dragStartScene;
	private EditableWell newWellInProgress;
	private int activeHandle = -1; // 0..3 corners, -1 none
	private boolean movingBody = false;
	private int panLastX, panLastY;
	private boolean panning = false;
	private final List<Point2D.Double> measurePoints = new ArrayList<Point2D.Double>();

	// undo: a stack of actions that revert the most recent edit (move/resize/add/delete)
	private final Deque<Runnable> undoStack = new ArrayDeque<Runnable>();
	private static final int UNDO_LIMIT = 200;
	// snapshot of the well being moved/resized, taken when the gesture starts
	private EditableWell editPreWell;
	private double editPreX, editPreY, editPreW, editPreH;

	private final List<Listener> listeners = new ArrayList<Listener>();

	private static final double HANDLE_SIZE = 8;

	public CameCanvas(CameLayoutModel model)
	{
		this.model = model;
		setBackground(new Color(0x2b2b2b));
		setFocusable(true);
		MouseHandler handler = new MouseHandler();
		addMouseListener(handler);
		addMouseMotionListener(handler);
		addMouseWheelListener(handler);
		addKeyListener(new KeyHandler());

		// Ctrl+Z (Cmd+Z on macOS) to undo the last move/resize/add/delete, even if focus drifted.
		int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask), "came-undo");
		getActionMap().put("came-undo", new AbstractAction()
		{
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent e) { undo(); }
		});

		// Ctrl+A (Cmd+A on macOS) to select every well, e.g. to batch-delete them.
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuMask), "came-select-all");
		getActionMap().put("came-select-all", new AbstractAction()
		{
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent e) { selectAll(); }
		});
	}

	// Undo

	private void pushUndo(Runnable revert)
	{
		undoStack.push(revert);
		while(undoStack.size() > UNDO_LIMIT)
			undoStack.removeLast();
	}

	/** @return {@code true} if there is an edit that can be undone. */
	public boolean canUndo() { return !undoStack.isEmpty(); }

	/** Reverts the most recent move/resize/add/delete. */
	public void undo()
	{
		if(undoStack.isEmpty())
			return;
		undoStack.pop().run();
		model.reassignIdentifiers();
		fireSelection();
		fireModelChanged();
		repaint();
	}

	/** Clears the measure overlay (the two crosshairs and their connecting line). */
	public void clearMeasure() { measurePoints.clear(); repaint(); }

	private void snapshotEditStart(EditableWell well)
	{
		editPreWell = well;
		if(well != null)
		{
			editPreX = well.getXPx();
			editPreY = well.getYPx();
			editPreW = well.getWidthPx();
			editPreH = well.getHeightPx();
		}
	}

	public void addListener(Listener listener) { listeners.add(listener); }

	private void fireSelection() { for(Listener l : listeners) l.selectionChanged(selected); }
	private void fireModelChanged() { for(Listener l : listeners) l.modelChanged(); }

	public void setBackdrop(BufferedImage image)
	{
		this.backdrop = image;
		fitToView();
		repaint();
	}

	public BufferedImage getBackdrop() { return backdrop; }

	public void setMode(Mode mode)
	{
		this.mode = mode;
		measurePoints.clear();
		repaint();
	}

	public Mode getMode() { return mode; }

	public void setShowGrid(boolean show) { this.showGrid = show; repaint(); }
	public void setSnapToGrid(boolean snap) { this.snapToGrid = snap; }
	public void setGridSpacingScene(double spacing) { if(spacing > 0) { this.gridSpacingScene = spacing; repaint(); } }
	public double getGridSpacingScene() { return gridSpacingScene; }

	public EditableWell getSelected() { return selected; }

	public void setSelected(EditableWell well)
	{
		this.selected = well;
		fireSelection();
		repaint();
	}

	public void deleteSelected()
	{
		if(selected != null)
		{
			final EditableWell removed = selected;
			final double x = removed.getXPx(), y = removed.getYPx(), w = removed.getWidthPx(), h = removed.getHeightPx();
			final WellShape shape = removed.getShape();
			final boolean active = removed.isActive();
			model.removeWell(removed);
			pushUndo(new Runnable()
			{
				@Override public void run()
				{
					EditableWell re = model.addWell(x, y, w, h);
					re.setShape(shape);
					re.setActive(active);
					selected = re;
				}
			});
			selected = null;
			fireSelection();
			fireModelChanged();
			repaint();
		}
	}

	/** Selects every well (for batch operations such as delete). Clears any single selection. */
	public void selectAll()
	{
		multiSelection.clear();
		multiSelection.addAll(model.getWells());
		selected = null;
		fireSelection();
		repaint();
	}

	/** @return an unmodifiable view of the wells currently in the batch selection (may be empty). */
	public Set<EditableWell> getMultiSelection() { return java.util.Collections.unmodifiableSet(multiSelection); }

	/**
	 * Deletes all currently selected wells: the batch selection if there is one, otherwise the single
	 * selected well. The whole deletion is a single undo step.
	 */
	public void deleteSelection()
	{
		final List<EditableWell> toDelete = new ArrayList<EditableWell>();
		if(!multiSelection.isEmpty())
			toDelete.addAll(multiSelection);
		else if(selected != null)
			toDelete.add(selected);
		if(toDelete.isEmpty())
			return;

		final double[][] geom = new double[toDelete.size()][4];
		final WellShape[] shapes = new WellShape[toDelete.size()];
		final boolean[] actives = new boolean[toDelete.size()];
		for(int i = 0; i < toDelete.size(); i++)
		{
			EditableWell w = toDelete.get(i);
			geom[i][0] = w.getXPx(); geom[i][1] = w.getYPx(); geom[i][2] = w.getWidthPx(); geom[i][3] = w.getHeightPx();
			shapes[i] = w.getShape();
			actives[i] = w.isActive();
			model.removeWell(w);
		}
		pushUndo(new Runnable()
		{
			@Override public void run()
			{
				for(int i = 0; i < geom.length; i++)
				{
					EditableWell re = model.addWell(geom[i][0], geom[i][1], geom[i][2], geom[i][3]);
					re.setShape(shapes[i]);
					re.setActive(actives[i]);
				}
			}
		});
		multiSelection.clear();
		selected = null;
		fireSelection();
		fireModelChanged();
		repaint();
	}

	public void toggleSelectedActive()
	{
		if(selected != null)
		{
			selected.setActive(!selected.isActive());
			fireModelChanged();
			repaint();
		}
	}

	public void setSelectedShape(WellShape shape)
	{
		if(selected != null)
		{
			selected.setShape(shape);
			fireModelChanged();
			repaint();
		}
	}

	// View helpers

	public void fitToView()
	{
		int w = getWidth() <= 0 ? 800 : getWidth();
		int h = getHeight() <= 0 ? 600 : getHeight();
		double contentW, contentH;
		if(backdrop != null)
		{
			contentW = backdrop.getWidth();
			contentH = backdrop.getHeight();
		}
		else
		{
			double[] b = modelBounds();
			contentW = Math.max(1, b[2]);
			contentH = Math.max(1, b[3]);
		}
		double zx = (w - 40) / contentW;
		double zy = (h - 40) / contentH;
		zoom = Math.min(zx, zy);
		if(zoom <= 0 || Double.isInfinite(zoom))
			zoom = 1;
		offsetX = (w - contentW * zoom) / 2.0;
		offsetY = (h - contentH * zoom) / 2.0;
		repaint();
	}

	private double[] modelBounds()
	{
		double minX = 0, minY = 0, maxX = 1, maxY = 1;
		boolean any = false;
		for(EditableWell wl : model.getWells())
		{
			if(!any) { minX = wl.getXPx(); minY = wl.getYPx(); maxX = wl.getXPx()+wl.getWidthPx(); maxY = wl.getYPx()+wl.getHeightPx(); any = true; }
			minX = Math.min(minX, wl.getXPx());
			minY = Math.min(minY, wl.getYPx());
			maxX = Math.max(maxX, wl.getXPx()+wl.getWidthPx());
			maxY = Math.max(maxY, wl.getYPx()+wl.getHeightPx());
		}
		return new double[]{minX, minY, maxX - minX, maxY - minY};
	}

	private Point2D.Double toScene(int sx, int sy)
	{
		return new Point2D.Double((sx - offsetX) / zoom, (sy - offsetY) / zoom);
	}

	private double sceneToScreenX(double x) { return x * zoom + offsetX; }
	private double sceneToScreenY(double y) { return y * zoom + offsetY; }

	private double snap(double value)
	{
		if(!snapToGrid || gridSpacingScene <= 0)
			return value;
		return Math.round(value / gridSpacingScene) * gridSpacingScene;
	}

	// Painting 

	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if(backdrop != null)
		{
			int dx = (int) Math.round(offsetX);
			int dy = (int) Math.round(offsetY);
			int dw = (int) Math.round(backdrop.getWidth() * zoom);
			int dh = (int) Math.round(backdrop.getHeight() * zoom);
			g.drawImage(backdrop, dx, dy, dw, dh, null);
		}

		if(showGrid)
			paintGrid(g);

		for(EditableWell well : model.getWells())
			paintWell(g, well);

		paintMeasure(g);

		g.dispose();
	}

	private void paintGrid(Graphics2D g)
	{
		g.setColor(new Color(255, 255, 255, 40));
		g.setStroke(new BasicStroke(1f));
		double stepScreen = gridSpacingScene * zoom;
		if(stepScreen < 4)
			return; // too dense to be useful
		double startX = offsetX % stepScreen;
		double startY = offsetY % stepScreen;
		for(double x = startX; x < getWidth(); x += stepScreen)
			g.draw(new Line2D.Double(x, 0, x, getHeight()));
		for(double y = startY; y < getHeight(); y += stepScreen)
			g.draw(new Line2D.Double(0, y, getWidth(), y));
	}

	private void paintWell(Graphics2D g, EditableWell well)
	{
		double x = sceneToScreenX(well.getXPx());
		double y = sceneToScreenY(well.getYPx());
		double w = well.getWidthPx() * zoom;
		double h = well.getHeightPx() * zoom;

		boolean isSelected = well == selected || multiSelection.contains(well);
		Color fill = well.isActive() ? new Color(0x33, 0x99, 0xFF, 70) : new Color(0x88, 0x88, 0x88, 70);
		Color line = well.isActive() ? new Color(0x33, 0x99, 0xFF) : new Color(0xAA, 0xAA, 0xAA);
		if(isSelected)
			line = new Color(0xFF, 0xCC, 0x00);

		g.setColor(fill);
		if(well.getShape() == WellShape.ELLIPSE)
			g.fill(new Ellipse2D.Double(x, y, w, h));
		else
			g.fill(new Rectangle2D.Double(x, y, w, h));

		g.setStroke(new BasicStroke(isSelected ? 2.2f : 1.4f));
		g.setColor(line);
		if(well.getShape() == WellShape.ELLIPSE)
			g.draw(new Ellipse2D.Double(x, y, w, h));
		else
			g.draw(new Rectangle2D.Double(x, y, w, h));

		// identifier label
		if(well.hasIdentifier() && w > 16 && h > 12)
		{
			g.setColor(isSelected ? Color.YELLOW : Color.WHITE);
			String label = new Well(Math.max(0, well.getWellY()), Math.max(0, well.getWellX())).getWellName();
			g.drawString(label, (int) (x + 3), (int) (y + 13));
		}

		// resize handles (4 corners + 4 edge mid-points) when selected
		if(isSelected)
		{
			g.setColor(new Color(0xFF, 0xCC, 0x00));
			for(double[] handle : handlePoints(x, y, w, h))
				g.fill(new Rectangle2D.Double(handle[0] - HANDLE_SIZE / 2, handle[1] - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE));
		}
	}

	/**
	 * The eight resize-handle anchor points for a rectangle, in a fixed order used by painting, hit
	 * testing and resizing. Index: 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right,
	 * 4 top-mid, 5 bottom-mid, 6 left-mid, 7 right-mid. Works in either screen or scene coordinates.
	 */
	private double[][] handlePoints(double x, double y, double w, double h)
	{
		return new double[][]{
			{x, y}, {x + w, y}, {x, y + h}, {x + w, y + h},
			{x + w / 2, y}, {x + w / 2, y + h}, {x, y + h / 2}, {x + w, y + h / 2}
		};
	}

	private void paintMeasure(Graphics2D g)
	{
		if(measurePoints.isEmpty())
			return;
		g.setColor(new Color(0xFF, 0x55, 0x55));
		g.setStroke(new BasicStroke(2f));
		for(Point2D.Double p : measurePoints)
		{
			double sx = sceneToScreenX(p.x);
			double sy = sceneToScreenY(p.y);
			g.draw(new Line2D.Double(sx - 6, sy, sx + 6, sy));
			g.draw(new Line2D.Double(sx, sy - 6, sx, sy + 6));
		}
		if(measurePoints.size() == 2)
		{
			Point2D.Double a = measurePoints.get(0), b = measurePoints.get(1);
			g.draw(new Line2D.Double(sceneToScreenX(a.x), sceneToScreenY(a.y), sceneToScreenX(b.x), sceneToScreenY(b.y)));
		}
	}

	// Hit testing

	private int hitHandle(EditableWell well, Point2D.Double scene)
	{
		if(well == null)
			return -1;
		double x = well.getXPx(), y = well.getYPx(), w = well.getWidthPx(), h = well.getHeightPx();
		double[][] handles = handlePoints(x, y, w, h);
		double tol = (HANDLE_SIZE + 2) / zoom;
		for(int i = 0; i < handles.length; i++)
		{
			if(Math.abs(scene.x - handles[i][0]) <= tol && Math.abs(scene.y - handles[i][1]) <= tol)
				return i;
		}
		return -1;
	}

	private EditableWell hitWell(Point2D.Double scene)
	{
		List<EditableWell> wells = model.getWells();
		for(int i = wells.size() - 1; i >= 0; i--)
		{
			if(wells.get(i).containsPx(scene.x, scene.y))
				return wells.get(i);
		}
		return null;
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(900, 640);
	}

	// Mouse / wheel

	private final class MouseHandler extends MouseAdapter implements MouseWheelListener
	{
		@Override
		public void mousePressed(MouseEvent e)
		{
			requestFocusInWindow();
			if(javax.swing.SwingUtilities.isRightMouseButton(e) || javax.swing.SwingUtilities.isMiddleMouseButton(e))
			{
				panning = true;
				panLastX = e.getX();
				panLastY = e.getY();
				return;
			}
			Point2D.Double scene = toScene(e.getX(), e.getY());
			dragStartScene = scene;

			if(mode == Mode.MEASURE)
			{
				if(measurePoints.size() >= 2)
					measurePoints.clear();
				measurePoints.add(new Point2D.Double(scene.x, scene.y));
				repaint();
				if(measurePoints.size() == 2)
				{
					final double d = measurePoints.get(0).distance(measurePoints.get(1));
					// Defer the (modal) distance prompt so the pending repaint runs first: this way both
					// crosshairs and the connecting line are visible on screen while the dialog is open.
					javax.swing.SwingUtilities.invokeLater(new Runnable()
					{
						@Override public void run()
						{
							for(Listener l : listeners)
								l.distanceMeasured(d);
						}
					});
				}
				return;
			}

			if(mode == Mode.ADD)
			{
				double sx = snap(scene.x), sy = snap(scene.y);
				newWellInProgress = model.addWell(sx, sy, 1, 1);
				selected = newWellInProgress;
				fireSelection();
				repaint();
				return;
			}

			// SELECT
			if(!multiSelection.isEmpty())
				multiSelection.clear();
			int handle = hitHandle(selected, scene);
			if(handle >= 0)
			{
				activeHandle = handle;
				movingBody = false;
				snapshotEditStart(selected);
				return;
			}
			EditableWell hit = hitWell(scene);
			selected = hit;
			activeHandle = -1;
			movingBody = hit != null;
			if(movingBody)
				snapshotEditStart(hit);
			fireSelection();
			repaint();
		}

		@Override
		public void mouseDragged(MouseEvent e)
		{
			if(panning)
			{
				offsetX += e.getX() - panLastX;
				offsetY += e.getY() - panLastY;
				panLastX = e.getX();
				panLastY = e.getY();
				repaint();
				return;
			}
			Point2D.Double scene = toScene(e.getX(), e.getY());

			if(mode == Mode.ADD && newWellInProgress != null)
			{
				double x0 = Math.min(dragStartScene.x, scene.x);
				double y0 = Math.min(dragStartScene.y, scene.y);
				double w = Math.abs(scene.x - dragStartScene.x);
				double h = Math.abs(scene.y - dragStartScene.y);
				newWellInProgress.setBoundsPx(snap(x0), snap(y0), Math.max(1, w), Math.max(1, h));
				repaint();
				return;
			}

			if(mode == Mode.SELECT && selected != null)
			{
				if(activeHandle >= 0)
				{
					resizeSelected(scene);
					repaint();
					return;
				}
				if(movingBody && dragStartScene != null)
				{
					double dx = scene.x - dragStartScene.x;
					double dy = scene.y - dragStartScene.y;
					selected.setXPx(snap(selected.getXPx() + dx));
					selected.setYPx(snap(selected.getYPx() + dy));
					dragStartScene = scene;
					repaint();
					return;
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			if(panning)
			{
				panning = false;
				return;
			}
			if(mode == Mode.ADD && newWellInProgress != null)
			{
				// discard zero-size accidental clicks
				if(newWellInProgress.getWidthPx() < 3 || newWellInProgress.getHeightPx() < 3)
				{
					model.removeWell(newWellInProgress);
				}
				else
				{
					final EditableWell added = newWellInProgress;
					pushUndo(new Runnable()
					{
						@Override public void run()
						{
							model.removeWell(added);
							if(selected == added)
								selected = null;
						}
					});
					model.reassignIdentifiers();
				}
				newWellInProgress = null;
				fireModelChanged();
				repaint();
				return;
			}
			if(mode == Mode.SELECT && (movingBody || activeHandle >= 0))
			{
				// record an undo only if the move/resize actually changed the geometry
				if(editPreWell != null
						&& (editPreX != editPreWell.getXPx() || editPreY != editPreWell.getYPx()
						|| editPreW != editPreWell.getWidthPx() || editPreH != editPreWell.getHeightPx()))
				{
					final EditableWell w = editPreWell;
					final double ox = editPreX, oy = editPreY, ow = editPreW, oh = editPreH;
					pushUndo(new Runnable()
					{
						@Override public void run()
						{
							w.setBoundsPx(ox, oy, ow, oh);
							selected = w;
						}
					});
				}
				model.reassignIdentifiers();
				fireModelChanged();
			}
			editPreWell = null;
			activeHandle = -1;
			movingBody = false;
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e)
		{
			double factor = e.getPreciseWheelRotation() < 0 ? 1.1 : 1 / 1.1;
			// zoom around the cursor
			double sceneX = (e.getX() - offsetX) / zoom;
			double sceneY = (e.getY() - offsetY) / zoom;
			zoom *= factor;
			if(zoom < 0.02) zoom = 0.02;
			if(zoom > 50) zoom = 50;
			offsetX = e.getX() - sceneX * zoom;
			offsetY = e.getY() - sceneY * zoom;
			repaint();
		}
	}

	private void resizeSelected(Point2D.Double scene)
	{
		double x = selected.getXPx(), y = selected.getYPx();
		double x2 = x + selected.getWidthPx(), y2 = y + selected.getHeightPx();
		double sx = snap(scene.x), sy = snap(scene.y);
		switch(activeHandle)
		{
			case 0: x = sx; y = sy; break;          // top-left
			case 1: x2 = sx; y = sy; break;         // top-right
			case 2: x = sx; y2 = sy; break;         // bottom-left
			case 3: x2 = sx; y2 = sy; break;        // bottom-right
			case 4: y = sy; break;                  // top edge
			case 5: y2 = sy; break;                 // bottom edge
			case 6: x = sx; break;                  // left edge
			case 7: x2 = sx; break;                 // right edge
			default: break;
		}
		double nx = Math.min(x, x2), ny = Math.min(y, y2);
		double nw = Math.max(1, Math.abs(x2 - x)), nh = Math.max(1, Math.abs(y2 - y));
		selected.setBoundsPx(nx, ny, nw, nh);
	}

	private final class KeyHandler extends KeyAdapter
	{
		@Override
		public void keyPressed(KeyEvent e)
		{
			if(e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
				deleteSelection();
			else if(e.getKeyCode() == KeyEvent.VK_SPACE)
				toggleSelectedActive();
			else if(e.getKeyCode() == KeyEvent.VK_F)
				fitToView();
		}
	}
}
