package games.KuhnPoker;

import games.Arena;
import tools.Types;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

public class GameBoardKuhnPokerGui extends JFrame {

    // GB
    private final GameBoardKuhnPoker m_gb;

    CardPanel[] holeCardsPanels;

    // Dummy
    StateObserverKuhnPoker test;

    // Log Panel
    private JScrollPane scrollPaneLog;
    private JTextArea log;

    // Action Panel
    private JButton checkButton;
    private JButton betButton;
    private JButton callButton;
    private JButton foldButton;
    private JButton continueButton;

    private JLabel currentPlayerChipsLabel;

    // Pot Panel
    JLabel pot;

    // info panel
    JLabel[] playerChips;
    JPanel[] playerNamePanel;
    JLabel[] playerCall;

    // Pause workaround
    boolean pause;

    // color scheme
    Color foldedColor = new Color(246, 81, 29);
    Color inactiveColor = new Color(128, 128, 128);
    Color lostColor = new Color(13, 44, 84);
    Color waitingColor = new Color(127, 184, 0);
    Color currentColor = new Color(0, 166, 237);
    Color openColor = new Color(255, 180, 0);
    Color allInColor = new Color(98, 143, 0);

    boolean check = true;

    GameBoardKuhnPokerGui(GameBoardKuhnPoker gb){
        super("Poker");

        m_gb = gb;

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // set Layout
        this.setLayout(new BorderLayout());

        // NORTH
        this.add(initPotPanel(),BorderLayout.NORTH);

        // EAST
        //this.add(getInfoPanel(),BorderLayout.WEST);

        // CENTER
        this.add(getCardPanel(),BorderLayout.CENTER);

        // EAST
        this.add(getLogPanel(),BorderLayout.EAST);

        // SOUTH
        this.add(initActionButtonsPanel(),BorderLayout.SOUTH);

        this.pack();
        this.setMinimumSize(this.getSize());
        this.setVisible(false);
    }

    private Panel initActionButtonsPanel(){

        checkButton     = new JButton("check");
        betButton       = new JButton("bet");
        callButton      = new JButton("call");
        foldButton      = new JButton("fold");
        continueButton  = new JButton("continue");

        checkButton.addActionListener( e -> {
            if (m_gb.m_Arena.taskState == Arena.Task.PLAY){
                m_gb.HGameMove(1);
            }
        });
        betButton.addActionListener(e -> {
            if (m_gb.m_Arena.taskState == Arena.Task.PLAY){
                m_gb.HGameMove(2);
            }
        });
        callButton.addActionListener(e -> {
            if (m_gb.m_Arena.taskState == Arena.Task.PLAY){
                m_gb.HGameMove(3);
            }
        });
        foldButton.addActionListener(e -> {
            if (m_gb.m_Arena.taskState == Arena.Task.PLAY){
                m_gb.HGameMove(0);
            }
        });

        continueButton.addActionListener(e -> {
            if (m_gb.m_Arena.taskState == Arena.Task.PLAY)
                continueWithTheGame();
        });

        currentPlayerChipsLabel = new JLabel("Chips: ");

        Panel actionPanel = new Panel(new FlowLayout());
        actionPanel.add(currentPlayerChipsLabel);
        actionPanel.add(checkButton);
        actionPanel.add(betButton  );
        actionPanel.add(callButton );
        actionPanel.add(foldButton );
        actionPanel.add(continueButton);
        return actionPanel;

    }

    private Panel getCardPanel(){
        Panel centerPanel = new Panel();
        //centerPanel.setLayout(new GridLayout(1,2));
        Panel centerPanelLeft = new Panel();
        centerPanelLeft.setLayout(new BoxLayout(centerPanelLeft,BoxLayout.PAGE_AXIS));
        Panel centerPanelRight = new Panel();
        centerPanelRight.setLayout(new BoxLayout(centerPanelRight,BoxLayout.PAGE_AXIS));

        holeCardsPanels = new CardPanel[2];

        holeCardsPanels[0] =  new CardPanel(150);
        JLabel playerLabel = new JLabel("Player 0");
        playerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        holeCardsPanels[1] =  new CardPanel(150);
        JLabel playerLabelTwo = new JLabel("Player 1");
        playerLabelTwo.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanelLeft.add(holeCardsPanels[0]);
        centerPanelLeft.add(playerLabel);

        centerPanelRight.add(holeCardsPanels[1]);
        centerPanelRight.add(playerLabelTwo);


        centerPanel.add(centerPanelLeft);
        centerPanel.add(centerPanelRight);

        return  centerPanel;
    }

    private Panel initPotPanel(){
        Panel potPanel = new Panel();
        pot = new JLabel("Pot: 0");
        potPanel.add(pot);
        return potPanel;
    }

    public void updatePotValue(double newValue){
        if(pot!=null)
            pot.setText("Pot: "+newValue);
    }

    private Panel getLogPanel(){
        Panel logPanel = new Panel();
        log = new JTextArea(20, 30);
        log.setEditable(false);
        scrollPaneLog = new JScrollPane(log,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        logPanel.add(scrollPaneLog);

        return logPanel;
    }

    private Panel getInfoPanel(){
        Panel infoPanel = new Panel();

        int count = StateObserverKuhnPoker.NUM_PLAYER;

        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));
        playerChips        = new JLabel[count];
        playerNamePanel    = new JPanel[count];
        playerCall         = new JLabel[count];
        //JPanel[] playerPanel = new JPanel[StateObserverPoker.NUM_PLAYER];

        for(int i = 0 ; i < count ; i++){
            // Define JPanel for name
            JPanel nameBox = new JPanel();
            nameBox.setLayout(new GridLayout(1,1));
            JLabel name = new JLabel(getPlayerName(i% StateObserverKuhnPoker.NUM_PLAYER),
                    SwingConstants.CENTER);
            nameBox.setAlignmentY(Component.CENTER_ALIGNMENT);
            nameBox.setBorder(BorderFactory.createLineBorder(Color.black));
            nameBox.add(name);
            nameBox.setMinimumSize(new Dimension(50, 50));
            nameBox.setPreferredSize(new Dimension(50, 50));
            nameBox.setMaximumSize(new Dimension(50, 50));

            playerNamePanel[i] = nameBox;

            playerCall[i] = new JLabel("To Call: 0");
            playerChips[i] = new JLabel("Chips: 0");

            playerCall[i].setBorder(new EmptyBorder(0, 5, 0, 0));
            playerChips[i].setBorder(new EmptyBorder(0, 5, 0, 0));

            JPanel chipsData = new JPanel();
            chipsData.setLayout(new GridLayout(2,1));
            chipsData.setBorder(BorderFactory.createLineBorder(Color.black));
            chipsData.setPreferredSize(new Dimension(150, 50));
            chipsData.setMaximumSize(new Dimension(150, 50));
            chipsData.add(playerCall[i]);
            chipsData.add(playerChips[i]);

            JPanel playerData = new JPanel();
            playerData.add(nameBox);
            playerData.add(chipsData);

            playerData.setLayout(new BoxLayout(playerData,BoxLayout.LINE_AXIS));
            playerData.setPreferredSize(new Dimension(150, 50));
            playerData.setMaximumSize(new Dimension(150, 50));
            //playerPanel[i] = playerData;
            infoPanel.add(playerData);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));
        JPanel legendTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel legendLabel = new JLabel("Legend:",SwingConstants.LEFT);
        legendTitlePanel.add(legendLabel);
        legendTitlePanel.setPreferredSize(new Dimension(150,25));
        legendTitlePanel.setMaximumSize(new Dimension(150,25));
        legendPanel.add(legendTitlePanel);
        legendPanel.add(new LegendPanel(currentColor,"Current"));
        legendPanel.add(new LegendPanel(waitingColor,"Done"));
        legendPanel.add(new LegendPanel(openColor,"Open"));
        legendPanel.add(new LegendPanel(foldedColor,"Folded"));
        legendPanel.add(new LegendPanel(allInColor,"All In"));
        legendPanel.add(new LegendPanel(lostColor,"Lost"));

        infoPanel.add(legendPanel);
        infoPanel.add(Box.createVerticalGlue());

        return infoPanel;
    }

    class LegendPanel extends  JPanel{
        LegendPanel(Color color, String label){
            this.setLayout(new FlowLayout(FlowLayout.LEFT));
            JPanel colorPanel = new JPanel();
            colorPanel.setBackground(color);
            JLabel labelPanel = new JLabel(label);
            this.add(colorPanel);
            this.add(labelPanel);
            this.setPreferredSize(new Dimension(150,25));
            this.setMaximumSize(new Dimension(150,25));
        }
    }

    private String getPlayerName(int i){
        return Types.GUI_PLAYER_NAME[i];
    }

    private void updateActions(StateObserverKuhnPoker SoP){
        if(SoP.isRoundOver()){

            pause = true;
            continueButton.setEnabled(true);
            foldButton.setEnabled(false);
            checkButton.setEnabled(false);
            betButton.setEnabled(false);
            callButton.setEnabled(false);

            holeCardsPanels[0].setCard(SoP.getHoleCards(0)[0].getImagePath());
            holeCardsPanels[1].setCard(SoP.getHoleCards(1)[0].getImagePath());

            //m_gb.m_Arena.roundOverWait = true;

            if(!m_gb.getWaitAtEndOfRound())
                continueWithTheGame();

            while(pause){
                try {
                    Thread.sleep(500);
                } catch (Exception e){

                }
            }

        }else{
            ArrayList<Types.ACTIONS> allActions = SoP.getAllAvailableActions();
            for (Types.ACTIONS action : allActions) {
                    switch (action.toInt()) {
                        // FOLD
                        case 0 -> foldButton.setEnabled(SoP.getAvailableActions().contains(action));
                        // CHECK
                        case 1 -> checkButton.setEnabled(SoP.getAvailableActions().contains(action));
                        // BET
                        case 2 -> betButton.setEnabled(SoP.getAvailableActions().contains(action));
                        // CALL
                        case 3 -> callButton.setEnabled(SoP.getAvailableActions().contains(action));
                    }
            }
            currentPlayerChipsLabel.setText("Chips: "+(int)SoP.getChips()[SoP.getPlayer()]);
            if(betButton.isEnabled())
                betButton.setText("bet ("+SoP.getBigblind()+")");
            if(callButton.isEnabled())
                callButton.setText("call ("+(int)SoP.getOpenPlayer(SoP.getPlayer())+")");
            continueButton.setEnabled(false);
        }

    }

    public void addToLog(String line){
        log.append(line+"\r\n");
    }

    public void updateBoard(StateObserverKuhnPoker soT,
                            boolean withReset, boolean showValueOnGameboard) {

        if(withReset) {
            System.out.println("with reset is called");
        }

        if(showValueOnGameboard)
            System.out.println("show value on gameboard");
        else
            System.out.println("don't show value on gameboard");

        updateLog(soT);
        updateCards(soT);
        updatePotValue(soT.getPotSize());
        //updatePlayerInfo(soT);
        updateActions(soT);

        //repaint();
    }

    public void resetLog(){
        if(log!=null){
            log.setText("");
        }
    }
    private void updateLog(StateObserverKuhnPoker sop){

        // update log
        if(sop.getLastActions()!=null)
            for(String entry:sop.getLastActions())
                addToLog(entry);
        sop.resetLog();

        // scoll to the end of the log
        scrollPaneLog.getVerticalScrollBar().setValue(
                scrollPaneLog.getVerticalScrollBar().getMaximum()
        );
    }

    private void updateCards(StateObserverKuhnPoker sop){
        holeCardsPanels[0].reset();
        holeCardsPanels[1].reset();
        holeCardsPanels[sop.getPlayer()].setCard(sop.getHoleCards()[0].getImagePath());
    }

    public void showGameBoard(Arena arena, boolean alignToMain) {
        this.setVisible(true);
        if (alignToMain) {
            // place window with game board below the main window
            int x = arena.m_xab.getX() + arena.m_xab.getWidth() + 20;
            int y = arena.m_xab.getLocation().y;
            if (arena.m_ArenaFrame!=null) {
                x = arena.m_ArenaFrame.getX();
                y = arena.m_ArenaFrame.getY() + arena.m_ArenaFrame.getHeight() +1;
                this.setSize(600,200);
            }
            this.setLocation(x,y);
        }
    }

    public void toFront() {
        super.setState(JFrame.NORMAL);	// if window is iconified, display it normally
        super.toFront();
    }

    public void destroy() {
        this.setVisible(false);
        this.dispose();
    }

    private void updatePlayerInfo(StateObserverKuhnPoker sop){
        double[] chips = sop.getChips();

        for(int i = 0; i < StateObserverKuhnPoker.NUM_PLAYER ; i++){
            playerChips[i].setText("Chips: -");
            playerCall[i].setText("To call: -");
        }
    }

    public void enableInteraction(boolean enable) {
        if(enable)
            System.out.println("enable interaction!");
    }

    private void continueWithTheGame(){
        pause = false;
        //m_gb.m_Arena.roundOverWait = false;
        m_gb.setActionReq(true);
        continueButton.setEnabled(false);
    }
}