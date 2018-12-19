package TournamentSystem.tools;

import TournamentSystem.jheatchart.HeatChart;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class TSResultDataAnalysis extends JFrame {
    private final String TAG = "[TSResultDataAnalysis] ";
    private TSAnalysisDataTransfer[] data;
    private int numAgents;
    private NumberFormat numberFormat0 = new DecimalFormat("#0.0");
    private NumberFormat numberFormat00 = new DecimalFormat("#0.00");
    private NumberFormat numberFormat000 = new DecimalFormat("#0.000");
    private NumberFormat numberFormat0000 = new DecimalFormat("#0.0000");
    private String datechain = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH.mm.ss"));

    private JPanel mJPanel;
    private JTable tableAgentRanking;
    private JLabel labelAgentRanking;
    private JLabel labelTopInfo;
    private JScrollPane jspTableAgentRanking;
    private JTable tableHM1;
    private JScrollPane jspTableHM1;
    private JTable tableHM2;
    private JScrollPane jspTableHM2;
    private JLabel hm1;
    private JLabel hm2;
    private JTable tableSDHM1;
    private JTable tableSDHM2;

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

        doM1();
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
                //double glicko = Double.parseDouble("" + row[6]); // WTL statt Glicko2
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
                "standard deviation",
                "num Glickos"
        };
        Object[][] rowData = new Object[numAgents][columnNames.length];
        for (int i = 0; i < rowData.length; i++) {
            rowData[i][0] = i + 1;
            rowData[i][1] = agentsM2[i].name;
            rowData[i][2] = agentsM2[i].fileName.replace("HDD ", "");
            rowData[i][3] = numberFormat00.format(agentsM2[i].getAverageGlicko2());
            rowData[i][4] = numberFormat00.format(agentsM2[i].getStandardDeviation());
            rowData[i][5] = agentsM2[i].glickos.size();
        }

        // put data into UI
        DefaultTableModel m1 = new DefaultTableModel(rowData, columnNames);
        tableAgentRanking.setModel(m1);
        tableAgentRanking.setPreferredScrollableViewportSize(new Dimension(tableAgentRanking.getPreferredSize().width * 1, tableAgentRanking.getRowHeight() * tableAgentRanking.getRowCount()));
    }

    private void doM1() {
        HMDataAnalysis[][] dataHMOrigDetail = new HMDataAnalysis[numAgents][numAgents];
        HMDataAnalysis[][] dataHMA1Detail = new HMDataAnalysis[numAgents][numAgents];
        HMDataAnalysis[][] dataHMA2Detail = new HMDataAnalysis[numAgents][numAgents];
        for (int i = 0; i < numAgents; i++) { // init data structure
            for (int j = 0; j < numAgents; j++) {
                if (i != j) {
                    dataHMOrigDetail[i][j] = new HMDataAnalysis();
                    dataHMA1Detail[i][j] = new HMDataAnalysis();
                    dataHMA2Detail[i][j] = new HMDataAnalysis();
                }
            }
        }

        // feed data
        for (TSAnalysisDataTransfer tsADT : data) {
            for (int i = 0; i < numAgents; i++) {
                for (int j = 0; j < numAgents; j++) {
                    if (i == j) {
                        dataHMA1Detail[i][j] = null;
                        dataHMA2Detail[i][j] = null;
                    } else {
                        if (tsADT.dataHMAnalysis1[i][j] == HeatChart.COLOR_ANALYSISPOS)
                            tsADT.dataHMAnalysis1[i][j] = 0;
                        if (tsADT.dataHMAnalysis2[i][j] == HeatChart.COLOR_ANALYSISPOS)
                            tsADT.dataHMAnalysis2[i][j] = 0;
                        dataHMA1Detail[i][j].add(tsADT.dataHMAnalysis1[i][j]);
                        dataHMA2Detail[i][j].add(tsADT.dataHMAnalysis2[i][j]);

                        dataHMOrigDetail[i][j].add(tsADT.dataHMWTL[i][j]);
                    }
                }
            }
        }

        String[] agents = new String[numAgents];
        for (int i = 0; i < numAgents; i++)
            agents[i] = "A#" + i;

        String[][] dataHMA1Out = new String[numAgents][numAgents];
        String[][] dataHMA2Out = new String[numAgents][numAgents];
        String[][] dataHMA1SDOut = new String[numAgents][numAgents];
        String[][] dataHMA2SDOut = new String[numAgents][numAgents];
        for (int i = 0; i < numAgents; i++) {
            for (int j = 0; j < numAgents; j++) {
                if (i != j) {
                    dataHMA1Out[i][j] = numberFormat00.format(dataHMA1Detail[i][j].getAverage());
                    dataHMA2Out[i][j] = numberFormat00.format(dataHMA2Detail[i][j].getAverage());

                    if (dataHMA1Detail[i][j].getStandardDeviation() == 0)
                        dataHMA1SDOut[i][j] = "" + 0;
                    else
                        dataHMA1SDOut[i][j] = numberFormat00.format(dataHMA1Detail[i][j].getStandardDeviation());
                    dataHMA1Out[i][j] += " (" + dataHMA1SDOut[i][j] + ")";

                    if (dataHMA2Detail[i][j].getStandardDeviation() == 0)
                        dataHMA2SDOut[i][j] = "" + 0;
                    else
                        dataHMA2SDOut[i][j] = numberFormat00.format(dataHMA2Detail[i][j].getStandardDeviation());
                    dataHMA2Out[i][j] += " (" + dataHMA2SDOut[i][j] + ")";
                } else {
                    dataHMA1Out[i][j] = "NaN";
                    dataHMA2Out[i][j] = "NaN";
                    dataHMA1SDOut[i][j] = "NaN";
                    dataHMA2SDOut[i][j] = "NaN";
                }
            }
        }

        double[][] dataHMOrig = new double[numAgents][numAgents]; // original WTL heatmap un-normalisiert
        double[][] dataHMOrig2 = new double[numAgents][numAgents];
        double[][] dataHMA1 = new double[numAgents][numAgents]; // advanced HM1
        double[][] dataHMA2 = new double[numAgents][numAgents]; // advanced HM2

        for (int i = 0; i < numAgents; i++) {
            for (int j = 0; j < numAgents; j++) {
                if (i != j) {
                    dataHMOrig[i][j] = dataHMOrigDetail[i][j].getAverage();

                    if (j > i)
                        dataHMOrig2[i][j] = dataHMA1Detail[i][j].getAverage(); // rechts oben
                    else
                        dataHMOrig2[i][j] = dataHMA2Detail[i][j].getAverage(); // links unten

                    dataHMA1[i][j] = dataHMA1Detail[i][j].getAverage();
                    dataHMA2[i][j] = dataHMA2Detail[i][j].getAverage();
                } else {
                    dataHMOrig[i][j] = HeatChart.COLOR_DIAGONALE;
                    dataHMOrig2[i][j] = HeatChart.COLOR_DIAGONALE;
                    dataHMA1[i][j] = HeatChart.COLOR_DIAGONALE;
                    dataHMA2[i][j] = HeatChart.COLOR_DIAGONALE;
                }
            }
        }

        // tabelle normalisierte werte + (SD)
        DefaultTableModel m1 = new DefaultTableModel(dataHMA1Out, agents);
        tableHM1.setModel(m1);
        tableHM1.setPreferredScrollableViewportSize(new Dimension(tableHM1.getPreferredSize().width * 1, tableHM1.getRowHeight() * tableHM1.getRowCount()));
        DefaultTableModel m2 = new DefaultTableModel(dataHMA2Out, agents);
        tableHM2.setModel(m2);

        // tabelle standardabweichung
        DefaultTableModel m3 = new DefaultTableModel(dataHMA1SDOut, agents);
        tableSDHM1.setModel(m3);
        tableSDHM1.setPreferredScrollableViewportSize(new Dimension(tableSDHM1.getPreferredSize().width * 1, tableSDHM1.getRowHeight() * tableSDHM1.getRowCount()));
        DefaultTableModel m4 = new DefaultTableModel(dataHMA2SDOut, agents);
        tableSDHM2.setModel(m4);

        HeatChart mapOrig1 = new HeatChart(dataHMOrig, 0, HeatChart.max(dataHMOrig), true);
        mapOrig1.setXValues(agents);
        mapOrig1.setYValues(agents);
        mapOrig1.setCellSize(new Dimension(25, 25));

        HeatChart mapOrig2 = new HeatChart(dataHMOrig2, 0, HeatChart.max(dataHMOrig2), true);
        mapOrig2.setXValues(agents);
        mapOrig2.setYValues(agents);
        mapOrig2.setCellSize(new Dimension(25, 25));

        HeatChart mapA1 = new HeatChart(dataHMA1, 0, 1, true);
        mapA1.setXValues(agents);
        mapA1.setYValues(agents);
        mapA1.setCellSize(new Dimension(25, 25));
        Image hmA1 = mapA1.getChartImage();
        ImageIcon scoreHeatmapA1 = new ImageIcon(hmA1);
        hm1.setText("");
        hm1.setIcon(scoreHeatmapA1);

        HeatChart mapA2 = new HeatChart(dataHMA2, 0, 1, true);
        mapA2.setXValues(agents);
        mapA2.setYValues(agents);
        mapA2.setCellSize(new Dimension(25, 25));
        Image hmA2 = mapA2.getChartImage();
        ImageIcon scoreHeatmapA2 = new ImageIcon(hmA2);
        hm2.setText("");
        hm2.setIcon(scoreHeatmapA2);

        // save HM as image file to desktop
        String filename1 = "HeatMapOrig1-" + datechain;
        String filename2 = "HeatMapOrig2-" + datechain;
        String filename3 = "HeatMapA1-" + datechain;
        String filename4 = "HeatMapA2-" + datechain;
        File file1 = new File("C:\\Users\\Felix\\Desktop\\" + filename1 + ".png");
        File file2 = new File("C:\\Users\\Felix\\Desktop\\" + filename2 + ".png");
        File file3 = new File("C:\\Users\\Felix\\Desktop\\" + filename3 + ".png");
        File file4 = new File("C:\\Users\\Felix\\Desktop\\" + filename4 + ".png");
        try {
            mapOrig1.saveToFile(file1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mapOrig2.saveToFile(file2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mapA1.saveToFile(file3);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mapA2.saveToFile(file4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printEXCELM2(M2AgentScoreSort[] agentsM2) {
        // excel console boxplot output
        System.out.println("########################\nEXCEL BOXPLOT DATA START");
        String CSV = ";";
        ArrayList<Double> werte = new ArrayList<>();
        String[] names = {
                "M2-RSM0", // 0
                "M2-RSM1", // 1
                "M2-RSM2", // 2
                "M3-100%", // 3
                "M3-75%",  // 4
                "M3-50%",  // 5
                "M3-25%",  // 6
                "M3-12%",  // 7
                "M3-6%"    // 8
        };
        String reihe = names[8];
        StringBuilder output = new StringBuilder();

        switch (2) {
            case 1:
                output.append("Messreihe").append(CSV);//System.out.print("Messreihe" + CSV);
                for (int i = 0; i < agentsM2[0].glickos.size(); i++)
                    output.append(reihe).append(CSV);//System.out.print(reihe + CSV);
                output.append("\n");//System.out.println();
                for (M2AgentScoreSort m : agentsM2) {
                    output.append(m.name).append(CSV);//System.out.print(m.name + CSV);
                    for (double d : m.glickos) {
                        output.append(("" + d).replace('.', ',')).append(CSV);//System.out.print(("" + d).replace('.', ',') + CSV);
                        werte.add(d);
                    }
                    output.append("\n");//System.out.println();
                }
                break;

            case 2:
                output.append("Messreihe").append(CSV);//System.out.print("Messreihe" + CSV);
                for (M2AgentScoreSort m : agentsM2) {
                    output.append(m.name).append(CSV);//System.out.print(m.name + CSV);
                }
                output.append("\n");//System.out.println();
                int rows = agentsM2[0].glickos.size();
                for (int i = 0; i < rows; i++) {
                    output.append(reihe).append(CSV);//System.out.print(reihe + CSV);
                    for (M2AgentScoreSort m : agentsM2) {
                        //System.out.print(m.glickos.get(i) + CSV);((String) row[9]).replace(',', '.')
                        output.append(("" + m.glickos.get(i)).replace('.', ',')).append(CSV);//System.out.print(("" + m.glickos.get(i)).replace('.', ',') + CSV);
                        werte.add(m.glickos.get(i));
                    }
                    output.append("\n");//System.out.println();
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
                    output.append(m.name).append(CSV);//System.out.print(m.name + CSV);
                    for (double d : m.glickos) {
                        output.append(("" + d).replace('.', ',')).append(CSV);//System.out.print(("" + d).replace('.', ',') + CSV);
                        werte.add(d);
                    }
                    output.append("\n");//System.out.println();
                }
                break;
        }
        System.out.println(output);

        String filename = "BoxPlot-CSV_Data-" + datechain;
        File file = new File("C:\\Users\\Felix\\Desktop\\" + filename + ".csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(output.toString());
        } catch (IOException e) {
            e.printStackTrace();
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
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        mJPanel.add(labelAgentRanking, gbc);
        labelTopInfo = new JLabel();
        labelTopInfo.setText("Label");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mJPanel.add(labelTopInfo, gbc);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(0, 0, 5, 0);
        mJPanel.add(spacer1, gbc);
        jspTableAgentRanking = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.BOTH;
        mJPanel.add(jspTableAgentRanking, gbc);
        tableAgentRanking = new JTable();
        jspTableAgentRanking.setViewportView(tableAgentRanking);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 0, 0);
        mJPanel.add(spacer2, gbc);
        final JPanel spacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 10);
        mJPanel.add(spacer3, gbc);
        final JPanel spacer4 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(0, 0, 5, 0);
        mJPanel.add(spacer4, gbc);
        final JLabel label1 = new JLabel();
        label1.setText("M1 HM Data Wab = Wba + (SD)");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        mJPanel.add(label1, gbc);
        jspTableHM1 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.BOTH;
        mJPanel.add(jspTableHM1, gbc);
        tableHM1 = new JTable();
        jspTableHM1.setViewportView(tableHM1);
        final JPanel spacer5 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 11;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(0, 0, 10, 0);
        mJPanel.add(spacer5, gbc);
        final JPanel spacer6 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(10, 0, 0, 0);
        mJPanel.add(spacer6, gbc);
        final JPanel spacer7 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(0, 0, 5, 0);
        mJPanel.add(spacer7, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("M1 HM Data Wab = 1-Wba + (SD)");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 9;
        gbc.anchor = GridBagConstraints.WEST;
        mJPanel.add(label2, gbc);
        jspTableHM2 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 10;
        gbc.fill = GridBagConstraints.BOTH;
        mJPanel.add(jspTableHM2, gbc);
        tableHM2 = new JTable();
        jspTableHM2.setViewportView(tableHM2);
        final JScrollPane scrollPane1 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 10;
        gbc.fill = GridBagConstraints.BOTH;
        mJPanel.add(scrollPane1, gbc);
        hm2 = new JLabel();
        hm2.setText("Label");
        scrollPane1.setViewportView(hm2);
        final JScrollPane scrollPane2 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.BOTH;
        mJPanel.add(scrollPane2, gbc);
        hm1 = new JLabel();
        hm1.setText("Label");
        scrollPane2.setViewportView(hm1);
        final JPanel spacer8 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 5);
        mJPanel.add(spacer8, gbc);
        final JLabel label3 = new JLabel();
        label3.setText("Standardabweichung");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        mJPanel.add(label3, gbc);
        final JPanel spacer9 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 5);
        mJPanel.add(spacer9, gbc);
        final JScrollPane scrollPane3 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.BOTH;
        mJPanel.add(scrollPane3, gbc);
        tableSDHM1 = new JTable();
        scrollPane3.setViewportView(tableSDHM1);
        final JScrollPane scrollPane4 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 10;
        gbc.fill = GridBagConstraints.BOTH;
        mJPanel.add(scrollPane4, gbc);
        tableSDHM2 = new JTable();
        scrollPane4.setViewportView(tableSDHM2);
        final JLabel label4 = new JLabel();
        label4.setText("HM: weiss gut ; schwarz boese");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        mJPanel.add(label4, gbc);
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

        public double getStandardDeviation() {
            StandardDeviation sd = new StandardDeviation();
            double[] tmp = new double[glickos.size()];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = glickos.get(i);
            }
            return sd.evaluate(tmp);
        }
    }

    public class HMDataAnalysis {
        private ArrayList<Double> data = new ArrayList<>();

        public void add(double element) {
            data.add(element);
        }

        public int size() {
            return data.size();
        }

        public double getAverage() {
            double out = 0;
            for (double d : data)
                out += d;
            return out / size();
        }

        public double getStandardDeviation() {
            StandardDeviation sd = new StandardDeviation();
            double[] tmp = new double[data.size()];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = data.get(i);
            }
            return sd.evaluate(tmp);
        }
    }

}
