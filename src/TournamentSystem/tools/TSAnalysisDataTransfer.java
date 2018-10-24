package TournamentSystem.tools;

public class TSAnalysisDataTransfer {
    public String headLine = "";

    public String[] tabelAgentScoreLabels = null;
    public Object[][] tabelAgentScoreData = null;
    public String[] agentFilenames = null;

    public double[][] dataHMAnalysis1 = null; // advanced analysis 1 - is Wab = Wba
    public double[][] dataHMAnalysis2 = null; // advanced analysis 2 - is Wab = 1-Wba
    public double[][] dataHMAnalysis3 = null; // advanced analysis 3 - are both previous test true
}
