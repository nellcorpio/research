package advancejobapp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.Color;

// ==========================================
// 1. MAIN RUNNER
// ==========================================
public class EnterpriseSystem {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } 
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(Dashboard::new);
    }
}

// ==========================================
// 2. MODEL (JobOrder)
// ==========================================
class JobOrder {
    private static int counter = 1;
    private String ticketNumber, location, description, technician, priority, status, remarks;
    private double serviceFee;
    private LocalDateTime dateScheduled, dateCompleted;

    // Formatters
    public static final DateTimeFormatter DB_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter VIEW_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // Constructor for New Job
    public JobOrder(String location, String description, String priority, LocalDateTime scheduledDate) {
        this.ticketNumber = String.format("JO-%04d", counter++);
        this.location = location;
        this.description = description;
        this.priority = priority;
        this.dateScheduled = scheduledDate;
        this.technician = "Unassigned";
        this.status = "PENDING";
        this.serviceFee = 0.0;
        this.remarks = "";
    }

    // Constructor for Loading from File
    public JobOrder(String ticket, String loc, String desc, String tech, String prio, String stat, 
                    String sched, String completed, double fee, String rem) {
        this.ticketNumber = ticket; this.location = loc; this.description = desc;
        this.technician = tech; this.priority = prio; this.status = stat;
        this.dateScheduled = parseDate(sched); this.dateCompleted = parseDate(completed);
        this.serviceFee = fee; this.remarks = rem;
        
        // Fix ID counter so new jobs don't duplicate IDs
        try { int num = Integer.parseInt(ticket.split("-")[1]); if (num >= counter) counter = num + 1; } catch (Exception e) {}
    }

    private LocalDateTime parseDate(String d) { 
        return (d == null || d.equals("null") || d.isEmpty()) ? null : LocalDateTime.parse(d, DB_FMT); 
    }

    public void assignTechnician(String tech) { this.technician = tech; }
    
    public void updateStatus(String newStatus) { 
        this.status = newStatus;
        if(newStatus.equals("COMPLETED")) this.dateCompleted = LocalDateTime.now();
    }
    
    public void setCompletionDetails(double fee, String remarks) {
        this.serviceFee = fee;
        this.remarks = remarks;
        this.status = "COMPLETED";
        this.dateCompleted = LocalDateTime.now();
    }

    // CSV for Saving
    public String toCSV() {
        return String.join("|", ticketNumber, location, description, technician, priority, status, 
            (dateScheduled!=null?dateScheduled.format(DB_FMT):"null"), 
            (dateCompleted!=null?dateCompleted.format(DB_FMT):"null"), 
            String.valueOf(serviceFee), remarks);
    }

    // Table Row Data
    public Object[] toRowData() { 
        return new Object[]{ticketNumber, location, description, technician, priority, status, dateScheduled.format(VIEW_DATE)}; 
    }

    // Getters
    public String getTicketNumber() { return ticketNumber; }
    public String getStatus() { return status; }
    public String getTechnician() { return technician; }
    public String getLocation() { return location; }
    public LocalDateTime getDateScheduled() { return dateScheduled; }
    public double getServiceFee() { return serviceFee; }
}

// ==========================================
// 3. DATA MANAGER
// ==========================================
class DataManager {
    // CHANGED FILENAME to prevent crash from old format
    private static final String JOB_FILE = "jobs_research.txt"; 

    public static void saveJobs(List<JobOrder> jobs) {
        try (PrintWriter w = new PrintWriter(new FileWriter(JOB_FILE))) { 
            for (JobOrder j : jobs) w.println(j.toCSV()); 
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static List<JobOrder> loadJobs() {
        List<JobOrder> l = new ArrayList<>();
        File f = new File(JOB_FILE);
        if(!f.exists()) return l;
        
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line; 
            while ((line = r.readLine()) != null) {
                try {
                    String[] p = line.split("\\|");
                    if (p.length >= 8) { 
                        String rem = (p.length > 9) ? p[9] : "";
                        // Safe parsing
                        double fee = 0.0;
                        try { fee = Double.parseDouble(p[8]); } catch(Exception ignored) {} 
                        
                        l.add(new JobOrder(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], fee, rem));
                    }
                } catch (Exception e) {
                    System.out.println("Skipping corrupted line: " + line);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return l;
    }
}

// ==========================================
// 4. DASHBOARD (The Main System)
// ==========================================
class Dashboard extends JFrame {

    private List<JobOrder> allJobs;
    private final String[] technicians = {"Reahmeil", "Mark De Asis", "Wneljae Giangan", "E Reck Juns"};
    
    private DefaultTableModel jobModel;
    private JTable jobTable;
    private JTextField tfSearch;
    private JTextArea financialReportArea;
    private JPanel calendarGrid;
    private JLabel lblCalendarMonth;

    public Dashboard() {
        super("Enterprise Job Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        allJobs = DataManager.loadJobs();

        JTabbedPane tabbedPane = new JTabbedPane();
        
        // TAB 1: Job Operations
        JPanel jobPanel = new JPanel(new BorderLayout());
        jobPanel.add(createTopActionPanel(), BorderLayout.NORTH);
        jobPanel.add(createJobTablePanel(), BorderLayout.CENTER);
        
        // TAB 2: Calendar
        JPanel calendarPanel = createCalendarPanel();
        
        // TAB 3: Reports
        JPanel financePanel = createFinancialPanel();
        
        tabbedPane.addTab("Job Operations", jobPanel);
        tabbedPane.addTab("Daily Schedule", calendarPanel);
        tabbedPane.addTab("Financial Reports", financePanel);

        add(tabbedPane);
        refreshTable(); 

        // Auto-save on close
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { DataManager.saveJobs(allJobs); }
        });

        // Update when switching tabs
        tabbedPane.addChangeListener(e -> { 
            updateCalendar(); 
            updateFinancialReport(); 
            refreshTable();
        });

        setVisible(true);
    }

    // ---- END DE ASIS ----
    
    // --- GUI BUILDERS ---

    private JPanel createTopActionPanel() {
        JPanel container = new JPanel(new GridLayout(2, 1));
        
        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton btnCreate = new JButton("+ New Job Order");
        btnCreate.setBackground(new Color(60, 179, 113)); btnCreate.setForeground(new Color(33, 52, 72));
        
        JButton btnAssign = new JButton("Assign Tech");
        JButton btnStatus = new JButton("Update Status");
        JButton btnComplete = new JButton("Complete & Remarks");
        btnComplete.setBackground(new Color(100, 149, 237)); btnComplete.setForeground(new Color(33, 52, 72));
        
        actions.add(btnCreate);
        actions.add(new JSeparator(SwingConstants.VERTICAL));
        actions.add(btnAssign);
        actions.add(btnStatus);
        actions.add(btnComplete);

        // Filter
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        tfSearch = new JTextField(20);
        JButton btnSearch = new JButton("Filter Location/Dept");
        JButton btnReset = new JButton("Show All Pending");
        
        filter.add(new JLabel("Search Department/Location:"));
        filter.add(tfSearch);
        filter.add(btnSearch);
        filter.add(btnReset);

        container.add(actions);
        container.add(filter);

        // --- BUTTON LOGIC ---

        btnCreate.addActionListener(e -> {
            JTextField txtLoc = new JTextField();
            JTextField txtDesc = new JTextField();
            JComboBox<String> cbPrio = new JComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH"});
            JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
            dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "MMM dd, yyyy HH:mm"));

            JPanel p = new JPanel(new GridLayout(0, 1));
            p.add(new JLabel("Location/Room:")); p.add(txtLoc);
            p.add(new JLabel("Issue Description:")); p.add(txtDesc);
            p.add(new JLabel("Priority:")); p.add(cbPrio);
            p.add(new JLabel("Schedule Date:")); p.add(dateSpinner);

            if (JOptionPane.showConfirmDialog(this, p, "New Job Order", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                if(!txtLoc.getText().isEmpty()) {
                    LocalDateTime dt = ((Date)dateSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    allJobs.add(new JobOrder(txtLoc.getText(), txtDesc.getText(), (String)cbPrio.getSelectedItem(), dt));
                    refreshTable();
                    JOptionPane.showMessageDialog(this, "Job Created Successfully!");
                }
            }
        });

        btnAssign.addActionListener(e -> {
            JobOrder j = getSelectedJob();
            if(j != null) {
                String sel = (String) JOptionPane.showInputDialog(this, "Select Technician:", "Assign", 3, null, technicians, technicians[0]);
                if(sel != null) { j.assignTechnician(sel); refreshTable(); }
            }
        });

        btnStatus.addActionListener(e -> {
            JobOrder j = getSelectedJob();
            if(j != null && !j.getStatus().equals("COMPLETED")) {
                String[] states = {"PENDING", "IN_PROGRESS"};
                String s = (String) JOptionPane.showInputDialog(this, "Set Status:", "Update", 3, null, states, j.getStatus());
                if(s != null) { j.updateStatus(s); refreshTable(); }
            }
        });

        btnComplete.addActionListener(e -> {
            JobOrder j = getSelectedJob();
            if(j != null && !j.getStatus().equals("COMPLETED")) {
                JTextField txtFee = new JTextField("0.0");
                JTextField txtRem = new JTextField();
                JPanel p = new JPanel(new GridLayout(0, 1));
                p.add(new JLabel("Service Fee (\u20B1):")); p.add(txtFee);
                p.add(new JLabel("Completion Remarks:")); p.add(txtRem);

                if(JOptionPane.showConfirmDialog(this, p, "Complete Job", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    try {
                        double fee = Double.parseDouble(txtFee.getText());
                        j.setCompletionDetails(fee, txtRem.getText());
                        refreshTable();
                    } catch(Exception ex) { JOptionPane.showMessageDialog(this, "Invalid Fee Format"); }
                }
            }
        });

        btnSearch.addActionListener(e -> applyFilter(tfSearch.getText()));
        btnReset.addActionListener(e -> { tfSearch.setText(""); applyFilter(""); });

        return container;
    }

    private JScrollPane createJobTablePanel() {
        String[] cols = {"Ticket #", "Location", "Description", "Technician", "Priority", "Status", "Schedule"};
        jobModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        jobTable = new JTable(jobModel);
        jobTable.setRowHeight(25);
        return new JScrollPane(jobTable);
    }

    private JPanel createCalendarPanel() {
        JPanel main = new JPanel(new BorderLayout());
        lblCalendarMonth = new JLabel("", SwingConstants.CENTER);
        lblCalendarMonth.setFont(new Font("Segoe UI", Font.BOLD, 20));
        calendarGrid = new JPanel(new GridLayout(0, 7, 5, 5));
        calendarGrid.setBorder(new EmptyBorder(10,10,10,10));
        main.add(lblCalendarMonth, BorderLayout.NORTH);
        main.add(calendarGrid, BorderLayout.CENTER);
        return main;
    }

    private JPanel createFinancialPanel() {
        JPanel p = new JPanel(new BorderLayout());
        financialReportArea = new JTextArea();
        financialReportArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        financialReportArea.setEditable(false);
        p.add(new JScrollPane(financialReportArea), BorderLayout.CENTER);
        JButton btnGen = new JButton("Refresh Report");
        btnGen.addActionListener(e -> updateFinancialReport());
        p.add(btnGen, BorderLayout.SOUTH);
        return p;
    }

    // --- HELPERS ---

    private JobOrder getSelectedJob() {
        int r = jobTable.getSelectedRow();
        if(r == -1) { JOptionPane.showMessageDialog(this, "Please select a job from the table."); return null; }
        String t = (String) jobTable.getValueAt(r, 0);
        return allJobs.stream().filter(j -> j.getTicketNumber().equals(t)).findFirst().orElse(null);
    }

    private void refreshTable() {
        applyFilter(tfSearch.getText());
    }

    private void applyFilter(String query) {
        jobModel.setRowCount(0);
        String q = query.toLowerCase();
        
        List<JobOrder> displayList = allJobs.stream()
            .filter(j -> !j.getStatus().equals("COMPLETED")) // Only show active
            .sorted(Comparator.comparing(JobOrder::getDateScheduled))
            .collect(Collectors.toList());

        for (JobOrder j : displayList) {
            boolean match = q.isEmpty() || j.getLocation().toLowerCase().contains(q) || j.getTicketNumber().toLowerCase().contains(q);
            if(match) jobModel.addRow(j.toRowData());
        }
    }

    private void updateCalendar() {
        calendarGrid.removeAll();
        YearMonth cm = YearMonth.now();
        lblCalendarMonth.setText(cm.getMonth().name() + " " + cm.getYear());
        
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for(String d : days) calendarGrid.add(new JLabel(d, SwingConstants.CENTER));
        
        LocalDate first = cm.atDay(1);
        for(int i=0; i<first.getDayOfWeek().getValue()-1; i++) calendarGrid.add(new JLabel(""));

        for(int day=1; day<=cm.lengthOfMonth(); day++) {
            LocalDate date = cm.atDay(day);
            JButton b = new JButton(String.valueOf(day));
            b.setBackground(Color.WHITE);
            
            long count = allJobs.stream()
                .filter(j -> j.getDateScheduled().toLocalDate().equals(date) && !j.getStatus().equals("COMPLETED"))
                .count();

            if(count > 0) {
                b.setBackground(new Color(255, 200, 200));
                b.setText("<html><center>" + day + "<br/><small>" + count + " Jobs</small></center></html>");
            }
            
            b.addActionListener(e -> {
                String msg = allJobs.stream()
                    .filter(j -> j.getDateScheduled().toLocalDate().equals(date))
                    .map(j -> j.getTicketNumber() + ": " + j.getStatus() + " (" + j.getTechnician() + ")")
                    .collect(Collectors.joining("\n"));
                JOptionPane.showMessageDialog(this, msg.isEmpty() ? "No jobs." : msg);
            });
            calendarGrid.add(b);
        }
        calendarGrid.revalidate(); calendarGrid.repaint();
    }

    private void updateFinancialReport() {
        StringBuilder sb = new StringBuilder("=== FINANCIAL & PAYROLL REPORT ===\n\n");
        double totalRev = 0;
        Map<String, Integer> techJobs = new HashMap<>();

        sb.append("--- COMPLETED JOBS ---\n");
        sb.append(String.format("%-10s %-20s %-10s %-20s\n", "Ticket", "Location", "Fee", "Remarks"));
        sb.append("------------------------------------------------------------\n");
        
        for(JobOrder j : allJobs) {
            if(j.getStatus().equals("COMPLETED")) {
                totalRev += j.getServiceFee();
                techJobs.put(j.getTechnician(), techJobs.getOrDefault(j.getTechnician(), 0) + 1);
                sb.append(String.format("%-10s %-20s \u20B1%-9.2f %s\n", 
                    j.getTicketNumber(), j.getLocation(), j.getServiceFee(), "Done"));
            }
        }
        
        sb.append("\nTotal Revenue: \u20B1").append(String.format("%.2f", totalRev)).append("\n\n");
        
        sb.append("--- TECHNICIAN PERFORMANCE ---\n");
        for(String t : techJobs.keySet()) {
            if(!t.equals("Unassigned")) {
                sb.append("Technician: ").append(t).append("\n");
                sb.append("   Jobs Completed: ").append(techJobs.get(t)).append("\n");
                sb.append("   Estimated Pay (300/job): \u20B1").append(techJobs.get(t) * 300).append("\n\n");
            }
        }
        financialReportArea.setText(sb.toString());
    }
}
