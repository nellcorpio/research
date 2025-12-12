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
import java.util.Vector;
import java.util.stream.Collectors;

// ==========================================
// 1. THE MODEL (Enhanced JobOrder)
// Features: Auto-ID, Time Tracking, Logs
// ==========================================
class JobOrder {
    private static int counter = 1;
    
    private String ticketNumber; // JO-0001
    private String location;
    private String description;
    private String technician;
    private String priority;
    private String status;
    private StringBuilder historyLog; // For timeline/history
    
    // Time Tracking
    private LocalDateTime dateCreated;
    private LocalDateTime dateStarted;
    private LocalDateTime dateCompleted;

    // Formatting Tools
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
        addToHistory("Created Ticket at " + dateCreated.format(D_FMT));
    }

    // Constructor for LOADING from CSV
    public JobOrder(String ticket, String loc, String desc, String tech, String prio, String stat, String created, String started, String completed, String logs) {
        this.ticketNumber = ticket;
        this.location = loc;
        this.description = desc;
        this.technician = tech;
        this.priority = prio;
        this.status = stat;
        this.historyLog = new StringBuilder(logs.replace("~", "\n")); // Restore newlines
        
        // Parse dates safely
        this.dateCreated = parseDate(created);
        this.dateStarted = parseDate(started);
        this.dateCompleted = parseDate(completed);
        
        // Ensure counter keeps up with loaded IDs
        int num = Integer.parseInt(ticket.split("-")[1]);
        if (num >= counter) counter = num + 1;
    }

    private LocalDateTime parseDate(String d) {
        if(d == null || d.equals("null") || d.isEmpty()) return null;
        return LocalDateTime.parse(d, D_FMT);
    }

    public void addToHistory(String action) {
        historyLog.append("[").append(LocalDateTime.now().format(D_FMT)).append("] ").append(action).append("\n~");
    }

    // Business Logic
    public void assignTechnician(String tech) {
        if (!this.technician.equals(tech)) {
            addToHistory("Technician changed from " + this.technician + " to " + tech);
            this.technician = tech;
        }
    }

    public void updateStatus(String newStatus) {
        if (!this.status.equals(newStatus)) {
            addToHistory("Status changed: " + this.status + " -> " + newStatus);
            this.status = newStatus;

            if (newStatus.equals("IN_PROGRESS") && dateStarted == null) {
                dateStarted = LocalDateTime.now();
            }
            if (newStatus.equals("COMPLETED")) {
                dateCompleted = LocalDateTime.now();
                addToHistory("Job Completed. Duration: " + getDuration());
            }
        }
    }

    public String getDuration() {
        if (dateStarted == null || dateCompleted == null) return "N/A";
        long minutes = Duration.between(dateStarted, dateCompleted).toMinutes();
        long hours = minutes / 60;
        long mins = minutes % 60;
        return hours + "h " + mins + "m";
    }

    // Getters for Table
    public Object[] toRowData() {
        String createdStr = dateCreated.format(D_FMT);
        return new Object[]{ticketNumber, location, description, technician, priority, status, createdStr};
    }

    // Getters for CSV Saving
    public String toCSV() {
        // We use | as delimiter to avoid comma issues
        String startStr = (dateStarted != null) ? dateStarted.format(D_FMT) : "null";
        String endStr = (dateCompleted != null) ? dateCompleted.format(D_FMT) : "null";
        String cleanLog = historyLog.toString().replace("\n", ""); // Newlines handled by ~
        
        return String.join("|", ticketNumber, location, description, technician, priority, status, 
                           dateCreated.format(D_FMT), startStr, endStr, cleanLog);
    }

    // Getters for Slip Printing
    public String getPrintableSlip() {
        return "========================================\n" +
               "           JOB ORDER SLIP               \n" +
               "========================================\n" +
               "Ticket No:  " + ticketNumber + "\n" +
               "Date:       " + dateCreated.format(D_FMT) + "\n" +
               "Priority:   " + priority + "\n" +
               "----------------------------------------\n" +
               "Location:   " + location + "\n" +
               "Issue:      " + description + "\n" +
               "Technician: " + technician + "\n" +
               "Status:     " + status + "\n" +
               "Duration:   " + getDuration() + "\n" +
               "----------------------------------------\n" +
               "HISTORY LOGS:\n" + historyLog.toString().replace("~", "\n") +
               "\n========================================";
    }
    
    public String getTicketNumber() { return ticketNumber; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public String getHistory() { return historyLog.toString().replace("~", "\n"); }
}

// ==========================================
// 2. THE DATA MANAGER (Auto-Save to CSV)
// ==========================================
class DataManager {
    private static final String FILE_NAME = "database.txt";

    public static void save(java.util.List<JobOrder> jobs) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (JobOrder job : jobs) {
                writer.println(job.toCSV());
            }
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    public static java.util.List<JobOrder> load() {
        java.util.List<JobOrder> list = new ArrayList<>();
        File file = new File(FILE_NAME);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|"); // Split by pipe
                if (parts.length >= 10) {
                    list.add(new JobOrder(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7], parts[8], parts[9]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }
}

// ==========================================
// 3. THE UI RENDERER (Red Highlight Logic)
// ==========================================
class PriorityRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        // Get the Status and Priority column values safely
        String priority = (String) table.getModel().getValueAt(table.convertRowIndexToModel(row), 4); // Col 4 is Priority
        
        if ("HIGH".equals(priority)) {
            c.setBackground(new Color(255, 200, 200)); // Light Red
            c.setForeground(Color.RED);
        } else {
            c.setBackground(Color.WHITE);
            c.setForeground(Color.BLACK);
        }
        
        if (isSelected) {
            c.setBackground(new Color(184, 207, 229)); // Default selection blue
            c.setForeground(Color.BLACK);
        }
        return c;
    }
}

// ==========================================
// 4. THE MAIN APPLICATION
// ==========================================
public class AdvancedJobApp extends JFrame {

    private java.util.List<JobOrder> allJobs;
    private DefaultTableModel activeModel, archiveModel;
    private JTable activeTable, archiveTable;
    private JLabel lblStatus;

    // Inputs
    private JTextField tfLoc, tfDesc, tfSearch;
    private JComboBox<String> cbPrioCreate;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AdvancedJobApp::new);
    }

    public AdvancedJobApp() {
        super("Enterprise Job Order Tracking System v2.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);

        // Load Data
        allJobs = DataManager.load();

        // --- COMPONENTS SETUP ---
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Tab 1: Active Jobs
        JPanel activePanel = new JPanel(new BorderLayout());
        activePanel.add(createInputPanel(), BorderLayout.NORTH);
        activePanel.add(createActiveTablePanel(), BorderLayout.CENTER);
        activePanel.add(createActionPanel(), BorderLayout.SOUTH);
        
        // Tab 2: Archives (Completed)
        JPanel archivePanel = new JPanel(new BorderLayout());
        archivePanel.add(createArchiveTablePanel(), BorderLayout.CENTER);

        tabbedPane.addTab("Active Job Orders", activePanel);
        tabbedPane.addTab("Completed Archives", archivePanel);

        add(tabbedPane);
        
        // Initial Refresh
        refreshTables();

        // Add Window Listener to Auto-Save on Close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DataManager.save(allJobs);
            }
        });

        setVisible(true);
    }

    // --- PANEL BUILDERS ---

    private JPanel createInputPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        p.setBorder(BorderFactory.createTitledBorder("Create New Request"));
        
        tfLoc = new JTextField(10);
        tfDesc = new JTextField(25);
        String[] prios = {"LOW", "MEDIUM", "HIGH"};
        cbPrioCreate = new JComboBox<>(prios);
        JButton btnAdd = new JButton("Create Ticket");
        btnAdd.setBackground(new Color(70, 130, 180));
        btnAdd.setForeground(Color.WHITE);

        p.add(new JLabel("Location:")); p.add(tfLoc);
        p.add(new JLabel("Issue:")); p.add(tfDesc);
        p.add(new JLabel("Priority:")); p.add(cbPrioCreate);
        p.add(btnAdd);

        btnAdd.addActionListener(e -> createTicket());
        return p;
    }

    private JScrollPane createActiveTablePanel() {
        String[] cols = {"Ticket #", "Location", "Description", "Technician", "Priority", "Status", "Date Created"};
        activeModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        activeTable = new JTable(activeModel);
        activeTable.setRowHeight(25);
        
        // Apply Red Highlight Renderer
        activeTable.setDefaultRenderer(Object.class, new PriorityRenderer());

        return new JScrollPane(activeTable);
    }

    private JScrollPane createArchiveTablePanel() {
        String[] cols = {"Ticket #", "Location", "Description", "Technician", "Priority", "Status", "Date Created"};
        archiveModel = new DefaultTableModel(cols, 0);
        archiveTable = new JTable(archiveModel);
        return new JScrollPane(archiveTable);
    }

    private JPanel createActionPanel() {
        JPanel main = new JPanel(new BorderLayout());
        
        // Management Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBorder(BorderFactory.createTitledBorder("Manage Selected Ticket"));
        
        JButton btnAssign = new JButton("Assign Tech");
        JButton btnStatus = new JButton("Update Status");
        JButton btnPrint = new JButton("Print Slip");
        JButton btnHistory = new JButton("View History");

        controls.add(btnAssign);
        controls.add(btnStatus);
        controls.add(btnHistory);
        controls.add(btnPrint);

        // Search Controls
        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tfSearch = new JTextField(15);
        JButton btnSearch = new JButton("Search All");
        JButton btnReset = new JButton("Reset");
        
        search.add(new JLabel("Search (Loc/Tech/ID):"));
        search.add(tfSearch);
        search.add(btnSearch);
        search.add(btnReset);

        main.add(controls, BorderLayout.CENTER);
        main.add(search, BorderLayout.SOUTH);

        // --- LISTENERS ---
        btnAssign.addActionListener(e -> assignTechAction());
        btnStatus.addActionListener(e -> updateStatusAction());
        btnHistory.addActionListener(e -> viewHistoryAction());
        btnPrint.addActionListener(e -> printAction());
        
        btnSearch.addActionListener(e -> applyFilter(tfSearch.getText()));
        btnReset.addActionListener(e -> { tfSearch.setText(""); applyFilter(""); });

        return main;
    }

    // --- ACTIONS & LOGIC ---

    private void createTicket() {
        String loc = tfLoc.getText();
        String desc = tfDesc.getText();
        String prio = (String) cbPrioCreate.getSelectedItem();

        if (loc.isEmpty() || desc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!");
            return;
        }

        JobOrder job = new JobOrder(loc, desc, prio);
        allJobs.add(job);
        
        // Feature: Notification for High Priority
        if (prio.equals("HIGH")) {
            JOptionPane.showMessageDialog(this, "ALERT: High Priority Ticket " + job.getTicketNumber() + " Created!", "Priority Alert", JOptionPane.WARNING_MESSAGE);
        }

        refreshTables();
        tfLoc.setText(""); tfDesc.setText("");
        DataManager.save(allJobs); // Auto Save
    }

    private void assignTechAction() {
        JobOrder job = getSelectedJob();
        if (job == null) return;

        String tech = JOptionPane.showInputDialog(this, "Enter Technician Name:", job.toRowData()[3]);
        if (tech != null && !tech.trim().isEmpty()) {
            job.assignTechnician(tech);
            // Auto update status if still pending
            if(job.getStatus().equals("PENDING")) job.updateStatus("IN_PROGRESS");
            refreshTables();
            DataManager.save(allJobs);
        }
    }

    private void updateStatusAction() {
        JobOrder job = getSelectedJob();
        if (job == null) return;

        String[] options = {"PENDING", "IN_PROGRESS", "COMPLETED"};
        String current = (String) activeTable.getValueAt(activeTable.getSelectedRow(), 5);
        
        String newStatus = (String) JOptionPane.showInputDialog(this, "Select Status:", "Update", 
                JOptionPane.QUESTION_MESSAGE, null, options, current);

        if (newStatus != null) {
            job.updateStatus(newStatus);
            refreshTables();
            DataManager.save(allJobs);
        }
    }

    private void viewHistoryAction() {
        JobOrder job = getSelectedJob();
        if (job == null) return;
        
        JTextArea area = new JTextArea(15, 40);
        area.setText(job.getHistory());
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Timeline for " + job.getTicketNumber(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void printAction() {
        JobOrder job = getSelectedJob();
        if (job == null) return;

        JTextArea printArea = new JTextArea(job.getPrintableSlip());
        printArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        int choice = JOptionPane.showConfirmDialog(this, new JScrollPane(printArea), "Print Preview", JOptionPane.OK_CANCEL_OPTION);
        
        if (choice == JOptionPane.OK_OPTION) {
            try {
                boolean complete = printArea.print();
                if (complete) JOptionPane.showMessageDialog(this, "Printing Complete");
            } catch (PrinterException ex) {
                ex.printStackTrace();
            }
        }
    }

    // --- HELPERS ---

    private JobOrder getSelectedJob() {
        int row = activeTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a job from the table.");
            return null;
        }
        String ticketNum = (String) activeTable.getValueAt(row, 0);
        return allJobs.stream().filter(j -> j.getTicketNumber().equals(ticketNum)).findFirst().orElse(null);
    }

    private void refreshTables() {
        activeModel.setRowCount(0);
        archiveModel.setRowCount(0);

        for (JobOrder job : allJobs) {
            if (job.getStatus().equals("COMPLETED")) {
                archiveModel.addRow(job.toRowData());
            } else {
                activeModel.addRow(job.toRowData());
            }
        }
    }

    private void applyFilter(String query) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(activeModel);
        activeTable.setRowSorter(sorter);
        
        if (query.trim().length() == 0) {
            sorter.setRowFilter(null);
        } else {
            // Regex filter on multiple columns (Indices 0, 1, 3 -> Ticket, Loc, Tech)
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query, 0, 1, 3));
        }
    }
}
