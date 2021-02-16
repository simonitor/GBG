package games.CFour;

import java.util.ArrayList;
import java.util.Random;

import controllers.PlayAgent;
import games.GameBoard;
import games.StateObservation;
import games.Arena;
import tools.Types;

/**
 * This class implements the GameBoard interface for Connect Four.
 * Its member {@link GameBoardC4Gui} {@code m_gameFrame} has the game board GUI. 
 * {@code m_gameFrame} may be {@code null} in batch runs. 
 * <p>
 * It implements the interface functions and has the user interaction method HGameMove (used to enter 
 * legal moves during game play or to enter board positions during 'Inspect'), 
 * since HGameMove needs access to local  members. 
 * HGameMove is called from {@link C4GameGui}'s {@code handleMouseClick(int,int)}.
 * 
 * @author Wolfgang Konen, TH Koeln, May'18
 *
 */
public class GameBoardC4 implements GameBoard {

	protected Arena  m_Arena;		// a reference to the Arena object, needed to 
									// infer the current taskState
	protected StateObserverC4 m_so;
	protected Random rand;
	protected boolean arenaActReq=false;
	private GameBoardC4Gui m_gameGui = null;
	
	public GameBoardC4(Arena arGame) {
		initGameBoard(arGame);
	}
	
    private void initGameBoard(Arena arGame) 
	{
		m_Arena		= arGame;
		m_so		= new StateObserverC4();	// empty table
        rand 		= new Random(System.currentTimeMillis());	
        if (m_Arena.hasGUI() && m_gameGui==null) {
        	m_gameGui = new GameBoardC4Gui(this);
        }

	}

    @Override
    public void initialize() {}

    // --- obsolete? ---
//	private JPanel InitBoard()
//	{
//		JPanel panel=new JPanel();
//		//JButton b = new JButton();
//		panel.setLayout(new GridLayout(3,3,2,2));
////		int buSize = (int)(50*Types.GUI_SCALING_FACTORX);
////		Dimension minimumSize = new Dimension(buSize,buSize); //controls the button sizes
//		return panel;
//	}
	
	@Override
	public void clearBoard(boolean boardClear, boolean vClear) {
		if (boardClear) {
			m_so = new StateObserverC4();			// empty Table
		}
							// considerable speed-up during training (!)
        if (m_gameGui!=null && m_Arena.taskState!=Arena.Task.TRAIN)
			m_gameGui.clearBoard(boardClear, vClear, m_so);
	}

	/**
	 * Update the play board and the associated values (labels).
	 * 
	 * @param so	the game state
	 * @param withReset  if true, reset the board prior to updating it to state so
	 * @param showValueOnGameboard	if true, show the game values for the available actions
	 * 				(only if they are stored in in state {@code so}).
	 */
	@Override
	public void updateBoard(StateObservation so, 
							boolean withReset, boolean showValueOnGameboard) {
		StateObserverC4 soT = null;
		
		if (so!=null) {
	        assert (so instanceof StateObserverC4)
			: "StateObservation 'so' is not an instance of StateObserverC4";
			soT = (StateObserverC4) so;
			m_so = soT;//.copy();		// we do not need a copy here (!)
			// unclear why, but it leads to wrong behavior if we code
			//		m_so = (StateObserverC4) so;
			// (the GameBoard stays empty!)
			
		} // if(so!=null)
		
		if (m_gameGui!=null)
			m_gameGui.guiUpdateBoard(soT, m_Arena.taskState, withReset,showValueOnGameboard);
	}

	/**
	 * @return  true: if an action is requested from Arena or ArenaTrain
	 * 			false: no action requested from Arena, next action has to come 
	 * 			from GameBoard (e.g. user input / human move) 
	 */
	@Override
	public boolean isActionReq() {
		return arenaActReq;
	}

	/**
	 * @param	actReq true : GameBoard requests an action from Arena 
	 * 			(see {@link #isActionReq()})
	 */
	@Override
	public void setActionReq(boolean actReq) {
		arenaActReq=actReq;
	}

	protected void HGameMove(int x, int y)
	{
		Types.ACTIONS act = Types.ACTIONS.fromInt(x);
//		assert m_so.isLegalAction(act) : "Desired action is not legal";
		if (m_so.isLegalAction(act)) {
			m_so.advance(act);			// perform action (optionally add random elements from game 
										// environment - not necessary in ConnectFour)
			System.out.println(m_so.stringDescr());
			(m_Arena.getLogManager()).addLogEntry(act, m_so, m_Arena.getLogSessionID());
//			updateBoard(null,false,false);
			arenaActReq = true;			// ask Arena for next action
		}
	}
	
	// --- obsolete? (during Inspect we call just HGameMove) ---
//	private void InspectMove(int x, int y)
//	{
//		int iAction = 3*x+y;
//		Types.ACTIONS act = Types.ACTIONS.fromInt(iAction);
//		if (!m_so.isLegalAction(act)) {
//			System.out.println("Desired action is not legal!");
//			m_Arena.setStatusMessage("Desired action is not legal");
//			return;
//		} else {
//			m_Arena.setStatusMessage("Inspecting the value function ...");
//		}
//		m_so.advance(act);			// perform action (optionally add random elements from game 
//									// environment - not necessary in TicTacToe)
////		updateBoard(null,false,false);
//		arenaActReq = true;		
//	}
	
	public StateObservation getStateObs() {
		return m_so;
	}

	/**
	 * @return the 'empty-board' start state
	 */
	@Override
	public StateObservation getDefaultStartState() {
		clearBoard(true, true);
		return m_so;
	}

	/**
	 * @return a start state which is with probability 0.5 the empty board 
	 * 		start state and with probability 0.5 one of the possible one-ply 
	 * 		successors
	 */
	@Override
	public StateObservation chooseStartState() {
		clearBoard(true, true);			// m_so is in default start state 
		if (rand.nextDouble()>0.5) {
			// choose randomly one of the possible actions in default 
			// start state and advance m_so by one ply
			ArrayList<Types.ACTIONS> acts = m_so.getAvailableActions();
			int i = rand.nextInt(acts.size());
			m_so.advance(acts.get(i));
		}
		return m_so;
	}

	@Override
    public StateObservation chooseStartState(PlayAgent pa) {
    	return chooseStartState();
    }
    
	@Override
	public String getSubDir() {
		return null;
	}
	
    @Override
    public Arena getArena() {
        return m_Arena;
    }
    
	@Override
	public void enableInteraction(boolean enable) {
		if (m_gameGui!=null)
			m_gameGui.enableInteraction(enable);
	}

	@Override
	public void showGameBoard(Arena ticGame, boolean alignToMain) {
		if (m_gameGui!=null)
			m_gameGui.showGameBoard(ticGame, alignToMain);
	}

   @Override
	public void toFront() {
		if (m_gameGui!=null)
			m_gameGui.toFront();
	}

   @Override
   public void destroy() {
		if (m_gameGui!=null)
			m_gameGui.destroy();
   }

}
