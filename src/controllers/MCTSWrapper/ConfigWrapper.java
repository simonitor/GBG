package controllers.MCTSWrapper;

import controllers.MCTSWrapper.stateApproximation.PlayAgentApproximator;

public class ConfigWrapper {

    /**
     * A switch for {@link PlayAgentApproximator}:
     * <ul>
     *  <li> If false (recommended setting for RubiksCube), do not use softmax squashing for move probabilities.
     *  <li> If true (recommended setting for Othello), use softmax squashing.
     * </ul>
     */
    public static boolean USESOFTMAX=true;


    /**
     * A switch for {@link MCTSWrapperAgent}:
     * <ul>
     *  <li>  If false (recommended setting for ConnectFour), force tree re-build in every call.
     *  <li>  If true (recommended setting for Othello), re-use the tree (i.e. as in JS's master code).
     * </ul>
     */
    public static boolean USELASTMCTS = true;

    /**
     * EPS is a parameter for {@link MCTSNode#selectChild(double)}, it should be &lt;&lt; 1. It controls the behavior
     * if N(s)==0, where N(s) is the number of visits to {@code this} node:
     * <ul>
     *  <li> Case A)  EPS = +1e-8 (a small positive value): The Nair-case, like in <a href="https://web.stanford.edu/~surag/posts/alphazero.html">
     *           https://web.stanford.edu/~surag/posts/alphazero.html</a>:
     *           If N(s)==0, then select the action with highest getP(a).
     *  <li> Case B)  EPS = 0: The JS-case: If N(s)==0, select the 1st action from the available actions.
     *  <li> Case C)  EPS = -1e-8 (a small negative value): The random case: select a random action from the available actions.
     * </ul>
     * While Case A) is a more exploiting start, Cases B) and C) give the node a more exploring start.
     * Case A) should be from theory the best choice in general.
     * For N(s) &gt; 0, EPS is negligible, if condition  <code>|EPS| << 1</code> is fulfilled: Thus all cases are the
     * same, they use the PUCT formula for selection.
     * <p>
     * Recommendations:
     * <ul>
     *  <li> Use EPS = 1e-8 (Case A) for RubiksCube, much better results.
     *  <li> Use EPS = 0.0  (Case B) for Othello, better results.
     * </ul>
     */
    public static double EPS = 1e-8;  //1e-8; 0.0; -1e-8
}
