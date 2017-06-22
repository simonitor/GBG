package games.ZweiTausendAchtundVierzig;

import controllers.MC.MCAgent;
import controllers.MCTS.MCTSAgentT;
import controllers.PlayAgent;
import games.Evaluator;
import games.GameBoard;
import params.MCParams;
import params.MCTSParams;
import tools.Types;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Arrays.deepEquals;

/**
 * Created by Johannes on 09.02.2016.
 */
public class Evaluator2048_BoardPositions extends Evaluator{
    private Random random = new Random();
    private ExecutorService executorService = Executors.newWorkStealingPool();



    public Evaluator2048_BoardPositions(PlayAgent e_PlayAgent, GameBoard gb, int stopEval, int mode, int verbose) {
        super(e_PlayAgent, stopEval, verbose);
        if (isAvailableMode(mode)==false) 
        	throw new RuntimeException("Evaluator2048: Value mode = "+mode+" for parameter mode not allowed." );
    }

    @Override
    protected boolean eval_Agent() {
        //find new realistic gameStates
        if(ConfigEvaluator.GENERATENEWGAMESTATES) {
            newGameStates();
        }

        //load saved gameStates
        List<StateObserver2048> gameStates = loadGameStates();
        System.out.println("Found " + gameStates.size() + " gameStates\n");

        //group gameStates by numEmptyTiles
        TreeMap<String, List<StateObserver2048>> gameStateGroups = groupGameStates(gameStates);

        //remove gameStates until numGameStates are left for each group
        for(List<StateObserver2048> gameStateGroup : gameStateGroups.values()) {
            while (gameStateGroup.size() > ConfigEvaluator.BOARDPOSITIONS) {
                gameStateGroup.remove(random.nextInt(gameStateGroup.size()-1));
            }
        }

        //Analyse the gameStateGroups
        System.out.println("Analysing gameStates, this may take a while...");
        List<Callable<ResultContainer>> callables = new ArrayList<>();
        for(List<StateObserver2048> gameStateGroup : gameStateGroups.values()) {
            callables.add(() -> {
                return analyseGameStateGroup(gameStateGroup);
            });
        }

        List<ResultContainer> resultContainers = new ArrayList<>();
        try {
            executorService.invokeAll(callables).stream().map(future -> {
                try {
                    return future.get();
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).forEach(resultContainers::add);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        String[][] results = new String[3][10];
        /*for(ResultContainer resultContainer : resultContainers) {
            results[resultContainer.numberAvailableActions-2][resultContainer.numberEmptyTiles] =
                    "mcCertainty: " + new BigDecimal(resultContainer.mcCertainty).setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",") +
                    "\nmctsCertainty: " + new BigDecimal(resultContainer.mctsCertainty).setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",") +
                    "\nsameActionCounter: " + new BigDecimal(resultContainer.sameActionCounter).setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",") +
                    "\nmcRolloutDepth: " + new BigDecimal(resultContainer.mcRolloutDepth).setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",") +
                    "\nmctsRolloutDepth: " + new BigDecimal(resultContainer.mctsRolloutDepth).setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",");
        }*/

        for(ResultContainer resultContainer : resultContainers) {
            results[resultContainer.numberAvailableActions-2][resultContainer.numberEmptyTiles-1] =
                    new BigDecimal(resultContainer.mcCertainty).setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",") +
                    "\n" + new BigDecimal(resultContainer.mctsCertainty).setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",") +
                    "\n" + new BigDecimal(resultContainer.sameActionCounter).setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",") +
                    "\n" + new BigDecimal(resultContainer.mcRolloutDepth).setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",") +
                    "\n" + new BigDecimal(resultContainer.mctsRolloutDepth).setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",");
        }

        System.out.println("\n\n\n\n\nResults:");
        for(int i = 0; i < 3; i++) {
            System.out.println("\nnumberAvailableActions: " + (i+2));
            for(int j = 0; j < 10; j++) {
                System.out.println("\nnumberEmptyTiles: " + (j+1));
                System.out.println(results[i][j]);
            }
        }

        return true;
    }

    private ResultContainer analyseGameStateGroup(List<StateObserver2048> gameStateGroup) {
        //create Agents
        MCTSParams mctsParams = new MCTSParams();
        mctsParams.setNumIter(ConfigEvaluator.ITERATIONS * ConfigEvaluator.NUMBERAGENTS * gameStateGroup.get(0).getNumAvailableActions()); //MC and MCTS now have the same Number of Iterations per Action
        mctsParams.setK_UCT(ConfigEvaluator.KUCT);
        mctsParams.setTreeDepth(ConfigEvaluator.TREEDEPTH);
        mctsParams.setRolloutDepth(ConfigEvaluator.ROLLOUTDEPTH);
        MCTSAgentT mctsAgent = new MCTSAgentT("MCTS", null, mctsParams);

        MCParams mcParams = new MCParams();
        mcParams.setRolloutdepth(ConfigEvaluator.ROLLOUTDEPTH - 1);
        mcParams.setIterations(ConfigEvaluator.ITERATIONS);
        mcParams.setNumberAgents(ConfigEvaluator.NUMBERAGENTS);
        MCAgent mcAgent = new MCAgent(mcParams);

        int maxCertainty = ConfigEvaluator.NC*gameStateGroup.size();
        double mcCertainty = 0;
        double mctsCertainty = 0;
        double sameActionCounter = 0;
        double mcRolloutDepth = 0;
        double mctsRolloutDepth = 0;

        for(StateObserver2048 gameState : gameStateGroup) {
            int[] mcActions = {0,0,0,0};
            int[] mctsActions = {0,0,0,0};
            int highestMCValue = 0;
            int bestMCAction = 0;
            int highestMCTSValue = 0;
            int bestMCTSAction = 0;

            //analyse for MCTS Agent
            if(ConfigEvaluator.EVALUATEMCTS) {
                for (int i = 0; i < ConfigEvaluator.NC; i++) {
                    int MCTSAction = mctsAgent.getNextAction(gameState, false, new double[gameState.getNumAvailableActions() + 1], true).toInt();
                    mctsActions[MCTSAction] += 1;
                }
            }

            //analyse for MC Agent
            if(ConfigEvaluator.EVALUATEMC) {
                for (int i = 0; i < ConfigEvaluator.NC; i++) {
                    int MCAction = mcAgent.getNextAction(gameState, false, new double[gameState.getNumAvailableActions() + 1], true).toInt();
                    mcActions[MCAction] += 1;
                    mcRolloutDepth += mcAgent.getAverageRolloutDepth();
                }
            }

            //find bestAction and the number of moves for this Action
            for(int i = 0; i < 4; i++) {
                if(highestMCValue < mcActions[i]) {
                    highestMCValue = mcActions[i];
                    bestMCAction = i;
                }
                if(highestMCTSValue < mctsActions[i]) {
                    highestMCTSValue = mctsActions[i];
                    bestMCTSAction = i;
                }
            }

            //add the number of moves for the best Action to Counter
            mcCertainty += highestMCValue;
            mctsCertainty += highestMCTSValue;
            if(bestMCAction == bestMCTSAction) {
                sameActionCounter += 1;
            }
        }

        //calculate Certainty in %
        mcCertainty=(mcCertainty/maxCertainty)*100;
        mctsCertainty=(mctsCertainty/maxCertainty)*100;
        sameActionCounter=(sameActionCounter/gameStateGroup.size())*100;
        mcRolloutDepth = mcRolloutDepth/gameStateGroup.size()/ ConfigEvaluator.NC;

        System.out.println("Analysed " + gameStateGroup.size() + " gameStates with " + gameStateGroup.get(0).getNumEmptyTiles() + " emptyTile(s) and " + gameStateGroup.get(0).getNumAvailableActions() + " availableAction(s)");

        return new ResultContainer(gameStateGroup.get(0).getNumAvailableActions(), gameStateGroup.get(0).getNumEmptyTiles(), mcCertainty, mctsCertainty, sameActionCounter, mcRolloutDepth, mctsRolloutDepth);
    }

    private TreeMap<String, List<StateObserver2048>> groupGameStates(List<StateObserver2048> gameStates) {
        TreeMap<String, List<StateObserver2048>> gameStateGroups = new TreeMap<>();

        for(StateObserver2048 gameState : gameStates) {
            String key = ""+gameState.getNumEmptyTiles()+", "+gameState.getNumAvailableActions();
            List<StateObserver2048> gameStateGroup = gameStateGroups.get(key);
            if(gameStateGroup == null) {
                gameStateGroup = new ArrayList<>();
            }
            gameStateGroup.add(gameState);
            gameStateGroups.put(key, gameStateGroup);
        }

        return gameStateGroups;
    }

    private void newGameStates() {
        System.out.println("Looking for gameStates, this may take a while...");

        List<StateObserver2048> gameStates = new ArrayList<>();

        //play i games
        List<Callable<List<StateObserver2048>>> callables = new ArrayList<>();
        for(int i = ConfigEvaluator.GAMESFORNEWGAMESTATES; i > 0; i--) {
            int gameNumber = i;
            callables.add(() -> {
                StateObserver2048 gameState = new StateObserver2048();
                PlayAgent playAgent = new MCAgent(new MCParams());
                List<StateObserver2048> tempGameStates = new ArrayList<>();
                while (!gameState.isGameOver()) {
                    tempGameStates.add(gameState.copy());
                    gameState.advance(playAgent.getNextAction(gameState, false, new double[gameState.getNumAvailableActions() + 1], true));
                }
                System.out.println("Finished with Game " + gameNumber);
                return tempGameStates;
            });
        }

        //merge all gameStates
        try {
            executorService.invokeAll(callables).stream().map(future -> {
                try {
                    return future.get();
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).forEach(gameStates::addAll);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /*remove inconclusive gameStates like
        0 | 0 | 0 | 0
        0 | 2 | 4 | 0
        0 | 2 | 0 | 0
        0 | 0 | 0 | 0
        because up and down are equally good*/
        System.out.println("Found " + gameStates.size() + " gameStates, removing inconclusiveGameStates");
        List<StateObserver2048> inconclusiveGameStates = new ArrayList<>();

        for(StateObserver2048 gameState : gameStates) {
            if(gameState.getNumAvailableActions() > 1 && gameState.getNumEmptyTiles() >= 1 && gameState.getNumEmptyTiles() <= 10) {
                //remove gameStates with mirrored Actions and uncommon gameStates (0 empty Tiles or > 10 empty Tiles)
                boolean conclusive = true;

                List<int[][]> gameStateArrays = new ArrayList<>();
                for(Types.ACTIONS action : gameState.getAvailableActions()) {
                    //copy and advance the gameState for each availableAction without spawning a new random Tile
                    StateObserver2048 newGameState = gameState.copy();
                    newGameState.move(action.toInt());
                    gameStateArrays.add(newGameState.toArray());
                }

                while(gameStateArrays.size() > 1 && conclusive) {
                    //get one gameState while there are at least 2 cameStates left
                    int[][] currentGameStateArray = gameStateArrays.get(0);
                    gameStateArrays.remove(currentGameStateArray);

                    //mirror and rotate the gameState to get 8 equal gameStates
                    List<int[][]> modifiedGameStateArrays = new ArrayList<>();
                    modifiedGameStateArrays.add(currentGameStateArray);
                    modifiedGameStateArrays.add(mirrorArray(currentGameStateArray));
                    for(int i = 0; i < 3; i++) {
                        modifiedGameStateArrays.add(rotateArray(modifiedGameStateArrays.get(i*2)));
                        modifiedGameStateArrays.add(rotateArray(modifiedGameStateArrays.get((i*2)+1)));
                    }

                    //compare the modifiedGameStates to gameStates for other Actions
                    for(int [][] gameStateArray : gameStateArrays) {
                        for (int[][] modifiedGameStateArray : modifiedGameStateArrays) {
                            if(deepEquals(gameStateArray, modifiedGameStateArray)) {
                                conclusive = false;
                            }
                        }
                    }

                    if(!conclusive) {
                        //we found a inconclusive gameState
                        inconclusiveGameStates.add(gameState);

                        gameState.printBoard();
                    }
                }

            }
            else {
                //remove gameStates with only 1 available Action
                inconclusiveGameStates.add((gameState));
            }
        }

        gameStates.removeAll(inconclusiveGameStates);
        System.out.println("Removed " + inconclusiveGameStates.size() + " inconclusiveGameStates");

        List<GameStateContainer> gameStateContainers = new ArrayList<>();
        for(StateObserver2048 gameState : gameStates) {
            gameStateContainers.add(new GameStateContainer(gameState));
        }

        //save all gameStates
        try {
            FileOutputStream fos = new FileOutputStream("games\\ZweiTausendAchtundVierzig\\gameStates.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(gameStateContainers);
            fos.close();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<StateObserver2048> loadGameStates() {
        System.out.println("Loading gameStates");

        List<StateObserver2048> gameStates = new ArrayList<>();

        //load gameStates
        try {
            FileInputStream fis = new FileInputStream("games\\ZweiTausendAchtundVierzig\\gameStates.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            List<GameStateContainer> gameStateContainers = (List<GameStateContainer>)ois.readObject();
            fis.close();
            ois.close();

            for(GameStateContainer gameStateContainer : gameStateContainers) {
                gameStates.add(new StateObserver2048(gameStateContainer.values, gameStateContainer.score, gameStateContainer.winState, gameStateContainer.isNextActionDeterministic));
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return gameStates;
    }

    private int[][] rotateArray(int[][] array) {
        int[][] rotatedArray = new int[ConfigGame.ROWS][ConfigGame.COLUMNS];
        for(int i = 0; i < ConfigGame.ROWS; i++) {
            for(int j = 0; j < ConfigGame.COLUMNS; j++) {
                rotatedArray[j][3-i] = array[i][j];
            }
        }
        return rotatedArray;
    }

    private int[][] mirrorArray(int[][] array) {
        int[][] mirroredArray = new int[ConfigGame.ROWS][ConfigGame.COLUMNS];
        for(int i = 0; i < ConfigGame.ROWS; i++) {
            for(int j = 0; j < ConfigGame.COLUMNS; j++) {
                mirroredArray[3-i][j] = array[i][j];
            }
        }
        return mirroredArray;
    }

    @Override
    public double getLastResult() {
        return 0;
    }

    @Override
    public String getMsg() {
        return "use this Spreedsheat to analyse output: https://docs.google.com/spreadsheets/d/1fAX-gwf4keZut4vuAZ2GQro5ubiLOeVvwhzn74zPTKs/edit?usp=sharing";
    }
    
 	/**
 	 * Since Evaluator2048 does not use mode, this function returns always true
 	 */
	@Override
 	public boolean isAvailableMode(int mode) {
		return true;
 	}

 	@Override
 	public int[] getAvailableModes() {
 		return null;
 	}

	@Override
	public int getQuickEvalMode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTrainEvalMode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMultiTrainEvalMode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getPrintString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPlotTitle() {
		// TODO Auto-generated method stub
		return null;
	}
 	
}

class GameStateContainer implements Serializable {
    public int[][] values;
    public int score;
    public int winState;
    public boolean isNextActionDeterministic;

    public GameStateContainer(StateObserver2048 gameState) {
        this.values = gameState.toArray();
        this.score = gameState.getScore();
        this.winState = gameState.getWinState();
        this.isNextActionDeterministic = gameState.isNextActionDeterminisitc();
    }
}

class ResultContainer {
    public int numberEmptyTiles;
    public int numberAvailableActions;
    public double mcCertainty;
    public double mctsCertainty;
    public double sameActionCounter;
    public double mcRolloutDepth;
    public double mctsRolloutDepth;

    public ResultContainer(int numberAvailableActions, int numberEmptyTiles, double mcCertainty, double mctsCertainty, double sameActionCounter, double mcRolloutDepth, double mctsRolloutDepth) {
        this.numberEmptyTiles = numberEmptyTiles;
        this.numberAvailableActions = numberAvailableActions;
        this.mcCertainty = mcCertainty;
        this.mctsCertainty = mctsCertainty;
        this.sameActionCounter = sameActionCounter;
        this.mcRolloutDepth = mcRolloutDepth;
        this.mctsRolloutDepth = mctsRolloutDepth;
    }
}