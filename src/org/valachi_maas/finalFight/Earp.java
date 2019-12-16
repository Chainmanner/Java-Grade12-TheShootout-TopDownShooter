package org.valachi_maas.finalFight;

import java.awt.Rectangle;

/**
 * Earps.
 * Enemies to the player and the Dalton Bros, allies to the Police.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
public class Earp extends BaseCombatCharacter
{
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- New Earp instance created.
	 * @param name
	 * @param x
	 * @param y
	 * @param health
	 */
	public Earp(String name, float x, float y, int health )
	{
		super(name, "materials/Badguys/badguy", x, y, health,
				new Rectangle((int)x, (int)y, Utils.npcBox.width, Utils.npcBox.height), null);
		this.m_hRelationships.put("daltonbro", relationship_t.HOSTILE);
		this.m_hRelationships.put("earp", relationship_t.ALLY);
		this.m_hRelationships.put("police", relationship_t.ALLY);
		this.m_hRelationships.put("player", relationship_t.HOSTILE);
		bAllEarpsDead = false;
	}
	
	@Override
	public String getClassname()
	{
		return "earp";
	}
	
	static boolean bAllEarpsDead = false;
	/**
	 * Checks if all the Earps are dead.
	 * If they are, pings an event to the current map.
	 * Preconditions:
	 * 	- g_hCurMap != null.
	 * Postconditions:
	 * 	- If g_hAllEntities has no Earp instances,
	 * 		calls the event "allearpsdead".
	 */
	public static void allEarpsDead()
	{
		int iLiveEarps = 0;
		Earp curNPC = null;
		for ( int i = 0; i < Utils.ts().g_hAllEntities.size(); i++ )
		{
			if ( Utils.ts().g_hAllEntities.get(i) instanceof Earp )
				curNPC = (Earp)Utils.ts().g_hAllEntities.get(i);
			else continue;
			
			if ( curNPC.getHealth() > 0 )
			{
				iLiveEarps++;
			}
		}
		
		if ( iLiveEarps == 0 && !bAllEarpsDead )
		{
			bAllEarpsDead = true;
			Utils.ts().g_hCurMap.callEvent( "allearpsdead" );
		}
	}
}