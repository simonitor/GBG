package ludiiInterface.gateway;

import controllers.PlayAgent;
import game.Game;
import games.Othello.ArenaTrainOthello;
import ludiiInterface.othello.useCases.moves.LudiiMoves;
import ludiiInterface.othello.useCases.state.AsStateObserverOthello;
import ludiiInterface.othello.useCases.state.GbgStateFromLudiiContext;
import tools.Types;
import util.AI;
import util.Context;
import util.Move;

import static ludiiInterface.Util.errorDialog;
import static ludiiInterface.Util.loadFileFromDialog;

public final class GbgAsLudiiAgent extends AI {
    private PlayAgent gbgAgent;
    
    // /WK/ just during debugging:
//    private final String gbgAgentPath = "D:\\GitHub Repositories\\GBG\\agents\\Othello\\TCL3-fixed6_250k-lam05_P4_H001-diff2-FAm.agt.zip";
    private final String gbgAgentPath = "C:\\Users\\wolfgang\\Documents\\GitHub\\GBG\\agents\\Othello\\TCL3-fixed6_250k-lam05_P4_H001-diff2-FAm.agt.zip";

    public GbgAsLudiiAgent() {
        friendlyName = getClass().getSimpleName();
    }

    @Override
    public void initAI(final Game game, final int playerID)
    {
        try {
            gbgAgent = new ArenaTrainOthello(
                    "GBG vs. Ludii - Othello Arena",
                    false
//            ).tdAgentIO.loadGBGAgent(gbgAgentPath			// /WK/ just during debugging
            ).tdAgentIO.loadGBGAgent(
                    loadFileFromDialog("GBG Agenten auswählen")
            );
            friendlyName = "GBG "+ gbgAgent.getName();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Move selectAction(
        final Game game,
        final Context context,
        final double maxSeconds,
        final int maxIterations,
        final int maxDepth
    ) {
        if (game.moves(context).moves().isEmpty())
            return Game.createPassMove(context);

        try {
            return new LudiiMoves(context)
                .availableMoveBy(
                    gbgAction(context))
                .get();
        } catch (final RuntimeException e) {
            errorDialog(e);
            throw e;
        }
    }

    private Types.ACTIONS gbgAction(final Context ludiiContext) {
        return gbgAgent
            .getNextAction2(
                new AsStateObserverOthello(
                    new GbgStateFromLudiiContext(ludiiContext)
                ).partialState(),
                false,
                true
            );
    }

}
