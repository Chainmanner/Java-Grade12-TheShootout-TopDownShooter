package org.valachi_maas.finalFight;

import java.awt.Toolkit;

/**
 * Pump-action rifle.
 * Extremely accurate and damaging, but has a slow rate of fire.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
public class Rifle extends BaseCombatWeapon
{
	static final int RF_MIN_DMG = 15;
	static final int RF_MAX_DMG = 40;
	
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- New Rifle instance created.
	 * @param x - X coordinate.
	 * @param y - Y coordinate.
	 * @param owner - Owner of this gun.
	 * @param curAmmo - Loaded ammunition.
	 * @param extraAmmo - Spare ammunition.
	 */
	public Rifle( float x, float y, BaseCombatCharacter owner, int curAmmo, int extraAmmo )
	{
		super("rifle", Toolkit.getDefaultToolkit().getImage("materials/gunsDropped/Rifle.png"),
				x, y, RF_MIN_DMG, RF_MAX_DMG, owner, 5, 30*2, 3000, curAmmo, 4, extraAmmo);
		this.m_iFireSounds = new String[1];
		this.m_iFireSounds[0] = "weapons/fire_sniper.wav";
	}
	
	@Override
	public String getWeaponHoldName()
	{
		return "RF.png";
	}
	
	@Override
	public String getWeaponFireName()
	{
		return "RFShoot.png";
	}
	
	@Override
	public String getClassname()
	{
		return "rifle";
	}
}
