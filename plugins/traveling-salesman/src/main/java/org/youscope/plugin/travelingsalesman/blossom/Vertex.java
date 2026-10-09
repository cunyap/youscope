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
 ******************************************************************************/
package org.youscope.plugin.travelingsalesman.blossom;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A vertex (elementary node) in the graph.
 * Each vertex can contain some content which might be used to identify or label the vertex, or to store anything one wants.
 * @author Moritz Lang
 *
 */
public class Vertex extends PseudoNode
{
	private final Object content;
	private final ArrayList<Edge> edgesToChilds = new ArrayList<Edge>();
	private Edge edgeToParent = null;
	
	/**
	 * Contstructor.
	 * @param content Content stored in this vertex.
	 */
	public Vertex(Object content)
	{
		this.content = content;
	}
	@Override
	boolean isBoundary(Edge edge) 
	{
		if(edge.getV1().equals(this) || edge.getV2().equals(this))
			return true;
		return false;
	}
	
	@Override
	boolean contains(PseudoNode node) 
	{
		return equals(node);
	}
	
	/**
	 * Returns the content stored in this vertex.
	 * @return Content of vertex.
	 */
	public Object getContent()
	{
		return content;
	}
	
	@Override
	public String toString()
	{
		String contentString = content == null ? "UNDEFINED" : content.toString();
		if(containedIn == null)
			return contentString+"["+getLabel().toString()+","+Double.toString(dual)+"]";
		return contentString+"["+Double.toString(dual)+"]";
	}
	@Override
	Edge getEdgeToParent() {
		return edgeToParent;
	}
	@Override
	Collection<Edge> getEdgesToChildren() {
		return edgesToChilds;
	}
	@Override
	void setEdgeToParent(Edge edge) throws BlossomException {
		if(edge!=null &&edge.getV1() != this && edge.getV2() != this)
			throw new BlossomException("Edge does not link to this vertex.");
		edgeToParent = edge;
	}
	@Override
	void addEdgeToChild(Edge edge) throws BlossomException 
	{
		if(edge.getV1() != this && edge.getV2() != this)
			throw new BlossomException("Edge does not link to this vertex.");
		edgesToChilds.add(edge);
	}
	@Override
	void clearEdgesToChilds() {
		edgesToChilds.clear();
	}
	@Override
	void removeEdgesToChilds(Collection<Edge> edges) {
		edgesToChilds.removeAll(edges);
	}
	@Override
	void removeEdgeToChild(Edge edge) {
		edgesToChilds.remove(edge);
	}
}
