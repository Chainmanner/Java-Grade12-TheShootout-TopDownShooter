package org.valachi_maas.finalFight;

import java.awt.*;

/**
 * Single-action revolver.
 * Quick to draw and shoot, but not very accurate.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
public class Revolver extends BaseCombatWeapon
{
	static final int R_MIN_DMG = 7;	//Minimum damage per bullet.
	static final int R_MAX_DMG = 20;//Maximum damage.
	
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- New Revolver instance created.
	 * @param x - X coordinate.
	 * @param y - Y coordinate.
	 * @param owner - Owner of this gun.
	 * @param curAmmo - Loaded ammunition.
	 * @param extraAmmo - Spare ammunition.
	 */
	public Revolver( float x, float y, BaseCombatCharacter owner, int curAmmo, int extraAmmo )
	{
		super("revolver", Toolkit.getDefaultToolkit().getImage("materials/gunsDropped/Revolver.png"),
				x, y, R_MIN_DMG, R_MAX_DMG, owner, 10, 25+10, 500, curAmmo, 6, extraAmmo);
		this.m_iFireSounds = new String[9];
		this.m_iFireSounds[0] = "weapons/Pistolshot1short.wav";
		this.m_iFireSounds[1] = "weapons/Pistolshot2short.wav";
		this.m_iFireSounds[2] = "weapons/Pistolshot3short.wav";
		this.m_iFireSounds[3] = "weapons/Pistolshot4short.wav";
		this.m_iFireSounds[4] = "weapons/Pistolshot5short.wav";
		this.m_iFireSounds[5] = "weapons/Pistolshot6short.wav";
		this.m_iFireSounds[6] = "weapons/Pistolshot7short.wav";
		this.m_iFireSounds[7] = "weapons/Pistolshot8short.wav";
		this.m_iFireSounds[8] = "weapons/Pistolshot9short.wav";
	}
	
	/**
	 * Called every tick.
	 * For Revolver, this sets the next firing time for the Player
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
		return "Revolver.png";
	}
	
	@Override
	public String getWeaponFireName()
	{
		return "RShoot.png";
	}
	
	@Override
	public String getClassname()
	{
		return "revolver";
	}
}
