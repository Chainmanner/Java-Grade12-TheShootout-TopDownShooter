package org.valachi_maas.finalFight;

import java.awt.*;

/**
 * Base class for all weapons that make stuff dead.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
abstract public class BaseCombatWeapon extends BaseEntity
{
	private final int m_iMinDamage;
	private final int m_iMaxDamage;
	private final int m_iBaseAccuracy;						//The higher this value, the greater the spread.
	private final int m_iRecoil;
	private final int m_iROF;								//Rate of fire
	
	private int m_iCurAmmo;
	private final int m_iMaxAmmo;
	private int m_iExtraAmmo;
	protected float m_fCurRecoil = 0;
	protected int m_iNextFireTime = 0;
	
	protected boolean m_bBulletPerBulletReload = true;		//Shotgun-style reloading (inserts one bullet at a time).
	protected int m_iFireAfterReloadTime = 1000;			//Time when this gun can be fired after reloading.
	protected boolean m_bReloading = false;					//Whether or not the gun is reloading right now.
	protected int m_iReloadRate = 250;						//Rate of which bullets are inserted.
	protected int m_iNextReloadTime = 0;					//Next time a bullet can be added.
	
	protected String[] m_iFireSounds;
	protected String m_szReloadSound = "weapons/weapon_raise.wav";
	protected String m_szOpenChamberSound = "weapons/click_01.wav";
	protected String m_szCloseChamberSound = "weapons/clack_01.wav";
	protected String[] m_szAddAmmoSounds = {
			"weapons/insert_01.wav",
			"weapons/insert_02.wav",
			"weapons/insert_03.wav",
	};
	
	private BaseCombatCharacter m_hOwner;					//The owner of this gun.
	private boolean m_bGiveToOwner = false;	//If this gun starts with an owner, this actually *gives* the gun itself (not just ownership).
	
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- icon != null.
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * 	- curAmmo, maxAmmo, extraAmmo >= 0.
	 * 	- baseAccuracy, recoil, ROF >= 0.
	 * Postconditions:
	 * 	- New BaseCombatWeapon instance created.
	 * @param name - Naming your guns is fun.
	 * @param icon - This gun's icon to show when dropped on the ground.
	 * @param x - X coordinate.
	 * @param y - Y coordinate.
	 * @param minDamage - Minimum damage.
	 * @param maxDamage - Maximum damage.
	 * @param owner - Owner of this gun.
	 * @param baseAccuracy - Minimum angular spread of this gun.
	 * @param recoil - Recoil to induce when firing.
	 * @param ROF - Rate of fire.
	 * @param curAmmo - Ammo currently loaded.
	 * @param maxAmmo - Maximum ammo.
	 * @param extraAmmo - Stored ammo.
	 */
	public BaseCombatWeapon( String name, Image icon, float x, float y, int minDamage, int maxDamage,
			BaseCombatCharacter owner, int baseAccuracy, int recoil, int ROF, int curAmmo, int maxAmmo, int extraAmmo )
	{
		super( name, icon, x, y, -1, 0, null, false );
		m_hOwner = owner;
		if ( m_hOwner != null )
		{
			m_bGiveToOwner = true;
		}
		
		m_iMinDamage = minDamage;
		m_iMaxDamage = maxDamage;
		m_iBaseAccuracy = baseAccuracy;
		m_iRecoil = recoil;
		m_iROF = ROF;
		m_iCurAmmo = curAmmo;
		m_iMaxAmmo = maxAmmo;
		m_iExtraAmmo = extraAmmo;
	}
	
	/**
	 * Called every tick.
	 * For BaseCombatWeapon, if a BaseCombatCharacter owns this gun, this
	 * hides the gun and shadows the owner's position and angle.
	 * Also reduces accuracy offset due to recoil.
	 * If the gun is currently reloading, then it tries to insert a bullet when possible.
	 * No preconditions.
	 * Postconditions:
	 * 	- If this gun was created with an owner in mind, then it is given to the owner.
	 * 	- If m_iCurAmmo or m_iExtraAmmo < 0, the respective variable == 0.
	 * 	- m_fCurRecoil is lessened by 0.0025%.
	 * 	- If m_bReloading, tries to insert a bullet.
	 * 	- If m_hOwner != null, hides this and sets this weapon's angle and position to
	 * 		that of the owner's.
	 */
	@Override
	public void think()
	{
		super.think();
		if ( m_bGiveToOwner )
		{
			m_bGiveToOwner = false;
			m_hOwner.giveWeapon(this);
		}
		
		if ( m_iCurAmmo < 0 ) m_iCurAmmo = 0;
		if ( m_iExtraAmmo < 0 ) m_iExtraAmmo = 0;
		this.setCollisionRect( new Rectangle((int)this.getPosition()[0], (int)this.getPosition()[1],
				this.getWidth(Utils.ts()), this.getHeight(Utils.ts())) );
		
		if ( m_fCurRecoil != 0 )
		{
			m_fCurRecoil *= 0.99f;
			
			if ( m_fCurRecoil < 1 )
				m_fCurRecoil = 0;
		}
		
		if ( m_hOwner != null )
		{
			this.setPosition( m_hOwner.getPosition()[0] + m_hOwner.getCollisionRect().width / 2 - Projectile.ICON.getWidth(Utils.ts()) / 2,
					m_hOwner.getPosition()[1] + m_hOwner.getCollisionRect().height / 2 - Projectile.ICON.getHeight(Utils.ts()) / 2, false );
			this.setAngle( m_hOwner.getAngle() );
			this.setShouldDraw( false );
		}
		else
		{
			this.setShouldDraw( true );
		}
		
		if ( m_bReloading )
		{
			this.insertOneAmmo();
		}
	}
	
	/**
	 * Gets the base inaccuracy of this weapon.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns this weapon's minimum angular spread.
	 * @return m_iBaseAccuracy
	 */
	public int getBaseAccuracy()
	{
		return m_iBaseAccuracy;
	}
	
	/**
	 * Gets the recoil induced when shooting this gun.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns this weapon's recoil.
	 * @return m_iRecoil
	 */
	public int getRecoil()
	{
		return m_iRecoil;
	}
	
	/**
	 * Gets this weapon's rate of fire.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns this weapon's rate of fire.
	 * @return m_iROF
	 */
	public int getROF()
	{
		return m_iROF;
	}
	
	/**
	 * Gets the recoil that currently decreases this gun's accuracy.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns this weapon's current recoil.
	 * @return m_fCurRecoil
	 */
	public float getCurrentRecoil()
	{
		return m_fCurRecoil;
	}
	
	/**
	 * Gets the ammunition currently loaded in this gun.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns the ammo currently in this gun's magazine.
	 * @return m_iCurAmmo.
	 */
	public int getCurrentAmmo()
	{
		return m_iCurAmmo;
	}
	
	/**
	 * Sets the current ammo in this magazine.
	 * No preconditions.
	 * Postconditions:
	 * 	- Loaded ammo set to the input integer.
	 * @param ammo - Ammunition this gun should have.
	 */
	public void setCurrentAmmo( int ammo )
	{
		if ( ammo < 0 ) ammo = 0;
		m_iCurAmmo = ammo;
	}
	
	/**
	 * Gets the size of this weapon's magazine.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns the maximum ammunition in the magazine.
	 * @return m_iMaxAmmo
	 */
	public int getMaxAmmo()
	{
		return m_iMaxAmmo;
	}
	
	/**
	 * Gets this gun's stored ammunition.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns the extra ammunition available.
	 * @return m_iExtraAmmo
	 */
	public int getExtraAmmo()
	{
		return m_iExtraAmmo;
	}
	
	/**
	 * Sets the extra ammunition available.
	 * No preconditions.
	 * Postconditions:
	 * 	- Stored ammunition set to the integer argument.
	 * @param extraAmmo - The stored ammunition available.
	 */
	public void setExtraAmmo( int extraAmmo )
	{
		m_iExtraAmmo = extraAmmo;
	}
	
	/**
	 * Gets the owner of this gun.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns the owner of this gun, or null if there is none.
	 * @return m_hOwner
	 */
	public BaseCombatCharacter getOwner()
	{
		return m_hOwner;
	}
	
	/**
	 * Sets the owner of this gun.
	 * No preconditions.
	 * Postconditions:
	 * 	- Previous owner loses ownership of this gun, and
	 * 		another BaseCombatCharacter gets it.
	 * @param owner - The new owner.
	 */
	public void setOwner( BaseCombatCharacter owner )
	{
		m_hOwner = owner;
	}
	
	/**
	 * Fires this weapon.
	 * No preconditions.
	 * Postconditions:
	 * 	- If successful:
	 * 		The gun has one less bullet.
	 * 		A new Projectile is created.
	 * 		Recoil is added.
	 * 		Gunshot sound is played.
	 * @return Whether or not the gun is shot.
	 */
	public boolean fire()
	{
		if ( !Utils.ts().g_bCutscene &&
			 (Utils.ts().g_iTicks < m_iNextFireTime
				|| m_iCurAmmo == 0
				|| m_bReloading) )
			return false;
		
		Utils.ts().g_hAllEntities.add( new Projectile( this.getPosition()[0], this.getPosition()[1],
				(int)(this.getAngle() + (m_iBaseAccuracy + m_fCurRecoil) * (0.5 - Math.random())),
				(int)(m_iMinDamage + Math.random() * (m_iMaxDamage - m_iMinDamage)),
				2.0f, m_hOwner ) );
		
		m_fCurRecoil += m_iRecoil;
		m_iNextFireTime = Utils.ts().g_iTicks + m_iROF;
		m_iCurAmmo--;
		Utils.playSound( m_iFireSounds[(int)(Math.random() * m_iFireSounds.length)] );
		return true;
	}
	
	/**
	 * Returns whether or not this weapon's magazine is empty.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns whether or not the loaded ammo == zero.
	 * @return m_iCurAmmo == 0
	 */
	public boolean isEmpty()
	{
		return m_iCurAmmo == 0;
	}
	
	/**
	 * Returns whether or not this weapon's magazine is full.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns whether or not the loaded ammo == max ammo.
	 * @return m_iCurAmmo == m_iMaxAmmo
	 */
	public boolean isFull()
	{
		return m_iCurAmmo == m_iMaxAmmo;
	}
	
	/**
	 * Reloads this gun.
	 * Preconditions:
	 * 	- Path to m_szOpenChamberSound exists.
	 * Postconditions:
	 * 	- If m_bBulletPerBulletReload, sets m_bReloading to true,
	 * 		sets the reloading delay, and plays the sound of the chamber opening.
	 * 	- If !m_bBulletPerBulletReload, fills up the magazine immediately,
	 * 		removes the number of loaded bullets from the number of extra bullets,
	 * 		and sets the firing delay.
	 */
	public void reload()
	{
		if ( m_iExtraAmmo <= 0 || m_bReloading || m_iCurAmmo == m_iMaxAmmo ) return;
		
		if ( m_bBulletPerBulletReload )
		{
			m_bReloading = true;
			m_iNextReloadTime = Utils.ts().g_iTicks + m_iReloadRate * 2;
			Utils.playSound( m_szOpenChamberSound );
		}
		else
		{
			if ( m_iExtraAmmo < m_iMaxAmmo )
			{
				m_iCurAmmo = m_iExtraAmmo;
				m_iExtraAmmo = 0;
			}
			else
			{
				m_iCurAmmo = m_iMaxAmmo;
				m_iExtraAmmo = 0;
			}
			m_iNextFireTime = Utils.ts().g_iTicks + m_iFireAfterReloadTime;
			Utils.playSound( m_szReloadSound );
		}
	}
	
	/**
	 * Inserts one bullet/shell into this weapon's magazine.
	 * Preconditions:
	 * 	- Paths to m_szCloseChamberSound and the contents of m_szAddAmmoSounds exist.
	 * Postconditions:
	 * 	- If the loaded ammo is not equal to the max ammo, and there is extra ammo available,
	 * 		increments the loaded ammo by one and decrements the extra ammo by one.
	 * 	- If the loaded ammo equals the max ammo, or there's no more extra ammo,
	 * 		stop reloading.
	 * @return Whether or not a bullet is inserted.
	 */
	private boolean insertOneAmmo()
	{
		if ( !m_bReloading ) return false;
		
		if ( m_iCurAmmo != m_iMaxAmmo && m_iExtraAmmo > 0 && Utils.ts().g_iTicks > m_iNextReloadTime )
		{
			m_iCurAmmo++;
			m_iExtraAmmo--;
			m_iNextReloadTime = Utils.ts().g_iTicks + m_iReloadRate;
			Utils.playSound( m_szAddAmmoSounds[(int)(Math.random() * m_szAddAmmoSounds.length)] );
			return true;
		}
		else if ( m_iCurAmmo == m_iMaxAmmo || m_iExtraAmmo <= 0 && Utils.ts().g_iTicks > m_iNextReloadTime )
		{
			m_bReloading = false;
			Utils.playSound( m_szCloseChamberSound );
			m_iNextFireTime = Utils.ts().g_iTicks + m_iFireAfterReloadTime;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Gets the weapon hold name.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns the suffix for the icon of an NPC holding this gun.
	 * @return String
	 */
	abstract public String getWeaponHoldName();
	
	/**
	 * Gets the weapon shot name.
	 * No preconditions.
	 * Postconditions:
	 * 	- Returns the suffix for the icon of an NPC shooting this gun.
	 * @return String
	 */
	abstract public String getWeaponFireName();
}
