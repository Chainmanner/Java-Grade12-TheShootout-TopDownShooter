package org.valachi_maas.finalFight;

import java.awt.Rectangle;

/**
 * Police.
 * Enemies to the player and the Dalton Bros, allies to the Earps.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
public class Police extends BaseCombatCharacter
{
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- New Police instance created.
	 * @param name
	 * @param x
	 * @param y
	 * @param health
	 */
	public Police(String name, float x, float y, int health )
	{
		super(name, "materials/Police/police", x, y, health,
				new Rectangle((int)x, (int)y, Utils.npcBox.width, Utils.npcBox.height), null);
		this.m_hRelationships.put("daltonbro", relationship_t.HOSTILE);
		this.m_hRelationships.put("earp", relationship_t.ALLY);
		this.m_hRelationships.put("police", relationship_t.ALLY);
		this.m_hRelationships.put("player", relationship_t.HOSTILE);
		bAllCopsDead = false;
	}
	
	@Override
	public String getClassname()
	{
		return "police";
	}
	
	static boolean bAllCopsDead = false;
	/**
	 * Checks if all the Cops are dead.
	 * If they are, pings an event to the current map.
	 * Preconditions:
	 * 	- g_hCurMap != null.
	 * Postconditions:
	 * 	- If g_hAllEntities has no Police instances,
	 * 		calls the event "allcopsdead".
	 */
	public static void allCopsDead()
	{
		int iLiveCops = 0;
		Police curNPC = null;
		for ( int i = 0; i < Utils.ts().g_hAllEntities.size(); i++ )
		{
			if ( Utils.ts().g_hAllEntities.get(i) instanceof Police )
				curNPC = (Police)Utils.ts().g_hAllEntities.get(i);
			else continue;
			
			if ( curNPC.getHealth() > 0 )
			{
				iLiveCops++;
			}
		}
		
		if ( iLiveCops == 0 && !bAllCopsDead )
		{
			bAllCopsDead = true;
			Utils.ts().g_hCurMap.callEvent( "allcopsdead" );
		}
	}
}