package org.valachi_maas.finalFight;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.util.*;

import javax.sound.sampled.*;

/**
 * Some useful functions and variables, kept in one place.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
public class Utils
{
	static final float max32f = 3.402823E38f;
	static final Rectangle npcBox = new Rectangle( 0, 0, 32, 32 );
	
	/**
	 * Returns the active instance of TheShootout.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns the active instance of TheShootout.
	 * @return g_hActiveInstance
	 */
	public static TheShootout ts()
	{
		return TheShootout.g_hActiveInstance;
	}
	
	/**
	 * If condition == false, then this crashes the thread
	 * it's called on with an AssertionError.
	 * Good for checking to make sure that preconditions
	 * and postconditions are fulfilled.
	 * No preconditions.
	 * Postconditions:
	 * 	- Crashes thread this is called on if condition == false.
	 * @param condition - The condition to check.
	 */
	public static void Assert( boolean condition )
	{
		if ( !condition ) throw new AssertionError();
	}
	
	/**
	 * Prevents zero-alpha pixels of images from appearing as white.
	 * @param im - The image to filter out.
	 * @return The same image, but with the alpha channel filtered out.
	 * No preconditions.
	 * Postconds:
	 * 	- The image is modified to remove pixels with zero alpha, and nothing else.
	 */
	public static Image makeColorTransparent(Image im)
	{
		ImageFilter filter = new RGBImageFilter() {
	        int transparentColor = Color.white.getRGB() | 0xFF000000;

	        public final int filterRGB(int x, int y, int rgb) {
	           if ((rgb | 0xFF000000) == transparentColor) {
	              return 0x00FFFFFF & rgb;
	           } else {
	              return rgb;
	           }
	        }
	     };
	    ImageProducer imgprod = new FilteredImageSource(im.getSource(), filter);
	    return Toolkit.getDefaultToolkit().createImage( imgprod );
	}
	
	/**
	 * Links two nodes together.
	 * Preconditions:
	 * 	- g_hNodes is populated with nodes.
	 * 	- Nodes with IDs id_a and id_b exist in g_hNodes.
	 * Postconditions:
	 * 	- Nodes with IDs id_a and id_b have each other as neighbors.
	 * @param id_a - First node.
	 * @param id_b - Second node.
	 */
	public static void linkNodes( int id_a, int id_b )
	{
		Assert( id_a != id_b );
		NodeDef a = findNode(id_a), b = findNode(id_b);
		if ( !a.m_hLinkedNodes.contains(b) )
			a.m_hLinkedNodes.add(b);
		if ( !b.m_hLinkedNodes.contains(a) )
			b.m_hLinkedNodes.add(a);
	}
	
	/**
	 * Finds a node using its ID, via binary search.
	 * Preconditions:
	 * 	- g_hNodes is populated with nodes.
	 * @param node - The node ID to search for.
	 * @return NodeDef with the input ID.
	 */
	public static NodeDef findNode( int node )
	{
		int start = 0, mid, end;
		end = ts().g_hNodes.size() - 1;
		do	//Non-recursive binary search.
		{
			if ( start > end )
			{
				System.err.println( "Node " + node + " not found!" );
				break;
			}
			
			mid = (start + end) / 2;
			if ( ((NodeDef)ts().g_hNodes.get(mid)).m_iNodeID == node )
			{
				return ts().g_hNodes.get(mid);
			}
			else if ( node < ((NodeDef)ts().g_hNodes.get(mid)).m_iNodeID )
			{
				start = 0;
				end = mid - 1;
				continue;
			}
			else
			{
				start = mid + 1;
				continue;
			}
		}
		while ( true );
		
		return null;
	}
	
	/**
	 * Performs an insertion sort on an array of objects.
	 * Preconditions:
	 * 	- Input objects implement the Comparable interface.
	 * 	- objs[] != null.
	 * Postconditions:
	 * 	- Array objs[] is sorted, from low to high.
	 * @param objs[] - The array to sort.
	 */
	@SuppressWarnings("unchecked")
	public static void insertionSort( Object[] objs )
	{
		Object curObj;
		int hole;
		for ( int i = 0; i < objs.length; i++ )
		{
			curObj = objs[i];
			hole = i;
			
			while ( hole > 0 && ((Comparable<Object>)objs[hole-1]).compareTo( curObj ) >= 0 )
			{
				objs[hole] = objs[--hole];
			}
			
			objs[hole] = curObj;
		}
	}
	
	/**
	 * Performs an insertion sort on an ArrayList.
	 * Preconditions:
	 * 	- Input objects implement the Comparable interface.
	 * 	- objs != null.
	 * Postconditions:
	 * 	- ArrayList objs is sorted, from low to high.
	 * @param objs - The ArrayList to sort.
	 */
	public static <T extends Comparable<Object>> void insertionSort( ArrayList<T> objs )
	{
		Assert( objs.get(0) instanceof Comparable<?> );
		T curObj;
		int hole;
		for ( int i = 0; i < objs.size(); i++ )
		{
			curObj = objs.get(i);
			hole = i;
			
			while ( hole > 0 && (objs.get(hole-1)).compareTo( curObj ) >= 0 )
			{
				objs.set(hole, objs.get(--hole));
			}
			
			objs.set(hole, curObj);
		}
	}
	
	/**
	 * Gets the distance between two entities.
	 * Preconditions:
	 * 	- a and b != null.
	 * 	- x and y values of a and b >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- Returns the two-dimensional distance between the two entities.
	 * @param a - Entity A.
	 * @param b - Entity B.
	 * @return Floating-point number representing distance between entities.
	 */
	public static float getDistance( BaseEntity a, BaseEntity b )
	{
		return (float)Math.sqrt(
				(b.getPosition()[0] - a.getPosition()[0]) * (b.getPosition()[0] - a.getPosition()[0]) +
				(b.getPosition()[1] - a.getPosition()[1]) * (b.getPosition()[1] - a.getPosition()[1])
				);
	}
	
	/**
	 * Gets the distance between two nodes.
	 * Preconditions:
	 * 	- a and b != null.
	 * 	- x and y values of a and b >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- Returns the two-dimensional distance between the two nodes.
	 * @param a - Node A.
	 * @param b - Node B.
	 * @return Floating-point number representing distance between nodes.
	 */
	public static float getDistance( NodeDef a, NodeDef b )
	{
		return (float)Math.sqrt(
				(b.m_fXPos - a.m_fXPos) * (b.m_fXPos - a.m_fXPos) +
				(b.m_fYPos - a.m_fYPos) * (b.m_fYPos - a.m_fYPos)
				);
	}
	
	/**
	 * Gets the distance between two points.
	 * Preconditions:
	 * 	- (x1,y1) and (x2,y2) >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- Returns the two-dimensional distance between the two points.
	 * @param x1 - Point 1 X.
	 * @param y1 - Point 1 Y.
	 * @param x2 - Point 2 X.
	 * @param y2 - Point 2 Y.
	 * @return Floating-point number representing distance between points.
	 */
	public static float getDistance( float x1, float y1, float x2, float y2 )
	{
		return (float)Math.sqrt( (x2-x1) * (x2-x1) + (y2-y1) * (y2-y1) );
	}
	
	/**
	 * Finds the order of nodes an NPC must follow to get to a destination node.
	 * Does this using an A* search algorithm.
	 * Preconditions:
	 * 	- start and goal != null.
	 *  - x and y values of start and goal >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- Stack containing the nodes to follow to get to goal is returned.
	 * @param start - Start node.
	 * @param goal - End node.
	 * @return Stack containing the nodes to go to, in order to get to goal.
	 */
	//Direct implementation of the pseudocode found on: https://en.wikipedia.org/wiki/A*_search_algorithm
	//@SuppressWarnings("null")
	public static Stack findPathUsingAStar( NodeDef start, NodeDef goal )
	{
		Assert( start != null );
		Assert( goal != null );
		
		ArrayList<NodeDef> closedSet = new ArrayList<NodeDef>();
		ArrayList<NodeDef> openSet = new ArrayList<NodeDef>();
		openSet.add( start );
		
		Map<NodeDef,NodeDef> cameFrom = new HashMap<NodeDef,NodeDef>();
		
		Map<NodeDef,Float> gScore = new HashMap<NodeDef,Float>();
		
		gScore.put(start, 0.0f);	//start's distance to itself is zero.
		
		Map<NodeDef,Float> fScore = new HashMap<NodeDef,Float>();
		fScore.put(start, getDistance( start, goal ));	//We want to get the most efficient path.
		
		NodeDef currentNode = start;
		while ( !openSet.isEmpty() )
		{
			//Find the node with the lowest score, and work based off that.
			float lowestScore = 3.402823E38f;
			NodeDef nodeWithLowestScore = null;
			NodeDef temp_currentNode;
			for ( int i = 0; i < openSet.size(); i++ )
			{
				temp_currentNode = openSet.get(i);
				if ( temp_currentNode == null ) continue;
				
				if ( fScore.get(temp_currentNode) < lowestScore )
				{
					lowestScore = fScore.get(temp_currentNode);
					nodeWithLowestScore = temp_currentNode;
				}
			}
			currentNode = nodeWithLowestScore;
			if ( currentNode == goal )
				return rebuildPath( cameFrom, currentNode );
			
			openSet.remove( currentNode );
			closedSet.add( currentNode );
			
			//Cycle through all the neighbors, and pick out the ones with the least distance.
			NodeDef curNeighbor;
			for ( int i = 0; i < currentNode.m_hLinkedNodes.size(); i++ )
			{
				curNeighbor = currentNode.m_hLinkedNodes.get(i);
				if ( closedSet.contains(curNeighbor) )
					continue;
				
				if ( !openSet.contains(curNeighbor) )
					openSet.add( curNeighbor );
				
				float tentative_gScore = gScore.get(currentNode) + getDistance(currentNode, curNeighbor);
				if ( tentative_gScore >= (gScore.containsKey(curNeighbor) ? gScore.get(curNeighbor) : 3.402823E38f) )
					continue;	//This is not a better node.
				
				//Best neighbor node so far. Store it.
				cameFrom.put(curNeighbor, currentNode);
				gScore.put(curNeighbor, tentative_gScore);
				fScore.put(curNeighbor, gScore.get(curNeighbor) + getDistance(curNeighbor, goal) ); 
			}
		}
		
		return null;	//None could be found.
	}
	
	/**
	 * Helper function, to rebuild the path generated by {@link Utils#findPathUsingAStar(NodeDef, NodeDef)}.
	 * Preconditions:
	 * 	- nodes and current != null.
	 * Postconditions:
	 * 	- New stack with the nodes in the correct order is returned.
	 * @param nodes
	 * @param current
	 * @return Stack with the nodes to follow in the correct order.
	 */
	private static Stack rebuildPath( Map<NodeDef,NodeDef> nodes, NodeDef current )
	{
		Stack temp = new Stack();
		temp.push(current);
		while ( nodes.keySet().contains(current) )
		{
			current = nodes.get(current);
			temp.push( current );
		}
		return temp;
	}
	
	/**
	 * Plays a sound once.
	 * Preconditions:
	 * 	- sound/[filename] points to a valid .wav file.
	 * Postconditions:
	 * 	- The sound is played.
	 * @param filename - The file to play.
	 */
	public static synchronized void playSound( final String filename )
	{
		new Thread( new Runnable() {
			public void run()
			{
				try
				{
					AudioInputStream inputStream = AudioSystem.getAudioInputStream( new File("sound/" + filename) );
					AudioFormat fmt = inputStream.getFormat();
					DataLine.Info info = new DataLine.Info(Clip.class, fmt);
					
					Clip clip = (Clip)AudioSystem.getLine( info );
					clip.open(inputStream);
					//((FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(volume);
					clip.start();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		} ).start();
	}
	
	/**
	 * Plays a sound, and loops it indefinitely.
	 * Preconditions:
	 * 	- sound/[filename] points to a valid .wav file.
	 * Postconditions:
	 * 	- The sound is looped, and returned.
	 * @param filename - The file to loop.
	 * @return Clip of the looped sound.
	 */
	public static Clip playLoopedSound( final String filename )
	{
		try
		{
			AudioInputStream inputStream = AudioSystem.getAudioInputStream( new File("sound/" + filename) );
			AudioFormat fmt = inputStream.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, fmt);
			
			Clip clip = (Clip)AudioSystem.getLine( info );
			clip.open(inputStream);
			clip.setLoopPoints(0, clip.getFrameLength() - 1);
			clip.loop(Clip.LOOP_CONTINUOUSLY);
			return clip;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Finds an entity using its name.
	 * Preconditions:
	 * 	- g_hAllEntities is populated with BaseEntity objects.
	 * Postconditions:
	 * 	- The entity with the input name is returned, if found. Null otherwise.
	 * @param name - Entity name.
	 * @return The entity with that name, or null if no such entity is found.
	 */
	public static BaseEntity findEntityByName( String name )
	{
		if ( name == null ) return null;
		
		for ( int i = 0; i < ts().g_hAllEntities.size(); i++ )
		{
			if ( ts().g_hAllEntities.get(i).getName().equals(name) )
				return ts().g_hAllEntities.get(i);
		}
		
		return null;
	}
	
	/**
	 * Creates a new entity, with a specific classname and properties.
	 * Primarily useful for {@link GameMap#parseInstructionQueue(Queue, boolean)},
	 * for the "createobj" command.
	 * A new entity will not be created, however, if it's in the exact same position as another.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- New BaseEntity created and returned. Null if not possible (ie. no such subclass with given classname exists).
	 * @param classname - The classname of the entity to create.
	 * @param name - The name of this new entity.
	 * @param x - New entity's X coordinate.
	 * @param y - New Entity's Y coordinate.
	 * @param health - New entity's starting health.
	 * @return New BaseEntity created.
	 */
	public static BaseEntity createNewEntity( String classname, String name, float x, float y, int health )
	{
		//Before we make a new entity, we gotta make sure nothing inhabits this space.
		BaseEntity curEnt = null;
		for ( int i = 0; i < ts().g_hAllEntities.size(); i++ )
		{
			curEnt = ts().g_hAllEntities.get(i);
			if ( curEnt.getPosition()[0] == x && curEnt.getPosition()[1] == y )
				return null;
		}
		
		BaseEntity newEnt = null;
		if ( classname.equals("daltonbro") )
		{
			newEnt = new DaltonBro( name, x, y, health );
		}
		else if ( classname.equals("earp") )
		{
			newEnt = new Earp( name, x, y, health );
		}
		else if ( classname.equals("police") )
		{
			newEnt = new Police( name, x, y, health );
		}
		else if ( classname.equals("revolver") )
		{
			newEnt = new Revolver( x, y, (BaseCombatCharacter)findEntityByName(name), 6, 0 );
		}
		else if ( classname.equals("dbshotgun") )
		{
			newEnt = new DBShotgun( x, y, (BaseCombatCharacter)findEntityByName(name), 2, 0 );
		}
		if ( classname.equals("rifle") )
		{
			newEnt = new Rifle( x, y, (BaseCombatCharacter)findEntityByName(name), 4, 0 );
		}
		
		return newEnt;
	}
}
