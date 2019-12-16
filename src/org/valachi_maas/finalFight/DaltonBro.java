package org.valachi_maas.finalFight;

import java.awt.Rectangle;

/**
 * Dalton Brothers.
 * Allies to the player, enemies to the Earps and the Police.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
public class DaltonBro extends BaseCombatCharacter
{
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- New DaltonBro instance created.
	 * @param name
	 * @param x
	 * @param y
	 * @param health
	 */
	public DaltonBro(String name, float x, float y, int health )
	{
		super(name, "materials/Team/team", x, y, health,
				new Rectangle((int)x, (int)y, Utils.npcBox.width, Utils.npcBox.height), null);
		this.m_hRelationships.put("daltonbro", relationship_t.ALLY);
		this.m_hRelationships.put("earp", relationship_t.HOSTILE);
		this.m_hRelationships.put("police", relationship_t.HOSTILE);
		this.m_hRelationships.put("player", relationship_t.ALLY);
		bAllDaltonsDead = false;
	}
	
	@Override
	public String getClassname()
	{
		return "daltonbro";
	}
	
	static boolean bAllDaltonsDead = false;
	/**
	 * Checks if all the Dalton Bros are dead.
	 * If they are, pings an event to the current map.
	 * Preconditions:
	 * 	- g_hCurMap != null.
	 * Postconditions:
	 * 	- If g_hAllEntities has no DaltonBro instances,
	 * 		calls the event "alldaltonsdead".
	 */
	public static void allDaltonsDead()
	{
		int iLiveDaltons = 0;
		DaltonBro curNPC = null;
		for ( int i = 0; i < Utils.ts().g_hAllEntities.size(); i++ )
		{
			if ( Utils.ts().g_hAllEntities.get(i) instanceof DaltonBro )
				curNPC = (DaltonBro)Utils.ts().g_hAllEntities.get(i);
			else continue;
			
			if ( curNPC.getHealth() > 0 )
			{
				iLiveDaltons++;
			}
		}
		
		if ( iLiveDaltons == 0 && !bAllDaltonsDead )
		{
			bAllDaltonsDead = true;
			Utils.ts().g_hCurMap.callEvent( "alldaltonsdead" );
		}
	}
}
