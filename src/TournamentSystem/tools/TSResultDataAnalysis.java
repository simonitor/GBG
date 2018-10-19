package TournamentSystem.tools;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLOutput;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class TSResultDataAnalysis extends JFrame {
    private final String TAG = "[TSResultDataAnalysis] ";
    private TSAnalysisDataTransfer[] data;
    private int numAgents;

    private JPanel mJPanel;
    private JTable tableAgentRanking;
    private JLabel labelAgentRanking;
    private JLabel labelTopInfo;
    private JScrollPane jspTableAgentRanking;

    public TSResultDataAnalysis(TSAnalysisDataTransfer[] tsAD) {
        super("TS Analysis");
        $$$setupUI$$$();
        data = tsAD;

        numAgents = data[0].agentFilenames.length;
        for (TSAnalysisDataTransfer t : data) {
            if (t.agentFilenames.length != numAgents) {
                System.out.println(TAG + "ERROR: numAgents not consistent! Exiting");
                return;
            }
        }

        run();

        JScrollPane scroll = new JScrollPane(mJPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        //setContentPane(mJPanel);
        setContentPane(scroll);
        //setSize(1000,1000);
        revalidate();
        repaint();
        pack();
        setVisible(true);
    }

    private void run() {
        String info = "Num TSRs analysed: " + data.length;
        labelTopInfo.setText(info);

        doM2();
    }

    private void doM2() {
        // init help class array
        M2AgentScoreSort[] agentsM2 = new M2AgentScoreSort[numAgents];
        for (int i = 0; i < numAgents; i++) {
            agentsM2[i] = new M2AgentScoreSort(data[0].agentFilenames[i]);
        }
        for (Object row[] : data[0].tabelAgentScoreData) {
            String agent = (String) row[1];
            String filename = (String) row[2];
            for (M2AgentScoreSort m : agentsM2) {
                if (m.fileName.equals(filename)) {
                    m.name = agent;
                    break;
                }
            }
        }

        // read data
        for (TSAnalysisDataTransfer tsAD : data) {
            for (Object row[] : tsAD.tabelAgentScoreData) {
                String filename = (String) row[2];
                double glicko = Double.parseDouble(((String) row[9]).replace(',', '.')); // kommastelle von komma zu punkt
                //System.out.println(TAG + "FN: " + filename + " Gl: " + glicko);
                for (M2AgentScoreSort m : agentsM2) {
                    if (m.fileName.equals(filename)) {
                        m.glickos.add(glicko);
                        break;
                    }
                }
            }
        }

        printEXCELM2(agentsM2); // an dieser porition sind die agenten noch nach nicht nach glicko sortiert!

        // sort by glicko rating
        Arrays.sort(agentsM2, (entry1, entry2) -> { // same as above
            if (entry1.getAverageGlicko2() > entry2.getAverageGlicko2())
                return -1;
            if (entry1.getAverageGlicko2() < entry2.getAverageGlicko2())
                return +1;
            return 0;
        });

        // create table data
        NumberFormat numberFormat0 = new DecimalFormat("#0.0");
        NumberFormat numberFormat00 = new DecimalFormat("#0.00");
        String[] columnNames = {
                "Rank",
                "Agent",
                "Filename",/*
                "Games Won",
                "Games Tie",
                "Games Lost",
                "WTL Score",
                "FIDE Elo",
                "USCF Elo",
                "Glicko2",
                "WonGameRatio"*/
                "average Glicko2",
                "num Glickos"
        };
        Object[][] rowData = new Object[numAgents][columnNames.length];
        for (int i = 0; i < rowData.length; i++) {
            rowData[i][0] = i + 1;
            rowData[i][1] = agentsM2[i].name;
            rowData[i][2] = agentsM2[i].fileName;
            rowData[i][3] = numberFormat00.format(agentsM2[i].getAverageGlicko2());
            rowData[i][4] = agentsM2[i].glickos.size();
        }

        // put data into UI
        DefaultTableModel m1 = new DefaultTableModel(rowData, columnNames);
        tableAgentRanking.setModel(m1);
        //tableAgentRanking.setPreferredScrollableViewportSize(new Dimension(tableAgentRanking.getPreferredSize().width, tableAgentRanking.getRowHeight() * tableAgentRanking.getRowCount()));


    }

    private void printEXCELM2(M2AgentScoreSort[] agentsM2) {
        // excel console boxplot output
        System.out.println("########################\nEXCEL BOXPLOT DATA START");
        String CSV = ";";
        ArrayList<Double> werte = new ArrayList<>();
        String reihe = "M2-RSM2";

        switch (2) {
            case 1:
                System.out.print("Messreihe" + CSV);
                for (int i = 0; i < agentsM2[0].glickos.size(); i++)
                    System.out.print(reihe + CSV);
                System.out.println();
                for (M2AgentScoreSort m : agentsM2) {
                    System.out.print(m.name + CSV);
                    for (double d : m.glickos) {
                        System.out.print(("" + d).replace('.', ',') + CSV);
                        werte.add(d);
                    }
                    System.out.println();
                }
                break;

            case 2:
                System.out.print("Messreihe" + CSV);
                for (M2AgentScoreSort m : agentsM2) {
                    System.out.print(m.name + CSV);
                }
                System.out.println();
                int rows = agentsM2[0].glickos.size();
                for (int i = 0; i < rows; i++) {
                    System.out.print(reihe + CSV);
                    for (M2AgentScoreSort m : agentsM2) {
                        //System.out.print(m.glickos.get(i) + CSV);((String) row[9]).replace(',', '.')
                        System.out.print(("" + m.glickos.get(i)).replace('.', ',') + CSV);
                        werte.add(m.glickos.get(i));
                    }
                    System.out.println();
                }
                break;

            case 3:
                /*
                System.out.print("Messreihe" + CSV);
                for (M2AgentScoreSort m : agentsM2) {
                    System.out.print("M2" + CSV);
                }
                System.out.println();*/
                for (M2AgentScoreSort m : agentsM2) {
                    System.out.print(m.name + CSV);
                    for (double d : m.glickos) {
                        System.out.print(("" + d).replace('.', ',') + CSV);
                        werte.add(d);
                    }
                    System.out.println();
                }
                break;
        }

        /*
        // https://www.youtube.com/watch?v=0K-wSJNC8jo
        for (M2AgentScoreSort m : agentsM2) {
            for (double d : m.glickos) {
                System.out.println(("" + d).replace('.', ',') + CSV + m.name + CSV);
            }
        }
        */
        System.out.println("########################\nEXCEL BOXPLOT DATA END");
        System.out.println("MinValue: " + Collections.min(werte) + " MaxValue: " + Collections.max(werte));
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mJPanel = new JPanel();
        mJPanel.setLayout(new GridBagLayout());
        labelAgentRanking = new JLabel();
        labelAgentRanking.setText("M2 tableAgentRanking");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        mJPanel.add(labelAgentRanking, gbc);
        labelTopInfo = new JLabel();
        labelTopInfo.setText("Label");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        mJPanel.add(labelTopInfo, gbc);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(0, 0, 5, 0);
        mJPanel.add(spacer1, gbc);
        jspTableAgentRanking = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.BOTH;
        mJPanel.add(jspTableAgentRanking, gbc);
        tableAgentRanking = new JTable();
        tableAgentRanking.setAutoResizeMode(4);
        tableAgentRanking.setPreferredScrollableViewportSize(new Dimension(900, 400));
        jspTableAgentRanking.setViewportView(tableAgentRanking);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mJPanel.add(spacer2, gbc);
        final JPanel spacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mJPanel.add(spacer3, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mJPanel;
    }

    public class M2AgentScoreSort {
        public String name = "NaN";
        public final String fileName;
        public ArrayList<Double> glickos = new ArrayList<>();

        public M2AgentScoreSort(String fileName) {
            this.fileName = fileName;
        }

        public double getAverageGlicko2() {
            double av = 0;
            for (double d : glickos)
                av += d;
            return av / glickos.size();
        }
    }

}
