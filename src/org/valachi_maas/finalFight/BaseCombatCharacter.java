package org.valachi_maas.finalFight;

import java.awt.*;
import java.util.*;

enum relationship_t
{
	ALLY,	//Currently no different from neutral, except that the player's crosshair turns green over this.
	NEUTRAL,//Neither ally nor enemy.
	HOSTILE,//Enemy.
}

enum state_t
{
	IDLE,	//Calmly patrolling the hallways, will react to spotted threats.
	HUNTING,//Actively searching for a threat. Same as IDLE but moves and looks twice as fast.
	HOSTILE	//Known threat in the vicinity. Currently trying to kill said threat.
}

enum action_t
{
	DISABLED,		//AI Disabled.
	NOTHING,		//Not currently doing anything.
	DEAD,			//Just hanging around, being dead.
	LOOKING,		//Scanning the scenery in front of the NPC.
	DONELOOKING,	//Finished looking around.
	MOVING,			//Currently moving, be it to a position or in general.
	DONEMOVING,		//Finished moving.
	RELOADING,		//Currently reloading.
	DONERELOADING,	//Finished reloading.
	COMBAT,			//Fighting a hostile.
}

/**
 * Base class for all NPCs. That includes the player.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
abstract public class BaseCombatCharacter extends BaseEntity
{
	static boolean bAIEnabled = true;
	
	static final String _STAND = "Stand.png";
	static final String _WALK1 = "Walk1.png";
	static final String _WALK2 = "Walk2.png";
	
	static final int FOV = 120;								//NPC's field of view.
	protected Map<Object,relationship_t> m_hRelationships;	//Allies and enemies.
	protected state_t m_eCurState = state_t.IDLE;			//Level of combat.
	protected action_t m_eCurAction = action_t.NOTHING;		//Current action.
	protected BaseCombatWeapon m_hCurWeapon;				//Active weapon.
	protected BaseCombatWeapon m_hStoredWeapon;				//Stored weapon.
	private final String m_szBaseIcon;
	
	protected Stack m_hCurPath = new Stack();			//The path for this NPC to follow to get somewhere.
	protected NodeDef m_hCurNode = null;				//The current node to go to in the path.
	private float m_fCurMoveSpeed = Player.P_MV / 2.0f; //This value won't change if AI is disabled.
	private int m_iNextPatrolTime = 0;					//Next time this is allowed to move.
	private int m_iPatrolSpeed = 0;						//Rate of patrolling.
	private int m_iCurGlance = 0;						//Current glance in IDLE/HUNTING (0 = left, 1 = right, 2 = straight, 3 = finished).
	private int m_iGlanceSpeed = 0;						//Rate of which this NPC looks for enemies.
	private int m_iEndHuntTime = 0;						//Time before this NPC goes from HUNTING to IDLE.
	protected BaseCombatCharacter m_hCurEnemy = null;	//Current enemy.
	private float m_fEnemyLKPX = 0;						//Enemy's last known position - X.
	private float m_fEnemyLKPY = 0;						//Enemy's last known position - Y.
	
	private int m_iNextAnimateTime = 0;					//Next time this is allowed to change icons.
	private int m_iCurStep = 0;							//Current step, for the moving animation (0 = left, 1/3 = neutral, 2 = right)
	
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- Path to m_szBaseIcon files exists.
	 *  - health > 0 (why would you set it to zero, anyway?).
	 *  - x and y >= [min 32-bit integer] and <= [max 32-bit integer].
	 * Postconditions:
	 * 	- New BaseCombatCharacter instance created.
	 * @param name - The name of this NPC in particular.
	 * @param icon_base - The substring that all this NPC's animation frame files share.
	 * @param x - X-Position.
	 * @param y - Y-Position.
	 * @param health - The NPC's health.
	 * @param collisionRect - The NPC's collision bounding box.
	 * @param relationships - The relationships this NPC has to other NPCs. Doesn't have to be set in the constructor,
	 * 							it's mainly here as a reminder to define the relationships of the NPC.
	 */
	public BaseCombatCharacter( String name, String icon_base, float x, float y, int health, Rectangle collisionRect,
			Map<Object,relationship_t> relationships )
	{
		super( name, Toolkit.getDefaultToolkit().getImage( icon_base + "Stand.png" ), x, y, health, 0, collisionRect, true );
		m_szBaseIcon = icon_base;
		m_hRelationships = relationships == null ? new HashMap<Object,relationship_t>() : relationships;
		m_eCurState = state_t.IDLE;
		
		m_hCurPath.makeEmpty();
		m_iCurGlance = 3;
	}
	
	
	/**
	 * Called every tick.
	 * For BaseCombatCharacter, this handles the artificial intelligence (AI) and
	 * the decision-making.
	 * Preconditions:
	 * 	- g_hNodes has nodes in them that are connected together.
	 * Pseudo-postconditions (pseudo because these aren't guaranteed to happen):
	 * 	- NPC's position may change.
	 * 	- Some of this NPC's variables may be changed.
	 * 	- NPC's state and current action may change.
	 * 	- NPC's icon may change.
	 * 	- If an NPC has a weapon, its extra ammo WILL be set to the maximum 32-bit integer.
	 * 	- If an enemy NPC is spotted, the event "[enemyname]_spotted" is fired.
	 */
	@Override
	public void think()
	{
		super.think();
		if ( !bAIEnabled )
			return;
		
		if ( m_eCurAction == action_t.DEAD )
			return;
		if ( this.getHealth() <= 0 && m_eCurAction != action_t.DEAD )
		{
			this.die();
			return;
		}
		
		if ( Utils.ts().g_bCutscene )
		{
			if ( this.isMoving() )
			{
				m_eCurAction = action_t.MOVING;
			}
			else
			{
				m_eCurAction = action_t.NOTHING;
			}
		}
		
		//Process animation updates.
		if ( Utils.ts().g_iTicks > m_iNextAnimateTime )
		{
			if ( m_eCurAction == action_t.MOVING )	//Handle footsteps that happen when moving.
			{
				if ( m_hCurWeapon != null )
					this.setIcon( Toolkit.getDefaultToolkit().getImage( m_szBaseIcon + m_hCurWeapon.getWeaponHoldName() ) );
				
				switch ( m_iCurStep )
				{
					case 0:
						if ( m_hCurWeapon == null ) this.setIcon( Toolkit.getDefaultToolkit().getImage(m_szBaseIcon + "Walk1.png") );
						Utils.playSound("npc/footsteps/gear" + (int)(1 + Math.random() * 6) + ".wav");
						m_iCurStep = 1;
						m_iNextAnimateTime = Utils.ts().g_iTicks + (int)(100 / (4 * m_fCurMoveSpeed));
						break;
					case 1:
						if ( m_hCurWeapon == null ) this.setIcon( Toolkit.getDefaultToolkit().getImage(m_szBaseIcon + "Stand.png") );
						m_iCurStep = 2;
						m_iNextAnimateTime = Utils.ts().g_iTicks + (int)(100 / (4 * m_fCurMoveSpeed));
						break;
					case 2:
						if ( m_hCurWeapon == null ) this.setIcon( Toolkit.getDefaultToolkit().getImage(m_szBaseIcon + "Walk2.png") );
						Utils.playSound("npc/footsteps/gear" + (int)(1 + Math.random() * 6) + ".wav");
						m_iCurStep = 3;
						m_iNextAnimateTime = Utils.ts().g_iTicks + (int)(100 / (4 * m_fCurMoveSpeed));
						break;
					case 3:
						if ( m_hCurWeapon == null ) this.setIcon( Toolkit.getDefaultToolkit().getImage(m_szBaseIcon + "Stand.png") );
						m_iCurStep = 0;
						m_iNextAnimateTime = Utils.ts().g_iTicks + (int)(100 / (4 * m_fCurMoveSpeed));
						break;
				}
			}
			else	//No footsteps == icon of this NPC standing.
			{
				if ( m_hCurWeapon != null )
					this.setIcon( Toolkit.getDefaultToolkit().getImage( m_szBaseIcon + m_hCurWeapon.getWeaponHoldName() ) );
				else
					this.setIcon( Toolkit.getDefaultToolkit().getImage(m_szBaseIcon + "Stand.png") );
			}
		}
		
		if ( m_hCurWeapon != null && !(this instanceof Player) )	//NPCs have infinite ammo.
		{
			m_hCurWeapon.setExtraAmmo(2147483647);
		}
		
		//DISABLED, unlike DEAD, allows for this entity to animate without AI.
		//Also, if this is the player, we want it to animate but not to have AI.
		if ( m_eCurAction == action_t.DISABLED || this instanceof Player || Utils.ts().g_bCutscene )
			return;
		
		//I've spotted an enemy and I'm currently fighting it.
		if ( m_eCurState == state_t.HOSTILE && m_hCurEnemy != null )
		{
			m_fCurMoveSpeed = 0.25f;
			m_iPatrolSpeed = 1500;
			m_iGlanceSpeed = 5;
			
			//I've got line-of-sight to the enemy and he's not dead. Kill him!
			if ( this.hasLOSToPoint(m_hCurEnemy.getPosition()[0], m_hCurEnemy.getPosition()[1], true, m_hCurEnemy)
					&& m_hCurEnemy.getHealth() > 0 )
			{
				if ( m_hCurWeapon != null && !m_hCurWeapon.isEmpty() )
					m_eCurAction = action_t.COMBAT;
			}
			//I have line-of-sight to an enemy, but he's dead.
			//Scout the area for a short while, to make sure it's totally clear.
			else if ( this.hasLOSToPoint(m_hCurEnemy.getPosition()[0], m_hCurEnemy.getPosition()[1], true, m_hCurEnemy)
					&& m_hCurEnemy.getHealth() <= 0 )
			{
				if ( m_hCurWeapon != null && !m_hCurWeapon.isEmpty() )
				{
					m_eCurState = state_t.HUNTING;
					m_iEndHuntTime = Utils.ts().g_iTicks + 10000;
					m_hCurEnemy = null;
					m_hCurNode = null;
					m_eCurAction = action_t.MOVING;
					return;
				}
			}
			
			//I'm at the enemy's LKP, ready to fight him.
			if ( m_hCurWeapon != null )
			{
				if ( m_eCurAction == action_t.DONEMOVING && !m_hCurWeapon.isEmpty() )
				{
					//Enemy's no longer here. Hunt for him.
					if ( !this.hasLOSToPoint(m_hCurEnemy.getPosition()[0], m_hCurEnemy.getPosition()[1], true, m_hCurEnemy)
							|| ( this.hasLOSToPoint(m_hCurEnemy.getPosition()[0], m_hCurEnemy.getPosition()[1], true, m_hCurEnemy)
									&& m_hCurEnemy.getHealth() <= 0 ) )
					{
						m_eCurState = state_t.HUNTING;
						m_iEndHuntTime = Utils.ts().g_iTicks + 
								(m_hCurEnemy.getHealth() <= 0 ? 10000 : 30000 );
						m_hCurEnemy = null;
						m_hCurNode = null;
						m_eCurAction = action_t.MOVING;
						m_iNextPatrolTime = Utils.ts().g_iTicks + 500;
						return;
					}
					else	//Enemy's here. Kill him!
					{
						m_eCurAction = action_t.COMBAT;
					}
				}
			}
			
			//I'm immediately fighting an enemy.
			if ( m_eCurAction == action_t.COMBAT && m_hCurEnemy != null )
			{
				this.lookAt( m_hCurEnemy.getPosition()[0], m_hCurEnemy.getPosition()[1], false, 1 );
				if ( m_hCurWeapon != null )
				{
					//I'm out of ammo. Reload.
					if ( m_hCurWeapon.isEmpty() )
					{
						NodeDef nearestNode = this.getNearestNodeToPos( this.getPosition()[0], this.getPosition()[1], node_t.PATH, true );
						if ( nearestNode != null )
						{
							m_eCurAction = action_t.MOVING;
							m_hCurNode = null; //m_hCurNode MUST be set to NULL when switching between states or this won't move to the next node.
							m_hCurPath = Utils.findPathUsingAStar( nearestNode, this.findCover() );
							m_fEnemyLKPX = m_hCurEnemy.getPosition()[0];
							m_fEnemyLKPY = m_hCurEnemy.getPosition()[1];
							return;
						}
					}
					else	//I've got ammo. Shoot!
					{
						this.attack();
					}
				}
				//Move around randomly to avoid the enemy's bullets.
				if ( Utils.ts().g_iTicks > m_iNextPatrolTime )
				{
					float x = 0, y = 0;
					do
					{
						x = this.getPosition()[0] + (float)(Math.random() * 64 - 32);
						y = this.getPosition()[1] + (float)(Math.random() * 64 - 32);
					}
					while ( !this.hasLOSToPoint(x, y, false, this) );
					
					this.setIntendedPosition( x, y, 500 );
					m_iNextPatrolTime = Utils.ts().g_iTicks + 500;
				}
				
				//I've lost my line of sight to the enemy.
				//Head to his last known position and see if he's there.
				if ( !this.hasLOSToPoint(m_hCurEnemy.getPosition()[0], m_hCurEnemy.getPosition()[1], true, m_hCurEnemy) )
				{
					NodeDef nearestNode = this.getNearestNodeToPos( this.getPosition()[0], this.getPosition()[1], node_t.PATH, true );
					if ( nearestNode != null )
					{
						m_fEnemyLKPX = m_hCurEnemy.getPosition()[0];
						m_fEnemyLKPY = m_hCurEnemy.getPosition()[1];
						m_eCurAction = action_t.MOVING;
						m_hCurNode = null;
						m_hCurPath = Utils.findPathUsingAStar(this.getNearestNodeToPos(this.getPosition()[0], this.getPosition()[1],
										node_t.PATH, true),
										this.getNearestNodeToPos(m_fEnemyLKPX, m_fEnemyLKPY, node_t.PATH, false) );
						return;
					}
				}
			}
			
			//I have a weapon but I'm not ready to fight.
			//Either I'm reloading, or finding my way back to the enemy.
			if ( m_hCurWeapon != null && m_eCurAction != action_t.COMBAT )
			{
				//Made it to cover. Gotta reload my gun.
				if ( m_eCurAction == action_t.DONEMOVING && m_hCurWeapon.isEmpty() )
				{
					m_eCurAction = action_t.RELOADING;
					m_hCurWeapon.reload();
				}
				//Finished reloading.
				else if ( m_eCurAction == action_t.RELOADING && m_hCurWeapon.isFull() )
				{
					m_eCurAction = action_t.DONERELOADING;
				}
				//Now that I'm done reloading, head to the enemy's LKP.
				else if ( m_eCurAction == action_t.DONERELOADING )
				{
					NodeDef nearestNode = this.getNearestNodeToPos( this.getPosition()[0], this.getPosition()[1], node_t.PATH, true );
					if ( nearestNode != null )
					{
						m_eCurAction = action_t.MOVING;
						m_hCurPath = Utils.findPathUsingAStar(this.getNearestNodeToPos(this.getPosition()[0], this.getPosition()[1],
										node_t.PATH, true),
										this.getNearestNodeToPos(m_fEnemyLKPX, m_fEnemyLKPY, node_t.PATH, false) );
					}
				}
				//Enemy spotted. Stop dead in my tracks and fight him!
				else if ( m_eCurAction == action_t.MOVING && !m_hCurWeapon.isEmpty()
							&& this.hasLOSToPoint(m_hCurEnemy.getPosition()[0], m_hCurEnemy.getPosition()[1], true, m_hCurEnemy) )
				{
					m_hCurPath = null;
					m_eCurAction = action_t.DONEMOVING;
				}
			}
		}
		else	//Not currently engaging an enemy - patrol randomly.
		{
			//If I'm hunting, move twice as fast until I give up.
			if ( m_eCurState == state_t.HUNTING )
			{
				if ( Utils.ts().g_iTicks > m_iEndHuntTime )
				{
					this.holsterActiveWeapon();
					m_eCurState = state_t.IDLE;
					m_eCurAction = action_t.NOTHING;
					m_hCurNode = null;
					return;
				}
				m_fCurMoveSpeed = 0.25f;
				m_iPatrolSpeed = 1500;
				m_iGlanceSpeed = 5;
			}
			else	//I'm relaxed. Move at a walking pace.
			{
				m_fCurMoveSpeed = 0.125f;
				m_iPatrolSpeed = 3000;
				m_iGlanceSpeed = 10;
			}
			
			//I've come to the last node in the patrol route.
			//Look left and right for hostiles.
			if ( Utils.ts().g_iTicks < m_iNextPatrolTime && m_hCurPath.top() == null && m_eCurAction == action_t.LOOKING )
			{
				if ( !this.isTurning() )
				{
					switch ( m_iCurGlance )
					{
						case 0:
							setIntendedAngle( this.getAngle() - 60, m_iGlanceSpeed );
							m_iCurGlance = 1;
							break;
						case 1:
							setIntendedAngle( this.getAngle() + 120, m_iGlanceSpeed );
							m_iCurGlance = 2;
							break;
						case 2:
							setIntendedAngle( this.getAngle() - 60, m_iGlanceSpeed );
							m_iCurGlance = 3;
							m_iNextPatrolTime = Utils.ts().g_iTicks + m_iPatrolSpeed;
							break;
						case 3:
							m_eCurAction = action_t.DONELOOKING;
							break;
					}
				}
			}
			//Done looking. Generate a random patrol route and follow it.
			else if ( Utils.ts().g_iTicks > m_iNextPatrolTime && m_hCurPath.top() == null
					&& (m_eCurAction == action_t.NOTHING || m_eCurAction == action_t.DONELOOKING) )
			{
				m_iCurGlance = 0;
				NodeDef nearestNode = this.getNearestNodeToPos( this.getPosition()[0], this.getPosition()[1], node_t.PATH, true );
				if ( nearestNode != null )
				{
					m_hCurPath = Utils.findPathUsingAStar(
							nearestNode,
							Utils.ts().g_hNodes.get( (int)(Math.random() * Utils.ts().g_hNodes.size() ) ) );
				}
				m_eCurAction = action_t.MOVING;
			}
			
			//Check if any hostiles are in my line of sight.
			BaseCombatCharacter curPotentialHostile = null;
			for ( int i = 0; i < Utils.ts().g_hAllEntities.size(); i++ )
			{
				if ( Utils.ts().g_hAllEntities.get(i) instanceof BaseCombatCharacter)
					curPotentialHostile = (BaseCombatCharacter)Utils.ts().g_hAllEntities.get(i);
				else continue;
				
				if ( curPotentialHostile == this ) continue;
				if ( curPotentialHostile.getHealth() <= 0 ) continue;
				
				if ( this.getRelationshipToCharacter(curPotentialHostile) == relationship_t.HOSTILE
						&& this.hasLOSToPoint(curPotentialHostile.getPosition()[0], curPotentialHostile.getPosition()[1],
								true, curPotentialHostile) )
				{
					//Enemy spotted. Get ready for combat!
					if ( Utils.ts().g_hCurMap != null )
						Utils.ts().g_hCurMap.callEvent( curPotentialHostile.getName() + "_spotted" );
					this.equipStoredWeapon();
					m_eCurState = state_t.HOSTILE;
					m_eCurAction = action_t.COMBAT;
					m_hCurEnemy = curPotentialHostile;
					m_iNextPatrolTime = Utils.ts().g_iTicks + 1000;
					this.setIntendedPosition(this.getPosition()[0], this.getPosition()[1], 1);
				}
			}
			
			//Finished my patrol route. Look around a bit, and schedule my next patrol for later.
			if ( m_eCurAction == action_t.DONEMOVING )
			{
				m_iNextPatrolTime = Utils.ts().g_iTicks + m_iPatrolSpeed;
				m_eCurAction = action_t.LOOKING;
			}
		}
		
		//I've got a path to follow. So, follow it.
		if ( m_hCurPath.top() != null && m_hCurNode == null && m_eCurAction == action_t.MOVING )
		{
			m_hCurNode = (NodeDef)m_hCurPath.pop();
			
			this.setIntendedPosition( m_hCurNode.m_fXPos - this.getCollisionRect().width / 2,
										m_hCurNode.m_fYPos - this.getCollisionRect().height / 2,
					(int)(Utils.getDistance( this.getPosition()[0], this.getPosition()[1], m_hCurNode.m_fXPos, m_hCurNode.m_fYPos)
						/ m_fCurMoveSpeed) );
			this.lookAt(m_hCurNode.m_fXPos, m_hCurNode.m_fYPos, m_eCurState == state_t.IDLE, 3);
		}
		//I've come to a node. Clear it so that the next node can be popped off the path stack.
		else if ( m_eCurAction == action_t.MOVING && m_hCurNode != null )
		{
			if ( (this.getPosition()[0] == m_hCurNode.m_fXPos - this.getCollisionRect().width / 2
					&& this.getPosition()[1] == m_hCurNode.m_fYPos - this.getCollisionRect().height / 2) )
			{
				m_hCurNode = null;
			}
		}
		//I've reached the end of my route.
		else if ( m_hCurPath.top() == null && m_eCurAction == action_t.MOVING )
		{
			m_eCurAction = action_t.DONEMOVING;
		}
	}
	
	/**
	 * Finds the most suitable cover node for this NPC,
	 * ie. the closest one not in its line of sight.
	 * If that's not possible, find the one farthest from
	 * its current enemy.
	 * And if that isn't possible either, just find the one
	 * farthest from this.
	 * Preconditions:
	 * 	- g_hNodes has nodes in it, and they are connected to each other.
	 * Postconditions:
	 * 	- Returns a NodeDef representing the most suitable cover available.
	 * @return NodeDef that satisfies any of the above conditions.
	 */
	public NodeDef findCover()
	{
		//First, try finding the nearest obscure cover node.
		NodeDef closestCoverNode = null, curNode;
		float closestNodeScore = Utils.max32f;
		for ( int i = 0; i < Utils.ts().g_hNodes.size(); i++ )
		{
			curNode = Utils.ts().g_hNodes.get(i);
			if ( curNode.m_eType != node_t.COVER )
				continue;
			
			//if ( this.hasLOSToPoint(curNode.m_fXPos, curNode.m_fYPos, false, null) )
			//	continue;
			
			if ( m_hCurEnemy != null )
			{
				if ( m_hCurEnemy.hasLOSToPoint(curNode.m_fXPos, curNode.m_fYPos, false, this) )
					continue;
			}
			
			if ( Utils.getDistance(curNode.m_fXPos, curNode.m_fYPos, this.getPosition()[0], this.getPosition()[1] ) < closestNodeScore )
			{
				closestCoverNode = curNode;
				closestNodeScore = Utils.getDistance(curNode.m_fXPos, curNode.m_fYPos,
						this.getPosition()[0], this.getPosition()[1]);
			}
		}
		if ( closestCoverNode != null )
			return closestCoverNode;
		
		//Then, if we've got an enemy, try finding the node farthest from them.
		if ( m_hCurEnemy != null )
		{
			curNode = null;
			NodeDef farthestFromEnemyNode = null;
			float farthestFromEnemyNodeScore = -Utils.max32f;
			for ( int i = 0; i < Utils.ts().g_hNodes.size(); i++ )
			{
				curNode = Utils.ts().g_hNodes.get(i);
				
				if ( Utils.getDistance(curNode.m_fXPos, curNode.m_fYPos, m_hCurEnemy.getPosition()[0], m_hCurEnemy.getPosition()[1] )
						> farthestFromEnemyNodeScore )
				{
					farthestFromEnemyNode = curNode;
					farthestFromEnemyNodeScore = Utils.getDistance(curNode.m_fXPos, curNode.m_fYPos,
							m_hCurEnemy.getPosition()[0], m_hCurEnemy.getPosition()[1]);
				}
			}
			if ( farthestFromEnemyNode != null )
				return farthestFromEnemyNode;
		}
		
		//If step two fails, find the farthest path node from this instead. Visibility doesn't matter.
		curNode = null;
		NodeDef farthestPathNode = null;
		float farthestNodeScore = -Utils.max32f;
		for ( int i = 0; i < Utils.ts().g_hNodes.size(); i++ )
		{
			curNode = Utils.ts().g_hNodes.get(i);
			if ( curNode.m_eType != node_t.PATH )
				continue;
			
			if ( Utils.getDistance(curNode.m_fXPos, curNode.m_fYPos, this.getPosition()[0], this.getPosition()[1] ) > farthestNodeScore )
			{
				farthestPathNode = curNode;
				farthestNodeScore = Utils.getDistance(curNode.m_fXPos, curNode.m_fYPos,
						this.getPosition()[0], this.getPosition()[1]);
			}
		}
		
		return farthestPathNode;
	}
	
	/**
	 * Gets this NPC's relationship to another.
	 * Does not check the other NPC's relationship to this one.
	 * Preconditions:
	 * 	- hOther != null.
	 * Postconditions:
	 * 	- Returns a relationship_t that indicates this NPC's relationship
	 * 		to hOther.
	 * @param hOther - The other NPC.
	 * @return relationship_t indicating this NPC's relationship to hOther.
	 */
	public relationship_t getRelationshipToCharacter( BaseCombatCharacter hOther )
	{
		if ( this.m_hRelationships.get(hOther.getName()) != null )
			return this.m_hRelationships.get(hOther.getName());
		else if ( this.m_hRelationships.get(hOther.getClassname()) != null )
			return this.m_hRelationships.get(hOther.getClassname());
		else if ( this.m_hRelationships.get(hOther) != null )
			return this.m_hRelationships.get(hOther);
		
		return relationship_t.NEUTRAL;
	}
	
	/**
	 * Checks if this NPC has a line-of-sight to a point.
	 * Can do so with or without considering its field of view.
	 * Preconditions:
	 * 	- At least one of g_hWorldCollisionRects or g_hAllEntities
	 * 		is populated.
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer]
	 * Postconditions:
	 * 	- Returns whether or not this NPC has a line-of-sight to
	 * 		the point, taking into account the conditions specified.
	 * @param x - Point's X position.
	 * @param y - Point's Y position.
	 * @param bConsiderAngle - Whether or not to consider this
	 * 							entity's field of view to the point.
	 * @param hIgnoreThis - Entity to ignore when tracing to the point.
	 * 							Useful for checking LOS to enemies.
	 * @return True if this NPC has a line-of-sight to the point. False if not.
	 */
	public boolean hasLOSToPoint( float x, float y, boolean bConsiderAngle, BaseEntity hIgnoreThis )
	{
		//STEP ALPHA: Check angle
		if ( bConsiderAngle )
		{
			//First, get the direction vector of this NPC's cone of view.
			float dir_x = (float)Math.cos(Math.toRadians(this.getAngle()));
			float dir_y = (float)Math.sin(Math.toRadians(this.getAngle()));
			
			//Then, get the difference between the point position and this NPC's position.
			float diff_x = x - this.getPosition()[0];
			float diff_y = y - this.getPosition()[1];
			
			//Normalize the difference vector.
			float diff_length = (float)Math.sqrt(diff_x * diff_x + diff_y * diff_y);
			diff_x /= diff_length;
			diff_y /= diff_length;
			
			//Calculate the dot product and compare to cos(FOV/2).
			//If the dot product is less than the latter, it's not in the NPC's FOV.
			//Note that this assumes the NPC can see an infinite distance.
			float dotProduct = dir_x * diff_x + dir_y * diff_y;
			if ( dotProduct < Math.cos(Math.toRadians(FOV/2)) )
			{
				return false;
			}
		}
		
		//STEP BRAVO: Check intersection with collision bounding boxes
		ArrayList<Rectangle> hCollisionRects = new ArrayList<Rectangle>(Utils.ts().g_hWorldCollisionRects);
		BaseEntity curEnt = null;
		for ( int i = 0; i < Utils.ts().g_hAllEntities.size(); i++ )
		{
			curEnt = Utils.ts().g_hAllEntities.get(i);
			//Don't collide if the entity in question isn't solid, or is another NPC.
			if ( curEnt != null && curEnt.isSolid()  && !(curEnt instanceof BaseCombatCharacter) )
				hCollisionRects.add( curEnt.getCollisionRect() );
		}
		Rectangle curRect;
		for ( int i = 0; i < hCollisionRects.size(); i++ )
		{
			if ( hCollisionRects.get(i) == null ) break;
			curRect = (Rectangle)hCollisionRects.get(i);
			if ( curRect == this.getCollisionRect() )
			{
				continue;
			}
			if ( hIgnoreThis != null )
			{
				if ( hIgnoreThis.getCollisionRect() == curRect )
					continue;
			}
			
			//Half the width and height are added because we want to trace from the center, not the top left corner.
			if ( curRect.intersectsLine(this.getPosition()[0] + this.getCollisionRect().width / 2,
					this.getPosition()[1] + this.getCollisionRect().height / 2, x, y))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Gets the nearest node to a coordinate.
	 * Preconds:
	 * 	- g_hNodes is populated and the nodes are linked.
	 *  - x and y >= [min 32-bit integer] and <= [max 32-bit integer]
	 * Postconds:
	 * 	- Returns the nearest node to the specified position.
	 * @param x - Coordinate X position.
	 * @param y - Coordinate Y position.
	 * @param type - Type of node to find, or null if either works.
	 * @param bConsiderLOS - Consider the line-of-sight to the node.
	 * @return Nearest NodeDef to the coordinate specified.
	 */
	public NodeDef getNearestNodeToPos( float x, float y, node_t type, boolean bConsiderLOS )
	{
		NodeDef closestNode = null, curNode;
		float closestNodeScore = Utils.max32f;
		for ( int i = 0; i < Utils.ts().g_hNodes.size(); i++ )
		{
			curNode = Utils.ts().g_hNodes.get(i);
			if ( type != null && curNode.m_eType != type )
				continue;
			
			if ( bConsiderLOS && !this.hasLOSToPoint(curNode.m_fXPos, curNode.m_fYPos, false, null) )
				continue;
			
			if ( Utils.getDistance(curNode.m_fXPos, curNode.m_fYPos, x, y ) < closestNodeScore )
			{
				closestNode = curNode;
				closestNodeScore = Utils.getDistance(curNode.m_fXPos, curNode.m_fYPos, x, y );
			}
			
			if ( curNode.m_fXPos == x && curNode.m_fYPos == y )
				return curNode;
		}
		
		return closestNode;
	}
	
	/**
	 * Gives this NPC a weapon, which they kindly accept and equip.
	 * Preconds:
	 * 	- The weapon given is in g_hAllEntities.
	 * 	- wep != null.
	 * Postconditions:
	 * 	- m_hCurWeapon is now wep.
	 * 	- m_hCurWeapon's firing delay is set.
	 * 	- This NPC is given ownership of wep, the new m_hCurWeapon.
	 * @param wep - The weapon to be given and equipped.
	 */
	public void giveWeapon( BaseCombatWeapon wep )
	{
		if ( wep == null ) return;
		Utils.Assert( Utils.ts().g_hAllEntities.contains(wep) );
		
		Utils.playSound("weapons/weapon_raise.wav");
		m_hCurWeapon = wep;
		m_hCurWeapon.m_iNextFireTime = Utils.ts().g_iTicks + 750;
		m_hCurWeapon.setOwner(this);
	}
	
	/**
	 * Takes away the NPC's active weapon.
	 * Preconditions:
	 * 	- m_hCurWeapon != null;
	 * Postconditions:
	 * 	- Returns the NPC's current weapon.
	 * 	- If bRemove, removes NPC's ownership of the weapon,
	 * 		thereby making the weapon visible and no longer
	 * 		following this.
	 * @param bRemove - Set to true to remove ownership of the weapon.
	 * @return NPC's current weapon.
	 */
	public BaseCombatWeapon takeWeapon(boolean bRemove)
	{
		if ( m_hCurWeapon == null ) return null;
		
		Utils.playSound("weapons/weapon_lower.wav");
		BaseCombatWeapon temp = m_hCurWeapon;
		if ( bRemove ) temp.setOwner(null);
		m_hCurWeapon = null;
		return temp;
	}
	
	/**
	 * Equips the NPC's stored weapon, if there is no active
	 * weapon and there is a stored weapon available.
	 * Preconditions:
	 * 	- m_hCurWeapon == null.
	 * 	- m_hStoredWeapon != null.
	 * Postconditions:
	 * 	- m_hCurWeapon is now the previously stored weapon.
	 * 	- m_hStoredWeapon is null.
	 * @return Whether or not the equipping is successful.
	 */
	public boolean equipStoredWeapon()
	{
		if ( m_hCurWeapon != null || m_hStoredWeapon == null )
			return false;
		
		this.giveWeapon( m_hStoredWeapon );
		m_hStoredWeapon = null;
		return true;
	}
	
	/**
	 * Holsters this NPC's active weapon, if there is a weapon
	 * equipped and there is no weapon stored.
	 * Preconditions:
	 * 	- m_hCurWeapon != null.
	 * 	- m_hStoredWeapon == null.
	 * Postconditions:
	 * 	- m_hCurWeapon is null.
	 * 	- m_hStoredWeapon is now the previously active weapon.
	 * @return Whether or not the holstering is successful.
	 */
	public boolean holsterActiveWeapon()
	{
		if ( m_hCurWeapon == null || m_hStoredWeapon != null )
			return false;
		
		m_hStoredWeapon = this.takeWeapon(false);
		return true;
	}
	
	/**
	 * Sets this NPC's health, and plays a damage sound if
	 * the health is reduced.
	 * Preconditions:
	 * 	- Sounds npc/male/smallpain_0(1-3).wav,
	 * 			 npc/male/medpain_0(1-3).wav,
	 * 			 npc/male/largepain_0(1-3).wav, and
	 * 			 npc/male/dvision_0(1-3).wav exist.
	 * 	- This NPC is not dead.
	 * @param health - The value to set this NPC's health to.
	 */
	@Override
	public void setHealth( int health )
	{
		if ( m_eCurAction == action_t.DEAD )
			return;
		if ( health < this.getHealth() )
		{
			if ( health < 100 && health >= 80 )
				Utils.playSound("npc/male/smallpain_0" + (1 + (int)(Math.random() * 3)) + ".wav");
			else if ( health < 80 && health >= 60 )
				Utils.playSound("npc/male/medpain_0" + (1 + (int)(Math.random() * 3)) + ".wav");
			else if ( health < 60 && health >= 40 )
				Utils.playSound("npc/male/largepain_0" + (1 + (int)(Math.random() * 3)) + ".wav");
			else if ( health < 40 && health > 0 )
				Utils.playSound("npc/male/dvision_0" + (1 + (int)(Math.random() * 3)) + ".wav");
		}
		
		super.setHealth( health );
	}
	
	/**
	 * Called by a Projectile that hits this NPC.
	 * Makes the NPC turn toward the source of the disturbance.
	 * Preconditions:
	 * 	- x and y >= [min 32-bit integer] and <= [max 32-bit integer]
	 * Postconditions:
	 * 	- This NPC faces the position where the Projectile hit it.
	 * @param x - X coordinate to look to.
	 * @param y - Y coordinate to look to.
	 */
	public void onHit(float x, float y)
	{
		this.lookAt(x, y, true, 2);
	}
	
	/**
	 * Discharges the current weapon, if it's present, loaded, and ready to fire.
	 * Preconditions:
	 * 	- m_hCurWeapon != null.
	 * Postconditions:
	 * 	- Fires the event "[thisname]_attack".
	 * 	- If firing is successful, m_hCurWeapon has one less bullet.
	 * 	- If firing is successful, new Projectile(s) is/are created.
	 * 	- If firing is successful, the event "[thisname]_fire" is fired.
	 * 	- If firing is successful, the NPC's icon is switched to the one
	 * 		with it firing the gun, and sets it to revert after 100 ticks.
	 * @return
	 */
	public boolean attack()
	{
		if ( Utils.ts().g_hCurMap != null )
			Utils.ts().g_hCurMap.callEvent( this.getName() + "_attack" );
		if ( m_hCurWeapon == null ) return false;
		
		if ( m_hCurWeapon.fire() )
		{
			this.setIcon( Toolkit.getDefaultToolkit().getImage(m_szBaseIcon + m_hCurWeapon.getWeaponFireName()) );
			m_iNextAnimateTime = Utils.ts().g_iTicks + 100;
			if ( Utils.ts().g_hCurMap != null )
				Utils.ts().g_hCurMap.callEvent( this.getName() + "_fire" );
			return true;
		}
		
		return false;
	}
	
	/**
	 * Handles the death of this NPC.
	 * Preconditions:
	 * 	- Sounds npc/male/death_0(1-4).wav exist.
	 * Postconditions:
	 * 	- This NPC's icon is set to reflect their death.
	 * 	- This NPC is rotated to 90 degrees counter-clockwise.
	 * 	- m_eCurAction is set to DEAD.
	 * 	- If this NPC was headed somewhere, they will stop.
	 * 	- The active weapon is dropped and this NPC relinquishes
	 * 		ownership of it, if m_hCurWeapon != null.
	 * 	- If the current weapon is dropped, the extra ammo available
	 * 		is set to zero.
	 */
	public void die()
	{
		if ( Utils.ts().g_hCurMap != null )
			Utils.ts().g_hCurMap.callEvent( this.getName() + "_die" );
		this.setAngle(-90);
		Utils.playSound("npc/male/death_0" + (1 + (int)(Math.random() * 4)) + ".wav");
		if ( m_hCurWeapon != null )
		{
			if ( !(this instanceof Player) )
				m_hCurWeapon.setExtraAmmo(0);
			
			m_hCurWeapon.setOwner( null );
		}
		this.setIcon( Toolkit.getDefaultToolkit().getImage("materials/dead.png") );
		this.setIntendedPosition(this.getPosition()[0], this.getPosition()[1], 1);
		m_eCurAction = action_t.DEAD;
	}
	
	/**
	 * Coerces this NPC to do something that would normally
	 * be handled by its AI, or other conditions.
	 * Especially useful in
	 * {@link org.valachi_maas.finalFight.GameMap#parseInstructionQueue(Queue, boolean)}.
	 * No preconditions, save for those of the actions invoked.
	 * Postconditions:
	 * 	- The respective action is performed like so:
	 * 		attack: This NPC shoots its equipped gun, if it has one.
	 * 		reload: This NPC's gun is reloaded/starts reloading, if available.
	 * 		equipwpn: This NPC's holstered gun is equipped.
	 * 		holsterwpn: This NPC's equipped weapon is holstered.
	 * 		disableai: This NPC's AI is disabled.
	 * 		enableai: This NPC's AI is enabled, with the status set to IDLE.
	 * @param action - The name of the action to invoke.
	 */
	public void invokeAction( String action )
	{
		if ( action.equals("attack") )
		{
			this.attack();
		}
		else if ( action.equals("reload") )
		{
			if ( m_hCurWeapon != null )
			{
				m_hCurWeapon.reload();
			}
		}
		else if ( action.equals("equipwpn") )
		{
			this.equipStoredWeapon();
		}
		else if ( action.equals("holsterwpn") )
		{
			this.holsterActiveWeapon();
		}
		else if ( action.equals("disableai") )
		{
			this.m_eCurAction = action_t.DISABLED;
		}
		else if ( action.equals("enableai") )
		{
			this.m_eCurAction = action_t.NOTHING;
			this.m_eCurState = state_t.IDLE;
		}
	}
}
