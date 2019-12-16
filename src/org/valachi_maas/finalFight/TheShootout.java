package org.valachi_maas.finalFight;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

import javax.imageio.*;
import javax.swing.*;

enum gui_t	//GUI's current screen.
{
	GAME,	//In-game.
	PAUSE,	//Paused.
	PICTURE	//Displaying a still frame.
}

/**
 * A top-down 2D scrolling action-adventure game, with interactivity and cutscenes.
 * @author Gabriel Valachi
 * @author Anthony Maas
 * @since June 19, 2017
 * @category ICS4U
 */
@SuppressWarnings("serial")
public class TheShootout extends JFrame implements Runnable, KeyListener, MouseListener
{
	public static TheShootout g_hActiveInstance;								//Active instance of the game. There can only be one.
	public static final boolean SHOW_AI_NODES = true;
	public static final boolean SHOW_COLLISION_RECTS = true;
	
	public GUIInGame g_hContentPane = null;										//Content pane for everything in this game.
	public int center_x, center_y;												//Center X of the window.
	public int mouse_x, mouse_y;												//Center Y of the window.
	public ArrayList<BaseEntity> g_hAllEntities = new ArrayList<BaseEntity>();	//All the entities that must think and be drawn.
	public ArrayList<NodeDef> g_hNodes = new ArrayList<NodeDef>();				//All the nodes NPCs can go to.
	public Thread g_hTickThread;												//Refreshes the game every tick.
	
	public boolean g_bScroll = true;											//If true, the game follows the player's position.
	public int rx = 0, ry = 0;													//X render offset.
	public int g_iTicks = 0;													//Total number of ticks that passed since the start.
	public Image g_hBGImage;													//Background image of this level.
	public Image g_hFrameToBeDrawn = null;									//The frame to be drawn, if the g_eCurrentGUI == PICTURE.
	public ArrayList<Rectangle> g_hWorldCollisionRects = new ArrayList<Rectangle>();	//The world to collide with.
	
	public GameMap g_hCurMap;													//The current map loaded.
	public gui_t g_eCurrentGUI = gui_t.GAME;									//The current GUI to display.
	public boolean g_bGamePaused = false;										//Whether or not the game is paused.
	public boolean g_bCutscene = false;											//Whether or not we're seeing a cutscene.
	public boolean g_bShouldKeepTicking = true;									//Set to false to kill g_hTickThread.
	public Player g_hThePlayer = null;											//The player. There must always be one.
	public String g_szCurSpokenLine = null;										//Current line to speak, if in cutscene mode.
	public double g_fFadeLevel = 0.0;											//Fade-to-black amount.
	static final double FADE_AMT = 0.001;										//Rate of fading in/out.
	
	//Key presses
	public boolean w = false;
	public boolean a = false;
	public boolean s = false;
	public boolean d = false;
	public boolean zero = false;
	public boolean zero_oneshot = true;		//*_oneshot means that this key should only be executed once when pressed.
	public boolean one = false;
	public boolean one_oneshot = true;
	public boolean two = false;
	public boolean two_oneshot = true;
	public boolean m1 = false;
	public boolean r = false;
	public boolean r_oneshot = true;
	public boolean e = false;
	public boolean e_oneshot = true;
	public boolean esc = false;
	public boolean esc_oneshot = true;
	
	/**
	 * Entry point.
	 * No preconditions.
	 * Postconditions:
	 * 	- A new instance of TheShootout is created, and g_hActiveInstance is set
	 * 		to equal that new instance.
	 * @param args - Program takes no arguments.
	 */
	public static void main( String[] args )
	{
		javax.swing.SwingUtilities.invokeLater( new Runnable()
		{
			public void run()
			{
				g_hActiveInstance = new TheShootout();
			}
		} );
	}
	
	/**
	 * Constructor.
	 * Preconditions:
	 * 	- g_hActiveInstance == null.
	 * Postconditions:
	 * 	- New TheShootout instance created.
	 * 	- Content pane of type GUIInGame is created, and this
	 * 		instance is set to use that content pane.
	 * 	- Size is set to 1280 x 720.
	 * 	- g_hTickThread is created from this, and started.
	 */
	public TheShootout()
	{
		super("The Shootout");
		if ( g_hActiveInstance != null )
		{
			System.err.println( "Only one instance of The Shootout can be active." );
			return;
		}
		
		try
		{
			g_hBGImage = ImageIO.read( new File("materials/temp.png") );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		
		g_hContentPane = new GUIInGame();
		this.setContentPane( g_hContentPane );
		this.setSize( 1280, 720 );
		center_x = this.getSize().width / 2;
		center_y = this.getSize().height / 2;
		
		this.addKeyListener( this );
		this.addMouseListener( this );
		
		this.setResizable( false );
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		this.setVisible( true );
		g_hTickThread = new Thread(this);
		g_hTickThread.start();
	}
	
	/**
	 * Called when a key is pressed.
	 * No preconditions.
	 * Postconditions:
	 * 	- Boolean for the key pressed is set to true.
	 */
	@Override
	public void keyPressed(KeyEvent arg0)
	{
		switch ( arg0.getKeyCode() )
		{
			case 'W':
				w = true;
				break;
			case 'A':
				a = true;
				break;
			case 'S':
				s = true;
				break;
			case 'D':
				d = true;
				break;
			case 27:	//ESC
				esc = true;
				break;
			case '0':
				zero = true;
				break;
			case '1':
				one = true;
				break;
			case '2':
				two = true;
				break;
			case 'R':
				r = true;
				break;
			case 'E':
				e = true;
				break;
		}
	}
	
	/**
	 * Called when a key is released.
	 * No preconditions.
	 * Postconditions:
	 * 	- The respective key's boolean is set to false.
	 * 	- If the key is a one-shot action, reset the respective
	 * 		*_oneshot boolean to true.
	 */
	@Override
	public void keyReleased(KeyEvent arg0)
	{
		switch ( arg0.getKeyCode() )
		{
			case 'W':
				w = false;
				break;
			case 'A':
				a = false;
				break;
			case 'S':
				s = false;
				break;
			case 'D':
				d = false;
				break;
			case '0':
				zero = false;
				zero_oneshot = true;
				break;
			case '1':
				one = false;
				one_oneshot = true;
				break;
			case '2':
				two = false;
				two_oneshot = true;
				break;
			case 'R':
				r = false;
				r_oneshot = true;
				break;
			case 'E':
				e = false;
				e_oneshot = true;
				break;
			case 27:
				esc = false;
				esc_oneshot = true;
				break;
		}
	}

	//Move along, now. Nothing to see here.
	//The program never uses these, but still must implement them.
	@Override
	public void keyTyped(KeyEvent arg0){}
	@Override
	public void mouseClicked(MouseEvent arg0){}
	@Override
	public void mouseEntered(MouseEvent arg0){}
	@Override
	public void mouseExited(MouseEvent arg0){}
	
	/**
	 * Called when a mouse button is pressed.
	 * No preconditions.
	 * Postconditions:
	 * 	- The boolean for the respective mouse button is set to true.
	 */
	@Override
	public void mousePressed(MouseEvent arg0)
	{
		if ( arg0.getButton() == MouseEvent.BUTTON1 )
			m1 = true;
	}
	
	/**
	 * Called when a mouse button is released.
	 * No preconditions.
	 * Postconditions:
	 * 	- The boolean for the respective mouse button is set to false.
	 */
	@Override
	public void mouseReleased(MouseEvent arg0)
	{
		if ( arg0.getButton() == MouseEvent.BUTTON1 )
			m1 = false;
	}
	
	/**
	 * Called by g_hTickThread.
	 * No preconditions.
	 * Postconditions:
	 * 	- g_hContentPane is repainted.
	 * 	- All entities are simulated if the game's not paused.
	 * 	- Background music changes depending on other entities' current status.
	 * 	- Ambient music is played randomly, if available.
	 * 	- Mouse position is updated.
	 */
	@Override
	public void run()
	{
		pause(false);
		loadMap("start");	//From this point on, map loading is handled by the maps themselves. TODO
		
		PointerInfo mouse = MouseInfo.getPointerInfo();
		while ( g_bShouldKeepTicking )
		{
			g_hContentPane.repaint();
			
			if ( !g_bGamePaused && esc && esc_oneshot && !g_bCutscene )
			{
				esc_oneshot = false;
				pause(!g_bGamePaused);
			}
			if ( g_bGamePaused )	//Don't update entities if the game's paused.
				continue;
			
			mouse = MouseInfo.getPointerInfo();	//Refresh mouse data.
			mouse_x = mouse.getLocation().x - center_x - rx;
			mouse_y = mouse.getLocation().y - center_y - ry;
			g_iTicks++;
			
			//If we want to scroll with the player, offset the X and Y position
			//of everything by the player's current position.
			if ( g_bScroll )
			{
				rx = -(int)g_hThePlayer.getPosition()[0];
				ry = -(int)g_hThePlayer.getPosition()[1];
			}
			
			if ( !g_bCutscene )
			{
				//Check if all the Dalton Bros, Earps, or Police are dead.
				//Fire the appropriate map events if they are.
				//Also, wait five seconds to check if they're all dead.
				//They can't possibly ALL die in less than five seconds!
				if ( g_iTicks > 5000 )
				{
					DaltonBro.allDaltonsDead();
					Earp.allEarpsDead();
					Police.allCopsDead();
				}
				
				//Check if there are any entities hunting or fighting the player.
				//If there are, update the music accordingly.
				int iStatus = 0;
				BaseCombatCharacter curEnt = null;
				for ( int i = 0; i < g_hAllEntities.size(); i++ )
				{
					if ( g_hAllEntities.get(i) instanceof BaseCombatCharacter )
						curEnt = (BaseCombatCharacter)g_hAllEntities.get(i);
					else continue;
					
					if ( curEnt.getRelationshipToCharacter(g_hThePlayer) != relationship_t.HOSTILE
							|| curEnt.getHealth() <= 0 )
						continue;
					
					if ( curEnt.m_eCurState == state_t.HUNTING )
					{
						iStatus = 1;
					}
					else if ( curEnt.m_eCurState == state_t.HOSTILE
								&& curEnt.m_hCurEnemy == g_hThePlayer )
					{
						iStatus = 2;
						break;	//HOSTILE has greater weight than HUNTING.
					}
				}
				switch ( iStatus )
				{
					case 0:	//All NPCs idle.
						g_hCurMap.transitionToNoTrack();
						break;
					case 1:	//Some NPCs hunting.
						g_hCurMap.transitionToHuntingTrack();
						break;
					case 2:	//Some NPCs hostile.
						g_hCurMap.transitionToHostileTrack();
						break;
				}
			}
			
			//Play some ambience randomly. Give the map a nice dynamic feeling where there is none.
			if ( g_hCurMap.m_hAmbientSounds.size() > 0 )
			{
				if ( g_iTicks > g_hCurMap.m_iNextOneShotAmbTime )
				{
					Utils.playSound( g_hCurMap.m_hAmbientSounds.get(
							(int)(Math.random() * g_hCurMap.m_hAmbientSounds.size()) ) );
					g_hCurMap.m_iNextOneShotAmbTime = g_iTicks + GameMap.ONESHOT_AMB_DELAY; //g_hCurMap.m_iNextOneShotAmbTime;
				}
			}
			
			try
			{
				Thread.sleep(1);	//Anomalies will happen if the program runs too fast.
			}
			catch (InterruptedException e){}
			
			//Update all entities.
			for ( int i = 0; i < g_hAllEntities.size(); i++ )
			{
				g_hAllEntities.get(i).think();
			}
		}
	}
	
	/**
	 * Sets the game's pause state and changes the cursor.
	 * No preconditions.
	 * Postconditions:
	 * 	- The cursor is changed.
	 * 	- If bPause differs from g_bGamePaused,
	 * 		then g_bGamePaused, g_eCurrentGUI,
	 * 		and the cursor are changed.
	 * @param bPause - Whether or not the game should be paused.
	 */
	public void pause( boolean bPause )
	{
		if ( bPause )
		{
			this.setCursor(null);
			g_eCurrentGUI = gui_t.PAUSE;
			g_bGamePaused = true;
		}
		else
		{
			//Hides the cursor by setting it to a transparent image, so that we can see the crosshair.
			this.setCursor( Toolkit.getDefaultToolkit().createCustomCursor(
					Toolkit.getDefaultToolkit().getImage("materials/clear.png"),
					new Point(0,0), "invis") );
			g_eCurrentGUI = gui_t.GAME;
			g_bGamePaused = false;
			this.setFocusable(true);
		}
	}
	
	/**
	 * Loads a map.
	 * If no mapname is provided, then assume that it's the end of the game,
	 * and fade out to the end title card. After that, close the game.
	 * Preconditions:
	 * 	- Path to maps/[mapname].tsmap is valid, and the file exists.
	 * Postconditions:
	 * 	- If mapname != null:
	 * 		g_hAllEntities, g_hWorldCollisionRects, and g_hNodes are emptied.
	 * 		g_bScroll is set to true.
	 * 		g_hCurMap is loaded with a new map.
	 * 		g_hThePlayer is created at position (0,0).
	 * 		A new Thread that parses g_hCurMap's main script is created and started.
	 * 	- Otherwise:
	 * 		g_eCurrentGUI is set to PICTURE.
	 * 		Custom cursor is removed.
	 * 		"The End" is displayed and faded in.
	 * 		This program is terminated.
	 * 		
	 * @param mapname - Name of the map to load.
	 */
	public void loadMap(String mapname)
	{
		if ( mapname != null )
		{
			g_hAllEntities.clear();
			g_hWorldCollisionRects.clear();
			g_hNodes.clear();
			g_iTicks = 0;
			g_bScroll = true;
			if ( g_hCurMap != null ) g_hCurMap.cleanupMap();
			g_hCurMap = new GameMap( mapname );
			
			g_hThePlayer = new Player( 0, 0, 100 );
			g_hAllEntities.add( g_hThePlayer );
			new Thread( new Runnable() {
				public void run()
				{
					g_hCurMap.parseInstructionQueue( g_hCurMap.script_Main, false );
				}
			} ).start();
		}
		else
		{
			this.setCursor(null);
			g_eCurrentGUI = gui_t.PICTURE;
			g_fFadeLevel = 1.0;
			try
			{
				g_hFrameToBeDrawn = ImageIO.read( new File( "materials/frames/TitleCard.png" ) );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
			while ( g_fFadeLevel > 0.0 )
			{
				g_fFadeLevel -= FADE_AMT;
				try
				{
					Thread.sleep(1);
				}
				catch ( InterruptedException e ) {}
			}
			g_bShouldKeepTicking = false;
			try
			{
				Thread.sleep(5000);
			}
			catch ( InterruptedException e ) {}
			System.exit(0);
		}
	}
	
	/**
	 * The in-game GUI that draws the world, the pause menu, or a still frame.
	 * @author Gabriel Valachi
	 * @author Anthony Maas
	 */
	public class GUIInGame extends JPanel implements ActionListener
	{
		public Image hPauseImage = Toolkit.getDefaultToolkit().getImage("materials/frames/TitleCardStart.png");
		private Image revolver;
		private Image shotgun;
		private Image rifle;
		
		JButton resumeGame = new JButton("Resume Game");
		JButton quitGame = new JButton("Quit");
		
		/**
		 * Constructor.
		 * Preconditions:
		 * 	- Images of a dropped revolver, shotgun, and rifle exist.
		 * Postconditions:
		 * 	- New GUIInGame instance created.
		 * 	- Pause menu buttons created and set to listen to this instance.
		 */
		public GUIInGame()
		{
			this.setDoubleBuffered(true);	//Seems to reduce flickering a bit.
			
			//ImageIO.read() will hold the thread until it finds the image.
			//That said, it should be done in the constructor, or else it could cause freezes.
			try
			{
				//These images are fast to access.
				revolver = ImageIO.read( new File("materials/gunsDropped/Revolver.png") );
				shotgun = ImageIO.read( new File("materials/gunsDropped/Shotgun.png") );
				rifle = ImageIO.read( new File("materials/gunsDropped/Rifle.png") );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
			
			//Add the buttons in the pause menu.
			this.add( quitGame );
			quitGame.setActionCommand("gtfo");
			quitGame.addActionListener(this);
			this.add( resumeGame );
			resumeGame.setActionCommand("resume");
			resumeGame.addActionListener(this);
		}
		
		/**
		 * Listens for button click events.
		 * Specifically, listens for the resume and quit events.
		 * Preconditions:
		 * 	- ev != null.
		 * Postconditions:
		 * 	- Game is resumed or exited, depending on the button clicked.
		 */
		@Override
		public void actionPerformed( ActionEvent ev )
		{
			String action = ev.getActionCommand();
			if ( action.equals("resume") )
			{
				Utils.ts().pause(false);
				resumeGame.setFocusable(false);
				quitGame.setFocusable(false);
			}
			else if ( action.equals("gtfo") )
			{
				System.exit(0);
			}
		}
		
		/**
		 * Draws the world, the pause menu GUI, or a still frame, depending on g_eCurrentGUI.
		 * Preconditions:
		 * 	- g is valid.
		 * 	- revolver, shotgun, and rifle images have been created.
		 * Postconditions:
		 * 	- The world, pause menu GUI, or still frame are drawn.
		 */
		//@SuppressWarnings("null")
		@Override
		public void paintComponent(Graphics g)
		{
			Graphics2D g2D = (Graphics2D)g;
			super.paintComponent(g);
			
			g2D.setColor( Color.black );
			if ( g_eCurrentGUI == gui_t.PAUSE )				//Pause menu
			{
				g2D.drawImage(hPauseImage, 0, 0, this);
				resumeGame.setVisible(true);
				resumeGame.setBounds( 320, 640, resumeGame.getPreferredSize().width, resumeGame.getPreferredSize().height );
				resumeGame.setFocusable(true);
				quitGame.setVisible(true);
				quitGame.setBounds( 960, 640, quitGame.getPreferredSize().width, quitGame.getPreferredSize().height );
				quitGame.setFocusable(true);
			}
			else if ( g_eCurrentGUI == gui_t.PICTURE )		//Individual frame
			{
				if ( g_hFrameToBeDrawn != null )
				{
					g2D.drawImage(g_hFrameToBeDrawn, 0, 0, this);
				}
			}
			else											//The game itself
			{
				resumeGame.setVisible(false);
				quitGame.setVisible(false);
				
				g2D.setColor(Color.white);
				g2D.fillRect(-1073741824, -1073741824, 2147483647, 2147483647);
				g2D.setColor(Color.black);
				
				int cx = Utils.ts().center_x;
				int cy = Utils.ts().center_y;
				
				g2D.drawImage( Utils.ts().g_hBGImage, cx - (int)(Utils.ts().g_hBGImage.getWidth(this)/2) + Utils.ts().rx,
						(int)(cy - Utils.ts().g_hBGImage.getHeight(this)/2) + Utils.ts().ry, this );
				
				ArrayList<BaseEntity> hEnts = Utils.ts().g_hAllEntities;
				float x = 0, y = 0;
				for ( int i = 0; i < hEnts.size(); i++ )
				{
					BaseEntity curEnt = hEnts.get(i);
					if ( curEnt == null )
						continue;
					if ( curEnt.shouldDraw() )
					{
						x = cx - (int)(curEnt.getWidth(this) / 2) + Utils.ts().rx + curEnt.getPosition()[0] + curEnt.getWidth(this)/2;
						y = cy - (int)(curEnt.getHeight(this) / 2) + Utils.ts().ry + curEnt.getPosition()[1] + curEnt.getHeight(this)/2;
						g2D.rotate( Math.toRadians(curEnt.getAngle() + 90), x + curEnt.getWidth(this) / 2, y + curEnt.getHeight(this) / 2 );
						g2D.drawImage( curEnt.getIcon(), (int)x, (int)y, this );
						//rotate() is not absolute, so it needs to be negated for the next entity drawn.
						//This does, however, cause flickering.
						g2D.rotate( -Math.toRadians(curEnt.getAngle() + 90), x + curEnt.getWidth(this) / 2, y + curEnt.getHeight(this) / 2 );
						
						if ( SHOW_COLLISION_RECTS )
						{
							Rectangle rect = curEnt.getCollisionRect();
							g2D.drawRect( (int)x, (int)y, rect.width, rect.height );
						}
					}
				}
				
				if ( SHOW_COLLISION_RECTS )
				{
					for ( int i = 0; i < g_hActiveInstance.g_hWorldCollisionRects.size(); i++ )
					{
						Rectangle rect = g_hActiveInstance.g_hWorldCollisionRects.get(i);
						g2D.drawRect( cx + g_hActiveInstance.rx + rect.x, cy + g_hActiveInstance.ry + rect.y,
								rect.width, rect.height );
					}
				}
				
				if ( SHOW_AI_NODES )
				{
					x = 0; y = 0;
					NodeDef curNode = null;
					final Image nodeImg = Toolkit.getDefaultToolkit().getImage("materials/projectile.png");
					final Image coverImg = Toolkit.getDefaultToolkit().getImage("materials/cover.png");
					for ( int i = 0; i < Utils.ts().g_hNodes.size(); i++ )
					{
						curNode = Utils.ts().g_hNodes.get(i);
						x = cx - (int)(nodeImg.getWidth(this) / 2) + Utils.ts().rx + curNode.m_fXPos;
						y = cy - (int)(nodeImg.getHeight(this) / 2) + Utils.ts().ry + curNode.m_fYPos;
						g2D.drawImage( (curNode.m_eType == node_t.PATH ? nodeImg : coverImg), (int)x, (int)y, this );
						g2D.drawString( "" + curNode.m_iNodeID, x, y + 32);
						for ( int j = 0; j < curNode.m_hLinkedNodes.size(); j++ )
						{
							g2D.drawLine((int)(x + nodeImg.getWidth(this)/2), (int)(y + nodeImg.getHeight(this)/2),
									(int)(x - curNode.m_fXPos + curNode.m_hLinkedNodes.get(j).m_fXPos + nodeImg.getWidth(this)/2),
									(int)(y - curNode.m_fYPos + curNode.m_hLinkedNodes.get(j).m_fYPos) + nodeImg.getHeight(this)/2 );
						}
					}
				}
				
				if ( g_hThePlayer != null && !Utils.ts().g_bCutscene )
				{
					g2D.setFont( g2D.getFont().deriveFont(72.0f) );
					
					//Health
					Image health = Toolkit.getDefaultToolkit().getImage("materials/hud/health.png");
					g2D.drawImage(health, 0, Utils.ts().getHeight() - health.getHeight(this) - 24, this);
					g2D.drawString("" + g_hThePlayer.getHealth(), health.getWidth(this),
							Utils.ts().getHeight() - health.getHeight(this) / 4 - 24 );
					
					//Weapon and ammunition
					if ( g_hThePlayer.m_hCurWeapon != null )
					{
						String wpn_ammo = null;
						Image wpn = null;
						if ( g_hThePlayer.m_hCurWeapon instanceof Revolver )
						{
							wpn = revolver;
							wpn_ammo = g_hThePlayer.m_hCurWeapon.getCurrentAmmo() + " / " + g_hThePlayer.getNumberOfRevolvers();
						}
						else if ( g_hThePlayer.m_hCurWeapon instanceof DBShotgun )
						{
							wpn = shotgun;
							wpn_ammo = g_hThePlayer.m_hCurWeapon.getCurrentAmmo() + " / " + g_hThePlayer.m_hCurWeapon.getExtraAmmo();
						}
						else if ( g_hThePlayer.m_hCurWeapon instanceof Rifle )
						{
							wpn = rifle;
							wpn_ammo = g_hThePlayer.m_hCurWeapon.getCurrentAmmo() + " / " + g_hThePlayer.m_hCurWeapon.getExtraAmmo();
						}
						
						g2D.drawImage( wpn.getScaledInstance(wpn.getWidth(this) * 4, wpn.getHeight(this) * 4, Image.SCALE_DEFAULT),
								Utils.ts().getWidth() - wpn.getWidth(this) * 5,
								Utils.ts().getHeight() - wpn.getHeight(this) * 9, this );
						g2D.drawString( wpn_ammo,
								Utils.ts().getWidth() - wpn.getWidth(this) * 12,
								(int)(Utils.ts().getHeight() - wpn.getHeight(this) * 6) );
					}
					
					//Crosshair
					Image u = null;
					Image d = null;
					Image l = null;
					Image r = null;
					Rectangle relationshipIndicator = new Rectangle(mouse_x, mouse_y, 64, 64);
					BaseCombatCharacter curNPC = null;
					boolean foundNPC = false;
					for ( int i = 0; i < Utils.ts().g_hAllEntities.size(); i++ )
					{
						if ( Utils.ts().g_hThePlayer == null )
							break;
						
						if ( Utils.ts().g_hAllEntities.get(i) instanceof BaseCombatCharacter )
							curNPC = (BaseCombatCharacter)Utils.ts().g_hAllEntities.get(i);
						else continue;
						
						if ( relationshipIndicator.intersects( curNPC.getCollisionRect() ) )
						{
							if ( Utils.ts().g_hThePlayer.getRelationshipToCharacter(curNPC) == relationship_t.ALLY )
							{
								u = Toolkit.getDefaultToolkit().getImage("materials/crosshair/u_a.png");
								d = Toolkit.getDefaultToolkit().getImage("materials/crosshair/d_a.png");
								l = Toolkit.getDefaultToolkit().getImage("materials/crosshair/l_a.png");
								r = Toolkit.getDefaultToolkit().getImage("materials/crosshair/r_a.png");
								foundNPC = true;
								break;
							}
							else if ( Utils.ts().g_hThePlayer.getRelationshipToCharacter(curNPC) == relationship_t.HOSTILE )
							{
								u = Toolkit.getDefaultToolkit().getImage("materials/crosshair/u_e.png");
								d = Toolkit.getDefaultToolkit().getImage("materials/crosshair/d_e.png");
								l = Toolkit.getDefaultToolkit().getImage("materials/crosshair/l_e.png");
								r = Toolkit.getDefaultToolkit().getImage("materials/crosshair/r_e.png");
								foundNPC = true;
								break;
							}
						}
					}
					if ( !foundNPC )
					{
						u = Toolkit.getDefaultToolkit().getImage("materials/crosshair/u.png");
						d = Toolkit.getDefaultToolkit().getImage("materials/crosshair/d.png");
						l = Toolkit.getDefaultToolkit().getImage("materials/crosshair/l.png");
						r = Toolkit.getDefaultToolkit().getImage("materials/crosshair/r.png");
					}
					int crsx_ud = cx - (int)(u.getWidth(this) / 2) + Utils.ts().rx + Utils.ts().mouse_x + (u.getHeight(this) / 4);
					int crsy_ud = cy - (int)(d.getHeight(this) / 2) + Utils.ts().ry + Utils.ts().mouse_y + (d.getHeight(this) / 4);
					int crsx_lr = cx - (int)(l.getWidth(this) / 2) + Utils.ts().rx + Utils.ts().mouse_x + (l.getWidth(this) / 4);
					int crsy_lr = cy - (int)(r.getWidth(this) / 2) + Utils.ts().ry + Utils.ts().mouse_y + (u.getHeight(this) / 2)
							+ (r.getWidth(this) / 4);
					int inaccuracy = 0;
					if ( g_hThePlayer.m_hCurWeapon != null )
					{
						inaccuracy = (int)(Utils.getDistance(mouse_x, mouse_y, g_hThePlayer.getPosition()[0], g_hThePlayer.getPosition()[1])
								* Math.tan(Math.toRadians(g_hThePlayer.m_hCurWeapon.getBaseAccuracy()))) / 2
								+ (int)(Utils.getDistance(mouse_x, mouse_y, g_hThePlayer.getPosition()[0], g_hThePlayer.getPosition()[1])
										* Math.tan(Math.toRadians(g_hThePlayer.m_hCurWeapon.getCurrentRecoil()))) / 2;
					}
					g2D.drawImage( u, crsx_ud, crsy_ud - inaccuracy, this );
					g2D.drawImage( d, crsx_ud, crsy_ud + inaccuracy, this );
					g2D.drawImage( l, crsx_lr - inaccuracy, crsy_lr, this );
					g2D.drawImage( r, crsx_lr + inaccuracy, crsy_lr, this );
				}
				else if ( g_bCutscene )
				{
					g2D.fillRect(0, 0, Utils.ts().getWidth(), 128);
					g2D.fillRect(0, Utils.ts().getHeight() - 128, Utils.ts().getWidth(), 128);
					if ( g_szCurSpokenLine != null )
					{
						g2D.setFont( g2D.getFont().deriveFont(24.0f) );
						g2D.setColor( Color.white );
						g2D.drawString( g_szCurSpokenLine, 24, Utils.ts().getHeight() - 64 );
					}
				}
			}
			if ( g_hThePlayer != null )
				g2D.drawString( mouse_x + " " + mouse_y, 0, 64 );	//DELETTHIS
			
			if ( g_fFadeLevel < 0.0f ) g_fFadeLevel = 0.0f;
			if ( g_fFadeLevel > 1.0f ) g_fFadeLevel = 1.0f;
			AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)g_fFadeLevel);
			g2D.setComposite(ac);
			g2D.setColor(Color.black);
			g2D.fillRect(0, 0, Utils.ts().getWidth(), Utils.ts().getHeight());
		}
	}
}
