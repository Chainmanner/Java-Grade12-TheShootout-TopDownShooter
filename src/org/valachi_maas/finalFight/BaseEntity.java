package org.valachi_maas.finalFight;

import java.awt.*;
import java.awt.image.*;
import java.util.ArrayList;

/**
 * Base class for all entities. All NPC, weapons, and props must inherit this.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
abstract public class BaseEntity
{
	private final String m_szName;				//The name other entities refer to this by.
	private Image m_hIcon;
	private float m_fXPos, m_fYPos;				//X and Y coordinates of this entity.
	private float m_fMoveXSpeed, m_fMoveYSpeed;	//Move speed for when this entity glides somewhere.
	
	private int m_iHealth;
	private int m_iAngle;						
	private Rectangle m_hCollisionRect;			//Entity's collision bounding box.
	private boolean m_bSolid;					//Whether or not this collides with other things.
	
	private boolean m_bShouldDraw = true;
	
	private float m_fIntendedXPos, m_fIntendedYPos;	//Position to glide to.
	private int m_iPrevCollisions = 0;			//Collisions that occured the previous tick.
	private int m_iCurCollisions = 0;			//Collisions occuring this tick.
	private int m_iStartMoveTime = 0;			//The tick that this started gliding.
	private int m_iMoveTime = 0;				//The time, in ticks, this takes to glide to the intended position.
	private boolean m_bIntentToGlide = false;	//Whether or not this entity intends to glide somewhere.
	
	private int m_iIntendedAngle = m_iAngle;	//The angle to rotate to.
	private int m_iRotateSpeed = 0;				//Ticks before moving 1 degree.
	private int m_iNextRotateTime = 0;			//Time when it can rotate another degree.
	private boolean m_bRotateDir = false;		//Rotation direction. false = left, true = right
	private boolean m_bIntentToTurn = false;	//Whether or not this intends to turn smoothly.
	
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- health > 0 (why would you set it to zero, anyway?).
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- New BaseEntity instance created.
	 * @param name - The name that the engine refers to this by.
	 * @param icon - This entity's icon to be displayed.
	 * @param x - X position.
	 * @param y - Y position.
	 * @param health - This entity's health.
	 * @param angle - This entity's angle.
	 * @param collisionRect - This entity's collision bounding box.
	 * @param bSolid - Whether or not this entity collides with the world and other entities.
	 */
	public BaseEntity( String name, Image icon, float x, float y, int health, int angle, Rectangle collisionRect,
			boolean bSolid )
	{
		if ( icon == null )
		{
			icon = Toolkit.getDefaultToolkit().getImage("materials/temp.png");
		}
		m_szName = name;
		m_hIcon = Utils.makeColorTransparent(icon);
		m_fXPos = x;
		m_fYPos = y;
		m_iHealth = health;
		m_iAngle = angle;
		m_iIntendedAngle = angle;
		m_bSolid = bSolid;
		
		if ( collisionRect == null )
			m_hCollisionRect = new Rectangle(0, 0, 0, 0);
		else
			m_hCollisionRect = collisionRect;
	}
	
	/**
	 * Gets this entity's name.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns the entity's name.
	 * @return m_szName
	 */
	public String getName()
	{
		return m_szName;
	}
	
	/**
	 * Gets this entity's icon.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns the entity's drawn image.
	 * @return m_hIcon
	 */
	public Image getIcon()
	{
		return m_hIcon;
	}
	
	/**
	 * Sets this entity's icon.
	 * No preconditions.
	 * Postconditions:
	 * 	- Sets m_hIcon to the Image argument.
	 * @param icon - The image to replace m_hIcon with.
	 */
	public void setIcon( Image icon )
	{
		m_hIcon = icon;
	}
	
	/**
	 * Gets the width of this entity's icon.
	 * No preconditions.
	 * Postconditions:
	 * 	- Gets the icon's width.
	 * @param ptr - ImageObserver
	 * @return m_hIcon.getWidth(ImageObserver)
	 */
	public int getWidth(ImageObserver ptr)
	{
		return m_hIcon.getWidth(ptr);
	}
	
	/**
	 * Gets the height of this entity's icon.
	 * No preconditions.
	 * Postconditions:
	 * 	- Gets the icon's height.
	 * @param ptr - ImageObserver
	 * @return m_hIcon.getWidth(ImageObserver)
	 */
	public int getHeight(ImageObserver ptr)
	{
		return m_hIcon.getWidth(ptr);
	}
	
	/**
	 * Gets the collision bounding box of this entity.
	 * No preconditions.
	 * Postconditions:
	 * 	- Gets this entity's collision Rectangle.
	 * @return m_hCollisionRect
	 */
	public Rectangle getCollisionRect()
	{
		return m_hCollisionRect;
	}
	
	/**
	 * Sets this entity's collision bounding box.
	 * No preconditions.
	 * Postconditions:
	 * 	- m_hCollisionRect is set to the Rectangle argument.
	 * @param collisionRect - New Rectangle to replace this one's.
	 */
	protected void setCollisionRect( Rectangle collisionRect )
	{
		m_hCollisionRect = collisionRect;
	}
	
	/**
	 * Returns whether or not this entity is solid.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns whether or not entity collides with other things.
	 * @return m_bSolid
	 */
	public boolean isSolid()
	{
		return m_bSolid;
	}
	
	/**
	 * Sets this entity's solidity.
	 * No preconditions.
	 * Postconditions:
	 * 	- m_bSolid is set to the boolean argument.
	 * @param bSolid - Whether or not this entity is solid.
	 */
	public void setSolid( boolean bSolid )
	{
		m_bSolid = bSolid;
	}
	
	/**
	 * Gets this entity's position.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns an array of two floats, representing the x and y coordinates respectively.
	 * @return m_fXPos, m_fYPos
	 */
	public float[] getPosition()
	{
		float[] temp = { m_fXPos, m_fYPos };
		return temp;
	}
	
	/**
	 * Sets this entity's position.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- Entity is moved along the X and Y axes if possible, independent of each other.
	 * 		(ie. if it can move on X but not Y, moves on X only).
	 * 	- Returns an integer representing which collisions happen.
	 * @param x - X coordinate.
	 * @param y - Y coordinate.
	 * @param bCollide - Whether or not this should collide with other objects.
	 * @return 0 for no collisions; 1 for X collision; 2 for Y collision; 3 for X and Y collision together.
	 */
	public int setPosition( float x, float y, boolean bCollide )
	{
		if ( bCollide )	//If this is supposed to collide...
		{
			//Add all the collision rectangles that exist in the engine (world and entity alike) to an ArrayList.
			ArrayList<Rectangle> hCollisionRects = new ArrayList<Rectangle>(Utils.ts().g_hWorldCollisionRects);
			BaseEntity curEnt = null;
			for ( int i = 0; i < Utils.ts().g_hAllEntities.size(); i++ )
			{
				curEnt = Utils.ts().g_hAllEntities.get(i);
				//Don't collide if the entity in question isn't solid, or if they're an NPC along with this.
				if ( curEnt.isSolid() && !(this instanceof BaseCombatCharacter && curEnt instanceof BaseCombatCharacter) )
					hCollisionRects.add( curEnt.getCollisionRect() );
			}
			
			//For every axis, create a rectangle with the desired X and Y position.
			//If the rectangle intersects with another collision bounding box on one axis, don't move along that axis.
			{
				int iErrCode = 0;	//0 = no intersection, 1 = x-intersection, 2 = y-intersection, 3 = both
				boolean bXIntersects = false, bYIntersects = false;
				for ( int i = 0; i < hCollisionRects.size(); i++ )
				{
					if ( hCollisionRects.get(i) == null ) break;	//Here to prevent a nullptr error.
					Rectangle curRect = hCollisionRects.get(i);
					
					if ( curRect == this.m_hCollisionRect )
						continue;
					Rectangle rect2 = new Rectangle( (int)x, m_hCollisionRect.y, m_hCollisionRect.width, m_hCollisionRect.height );
					
					if ( curRect.intersects(rect2) && m_bSolid )
					{
						bXIntersects = true;
						iErrCode++;
						break;
					}
				}
				if ( !bXIntersects )
					m_fXPos = x;
				
				for ( int i = 0; i < hCollisionRects.size(); i++ )
				{
					if ( hCollisionRects.get(i) == null ) break;	//Here to prevent a nullptr error.
					Rectangle curRect = (Rectangle)hCollisionRects.get(i);
					if ( curRect == this.m_hCollisionRect )
						continue;
					Rectangle rect2 = new Rectangle( m_hCollisionRect.x, (int)y, m_hCollisionRect.width, m_hCollisionRect.height );

					if ( curRect.intersects(rect2) && m_bSolid )
					{
						bYIntersects = true;
						iErrCode += 2;
						break;
					}
				}
				if ( !bYIntersects )
					m_fYPos = y;
				
				return iErrCode;
			}
		}
		else	//If we don't care about collisions, no point in checking for them.
		{
			m_fXPos = x;
			m_fYPos = y;
		}
		
		return 0;
	}
	
	/**
	 * Sets the position where an entity wants to glide toward.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- m_fIntendedXPos is set to X argument.
	 * 	- m_fIntendedYPos is set to Y argument.
	 * 	- m_fMoveXSpeed and m_fMoveYSpeed values are calculated.
	 * 	- m_iStartMoveTime is set to the current time, in ticks.
	 * 	- m_iMoveTime is set to the moveTime argument.
	 * 	- m_bIntentToGlide is set to true.
	 * @param x - X coordinate.
	 * @param y - Y coordinate.
	 * @param moveTime - Time in ticks to get to the destination.
	 */
	public void setIntendedPosition( float x, float y, int moveTime )
	{
		Utils.Assert( !(this instanceof Projectile) );
		//Utils.Assert( moveTime >= 0 );
		if ( moveTime <= 0 ) moveTime = 1;
		m_fIntendedXPos = x;
		m_fIntendedYPos = y;
		m_fMoveXSpeed = (x - m_fXPos) / moveTime;
		m_fMoveYSpeed = (y - m_fYPos) / moveTime;
		
		m_iStartMoveTime = Utils.ts().g_iTicks;
		m_iMoveTime = moveTime;
		m_bIntentToGlide = true;
	}
	
	/**
	 * Gets whether or not this entity is gliding.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns whether or not this entity is moving.
	 * @return m_bIntentToGlide
	 */
	public boolean isMoving()
	{
		return m_bIntentToGlide;
	}
	
	/**
	 * Gets this entity's angle.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns this entity's angle in degrees.
	 * @return m_iAngle
	 */
	public int getAngle()
	{
		return m_iAngle;
	}
	
	/**
	 * Sets this entity's angle.
	 * No preconditions.
	 * Postconditions:
	 * 	- m_iAngle is set to the integer argument.
	 * 	- So is m_iIntendedAngle.
	 * @param angle - Angle to set this entity facing toward.
	 */
	public void setAngle( int angle )
	{
		this.m_iAngle = angle;
		this.m_iIntendedAngle = m_iAngle;
	}
	
	/**
	 * Sets the angle this entity should glide toward.
	 * No preconditions.
	 * Postconditions:
	 * 	- m_iIntendedAngle is set to the target argument.
	 * 	- m_iRotateSpeed is set to the rotateSpeed argument.
	 * 	- m_iNextRotateTime is set to the current time in ticks plus rotateSpeed.
	 * 	- m_bIntentToTurn is set to true.
	 * 	- m_bRotateDir is set toward the direction the entity should turn.
	 * @param target - Target angle.
	 * @param rotateSpeed - Number of ticks before turning by 1 degree toward target.
	 */
	public void setIntendedAngle( int target, int rotateSpeed )
	{
		target %= 360;
		if ( target < 0 ) target += 360;
		
		m_iIntendedAngle = target;
		m_iRotateSpeed = rotateSpeed;
		m_iNextRotateTime = Utils.ts().g_iTicks + rotateSpeed;
		m_bIntentToTurn = true;
		
		int alwaysLarger = m_iAngle < target ? (m_iAngle + 360) : m_iAngle;
		m_bRotateDir = (alwaysLarger - target) >= 180;
	}
	
	/**
	 * Returns whether or not this entity is turning.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns whether or not this entity is turning toward a target angle.
	 * @return m_bIntentToTurn
	 */
	public boolean isTurning()
	{
		return m_bIntentToTurn;
	}
	
	/**
	 * Points this entity to a coordinate.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- If bRotateToAngle, sets the intended angle to point to the coordinates.
	 * 	- If !bRotateToAngle, directly sets the angle to point to the coordinates.
	 * @param x - X coordinate.
	 * @param y - Y coordinate.
	 * @param bRotateToAngle - Whether or not the entity should smoothly rotate to the angle.
	 * @param rotateSpeed - The speed at which the entity rotates.
	 */
	public void lookAt( float x, float y, boolean bRotateToAngle, int rotateSpeed )
	{
		float distx = x - m_fXPos, disty = y - m_fYPos;
		int angle = (int)Math.toDegrees(Math.atan2(disty, distx));
		
		if ( bRotateToAngle )
		{
			this.setIntendedAngle( angle, rotateSpeed );
		}
		else
		{
			this.m_iAngle = angle;
			this.m_iIntendedAngle = m_iAngle;
		}
	}
	
	/**
	 * Returns this entity's health.
	 * No preconditions.
	 * Postconditions:
	 * 	- This entity's health is returned.
	 * @return m_iHealth
	 */
	public int getHealth()
	{
		return m_iHealth;
	}
	
	/**
	 * Sets this entity's health.
	 * No preconditions.
	 * Postconditions:
	 * 	- m_iHealth is set to the integer argument.
	 * @param health - The target health.
	 */
	public void setHealth( int health )
	{
		m_iHealth = health;
	}
	
	/**
	 * Whether or not this entity should be drawn.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns whether or not this entity is visible.
	 * @return m_bShouldDraw
	 */
	public boolean shouldDraw()
	{
		return m_bShouldDraw;
	}
	
	/**
	 * Sets this entity's visibility.
	 * No preconditions.
	 * Postconditions:
	 * 	- m_bShouldDraw is set to the boolean argument.
	 * @param shouldDraw - Whether or not this entity should be drawn.
	 */
	public void setShouldDraw( boolean shouldDraw )
	{
		m_bShouldDraw = shouldDraw;
	}
	
	/**
	 * Gets the classname of this entity, which is different for every subclass.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns this entity's classname.
	 * @return String
	 */
	abstract public String getClassname();
	
	/**
	 * Called every tick.
	 * For every entity, corrects the health and angle values, while
	 * shadowing the collision bounding box's position with this entity's
	 * position.
	 * Also handles gliding and rotating.
	 * Preconditions:
	 * 	- m_hCollisionRect != null
	 * Postconditions:
	 * 	- m_iAngle is kept between 0 and 360 degrees.
	 * 	- m_iHealth is kept no less than 0.
	 * 	- If intending to glide, this entity is moved toward the target
	 * 		position a bit.
	 * 	- If intending to rotate, this entity is turned toward the target
	 * 		angle if its time is now.
	 */
	public void think()
	{
		if ( m_iHealth < 0 ) m_iHealth = 0;
		
		//Keep the angle between 0 and 360.
		this.m_iAngle %= 360;
		if ( this.m_iAngle < 0 ) this.m_iAngle += 360;
		this.m_iIntendedAngle %= 360;
		if ( this.m_iIntendedAngle < 0 ) this.m_iIntendedAngle += 360;
		
		//Make sure the collision rectangle shadows this entity.
		m_hCollisionRect.setBounds((int)m_fXPos, (int)m_fYPos, (int)m_hCollisionRect.getWidth(), (int)m_hCollisionRect.getHeight());
		
		//If this entity wants to move to a position, then let's glide it there.
		//Unless it takes too long to get there, in which case assume it's unreachable.
		if ( m_bIntentToGlide && (m_fIntendedXPos != m_fXPos
			|| m_fIntendedYPos != m_fYPos) && Utils.ts().g_iTicks < m_iStartMoveTime + m_iMoveTime * 2 )
		{
			m_iPrevCollisions = m_iCurCollisions;
			m_iCurCollisions = this.setPosition( m_fXPos + m_fMoveXSpeed, m_fYPos + m_fMoveYSpeed, true );
			//If this stops colliding with another bounding box, then re-calculate the speed to the intended position.
			if ( m_iPrevCollisions != 0 && m_iCurCollisions == 0 )
				this.setIntendedPosition(m_fIntendedXPos, m_fIntendedYPos, m_iMoveTime - (Utils.ts().g_iTicks - m_iStartMoveTime));
		}
		//If this entity's near a point, then set its position to equal the position of the point.
		if ( (m_fIntendedXPos >= m_fXPos - 3
			&& m_fIntendedXPos <= m_fXPos + 3)
			&& (m_fIntendedYPos >= m_fYPos - 3
				&& m_fIntendedYPos <= m_fYPos + 3)
				&& m_bIntentToGlide )
		{
			this.setPosition( m_fIntendedXPos, m_fIntendedYPos, false );
			m_bIntentToGlide = false;
		}
		
		//If we want to smoothly rotate to a target angle, then let's do just that.
		if ( m_bIntentToTurn && this.m_iAngle != this.m_iIntendedAngle && Utils.ts().g_iTicks > this.m_iNextRotateTime )
		{
			this.m_iAngle = this.m_iAngle + (m_bRotateDir ? 1 : -1);
			this.m_iNextRotateTime = Utils.ts().g_iTicks + this.m_iRotateSpeed;
		}
		//Since the angle turns by one degree every few ticks or so, we don't need to worry about catching it within a range,
		//unlike with the position (which uses floating point numbers, whereas angles here use integers).
		else if ( m_bIntentToTurn && this.m_iAngle == this.m_iIntendedAngle)
		{
			m_bIntentToTurn = false;
		}
	}
}
