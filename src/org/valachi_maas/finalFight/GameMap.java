package org.valachi_maas.finalFight;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.sound.sampled.*;

/**
 * A map to be used by the game.
 * Contains the layout, the collision map, the music, and the ambient.
 * Also contains the script and event instructions, plus the ability to execute them.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
public class GameMap
{
	public String m_szFilename = "";
	public Image m_hMapLayout;
	public BufferedImage m_hMapCollisions;
	
	Queue script_Main = new Queue();
	Map<String,Queue> script_Events = new HashMap<String,Queue>();
	
	public static final float TRANSITION_RATE = 0.01f;
	public static final float MAX_DB = 4.0f;
	public static final float MIN_DB = -79.0f;
	static final int ONESHOT_AMB_DELAY = 7500;
	public Clip m_hHuntingMusic;
	public Clip m_hHostileMusic;
	public int m_iMaxMusicFrame = 0;
	public ArrayList<String> m_hAmbientSounds = new ArrayList<String>();
	public ArrayList<Clip> m_hLoopingAmbientSounds = new ArrayList<Clip>();
	public int m_iNextOneShotAmbTime = 0;
	
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- maps/[szFilename].tsmap must exist.
	 * Postconditions:
	 * 	- New GameMap instance created.
	 * 	- script_Main and possibly also script_Events are populated
	 * 		with instructions to be executed.
	 * @param szFilename - Name of the map to load.
	 * 						Be sure to omit the "maps/" and the ".tsmap".
	 */
	public GameMap(String szFilename)
	{
		m_szFilename = "maps/" + szFilename + ".tsmap";
		try
		{
			loadMapScript();
		}
		catch ( IOException e )
		{
			System.err.println("Error loading map script " + m_szFilename + "!");
		}
	}
	
	/**
	 * Cleans up the sounds used by this map.
	 * No preconditions.
	 * Postconditions:
	 * 	- Every sound in m_hLoopingAmbientSounds is stopped.
	 * 	- m_hHuntingMusic and m_hHostileMusic are stopped.
	 */
	public void cleanupMap()
	{
		playNoTrack();
		for ( int i = 0; i < m_hLoopingAmbientSounds.size(); i++ )
		{
			m_hLoopingAmbientSounds.get(i).stop();
		}
		if ( m_hHuntingMusic != null )
		{
			m_hHuntingMusic.stop();
			m_hHuntingMusic.close();
		}
		if ( m_hHostileMusic != null )
		{
			m_hHostileMusic.stop();
			m_hHostileMusic.close();
		}
	}
	
	/**
	 * Loads a map script onto script_Main and script_Events.
	 * Preconditions:
	 * 	- File indicated by m_szFilename exists.
	 * Postconditions:
	 * 	- script_Main and possibly also script_Events are populated.
	 * @throws IOException
	 */
	public void loadMapScript() throws IOException
	{
		File map = new File(m_szFilename);
		FileReader hReader1 = new FileReader(map);
		BufferedReader hReader2 = new BufferedReader(hReader1);
		Scanner hStringReader = null;
		
		String curLine = null;
		while ( (curLine = hReader2.readLine()) != null )
		{
			if ( curLine.startsWith("ev_start") )	//Custom events.
			{
				Queue evQueue = new Queue();
				String evName = curLine.substring("ev_start ".length());
				while ( !(curLine = hReader2.readLine()).equals("ev_end") )
				{
					evQueue.enqueue(curLine);
				}
				script_Events.put(evName, evQueue);
			}
			//createnode and linknodes commands are included here instead of in parseInstructionQueue()
			//because g_hNodes must be populated prior to the engine starting to tick.
			else if ( curLine.startsWith("createnode") )	//Create a node.
			{
				hStringReader = new Scanner( curLine.substring( "createnode ".length() ) );
				int id = hStringReader.nextInt();
				float x = hStringReader.nextFloat();
				float y = hStringReader.nextFloat();
				node_t type = hStringReader.next().toLowerCase().equals("cover") ? node_t.COVER : node_t.PATH;
				Utils.ts().g_hNodes.add( new NodeDef(id, x, y, type) );
			}
			else if ( curLine.startsWith("linknodes") )	//Link nodes.
			{
				hStringReader = new Scanner( curLine.substring( "linknodes ".length() ) );
				int id1 = hStringReader.nextInt();
				int id2 = hStringReader.nextInt();
				Utils.linkNodes(id1, id2);
			}
			else
			{
				if ( !curLine.equals("") )
					script_Main.enqueue(curLine);
			}
		}
		if ( !Utils.ts().g_hNodes.isEmpty() ) Utils.insertionSort( Utils.ts().g_hNodes );
		
		if ( hStringReader != null ) hStringReader.close();
		hReader2.close();
		hReader1.close();
	}
	
	/**
	 * Parses a Queue of instructions, and executes commands based on the line.
	 * Preconditions:
	 * 	- DO NOT call this on the EDT thread.
	 * Postconditions:
	 * 	- The commands in the Queue are read and executed accordingly.
	 * 	- If bEvent, then the contents of instructions are returned.
	 * @param instructions - Instructions to execute.
	 * @param bEvent - If this is an event, we'll want to regenerate the input Queue
	 * 					so that the event can be called again in the future.
	 * 					That's because the Queue is passed by reference.
	 * @return Regenerated Queue of instructions, equal to input Queue.
	 */
	public Queue parseInstructionQueue( Queue instructions, boolean bEvent )
	{
		Queue eventRegenerator = new Queue();
		
		Scanner hStringReader = null;
		String szHuntingMusic = null, szHostileMusic = null;
		String curLine = null;
		while ( instructions.front() != null )
		{
			Utils.Assert(instructions.front() instanceof String);
			curLine = (String)instructions.dequeue();
			if ( curLine.startsWith("setbg") )					//Set background image.
			{
				try
				{
					m_hMapLayout = Utils.ts().g_hBGImage = ImageIO.read( new File(curLine.substring("setbg ".length())) );
				}
				catch ( IOException e )
				{
					e.printStackTrace();
				}
			}
			else if ( curLine.startsWith("setcollision") )		//Set the collision map, and generate collision data.
			{
				try
				{
					m_hMapCollisions = ImageIO.read( new File(curLine.substring("setcollision ".length())) );
					generateCollisionsFromMap();
				}
				catch ( IOException e )
				{
					e.printStackTrace();
				}
			}
			else if ( curLine.startsWith("addambsnd") )			//Add a one-shot ambient sound.
			{
				m_hAmbientSounds.add( curLine.substring("addambsnd ".length()) );
			}
			else if ( curLine.startsWith("setstealthmusic") )	//Set the music to play when enemies are hunting.
			{
				szHuntingMusic = curLine.substring("setstealthmusic ".length());
				if ( szHuntingMusic != null && szHostileMusic != null )
					initializeSoundtracks(szHuntingMusic, szHostileMusic);
			}
			else if ( curLine.startsWith("setcombatmusic") )	//Set the music to play when enemies are in combat against the player.
			{
				szHostileMusic = curLine.substring("setcombatmusic ".length());
				if ( szHuntingMusic != null && szHostileMusic != null )
					initializeSoundtracks(szHuntingMusic, szHostileMusic);
			}
			else if ( curLine.startsWith("cutscene") )			//Show black bars, take control from the player, hide HUD.
			{
				Utils.ts().g_bCutscene = true;
			}
			else if ( curLine.startsWith("combat") )			//Hide black bars, give control to the player, show HUD.
			{
				Utils.ts().g_bCutscene = false;
				Utils.ts().g_szCurSpokenLine = null;
			}
			else if ( curLine.startsWith("endmap") )			//Change the level to the next one, or end the game if argument's blank.
			{
				try
				{
					Utils.ts().loadMap( curLine.substring("endmap ".length()) );
					break;
				}
				catch ( Exception e )
				{
					Utils.ts().loadMap(null);
				}
			}
			else if ( curLine.startsWith("createobj") )			//Spawn a new entity.
			{
				hStringReader = new Scanner( curLine.substring("createobj ".length()) );
				String classname = hStringReader.next();
				String name = hStringReader.next();
				if ( name.equals("null") ) name = null;
				float x = hStringReader.nextFloat();
				float y = hStringReader.nextFloat();
				int health = hStringReader.nextInt();
				BaseEntity ent = Utils.createNewEntity(classname, name, x, y, health);
				if ( ent != null )
					Utils.ts().g_hAllEntities.add( ent );
			}
			else if ( curLine.startsWith("destroyobj") )		//Destroy an entity.
			{
				BaseEntity ent = Utils.findEntityByName( curLine.substring("destroyobj ".length()) );
				if ( ent != null )
				{
					ent.setPosition(Utils.max32f, Utils.max32f, false);
					ent.setShouldDraw(false);
					if ( ent instanceof BaseCombatCharacter )	//If we're deleting an NPC, remove its inventory too.
					{
						if ( ((BaseCombatCharacter)ent).m_hCurWeapon != null )
						{
							((BaseCombatCharacter)ent).m_hCurWeapon.setPosition(Utils.max32f, Utils.max32f, false);
							((BaseCombatCharacter)ent).m_hCurWeapon.setShouldDraw(false);
							Utils.ts().g_hAllEntities.remove( ((BaseCombatCharacter)ent).m_hCurWeapon );
						}
						if ( ((BaseCombatCharacter)ent).m_hStoredWeapon != null )
						{
							((BaseCombatCharacter)ent).m_hStoredWeapon.setPosition(Utils.max32f, Utils.max32f, false);
							((BaseCombatCharacter)ent).m_hStoredWeapon.setShouldDraw(false);
							Utils.ts().g_hAllEntities.remove( ((BaseCombatCharacter)ent).m_hStoredWeapon );
						}
					}
					Utils.ts().g_hAllEntities.remove(ent);
				}
			}
			else if ( curLine.startsWith("sethealth") )			//Sets the health of an entity.
			{
				hStringReader = new Scanner( curLine.substring("sethealth ".length()) );
				String name = hStringReader.next();
				int health = hStringReader.nextInt();
				BaseEntity ent = Utils.findEntityByName(name);
				if ( ent != null )
				{
					ent.setHealth(health);
				}
			}
			else if ( curLine.startsWith("movent") )			//Move, glide, and/or rotate an entity.
			{
				hStringReader = new Scanner( curLine.substring("movent ".length()) );
				String name = hStringReader.next();
				float x = hStringReader.nextFloat();
				float y = hStringReader.nextFloat();
				int angle = hStringReader.nextInt();
				boolean bSmooth = hStringReader.nextBoolean();
				int mvSpeed = 1, angSpeed = 1;
				if ( bSmooth )
				{
					mvSpeed = hStringReader.nextInt();
					angSpeed = hStringReader.nextInt();
				}
				BaseEntity ent = Utils.findEntityByName(name);
				if ( ent != null )
				{
					if ( bSmooth )
					{
						ent.setIntendedPosition(x, y, mvSpeed);
						ent.setIntendedAngle(angle, angSpeed);
					}
					else
					{
						ent.setPosition(x, y, false);
						ent.setAngle(angle);
					}
				}
			}
			else if ( curLine.startsWith("lookatent") )			//Look at an entity.
			{
				hStringReader = new Scanner( curLine.substring("lookatent ".length()) );
				String name = hStringReader.next();
				String target = hStringReader.next();
				boolean bSmooth = hStringReader.nextBoolean();
				int rotateSpeed = hStringReader.nextInt();
				BaseEntity ent1 = Utils.findEntityByName(name);
				if ( ent1 != null )
				{
					BaseEntity ent2 = Utils.findEntityByName(target);
					if ( ent2 != null )
					{
						ent1.lookAt(ent2.getPosition()[0], ent2.getPosition()[1], bSmooth, rotateSpeed);
					}
				}
			}
			else if ( curLine.startsWith("lookat") )			//Look toward a position.
			{
				hStringReader = new Scanner( curLine.substring("lookat ".length()) );
				String name = hStringReader.next();
				float x = hStringReader.nextFloat();
				float y = hStringReader.nextFloat();
				boolean bSmooth = hStringReader.nextBoolean();
				int rotateSpeed = hStringReader.nextInt();
				BaseEntity ent = Utils.findEntityByName(name);
				if ( ent != null )
				{
					ent.lookAt(x, y, bSmooth, rotateSpeed);
				}
			}
			else if ( curLine.startsWith("sendnpctonode") )		//Gives an NPC a path to follow.
			{
				hStringReader = new Scanner( curLine.substring("sendnpctonode ".length()) );
				BaseCombatCharacter npc = (BaseCombatCharacter)Utils.findEntityByName( hStringReader.next() );
				int nodeID = hStringReader.nextInt();
				if ( npc != null )
				{
					npc.m_hCurNode = null;
					npc.m_hCurPath = Utils.findPathUsingAStar(npc.getNearestNodeToPos(npc.getPosition()[0], npc.getPosition()[1],
							node_t.PATH, true), Utils.findNode(nodeID));
				}
			}
			else if ( curLine.startsWith("speak") )				//Cutscene mode: Displays text near the bottom of the frame.
			{
				try
				{
					Utils.ts().g_szCurSpokenLine = curLine.substring("speak ".length());
				}
				catch ( Exception e )
				{
					Utils.ts().g_szCurSpokenLine = null;
				}
			}
			else if ( curLine.startsWith("wait") )				//Halts the parser for a set time, but not necessarily the game.
			{
				try
				{
					Thread.sleep( Integer.parseInt(curLine.substring("wait ".length())) );
				}
				catch (NumberFormatException | InterruptedException e) {}
			}
			else if ( curLine.startsWith("trigger") )			//Calls an event.
			{
				callEvent( curLine.substring("trigger ".length()) );
			}
			else if ( curLine.startsWith("playsound") )			//Plays a sound.
			{
				Utils.playSound( curLine.substring("playsound ".length()) );
			}
			else if ( curLine.startsWith("loopsound") )			//Loops a sound.
			{
				m_hLoopingAmbientSounds.add( Utils.playLoopedSound( curLine.substring("playsound ".length()) ) );
			}
			else if ( curLine.startsWith("stoploopingsounds") )	//Kills all looping sounds.
			{
				for ( int i = 0; i < m_hLoopingAmbientSounds.size(); i++ )
				{
					m_hLoopingAmbientSounds.get(i).stop();
				}
			}
			else if ( curLine.startsWith("createnode") )//This is just here so that the console isn't spammed with "unrecognized command".
			{
				/*hStringReader = new Scanner( curLine.substring( "createnode ".length() ) );
				int id = hStringReader.nextInt();
				float x = hStringReader.nextFloat();
				float y = hStringReader.nextFloat();
				node_t type = hStringReader.next().toLowerCase().equals("cover") ? node_t.COVER : node_t.PATH;
				Utils.ts().g_hNodes.add( new NodeDef(id, x, y, type) );*/
			}
			else if ( curLine.startsWith("linknodes") )			//Ditto.
			{
				/*hStringReader = new Scanner( curLine.substring( "linknodes ".length() ) );
				int id1 = hStringReader.nextInt();
				int id2 = hStringReader.nextInt();
				Utils.linkNodes(id1, id2);*/
			}
			else if ( curLine.startsWith("showframe") )			//Displays a fullscreen picture.
			{
				try
				{
					Utils.ts().g_hFrameToBeDrawn = ImageIO.read( new File( curLine.substring("showframe ".length() ) ) );
					Utils.ts().g_eCurrentGUI = gui_t.PICTURE;
				}
				catch ( IOException e )
				{
					e.printStackTrace();
				}
			}
			else if ( curLine.startsWith("hideframe") )			//Hides the fullscreen picture, and reverts to the game.
			{
				Utils.ts().g_eCurrentGUI = gui_t.GAME;
				Utils.ts().g_hFrameToBeDrawn = null;
			}
			else if ( curLine.startsWith("fadein") )			//Fade in from black.
			{
				while ( Utils.ts().g_fFadeLevel > 0.0f )
				{
					Utils.ts().g_fFadeLevel -= TheShootout.FADE_AMT;
					try
					{
						Thread.sleep(1);
					}
					catch (InterruptedException e) {}
				}
			}
			else if ( curLine.startsWith("fadeout") )			//Fade out to black.
			{
				while ( Utils.ts().g_fFadeLevel < 1.0f )
				{
					Utils.ts().g_fFadeLevel += TheShootout.FADE_AMT;
					try
					{
						Thread.sleep(1);
					}
					catch (InterruptedException e) {}
				}
			}
			else if ( curLine.startsWith("startblack") )		//Should the screen appear black?
			{
				hStringReader = new Scanner( curLine.substring("startblack ".length()) );
				boolean bStartBlack = hStringReader.nextBoolean();
				Utils.ts().g_fFadeLevel = bStartBlack ? 1.0f : 0.0f;
			}
			else if ( curLine.startsWith("notrack") )			//Disables both stealth and combat tracks.
			{
				playNoTrack();
			}
			else if ( curLine.startsWith("stealthtrack") )		//Immediately switches to stealth track.
			{
				playHuntingTrack();
			}
			else if ( curLine.startsWith("combattrack") )		//Immediately switches to combat track.
			{
				playHostileTrack();
			}
			else if ( curLine.startsWith("invokenpcaction") )	//Coerce an NPC to doing an action.
			{
				hStringReader = new Scanner( curLine.substring("invokenpcaction ".length()) );
				String name = hStringReader.next();
				String action = hStringReader.next();
				BaseCombatCharacter npc = (BaseCombatCharacter)Utils.findEntityByName(name);
				if ( npc != null )
				{
					npc.invokeAction(action);
				}
			}
			else if ( curLine.startsWith("scroll") )			//Enable scrolling.
			{
				Utils.ts().g_bScroll = true;
			}
			else if ( curLine.startsWith("noscroll") )			//Disable scrolling.
			{
				Utils.ts().g_bScroll = false;
			}
			else	//Unrecognized command.
			{
				System.err.println( "Unrecognized command \"" + curLine + "\" - if command is valid, ensure file is ANSI-encoded." );
			}
			
			//If this is an event, we should regenerate and return the instructions given.
			//That way we can call this again in the future.
			if ( bEvent )
				eventRegenerator.enqueue( curLine );
		}
		if ( hStringReader != null ) hStringReader.close();
		
		return eventRegenerator;
	}
	
	/**
	 * Calls an event asynchronously.
	 * Preconditions:
	 * 	- Event exists in script_Events.
	 * Postconditions:
	 * 	- The instructions pertaining to the event
	 * 		are executed.
	 * @param event - The event to be called.
	 */
	public void callEvent(final String event)
	{
		if ( script_Events.containsKey(event) )
		{
			new Thread( new Runnable() {
				public void run()
				{
					//If it's a one-shot event, don't bother regenerating it.
					Queue eventInstructions = parseInstructionQueue( script_Events.get(event), !event.startsWith("oneshot_") );
					script_Events.put(event, eventInstructions);
				}
			} ).start();
		}
	}
	
	/**
	 * Initializes the stealth/combat Clip objects from two filenames.
	 * Preconditions:
	 * 	- The files pointed to by szHuntingMusic and szHostileMusic
	 * 		actually exist.
	 * Postconditions:
	 * 	- m_hHuntingMusic and m_hHostileMusic are initialized, and their volumes
	 * 		set to be inaudible.
	 * @param szHuntingMusic - Stealth track filename.
	 * @param szHostileMusic - Combat track filename.
	 */
	public void initializeSoundtracks(String szHuntingMusic, String szHostileMusic)
	{
		m_hHuntingMusic = Utils.playLoopedSound(szHuntingMusic);
		((FloatControl)m_hHuntingMusic.getControl(FloatControl.Type.MASTER_GAIN)).setValue(MIN_DB);
		m_hHostileMusic = Utils.playLoopedSound(szHostileMusic);
		((FloatControl)m_hHostileMusic.getControl(FloatControl.Type.MASTER_GAIN)).setValue(MIN_DB);
		
		//Limits the times of both tracks, as they may be off by a few frames.
		//For the first few loops, it's barely noticeable, but as time goes on, the un-seamlessness becomes evident.
		m_iMaxMusicFrame = (m_hHuntingMusic.getFrameLength() <= m_hHostileMusic.getFrameLength() ? m_hHuntingMusic : m_hHostileMusic)
								.getFrameLength() - 1;
		m_hHuntingMusic.setLoopPoints(0, m_iMaxMusicFrame);
		m_hHostileMusic.setLoopPoints(0, m_iMaxMusicFrame);
	}
	
	/**
	 * Mutes both the stealth track and the combat track.
	 * Preconditions:
	 * 	- m_hHuntingMusic and m_hHostileMusic != null.
	 * Postconditions:
	 * 	- m_hHuntingMusic and m_hHostileMusic's master gains are set to MIN_DB.
	 */
	public void playNoTrack()
	{
		if ( m_hHuntingMusic == null || m_hHostileMusic == null ) return;
		
		((FloatControl)m_hHuntingMusic.getControl(FloatControl.Type.MASTER_GAIN)).setValue(MIN_DB);
		((FloatControl)m_hHostileMusic.getControl(FloatControl.Type.MASTER_GAIN)).setValue(MIN_DB);
	}
	
	/**
	 * Mutes the combat track, maximizes the stealth track volume.
	 * Preconditions:
	 * 	- m_hHuntingMusic and m_hHostileMusic != null.
	 * Postconditions:
	 * 	- m_hHuntingMusic's master gain is set to MAX_DB.
	 * 	- m_hHostileMusic's master gain is set to MIN_DB.
	 */
	public void playHuntingTrack()
	{
		if ( m_hHuntingMusic == null || m_hHostileMusic == null ) return;
		
		((FloatControl)m_hHuntingMusic.getControl(FloatControl.Type.MASTER_GAIN)).setValue(MAX_DB);
		((FloatControl)m_hHostileMusic.getControl(FloatControl.Type.MASTER_GAIN)).setValue(MIN_DB);
	}
	
	/**
	 * Mutes the stealth track, maximizes the combat track volume.
	 * Preconditions:
	 * 	- m_hHuntingMusic and m_hHostileMusic != null.
	 * Postconditions:
	 * 	- m_hHuntingMusic's master gain is set to MIN_DB.
	 * 	- m_hHostileMusic's master gain is set to MAX_DB.
	 */
	public void playHostileTrack()
	{
		if ( m_hHuntingMusic == null || m_hHostileMusic == null ) return;
		
		((FloatControl)m_hHuntingMusic.getControl(FloatControl.Type.MASTER_GAIN)).setValue(MIN_DB);
		((FloatControl)m_hHostileMusic.getControl(FloatControl.Type.MASTER_GAIN)).setValue(MAX_DB);
	}
	
	/**
	 * Smoothly transitions from the combat track to the stealth track.
	 * Preconditions:
	 * 	- m_hHuntingMusic and m_hHostileMusic != null.
	 * Postconditions:
	 * 	- m_hHuntingMusic's master gain is increased by TRANSITION_RATE * 6.
	 * 	- m_hHostileMusic's master gain is decreased by TRANSITION_RATE.
	 */
	public void transitionToHuntingTrack()
	{
		if ( m_hHuntingMusic == null || m_hHostileMusic == null ) return;
		
		FloatControl huntingCtrl = (FloatControl)m_hHuntingMusic.getControl(FloatControl.Type.MASTER_GAIN);
		FloatControl hostileCtrl = (FloatControl)m_hHostileMusic.getControl(FloatControl.Type.MASTER_GAIN);
		
		if ( huntingCtrl.getValue() < MAX_DB )
		{
			huntingCtrl.setValue( huntingCtrl.getValue() + TRANSITION_RATE * 6 );
		}
		if ( hostileCtrl.getValue() > MIN_DB )
		{
			hostileCtrl.setValue( hostileCtrl.getValue() - TRANSITION_RATE );
		}
	}
	
	/**
	 * Smoothly transitions from the stealth track to the combat track.
	 * Preconditions:
	 * 	- m_hHuntingMusic and m_hHostileMusic != null.
	 * Postconditions:
	 * 	- m_hHuntingMusic's master gain is decreased by TRANSITION_RATE.
	 * 	- m_hHostileMusic's master gain is increased by TRANSITION_RATE * 6.
	 */
	public void transitionToHostileTrack()
	{
		if ( m_hHuntingMusic == null || m_hHostileMusic == null ) return;
		
		FloatControl huntingCtrl = (FloatControl)m_hHuntingMusic.getControl(FloatControl.Type.MASTER_GAIN);
		FloatControl hostileCtrl = (FloatControl)m_hHostileMusic.getControl(FloatControl.Type.MASTER_GAIN);
		
		if ( huntingCtrl.getValue() > MIN_DB )
		{
			huntingCtrl.setValue( huntingCtrl.getValue() - TRANSITION_RATE );
		}
		if ( hostileCtrl.getValue() < MAX_DB )
		{
			hostileCtrl.setValue( hostileCtrl.getValue() + TRANSITION_RATE * 6 );
		}
	}
	
	/**
	 * Smoothly mutes both the stealth track and the combat track.
	 * Preconditions:
	 * 	- m_hHuntingMusic and m_hHostileMusic != null.
	 * Postconditions:
	 * 	- m_hHuntingMusic's master gain is decreased by TRANSITION_RATE.
	 * 	- m_hHostileMusic's master gain is decreased by TRANSITION_RATE.
	 */
	public void transitionToNoTrack()
	{
		if ( m_hHuntingMusic == null || m_hHostileMusic == null ) return;
		
		FloatControl huntingCtrl = (FloatControl)m_hHuntingMusic.getControl(FloatControl.Type.MASTER_GAIN);
		FloatControl hostileCtrl = (FloatControl)m_hHostileMusic.getControl(FloatControl.Type.MASTER_GAIN);
		
		if ( huntingCtrl.getValue() > MIN_DB )
		{
			huntingCtrl.setValue( huntingCtrl.getValue() - TRANSITION_RATE );
		}
		if ( hostileCtrl.getValue() > MIN_DB )
		{
			hostileCtrl.setValue( hostileCtrl.getValue() - TRANSITION_RATE );
		}
	}
	
	int rect_x = 0;
	int rect_y = 0;
	int width = 0;
	int height = 0;
	/**
	 * Generates collision rectangles from m_hMapCollisions.
	 * Preconditions:
	 * 	- m_hMapCollisions != null.
	 * 	- m_hMapCollisions must be rather low-resolution (ie. at least 4 times lower resolution
	 * 		than m_hMapLayout), or else the recursion will cause a call stack overflow.
	 * Postconditions:
	 * 	- m_hMapCollisions is entirely whitened, if all goes well.
	 * 	- g_hWorldCollisionRects is populated with collision bounding boxes.
	 */
	public void generateCollisionsFromMap()
	{
		Utils.ts().g_hWorldCollisionRects.clear();
		
		//The collision map must be of a substantially lower resolution, so the rectangles generated through
		//it must be scaled accordingly.
		float scale = 1;
		if ( m_hMapLayout != null )
			scale = (float)this.m_hMapLayout.getWidth(Utils.ts()) / this.m_hMapCollisions.getWidth() + 0.025f;
		
		//Checks for collision bounding boxes, from upper-left to bottom-right of m_hMapCollisions.
		BufferedImage collisionMap = m_hMapCollisions;
		for ( int j = 0; j < collisionMap.getHeight(); j++ )
		{
			for ( int i = 0; i < collisionMap.getWidth(); i++ )
			{
				rect_x = i;
				rect_y = j;
				width = 0;
				height = 0;
				collectPixels(collisionMap, i, j);
				if ( width > 0 && height > 0 )
				{
					Utils.ts().g_hWorldCollisionRects.add( new Rectangle( (int)(rect_x * scale
							- (int)(Utils.ts().g_hBGImage.getWidth(Utils.ts())/2) ),
						(int)(rect_y * scale - Utils.ts().center_y
							+ (int)(Utils.ts().g_hBGImage.getHeight(Utils.ts())/10) ),
						(int)(width * scale), (int)(height * scale) ) );
				}
			}
		}
	}
	
	/**
	 * Destructively and recursively gets the dimensions of a rectangle
	 * (see the variables for this above) from a BufferedImage.
	 * Preconditions:
	 * 	- img != null.
	 * Postconditions:
	 * 	- Variables rect_x, rect_y, width, and height will be set to the
	 * 		dimensions of a rectangle from img.
	 * 	- Where there used to be a black/colored shape in img, there will
	 * 		be a white rectangle encompassing the collision area.
	 * @param img - The BufferedImage to grab a Rectangle's dimensions from.
	 * @param x - X coordinate in img.
	 * @param y - Y coordinate in img.
	 */
	private void collectPixels(BufferedImage img, int x, int y)
	{
		try
		{
			Color pixel = new Color( img.getRGB(x, y) );
			if ( pixel.getRed() != 0 || pixel.getBlue() != 0 || pixel.getGreen() != 0
					|| x < 0 || x > img.getWidth()
					|| y < 0 || y > img.getHeight() )
			{
				return;
			}
			else
			{
				img.setRGB(x, y, -1);
				if ( x < rect_x ) rect_x = x;
				if ( y < rect_y ) rect_y = y;
				if ( x - rect_x > width ) width = x - rect_x;
				if ( y - rect_y > height ) height = y - rect_y;
				
				//Check all around this pixel.
				//Find the dimensions using recursion.
				collectPixels( img, x + 1, y );
				collectPixels( img, x, y + 1 );
				collectPixels( img, x + 1, y + 1 );
				collectPixels( img, x - 1, y );
				collectPixels( img, x, y - 1 );
				collectPixels( img, x - 1, y - 1 );
				collectPixels( img, x + 1, y - 1 );
				collectPixels( img, x - 1, y + 1 );
			}
		}
		catch ( ArrayIndexOutOfBoundsException e )
		{
			//This error happens sometimes. Not sure why - the coordinates are valid
			//- but no harm done by placing this try-catch block around the function code.
			return;
		}
	}
}