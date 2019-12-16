package org.valachi_maas.finalFight;

import java.awt.Rectangle;

/**
 * The player itself.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
public class Player extends BaseCombatCharacter
{
	static final float P_MV = 0.3f;
	static final int PUNCH_RATE = 1000;
	static final int PUNCH_MIN = 15;
	static final int PUNCH_MAX = 30;
	private Queue m_hRevolvers = new Queue();
	private int m_iNextPunchTime = 0;
	
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- New Player instance created.
	 * @param x - X coordinate.
	 * @param y - Y coordinate.
	 * @param health - Starting health.
	 */
	public Player(float x, float y, int health)
	{
		super("player", "materials/Player/player", x, y, health,
				new Rectangle((int)x, (int)y, Utils.npcBox.width, Utils.npcBox.height), null);
		this.m_hRelationships.put("daltonbro", relationship_t.ALLY);
		this.m_hRelationships.put("earp", relationship_t.HOSTILE);
		this.m_hRelationships.put("police", relationship_t.HOSTILE);
		this.m_hRelationships.put("player", relationship_t.ALLY);
		this.m_eCurAction = action_t.DISABLED;
	}
	
	/**
	 * Called every tick.
	 * For Player, handles controls and weapon/ammo pickups.
	 * Preconditions:
	 * 	- See {@link BaseCombatCharacter#think()}.
	 * Postconditions:
	 * 	- See {@link BaseCombatCharacter#think()}.
	 * 	- Ammo from dropped weapons may be picked up
	 * 	- New revolvers may be picked up.
	 * 	- m_hCurWeapon or m_hStoredWeapon may be swapped for one on the ground.
	 */
	@Override
	public void think()
	{
		super.think();
		if ( Utils.ts().g_bCutscene || this.m_eCurAction == action_t.DEAD )
			return;
		
		//Handle player movements and mouse position updates.
		this.lookAt( Utils.ts().mouse_x, Utils.ts().mouse_y, false, 1 );
		if ( Utils.ts().w )
		{
			this.setPosition(this.getPosition()[0], this.getPosition()[1] - P_MV, true);
			this.m_eCurAction = action_t.MOVING;
		}
		if ( Utils.ts().a )
		{
			this.setPosition(this.getPosition()[0] - P_MV, this.getPosition()[1], true);
			this.m_eCurAction = action_t.MOVING;
		}
		if ( Utils.ts().s )
		{
			this.setPosition(this.getPosition()[0], this.getPosition()[1] + P_MV, true);
			this.m_eCurAction = action_t.MOVING;
		}
		if ( Utils.ts().d )
		{
			this.setPosition(this.getPosition()[0] + P_MV, this.getPosition()[1], true);
			this.m_eCurAction = action_t.MOVING;
		}
		if ( !Utils.ts().w && !Utils.ts().a && !Utils.ts().s && !Utils.ts().d )
		{
			this.m_eCurAction = action_t.DISABLED;
		}
		
		//Keys that have to do with weaponry.
		if ( Utils.ts().m1 )
		{
			this.attack();
		}
		if ( Utils.ts().zero && Utils.ts().zero_oneshot )
		{
			Utils.ts().zero_oneshot = false;
			this.equipWeapon(0);
		}
		if ( Utils.ts().one && Utils.ts().one_oneshot )
		{
			Utils.ts().one_oneshot = false;
			this.equipWeapon(1);
		}
		if ( Utils.ts().two && Utils.ts().two_oneshot )
		{
			Utils.ts().two_oneshot = false;
			this.equipWeapon(2);
		}
		if ( Utils.ts().r && Utils.ts().r_oneshot )
		{
			Utils.ts().r_oneshot = false;
			this.reload();
		}
		if ( Utils.ts().e && Utils.ts().e_oneshot )
		{
			Utils.ts().e_oneshot = false;
			BaseCombatWeapon curWep = null;
			for ( int i = 0; i < Utils.ts().g_hAllEntities.size(); i++ )
			{
				if ( Utils.ts().g_hAllEntities.get(i) instanceof BaseCombatWeapon )
					curWep = (BaseCombatWeapon)Utils.ts().g_hAllEntities.get(i);
				else continue;
				
				if ( !curWep.shouldDraw() )
					continue;
				if ( curWep instanceof Revolver )
					continue;
				if ( m_hCurWeapon != null )
				{
					if ( curWep.getClassname().equals(m_hCurWeapon.getClassname()) )
						continue;
				}
				else if ( m_hStoredWeapon != null )
				{
					if ( curWep.getClassname().equals(m_hStoredWeapon.getClassname()) )
						continue;
				}
				
				if ( curWep.getCollisionRect().intersects(this.getCollisionRect()) )
				{
					this.swapWeapons(curWep);
					break;
				}
			}
		}
		
		//Check for weapons on the ground of which ammo can be picked up for.
		BaseCombatWeapon curAmmo = null;
		for ( int i = 0; i < Utils.ts().g_hAllEntities.size(); i++ )
		{
			if ( Utils.ts().g_hAllEntities.get(i) instanceof BaseCombatWeapon )
				curAmmo = (BaseCombatWeapon)Utils.ts().g_hAllEntities.get(i);
			else continue;
			
			if ( !curAmmo.shouldDraw() )
				continue;
			if ( curAmmo.getCurrentAmmo() <= 0 )
				continue;
			
			//Only get weapons that collide with the player.
			if ( curAmmo.getCollisionRect().intersects(this.getCollisionRect()) )
			{
				if ( curAmmo instanceof Revolver )
				{
					this.giveWeapon(curAmmo);
					continue;
				}
				if ( m_hCurWeapon != null )
				{
					if ( m_hCurWeapon.getClassname().equals(curAmmo.getClassname()) )
					{
						Utils.playSound( "weapons/9mmclip1.wav" );
						m_hCurWeapon.setExtraAmmo( m_hCurWeapon.getExtraAmmo() + curAmmo.getCurrentAmmo() );
						curAmmo.setCurrentAmmo(0);
						continue;
					}
				}
				if ( m_hStoredWeapon != null )
				{
					if ( m_hStoredWeapon.getClassname().equals(curAmmo.getClassname()) )
					{
						Utils.playSound( "weapons/9mmclip1.wav" );
						m_hStoredWeapon.setExtraAmmo( m_hStoredWeapon.getExtraAmmo() + curAmmo.getCurrentAmmo() );
						curAmmo.setCurrentAmmo(0);
						continue;
					}
				}
			}
		}
	}
	
	/**
	 * Fires the current weapon, if available.
	 * Preconditions:
	 * 	- See {@link BaseCombatCharacter#attack()}.
	 * 	- Paths to sound files weapons/cbar_hitbod(1-3).wav is valid.
	 * Postconditions:
	 * 	- See {@link BaseCombatCharacter#attack()}.
	 * 	- If the player has no weapon equipped, they will throw a punch
	 * 		and maybe hit something.
	 */
	@Override
	public boolean attack()
	{
		if ( m_hCurWeapon != null )
		{
			if ( m_hCurWeapon.isEmpty() && !m_hCurWeapon.m_bReloading )
			{
				this.reload();
				return false;
			}
			
			return super.attack();
		}
		else
		{
			if ( Utils.ts().g_iTicks < m_iNextPunchTime ) return false;
			Utils.ts().g_hCurMap.callEvent( this.getName() + "_attack" );
			
			m_iNextPunchTime = Utils.ts().g_iTicks + PUNCH_RATE;
			BaseEntity curEnt = null;
			for ( int i = 0; i < Utils.ts().g_hAllEntities.size(); i++ )
			{
				curEnt = Utils.ts().g_hAllEntities.get(i);
				if ( curEnt == this || !curEnt.shouldDraw() )
					continue;
				
				if ( curEnt instanceof BaseCombatCharacter )
				{
					if ( this.getRelationshipToCharacter((BaseCombatCharacter)curEnt) == relationship_t.ALLY )
						continue;
				}
				
				if ( curEnt.getCollisionRect().intersects(this.getCollisionRect()) )
				{
					curEnt.setHealth( curEnt.getHealth() - (int)(PUNCH_MIN + Math.random() * (PUNCH_MAX - PUNCH_MIN)) );
					Utils.playSound( "weapons/cbar_hitbod" + (1 + (int)(Math.random() * 3)) + ".wav" );
					return true;
				}
			}
			Utils.playSound("weapons/claw_miss1.wav");
		}
		
		return false;
	}
	
	/**
	 * Gets the number of revolvers this player owns.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns the number of revolvers in this player's inventory.
	 * @return m_hRevolvers.size()
	 */
	public int getNumberOfRevolvers()
	{
		return m_hRevolvers.size();
	}
	
	/**
	 * Holsters the current weapon and equips another one.
	 * No preconditions.
	 * Postconditions:
	 * 	- Current weapon is holstered.
	 * 	- If the desired weapon is available, it is equipped.
	 * @param wep - Weapon to equip. 0 = nothing, 1 = revolver, 2 = two-handed gun.
	 * @return Whether or not the equipping was successful.
	 */
	public boolean equipWeapon( int wep )
	{
		this.holsterActiveWeapon();
		switch ( wep )
		{
			case 0:
				return true;
			case 1:
				if ( m_hCurWeapon instanceof Revolver ) return false;
				if ( m_hRevolvers.front() != null )
				{
					super.giveWeapon( (Revolver)m_hRevolvers.dequeue() );
					return true;
				}
				else return false;
			case 2:
				if ( m_hCurWeapon instanceof Rifle || m_hCurWeapon instanceof DBShotgun ) return false;
				if ( this.m_hStoredWeapon != null )
				{
					super.equipStoredWeapon();
					return true;
				}
				else return false;
			
			default:
				return false;
		}
	}
	
	/**
	 * Swaps the player's stored two-handed weapon with the one on the ground.
	 * Preconditions:
	 * 	- wep != null.
	 * Postconditions:
	 * 	- The active weapon is holstered.
	 * 	- The player loses ownership of the former m_hStoredWeapon.
	 * 	- The player gains ownership of wep, which is the current m_hStoredWeapon.
	 * 	- The player equips the new m_hStoredWeapon.
	 * @param wep - Weapon to be exchanged for.
	 */
	public void swapWeapons( BaseCombatWeapon wep )
	{
		if ( wep instanceof Revolver ) return;
		if ( m_hCurWeapon != null)
			if ( wep.getClassname().equals(m_hCurWeapon.getClassname()) ) return;
		
		this.holsterActiveWeapon();
		if ( m_hStoredWeapon != null )
		{
			m_hStoredWeapon.setOwner(null);
			m_hStoredWeapon = null;
		}
		m_hStoredWeapon = wep;
		wep.setOwner(this);
		this.equipStoredWeapon();
	}
	
	/**
	 * Gives the player a weapon.
	 * Preconditions:
	 * 	- wep != null.
	 * Postconditions:
	 * 	- If wep is a Revolver, it is enqueued into m_hRevolvers.
	 * 	- Otherwise, it is given to the player and equipped if possible.
	 */
	@Override
	public void giveWeapon( BaseCombatWeapon wep )
	{
		Utils.Assert( Utils.ts().g_hAllEntities.contains(wep) );
		if ( wep instanceof Revolver )
		{
			if ( wep.isEmpty() )
				return;
			
			Utils.playSound( "weapons/9mmclip1.wav" );
			wep.setOwner(this);
			this.m_hRevolvers.enqueue( wep );
			return;
		}
		
		super.giveWeapon(wep);
	}
	
	/**
	 * Makes the player holster their active weapon.
	 * No preconditions.
	 * Postconditions:
	 * 	- m_hCurWeapon is holstered into m_hStoredWeapon, if it's
	 * 		not an instance of Revolver.
	 * 	- If m_hCurWeapon is a Revolver, then it is queued into m_hRevolvers.
	 */
	@Override
	public boolean holsterActiveWeapon()
	{
		if ( this.m_hCurWeapon == null )
		{
			return false;
		}
		
		if ( this.m_hCurWeapon instanceof Revolver )
		{
			m_hRevolvers.enqueue( this.takeWeapon(false) );
			return true;
		}
		else
		{
			return super.holsterActiveWeapon();
		}
	}
	
	/**
	 * Makes the player drop their weapon.
	 * No preconditions.
	 * Postconditions:
	 * 	- m_hCurWeapon is set to null if it isn't already.
	 * 	- Player loses ownership of m_hCurWeapon.
	 */
	public void drop()
	{
		if ( m_hCurWeapon == null ) return;
		
		this.takeWeapon(true);
		Utils.playSound("weapons/weapon_impact_hard" + (1 + (int)(Math.random() * 3)) + ".wav" );
	}
	
	/**
	 * Reloads the current weapon.
	 * If it's a Revolver, it'll either drop it or
	 * re-queue it (the former if it's out of ammo).
	 * No preconditions.
	 * Postconditions:
	 * 	- If m_hCurWeapon is a Revolver, it is dropped if it is
	 * 		out of ammo or requeued if there is still ammo in it.
	 * 	- Otherwise, m_hCurWeapon follows its reload subroutine.
	 */
	public void reload()
	{
		if ( m_hCurWeapon == null ) return;
		
		if ( this.m_hCurWeapon instanceof Revolver && this.m_hCurWeapon.isEmpty() )
		{
			this.drop();
			this.equipWeapon(1);
		}
		else if ( this.m_hCurWeapon instanceof Revolver && !this.m_hCurWeapon.isEmpty() )
		{
			this.holsterActiveWeapon();
			this.equipWeapon(1);
		}
		else
			this.m_hCurWeapon.reload();
	}
	
	@Override
	public String getClassname()
	{
		return "player";
	}
}
