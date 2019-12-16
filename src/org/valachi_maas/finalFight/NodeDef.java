package org.valachi_maas.finalFight;

import java.util.ArrayList;

enum node_t
{
	PATH,
	COVER
}

/**
 * Node to which an NPC can go to, in order to find its way around.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
public class NodeDef implements Comparable<Object>
{
	public float m_fXPos, m_fYPos;
	public int m_iNodeID;
	public node_t m_eType;
	public ArrayList<NodeDef> m_hLinkedNodes = new ArrayList<NodeDef>();
	
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * @param id - Node ID.
	 * @param x - X coordinate.
	 * @param y - Y coordinate.
	 * @param type - Type of node. Can be a path or cover node.
	 */
	public NodeDef( int id, float x, float y, node_t type )
	{
		m_iNodeID = id;
		m_fXPos = x;
		m_fYPos = y;
		m_eType = type;
	}
	
	/**
	 * Compares this node's ID to another's.
	 * Preconditions:
	 * 	- obj is a NodeDef instance.
	 * Postconditions:
	 * 	- Returns 0 if this node's ID is equal to the other's,
	 * 			 -1 if it's smaller, or
	 * 			  1 if it's greater.
	 */
	@Override
	public int compareTo( Object obj )
	{
		Utils.Assert( obj instanceof NodeDef );
		NodeDef node = (NodeDef)obj;
		if ( node.m_iNodeID == this.m_iNodeID )
			return 0;
		else if ( node.m_iNodeID < this.m_iNodeID )
			return 1;
		else
			return -1;
	}
	
	/**
	 * Returns this node in the form of a String.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns the node type, ID, and coordinates as a String.
	 */
	@Override
	public String toString()
	{
		return "[" +(m_eType == node_t.PATH ? "PATH" : "COVER")
				+ "|ID: " + m_iNodeID
				+ "|X: " + m_fXPos + " Y: " + m_fYPos + "]";
	}
}
