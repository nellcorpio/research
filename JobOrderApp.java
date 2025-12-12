/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package advancejobapp;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// ==========================================
// 1. THE MAIN RUNNER
// Entry point that launches the Login Screen
// ==========================================
public class EnterpriseSystem {
    public static void main(String[] args) {
        // Use Native Look and Feel for better UI
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } 
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(LoginScreen::new);
    }
}

// ==========================================
// 2. THE LOGIN SCREEN
// Handles Authentication
// ==========================================
class LoginScreen extends JFrame {
    
    public LoginScreen() {
        setTitle("System Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen
        setLayout(null);
        setResizable(false);

        // Header
        JLabel lblHeader = new JLabel("Maintenance MIS v2.0");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblHeader.setBounds(100, 20, 250, 30);
        add(lblHeader);

        // Username
        JLabel lblUser = new JLabel("Username:");
        lblUser.setBounds(50, 70, 80, 25);
        JTextField txtUser = new JTextField();
        txtUser.setBounds(130, 70, 180, 25);
        add(lblUser);
        add(txtUser);

        // Password
        JLabel lblPass = new JLabel("Password:");
        lblPass.setBounds(50, 110, 80, 25);
        JPasswordField txtPass = new JPasswordField();
        txtPass.setBounds(130, 110, 180, 25);
        add(lblPass);
        add(txtPass);

        // Login Button
        JButton btnLogin = new JButton("Login");
        btnLogin.setBounds(130, 155, 90, 30);
        add(btnLogin);

        // Exit Button
        JButton btnExit = new JButton("Exit");
        btnExit.setBounds(230, 155, 80, 30);
        add(btnExit);

        // --- Logic ---
        ActionListener loginAction = e -> {
            String user = txtUser.getText();
            String pass = new String(txtPass.getPassword());

            // HARDCODED CREDENTIALS
            if (user.equals("admin") && pass.equals("admin123")) {
                this.dispose(); // Close Login
                new Dashboard(); // Open Main System
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials!\nTry 'admin' and 'admin123'", "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        };

        btnLogin.addActionListener(loginAction);
        txtPass.addActionListener(loginAction); // Allow Enter key
        btnExit.addActionListener(e -> System.exit(0));

        setVisible(true);
    }
}

// ==========================================
// 3. THE MODEL (JobOrder Data Structure)
// Handles Logic, Time Tracking, and History
// ==========================================
class JobOrder {
    private static int counter = 1;
    
    private String ticketNumber;
    private String location;
    private String description;
    private String technician;
    private String priority;
    private String status;
    private StringBuilder historyLog;
    
    // Time Tracking
    private LocalDateTime dateCreated;
    private LocalDateTime dateStarted;
    private LocalDateTime dateCompleted;

    public static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Constructor for NEW jobs
    public JobOrder(String location, String description, String priority) {
        this.ticketNumber = String.format("JO-%04d", counter++);
        this.location = location;
        this.description = description;
        this.priority = priority;
        this.status = "PENDING";
        this.technician = "Unassigned";
        this.dateCreated = LocalDateTime.now();
        this.historyLog = new StringBuilder();
        addToHistory("Ticket Created");
    }

    // Constructor for LOADED jobs (from CSV)
    public JobOrder(String ticket, String loc, String desc, String tech, String prio, String stat, String created, String started, String completed, String logs) {
        this.ticketNumber = ticket;
        this.location = loc;
        this.description = desc;
        this.technician = tech;
        this.priority = prio;
        this.status = stat;
        this.historyLog = new StringBuilder(logs.replace("~", "\n"));
        
        this.dateCreated = parseDate(created);
        this.dateStarted = parseDate(started);
        this.dateCompleted = parseDate(completed);
        
        // Sync Counter
        try {
            int num = Integer.parseInt(ticket.split("-")[1]);
            if (num >= counter) counter = num + 1;
        } catch (Exception e) {}
    }

    private LocalDateTime parseDate(String d) {
        if(d == null || d.equals("null") || d.isEmpty()) return null;
        return LocalDateTime.parse(d, D_FMT);
    }

    public void addToHistory(String action) {
        historyLog.append("[").append(LocalDateTime.now().format(D_FMT)).append("] ").append(action).append("\n~");
    }

    public void assignTechnician(String tech) {
        if (!this.technician.equals(tech)) {
            addToHistory("Assigned to: " + tech);
            this.technician = tech;
        }
    }

    public void updateStatus(String newStatus) {
        if (!this.status.equals(newStatus)) {
            addToHistory("Status: " + this.status + " -> " + newStatus);
            this.status = newStatus;

            if (newStatus.equals("IN_PROGRESS") && dateStarted == null) dateStarted = LocalDateTime.now();
            if (newStatus.equals("COMPLETED")) {
                dateCompleted = LocalDateTime.now();
                addToHistory("Completed in " + getDuration());
            }
        }
    }

    public String getDuration() {
        if (dateStarted == null || dateCompleted == null) return "N/A";
        long minutes = Duration.between(dateStarted, dateCompleted).toMinutes();
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    // Export for CSV
    public String toCSV() {
        String startStr = (dateStarted != null) ? dateStarted.format(D_FMT) : "null";
        String endStr = (dateCompleted != null) ? dateCompleted.format(D_FMT) : "null";
        String cleanLog = historyLog.toString().replace("\n", ""); 
        return String.join("|", ticketNumber, location, description, technician, priority, status, dateCreated.format(D_FMT), startStr, endStr, cleanLog);
    }

    // Export for Printing
    public String getPrintableSlip() {
        return 
            "----------------------------------------\n" +
            "            JOB ORDER SLIP              \n" +
            "----------------------------------------\n" +
            "Ticket:  " + ticketNumber + "\n" +
            "Date:    " + dateCreated.format(D_FMT) + "\n" +
            "Prio:    " + priority + "\n" +
            "Loc:     " + location + "\n" +
            "Issue:   " + description + "\n" +
            "Tech:    " + technician + "\n" +
            "Status:  " + status + "\n" +
            "Time:    " + getDuration() + "\n" +
            "----------------------------------------\n" +
            "HISTORY LOGS:\n" + historyLog.toString().replace("~", "\n") +
            "\n----------------------------------------";
    }

    public Object[] toRowData() {
        return new Object[]{ticketNumber, location, description, technician, priority, status, dateCreated.format(D_FMT)};
    }
    
    public String getTicketNumber() { return ticketNumber; }
    public String getStatus() { return status; }
    public String getHistory() { return historyLog.toString().replace("~", "\n"); }
}

// ==========================================
// 4. THE DATA MANAGER (File I/O)
// ==========================================
class DataManager {
    private static final String FILE_NAME = "database.txt";

    public static void save(List<JobOrder> jobs) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (JobOrder job : jobs) writer.println(job.toCSV());
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static List<JobOrder> load() {
        List<JobOrder> list = new ArrayList<>();
        File file = new File(FILE_NAME);
        if (!file.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length >= 10) list.add(new JobOrder(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9]));
            }
        } catch (IOException e) { e.printStackTrace(); }
        return list;
    }
}

// ==========================================
// 5. THE MAIN DASHBOARD
// ==========================================
class Dashboard extends JFrame {

    private List<JobOrder> allJobs;
    private DefaultTableModel activeModel, archiveModel;
    private JTable activeTable;
    private JTextField tfSearch;
    // Removed class-level input fields because we use a pop-up now

    public Dashboard() {
        super("Enterprise Job Order System - Logged in as Admin");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);

        allJobs = DataManager.load();

        JTabbedPane tabbedPane = new JTabbedPane();
        
        // --- Tab 1: Active Jobs ---
        JPanel activePanel = new JPanel(new BorderLayout());
        activePanel.add(createInputPanel(), BorderLayout.NORTH); // This now creates the Pop-up Button
        activePanel.add(createActiveTablePanel(), BorderLayout.CENTER);
        activePanel.add(createActionPanel(), BorderLayout.SOUTH);
        
        // --- Tab 2: Archives ---
        JPanel archivePanel = new JPanel(new BorderLayout());
        archivePanel.add(createArchiveTablePanel(), BorderLayout.CENTER);

        tabbedPane.addTab("Active Job Orders", activePanel);
        tabbedPane.addTab("Completed Archives", archivePanel);

        add(tabbedPane);
        refreshTables();

        // Auto-save on close
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { DataManager.save(allJobs); }
        });

        setVisible(true);
    }

    // --- GUI Builders ---

    // === MODIFIED: This now creates a Button that launches a Pop-up ===
    private JPanel createInputPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        p.setBorder(BorderFactory.createTitledBorder("Ticket Management"));
        
        JButton btnCreate = new JButton("+ Create New Ticket");
        btnCreate.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCreate.setBackground(new Color(100, 200, 100));
        btnCreate.setForeground(Color.WHITE);
        
        p.add(btnCreate);

        btnCreate.addActionListener(e -> {
            // Create the Pop-up Components
            JTextField popupLoc = new JTextField();
            JTextField popupDesc = new JTextField();
            JComboBox<String> popupPrio = new JComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH"});

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("Location / Dept:"));
            panel.add(popupLoc);
            panel.add(new JLabel("Issue Description:"));
            panel.add(popupDesc);
            panel.add(new JLabel("Priority Level:"));
            panel.add(popupPrio);

            // Show the Pop-up
            int result = JOptionPane.showConfirmDialog(this, panel, "Create New Job Order",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            // Process the Input
            if (result == JOptionPane.OK_OPTION) {
                String loc = popupLoc.getText();
                String desc = popupDesc.getText();
                String prio = (String) popupPrio.getSelectedItem();

                if (loc.isEmpty() || desc.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill in all fields!", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JobOrder job = new JobOrder(loc, desc, prio);
                    allJobs.add(job);
                    if (prio.equals("HIGH")) {
                        JOptionPane.showMessageDialog(this, "High Priority Ticket Created!", "Alert", JOptionPane.WARNING_MESSAGE);
                    }
                    refreshTables();
                    DataManager.save(allJobs);
                }
            }
        });
        return p;
    }

    private JScrollPane createActiveTablePanel() {
        String[] cols = {"Ticket #", "Location", "Description", "Technician", "Priority", "Status", "Date Created"};
        activeModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        activeTable = new JTable(activeModel);
        activeTable.setRowHeight(25);
        
        // Red Highlight for HIGH priority
        activeTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, val, sel, foc, row, col);
                String p = (String) table.getModel().getValueAt(table.convertRowIndexToModel(row), 4);
                if ("HIGH".equals(p)) { c.setBackground(new Color(255, 200, 200)); c.setForeground(Color.RED); }
                else { c.setBackground(Color.WHITE); c.setForeground(Color.BLACK); }
                if (sel) { c.setBackground(new Color(184, 207, 229)); c.setForeground(Color.BLACK); }
                return c;
            }
        });
        return new JScrollPane(activeTable);
    }

    private JScrollPane createArchiveTablePanel() {
        String[] cols = {"Ticket #", "Location", "Description", "Technician", "Priority", "Status", "Date Created"};
        archiveModel = new DefaultTableModel(cols, 0);
        return new JScrollPane(new JTable(archiveModel));
    }

    private JPanel createActionPanel() {
        JPanel main = new JPanel(new BorderLayout());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBorder(BorderFactory.createTitledBorder("Actions"));
        
        JButton btnAssign = new JButton("Assign Tech");
        JButton btnStatus = new JButton("Update Status");
        JButton btnHistory = new JButton("View History");
        JButton btnPrint = new JButton("Print Slip");
        
        controls.add(btnAssign); controls.add(btnStatus); controls.add(btnHistory); controls.add(btnPrint);

        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tfSearch = new JTextField(12);
        JButton btnFilter = new JButton("Search");
        JButton btnReset = new JButton("Reset");
        JButton btnLogout = new JButton("Log Out");
        btnLogout.setBackground(new Color(255, 100, 100));
        btnLogout.setForeground(Color.WHITE);

        search.add(new JLabel("Find:")); search.add(tfSearch); search.add(btnFilter); search.add(btnReset); search.add(Box.createHorizontalStrut(20)); search.add(btnLogout);

        main.add(controls, BorderLayout.CENTER);
        main.add(search, BorderLayout.SOUTH);

        // --- Logic ---
        btnAssign.addActionListener(e -> {
            JobOrder j = getSel(); if(j==null) return;
            String t = JOptionPane.showInputDialog(this, "Tech Name:");
            if(t!=null && !t.isEmpty()) { j.assignTechnician(t); if(j.getStatus().equals("PENDING")) j.updateStatus("IN_PROGRESS"); refreshTables(); }
        });

        btnStatus.addActionListener(e -> {
            JobOrder j = getSel(); if(j==null) return;
            String s = (String) JOptionPane.showInputDialog(this, "Status:", "Update", 1, null, new String[]{"PENDING", "IN_PROGRESS", "COMPLETED"}, j.getStatus());
            if(s!=null) { j.updateStatus(s); refreshTables(); }
        });

        btnHistory.addActionListener(e -> {
            JobOrder j = getSel(); if(j==null) return;
            JOptionPane.showMessageDialog(this, new JScrollPane(new JTextArea(j.getHistory(), 10, 30)));
        });

        btnPrint.addActionListener(e -> {
            JobOrder j = getSel(); if(j==null) return;
            JTextArea area = new JTextArea(j.getPrintableSlip());
            if(JOptionPane.showConfirmDialog(this, new JScrollPane(area), "Print?", JOptionPane.OK_CANCEL_OPTION) == 0) {
                try { area.print(); } catch (PrinterException ex) {}
            }
        });

        btnFilter.addActionListener(e -> {
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(activeModel);
            activeTable.setRowSorter(sorter);
            if(tfSearch.getText().length()>0) sorter.setRowFilter(RowFilter.regexFilter("(?i)"+tfSearch.getText()));
        });
        
        btnReset.addActionListener(e -> { tfSearch.setText(""); activeTable.setRowSorter(null); });

        btnLogout.addActionListener(e -> {
            DataManager.save(allJobs);
            this.dispose();
            new LoginScreen(); // Return to Login
        });

        return main;
    }

    private JobOrder getSel() {
        int r = activeTable.getSelectedRow();
        if (r == -1) return null;
        String id = (String) activeTable.getValueAt(r, 0);
        return allJobs.stream().filter(j -> j.getTicketNumber().equals(id)).findFirst().orElse(null);
    }

    private void refreshTables() {
        activeModel.setRowCount(0);
        archiveModel.setRowCount(0);
        for (JobOrder j : allJobs) {
            if (j.getStatus().equals("COMPLETED")) archiveModel.addRow(j.toRowData());
            else activeModel.addRow(j.toRowData());
        }
    }
}
