package org.valachi_maas.finalFight;

import java.awt.*;

/**
 * Comes out of barrels from guns and hurts entities that touch it.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
public class Projectile extends BaseEntity
{
	public static final Image ICON = Toolkit.getDefaultToolkit().getImage("materials/projectile.png");
	
	private final int m_iDamage;
	private final float m_fSpeed;
	private final BaseEntity m_hTheGuyWhoShotThis;	//Do not hit the entity that shot this bullet.
	
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- New Projectile instance created.
	 * @param x	- X coordinate.
	 * @param y	- Y coordinate.
	 * @param angle	- Angle of the projectile.
	 * @param damage - Damage this projectile deals to whatever it hits.
	 * @param speed - Speed of the projectile, in pixels/tick.
	 * @param theGuyWhoShotThis - The BaseEntity that spawned this entity. Since Projectile objects
	 * 				usually originate from this entity's position, we don't want it hitting its creator.
	 */
	public Projectile( float x, float y, int angle, int damage, float speed, BaseEntity theGuyWhoShotThis )
	{
		super( "projectile", ICON, x, y, -1, angle, new Rectangle(0, 0, 8, 8), false);
		m_iDamage = damage;
		m_fSpeed = speed;
		this.setShouldDraw( true );
		m_hTheGuyWhoShotThis = theGuyWhoShotThis;
	}
	
	public String getClassname()
	{
		return "projectile";
	}
	
	/**
	 * Called every tick.
	 * For Projectile, checks if this entity should be drawn.
	 * If so, then it updates the position, and checks if it's hitting an entity.
	 * If it hits an entity, then damage the entity, teleport to the place where all entities go to die
	 * and set this to be so that this shouldn't render.
	 * Preconditions:
	 * 	- m_fSpeed != 0.
	 * Postconditions (if shouldDraw() == true):
	 * 	- This Projectile is moved forward along its path.
	 * 	- If an entity is hit, its health is reduced.
	 * 	- If an entity is hit and it's an NPC, it turns toward the Projectile's point of contact.
	 */
	@Override
	public void think()
	{
		if ( !shouldDraw() ) return;
		super.think();
		
		Utils.Assert(m_fSpeed != 0);
		//Move the projectile forward on its path to destruction.
		this.setPosition( getPosition()[0] + (float)(m_fSpeed * Math.cos(Math.toRadians(getAngle()))),
				getPosition()[1] + (float)(m_fSpeed * Math.sin(Math.toRadians(getAngle()))), false );
		
		//Check to see if this collides with any entities.
		BaseEntity curEnt = null;
		for ( int i = 0; i < Utils.ts().g_hAllEntities.size(); i++ )
		{
			curEnt = Utils.ts().g_hAllEntities.get(i);
			if ( curEnt == null )
				continue;
			if ( curEnt == this || curEnt == m_hTheGuyWhoShotThis || !curEnt.isSolid() )
			{
				continue;
			}
			
			if ( curEnt.getHealth() <= 0 )
				continue;
			
			//Either we're colliding with something, or we're way out of bounds.
			//In any case, hurt the current entity and disappear.
			if ( this.getCollisionRect().intersects( curEnt.getCollisionRect() )
					|| this.getPosition()[0] > Utils.max32f || this.getPosition()[0] < -Utils.max32f
					|| this.getPosition()[1] > Utils.max32f || this.getPosition()[1] < -Utils.max32f )
			{
				curEnt.setHealth( curEnt.getHealth() - m_iDamage );
				
				//Since getting shot is not a pleasant experience, we want NPCs hit by a Projectile
				//to turn toward the source of it.
				if ( curEnt instanceof BaseCombatCharacter && curEnt.getHealth() > 0 )
					((BaseCombatCharacter)curEnt).onHit(this.getPosition()[0], this.getPosition()[1]);
				
				this.setShouldDraw( false );
				this.setPosition( Utils.max32f, Utils.max32f, false );
				Utils.ts().g_hAllEntities.remove(this);
				break;
			}
		}
		//Check to see if this collides with any world collision rectangles.
		Rectangle curRect = null;
		for ( int i = 0; i < Utils.ts().g_hWorldCollisionRects.size(); i++ )
		{
			curRect = Utils.ts().g_hWorldCollisionRects.get(i);
			
			if ( this.getCollisionRect().intersects( curRect ) )
			{
				this.setShouldDraw( false );
				this.setPosition( Utils.max32f, Utils.max32f, false );	//The place where all entities go to die!
				Utils.ts().g_hAllEntities.remove(this);
				break;
			}
		}
	}
}
