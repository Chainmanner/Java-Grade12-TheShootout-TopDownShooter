package org.valachi_maas.finalFight;

import java.awt.Toolkit;

/**
 * Double barrel shotgun.
 * Great at close range, bad choice for anything further.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
public class DBShotgun extends BaseCombatWeapon
{
	public static final int DB_MIN_DMG = 8;		//Minimum damage per pellet.
	public static final int DB_MAX_DMG = 16;	//Maximum damage.
	public static final int DB_PELLETS = 6;		//Number of Projectile entities to spawn when firing.
	
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- New DBShotgun instance created.
	 * @param x - X coordinate.
	 * @param y - Y coordinate.
	 * @param owner - Owner of this gun.
	 * @param curAmmo - Loaded ammunition.
	 * @param extraAmmo - Spare ammunition.
	 */
	public DBShotgun( float x, float y, BaseCombatCharacter owner, int curAmmo, int extraAmmo )
	{
		super("dbshotgun", Toolkit.getDefaultToolkit().getImage("materials/gunsDropped/Shotgun.png"),
				x, y, DB_MIN_DMG, DB_MAX_DMG, owner, 30, 0, 1000, curAmmo, 2, extraAmmo);
		this.m_iReloadRate = 500;
	}
	
	/**
	 * Fires this weapon.
	 * Since this is a shotgun, fires a barrage of Projectile entities.
	 * No preconditions.
	 * Postconditions:
	 * 	- If successful:
	 * 		The gun has one less shell.
	 * 		Multiple Projectile entities are created.
	 * 		Gunshot sound is played.
	 */
	@Override
	public boolean fire()
	{
		if ( Utils.ts().g_iTicks < m_iNextFireTime
				|| this.getCurrentAmmo() == 0
				|| m_bReloading )
			return false;
		
		for ( int i = 0; i < DB_PELLETS; i++ )
		{
			Utils.ts().g_hAllEntities.add( new Projectile( this.getPosition()[0], this.getPosition()[1],
					(int)(this.getAngle() + (this.getBaseAccuracy() + m_fCurRecoil) * (0.5 - Math.random())),
					(int)(DB_MIN_DMG + Math.random() * (DB_MAX_DMG - DB_MIN_DMG)),
					2.0f, this.getOwner() ) );
		}
		
		m_fCurRecoil += this.getRecoil();
		m_iNextFireTime = Utils.ts().g_iTicks + this.getROF();
		this.setCurrentAmmo( this.getCurrentAmmo() - 1 );
		Utils.playSound( "weapons/scatter_gun_double_shoot.wav" );
		return true;
	}
	
	/**
	 * Called every tick.
	 * For DBShotgun, this sets the next firing time for the Player
	 * to be zero if they're not holding down MOUSE1. That way, it
	 * shoots in a semi-automatic fashion.
	 * Preconditions:
	 * 	- See {@link BaseEntity#think()}.
	 * Postconditions:
	 * 	- See {@link BaseEntity#think()}.
	 * 	- If MOUSE1 is not held down, m_iNextFireTime is set to the
	 * 		current time in ticks. Otherwise, set it to be a long time away.
	 */
	@Override
	public void think()
	{
		super.think();
		if ( this.getOwner() != null )
		{
			if ( this.getOwner() instanceof Player )
			{
				if ( Utils.ts().m1 )
				{
					m_iNextFireTime = Utils.ts().g_iTicks + 666666;
				}
				else
				{
					m_iNextFireTime = Utils.ts().g_iTicks;
				}
			}
		}
	}

	@Override
	public String getWeaponHoldName()
	{
		return "SG.png";
	}

	@Override
	public String getWeaponFireName()
	{
		return "SGShoot.png";
	}
	
	@Override
	public String getClassname()
	{
		return "dbshotgun";
	}
}
