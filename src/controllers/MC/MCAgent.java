package controllers.MC;

import controllers.AgentBase;
import controllers.PlayAgent;
import games.StateObservation;
import tools.Types;
import java.util.List;
import java.util.Random;

/**
 * Created by Johannes on 08.12.2016.
 */
public class MCAgent extends AgentBase implements PlayAgent {
    private List<Types.ACTIONS> actions;
    private Types.ACTIONS nextAction = Types.ACTIONS.fromInt(5);
    private double nextMoveScore = 0;
    private Random rnd = new Random();

    public MCAgent(String name)
    {
        super(name);
        setAgentState(AgentState.TRAINED);
    }

    @Override
    public Types.ACTIONS getNextAction(StateObservation sob, boolean random, double[] vtable, boolean silent) {
        nextAction = null;
        nextMoveScore = Double.NEGATIVE_INFINITY;

        actions = sob.getAvailableActions();

        for(int i = 0; i < sob.getNumAvailableActions(); i++) {
            double averageScore = 0;
            for (int j = 0; j < Config.ITERATIONS; j++) {
                StateObservation newSob = sob.copy();

                newSob.advance(actions.get(i));
                RandomSearch agent = new RandomSearch();
                agent.startAgent(newSob);

                averageScore += newSob.getGameScore();
            }
            averageScore /= Config.ITERATIONS;
            vtable[i] = averageScore;

            if (nextMoveScore <= averageScore) {
                nextAction = actions.get(i);
                nextMoveScore = averageScore;
            }
        }

        return nextAction;
    }

    @Override
    public double getScore(StateObservation sob) {
        return nextMoveScore;
    }

    @Override
    public boolean wasRandomAction() {
        return false;
    }

    @Override
    public String stringDescr() {
        String cs = getClass().getName();
        return cs;
    }
}