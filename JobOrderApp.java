package advancejobapp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

// ==========================================
// 1. MAIN RUNNER
// ==========================================
public class EnterpriseSystem {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } 
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(LoginScreen::new);
    }
}

// ==========================================
// 2. LOGIN SCREEN
// ==========================================
class LoginScreen extends JFrame {
    public LoginScreen() {
        setTitle("System Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);
        setResizable(false);

        JLabel lblHeader = new JLabel("Maintenance MIS v5.6");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblHeader.setBounds(100, 20, 250, 30);
        add(lblHeader);

        JLabel lblUser = new JLabel("Username:");
        lblUser.setBounds(50, 70, 80, 25);
        JTextField txtUser = new JTextField();
        txtUser.setBounds(130, 70, 180, 25);
        add(lblUser);
        add(txtUser);

        JLabel lblPass = new JLabel("Password:");
        lblPass.setBounds(50, 110, 80, 25);
        JPasswordField txtPass = new JPasswordField();
        txtPass.setBounds(130, 110, 180, 25);
        add(lblPass);
        add(txtPass);

        JButton btnLogin = new JButton("Login");
        btnLogin.setBounds(130, 155, 90, 30);
        add(btnLogin);

        JButton btnExit = new JButton("Exit");
        btnExit.setBounds(230, 155, 80, 30);
        add(btnExit);

        ActionListener loginAction = e -> {
            String user = txtUser.getText();
            String pass = new String(txtPass.getPassword());
            if (user.equals("admin") && pass.equals("admin123")) {
                this.dispose();
                new Dashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid! Try 'admin' / 'admin123'", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        btnLogin.addActionListener(loginAction);
        txtPass.addActionListener(loginAction);
        btnExit.addActionListener(e -> System.exit(0));
        setVisible(true);
    }
}

// ==========================================
// 3. MODELS
// ==========================================
class JobOrder {
    private static int counter = 1;
    private String ticketNumber, location, description, technician, priority, status;
    private StringBuilder historyLog;
    private double serviceFee;
    private LocalDateTime dateCreated, dateScheduled, dateStarted, dateCompleted;

    public static final DateTimeFormatter DB_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter VIEW_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    public static final DateTimeFormatter VIEW_TIME = DateTimeFormatter.ofPattern("hh:mm a");

    public JobOrder(String location, String description, String priority, LocalDateTime scheduledDate) {
        this.ticketNumber = String.format("JO-%04d", counter++);
        this.location = location;
        this.description = description;
        this.priority = priority;
        this.dateCreated = LocalDateTime.now();
        this.dateScheduled = scheduledDate;
        this.technician = "Unassigned";
        this.serviceFee = 0.0;
        this.historyLog = new StringBuilder();
        this.status = dateScheduled.isAfter(LocalDateTime.now().plusHours(1)) ? "SCHEDULED" : "PENDING";
        addToHistory("Ticket Created. Scheduled: " + dateScheduled.format(DB_FMT));
    }

    public JobOrder(String ticket, String loc, String desc, String tech, String prio, String stat, 
                    String created, String sched, String started, String completed, String logs, double fee) {
        this.ticketNumber = ticket; this.location = loc; this.description = desc;
        this.technician = tech; this.priority = prio; this.status = stat;
        this.historyLog = new StringBuilder(logs.replace("~", "\n"));
        this.serviceFee = fee;
        this.dateCreated = parseDate(created); this.dateScheduled = parseDate(sched);
        this.dateStarted = parseDate(started); this.dateCompleted = parseDate(completed);
        if(this.dateScheduled == null) this.dateScheduled = this.dateCreated;
        try { int num = Integer.parseInt(ticket.split("-")[1]); if (num >= counter) counter = num + 1; } catch (Exception e) {}
    }

    private LocalDateTime parseDate(String d) { return (d == null || d.equals("null") || d.isEmpty()) ? null : LocalDateTime.parse(d, DB_FMT); }

    public void addToHistory(String action) { historyLog.append("[").append(LocalDateTime.now().format(DB_FMT)).append("] ").append(action).append("\n~"); }

    public void assignTechnician(String tech) {
        if (!this.technician.equals(tech)) { addToHistory("Assigned to: " + tech); this.technician = tech; }
    }

    public void setLocation(String location) { this.location = location; }
    public void setDescription(String description) { this.description = description; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setDateScheduled(LocalDateTime dateScheduled) { this.dateScheduled = dateScheduled; }

    public void updateStatus(String newStatus) {
        if (!this.status.equals(newStatus)) {
            addToHistory("Status: " + this.status + " -> " + newStatus);
            this.status = newStatus;
            if (newStatus.equals("IN_PROGRESS") && dateStarted == null) dateStarted = LocalDateTime.now();
            if (newStatus.equals("COMPLETED")) { dateCompleted = LocalDateTime.now(); addToHistory("Completed. Fee: " + serviceFee); }
        }
    }

    public void setServiceFee(double fee) { this.serviceFee = fee; }
    public double getServiceFee() { return serviceFee; }
    public long getMinutesWorked() { return (dateStarted == null || dateCompleted == null) ? 0 : Duration.between(dateStarted, dateCompleted).toMinutes(); }
    
    public String toCSV() {
        return String.join("|", ticketNumber, location, description, technician, priority, status, 
            dateCreated.format(DB_FMT), (dateScheduled!=null?dateScheduled.format(DB_FMT):"null"), 
            (dateStarted!=null?dateStarted.format(DB_FMT):"null"), (dateCompleted!=null?dateCompleted.format(DB_FMT):"null"), 
            historyLog.toString().replace("\n", ""), String.valueOf(serviceFee));
    }

    public String getHtmlSlip() {
        String color = status.equals("COMPLETED") ? "green" : "red";
        return "<html><body style='font-family:sans-serif; padding:10px;'>" +
               "<div style='border:2px solid black; padding:10px; width:300px;'>" +
               "<h2 style='text-align:center;'>JOB ORDER</h2><hr>" +
               "<b>Ticket:</b> " + ticketNumber + "<br/>" +
               "<b>Schedule:</b> " + dateScheduled.format(VIEW_DATE) + "<br/>" +
               "<b>Tech:</b> " + technician + "<br/>" +
               "<b>Issue:</b> " + description + "<br/><hr>" +
               "<b>Status:</b> <span style='color:"+color+"'>" + status + "</span><br/>" +
               "<b>Fee:</b> \u20B1" + String.format("%.2f", serviceFee) + 
               "</div></body></html>";
    }

    public Object[] toRowData() { return new Object[]{ticketNumber, location, description, technician, priority, status, dateScheduled.format(VIEW_DATE) + " " + dateScheduled.format(VIEW_TIME)}; }
    
    public Object[] toCalendarDetailRow() {
        return new Object[]{ dateScheduled.format(VIEW_TIME), ticketNumber, location, description, technician, status };
    }

    public String getTicketNumber() { return ticketNumber; }
    public String getStatus() { return status; }
    public String getTechnician() { return technician; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public LocalDateTime getDateScheduled() { return dateScheduled; }
}

class Employee {
    private String name;
    private String role;
    private String status; 

    public Employee(String name, String role, String status) {
        this.name = name;
        this.role = role;
        this.status = status;
    }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String toCSV() { return name + "|" + role + "|" + status; }
    public Object[] toRowData() { return new Object[]{name, role, status}; }
}

// ==========================================
// 4. DATA MANAGER
// ==========================================
class DataManager {
    private static final String JOB_FILE = "jobs_v5.txt";
    private static final String EMP_FILE = "employees.db";

    public static void saveJobs(List<JobOrder> jobs) {
        try (PrintWriter w = new PrintWriter(new FileWriter(JOB_FILE))) { for (JobOrder j : jobs) w.println(j.toCSV()); } catch (IOException e) {}
    }
    public static List<JobOrder> loadJobs() {
        List<JobOrder> l = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(JOB_FILE))) {
            String line; while ((line = r.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length >= 11) l.add(new JobOrder(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], (p.length>11?Double.parseDouble(p[11]):0.0)));
            }
        } catch (IOException e) {}
        return l;
    }

    public static void saveEmployees(List<Employee> emps) {
        try (PrintWriter w = new PrintWriter(new FileWriter(EMP_FILE))) { for (Employee e : emps) w.println(e.toCSV()); } catch (IOException e) {}
    }
    public static List<Employee> loadEmployees() {
        List<Employee> l = new ArrayList<>();
        File f = new File(EMP_FILE);
        if(!f.exists()) { l.add(new Employee("John Doe", "Technician", "AVAILABLE")); return l; }
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line; while ((line = r.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length >= 3) l.add(new Employee(p[0], p[1], p[2]));
            }
        } catch (IOException e) {}
        return l;
    }
}

// ==========================================
// 5. DASHBOARD
// ==========================================
class Dashboard extends JFrame {

    private List<JobOrder> allJobs;
    private List<Employee> allEmployees;
    
    private DefaultTableModel activeModel, archiveModel, employeeModel;
    private JTable activeTable, archiveTable, employeeTable;
    private JTextField tfSearch, tfArchSearch;
    private JTextArea financialReportArea;
    private JPanel calendarGrid;
    private JLabel lblCalendarMonth;

    public Dashboard() {
        super("Enterprise System v5.6 - Admin");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1350, 850);
        setLocationRelativeTo(null);

        allJobs = DataManager.loadJobs();
        allEmployees = DataManager.loadEmployees();

        JTabbedPane tabbedPane = new JTabbedPane();
        
        JPanel activePanel = new JPanel(new BorderLayout());
        activePanel.add(createInputPanel(), BorderLayout.NORTH);
        activePanel.add(createActiveTablePanel(), BorderLayout.CENTER);
        activePanel.add(createActionPanel(), BorderLayout.SOUTH);
        
        JPanel calendarPanel = createCalendarPanel();
        JPanel employeePanel = createEmployeePanel();
        JPanel financePanel = createFinancialPanel();
        
        JPanel archivePanel = new JPanel(new BorderLayout());
        archivePanel.add(createArchiveTablePanel(), BorderLayout.CENTER);
        archivePanel.add(createArchiveBottomPanel(), BorderLayout.SOUTH);

        tabbedPane.addTab("Active Jobs", activePanel);
        tabbedPane.addTab("Calendar View", calendarPanel);
        tabbedPane.addTab("Team Management", employeePanel);
        tabbedPane.addTab("Reports", financePanel);
        tabbedPane.addTab("Archives", archivePanel);

        add(tabbedPane);
        refreshAll();

        tabbedPane.addChangeListener(e -> { refreshAll(); updateFinancialReport(); updateCalendar(); });
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { DataManager.saveJobs(allJobs); DataManager.saveEmployees(allEmployees); }
        });

        setVisible(true);
    }

    private void refreshAll() {
        refreshTables();
        refreshEmployeeTable();
    }

    // --- GUI BUILDERS ---

    private JPanel createInputPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        p.setBorder(BorderFactory.createTitledBorder("Job Management"));
        
        JButton btnCreate = new JButton("+ Schedule New Job");
        btnCreate.setBackground(new Color(60, 179, 113));
        btnCreate.setForeground(Color.BLACK); 
        btnCreate.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        p.add(btnCreate);

        btnCreate.addActionListener(e -> {
            JTextField popupLoc = new JTextField();
            JTextField popupDesc = new JTextField();
            JComboBox<String> popupPrio = new JComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH"});
            JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
            dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "MMM dd, yyyy HH:mm"));
            dateSpinner.setValue(new Date());

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("Location:")); panel.add(popupLoc);
            panel.add(new JLabel("Description:")); panel.add(popupDesc);
            panel.add(new JLabel("Priority:")); panel.add(popupPrio);
            panel.add(new JLabel("Schedule:")); panel.add(dateSpinner);

            if (JOptionPane.showConfirmDialog(this, panel, "New Job", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                if (!popupLoc.getText().isEmpty()) {
                    LocalDateTime dt = ((Date)dateSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    allJobs.add(new JobOrder(popupLoc.getText(), popupDesc.getText(), (String)popupPrio.getSelectedItem(), dt));
                    DataManager.saveJobs(allJobs);
                    refreshTables();
                }
            }
        });
        return p;
    }

    private JScrollPane createActiveTablePanel() {
        String[] cols = {"Ticket #", "Location", "Description", "Technician", "Priority", "Status", "Scheduled For"};
        activeModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        activeTable = new JTable(activeModel);
        activeTable.setRowHeight(30);
        activeTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                String stat = (String) t.getModel().getValueAt(t.convertRowIndexToModel(r), 5);
                if ("SCHEDULED".equals(stat)) comp.setBackground(new Color(255, 255, 224));
                else if ("IN_PROGRESS".equals(stat)) comp.setBackground(new Color(224, 255, 255));
                else comp.setBackground(Color.WHITE);
                if (s) { comp.setBackground(new Color(184, 207, 229)); comp.setForeground(Color.BLACK); }
                return comp;
            }
        });
        return new JScrollPane(activeTable);
    }

    private JScrollPane createArchiveTablePanel() {
        archiveModel = new DefaultTableModel(new String[]{"Ticket #", "Location", "Description", "Tech", "Priority", "Status", "Date"}, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        archiveTable = new JTable(archiveModel);
        archiveTable.setRowHeight(25);
        return new JScrollPane(archiveTable);
    }

    // === UPDATED: Archive Bottom Panel with DELETE ===
    private JPanel createArchiveBottomPanel() {
        JPanel main = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton btnPrint = new JButton("Print Selected Slip");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JButton btnDelete = new JButton("Delete Record");
        btnDelete.setBackground(new Color(255, 100, 100)); // Red warning color
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));

        left.add(btnPrint);
        left.add(btnDelete);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tfArchSearch = new JTextField(12);
        JButton btnSearch = new JButton("Search Archive");
        JButton btnReset = new JButton("Reset");
        right.add(new JLabel("Find:")); right.add(tfArchSearch); right.add(btnSearch); right.add(btnReset);

        main.add(left, BorderLayout.WEST); main.add(right, BorderLayout.EAST);

        // Print Logic
        btnPrint.addActionListener(e -> {
            JobOrder j = getArchivedSel(); 
            if(j!=null) {
                JEditorPane p = new JEditorPane("text/html", j.getHtmlSlip());
                if(JOptionPane.showConfirmDialog(this, new JScrollPane(p), "Print Archived Slip?", JOptionPane.OK_CANCEL_OPTION)==0) {
                    try { p.print(); } catch(Exception ex) { ex.printStackTrace(); }
                }
            } else { JOptionPane.showMessageDialog(this, "Please select an archived job first."); }
        });

        // NEW: Delete Logic
        btnDelete.addActionListener(e -> {
            JobOrder j = getArchivedSel();
            if (j != null) {
                int confirm = JOptionPane.showConfirmDialog(this, 
                    "Are you sure you want to PERMANENTLY delete this record?\nThis cannot be undone.", 
                    "Delete Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    allJobs.remove(j);
                    DataManager.saveJobs(allJobs);
                    refreshTables();
                    updateFinancialReport(); // Recalculate finances
                    JOptionPane.showMessageDialog(this, "Record successfully deleted.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a record to delete.");
            }
        });

        btnSearch.addActionListener(e -> {
            TableRowSorter<DefaultTableModel> s = new TableRowSorter<>(archiveModel);
            archiveTable.setRowSorter(s);
            if(tfArchSearch.getText().length()>0) s.setRowFilter(RowFilter.regexFilter("(?i)"+tfArchSearch.getText()));
        });
        btnReset.addActionListener(e -> { tfArchSearch.setText(""); archiveTable.setRowSorter(null); });
        return main;
    }

    private JPanel createEmployeePanel() {
        JPanel main = new JPanel(new BorderLayout());
        JPanel input = new JPanel(new FlowLayout(FlowLayout.LEFT));
        input.setBorder(BorderFactory.createTitledBorder("Add Employee"));
        JTextField txtName = new JTextField(15);
        JComboBox<String> cbRole = new JComboBox<>(new String[]{"Technician", "Supervisor", "Janitor"});
        JButton btnAdd = new JButton("Add Staff");
        input.add(new JLabel("Name:")); input.add(txtName); input.add(new JLabel("Role:")); input.add(cbRole); input.add(btnAdd);

        String[] cols = {"Name", "Role", "Availability Status"};
        employeeModel = new DefaultTableModel(cols, 0);
        employeeTable = new JTable(employeeModel);
        employeeTable.setRowHeight(25);
        employeeTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                String stat = (String) t.getModel().getValueAt(r, 2);
                if ("AVAILABLE".equals(stat)) comp.setForeground(new Color(0, 150, 0));
                else comp.setForeground(Color.RED);
                return comp;
            }
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnDelete = new JButton("Remove");
        JButton btnToggle = new JButton("Toggle Status");
        controls.add(btnToggle); controls.add(btnDelete);

        main.add(input, BorderLayout.NORTH); main.add(new JScrollPane(employeeTable), BorderLayout.CENTER); main.add(controls, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> {
            if(!txtName.getText().isEmpty()) {
                allEmployees.add(new Employee(txtName.getText(), (String)cbRole.getSelectedItem(), "AVAILABLE"));
                txtName.setText(""); refreshEmployeeTable(); DataManager.saveEmployees(allEmployees);
            }
        });
        btnDelete.addActionListener(e -> {
            int r = employeeTable.getSelectedRow();
            if(r != -1) { allEmployees.remove(r); refreshEmployeeTable(); DataManager.saveEmployees(allEmployees); }
        });
        btnToggle.addActionListener(e -> {
            int r = employeeTable.getSelectedRow();
            if(r != -1) {
                Employee emp = allEmployees.get(r);
                emp.setStatus(emp.getStatus().equals("AVAILABLE") ? "BUSY" : "AVAILABLE");
                refreshEmployeeTable(); DataManager.saveEmployees(allEmployees);
            }
        });
        return main;
    }

    private void refreshEmployeeTable() {
        employeeModel.setRowCount(0);
        for(Employee e : allEmployees) employeeModel.addRow(e.toRowData());
    }

    private JPanel createCalendarPanel() {
        JPanel main = new JPanel(new BorderLayout());
        JPanel header = new JPanel();
        lblCalendarMonth = new JLabel();
        lblCalendarMonth.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.add(lblCalendarMonth);
        calendarGrid = new JPanel(new GridLayout(0, 7, 5, 5));
        calendarGrid.setBorder(new EmptyBorder(10, 10, 10, 10));
        main.add(header, BorderLayout.NORTH); main.add(calendarGrid, BorderLayout.CENTER);
        return main;
    }

    private void updateCalendar() {
        calendarGrid.removeAll();
        YearMonth cm = YearMonth.now();
        lblCalendarMonth.setText(cm.getMonth().name() + " " + cm.getYear());
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String d : days) calendarGrid.add(new JLabel(d, SwingConstants.CENTER));
        LocalDate f = cm.atDay(1);
        for (int i = 0; i < f.getDayOfWeek().getValue() - 1; i++) calendarGrid.add(new JLabel(""));
        
        for (int day = 1; day <= cm.lengthOfMonth(); day++) {
            LocalDate date = cm.atDay(day);
            JButton b = new JButton("<html><center>" + day + "</center></html>");
            b.setBackground(Color.WHITE);
            
            List<JobOrder> dailyJobs = allJobs.stream()
                .filter(j -> j.getDateScheduled().toLocalDate().equals(date))
                .sorted(Comparator.comparing(JobOrder::getDateScheduled))
                .collect(Collectors.toList());
            
            long sc = dailyJobs.stream().filter(j -> !j.getStatus().equals("COMPLETED")).count();
            
            if (sc > 0) {
                b.setBackground(new Color(255, 228, 181));
                b.setText("<html><center>" + day + "<br/><font color='red'>" + sc + " Pending</font></center></html>");
            } else if (!dailyJobs.isEmpty()) {
                b.setBackground(new Color(152, 251, 152)); 
                b.setText("<html><center>" + day + "<br/><font color='green'>All Done</font></center></html>");
            }

            b.addActionListener(e -> showDayDetails(date, dailyJobs));
            
            calendarGrid.add(b);
        }
        calendarGrid.revalidate(); calendarGrid.repaint();
    }

    private void showDayDetails(LocalDate date, List<JobOrder> dailyJobs) {
        if (dailyJobs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No jobs scheduled for " + date);
            return;
        }

        JDialog dialog = new JDialog(this, "Schedule for " + date.format(JobOrder.VIEW_DATE), true);
        dialog.setSize(800, 400);
        dialog.setLocationRelativeTo(this);

        String[] cols = {"Time", "Ticket", "Location", "Description", "Technician", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        
        for (JobOrder j : dailyJobs) {
            model.addRow(j.toCalendarDetailRow());
        }

        JTable table = new JTable(model);
        table.setRowHeight(25);
        table.getColumnModel().getColumn(0).setPreferredWidth(80); 
        table.getColumnModel().getColumn(3).setPreferredWidth(200); 

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                String stat = (String) t.getModel().getValueAt(r, 5);
                if ("COMPLETED".equals(stat)) comp.setForeground(new Color(0, 100, 0));
                else comp.setForeground(Color.RED);
                return comp;
            }
        });

        dialog.add(new JScrollPane(table));
        dialog.setVisible(true);
    }

    private JPanel createFinancialPanel() {
        JPanel p = new JPanel(new BorderLayout());
        financialReportArea = new JTextArea();
        financialReportArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        p.add(new JScrollPane(financialReportArea), BorderLayout.CENTER);
        JButton r = new JButton("Refresh");
        r.addActionListener(e -> updateFinancialReport());
        p.add(r, BorderLayout.SOUTH);
        return p;
    }

    private void updateFinancialReport() {
        double total = allJobs.stream().filter(j -> j.getStatus().equals("COMPLETED")).mapToDouble(JobOrder::getServiceFee).sum();
        StringBuilder sb = new StringBuilder("=== FINANCIAL REPORT ===\n\nTotal Revenue: \u20B1" + String.format("%.2f", total) + "\n\nPAYROLL:\n");
        sb.append(String.format("%-15s %-10s %-10s\n", "Tech", "Hours", "Pay"));
        
        Map<String, Long> th = new HashMap<>();
        for (JobOrder j : allJobs) if (j.getStatus().equals("COMPLETED") && !j.getTechnician().equals("Unassigned")) 
            th.put(j.getTechnician(), th.getOrDefault(j.getTechnician(), 0L) + j.getMinutesWorked());
        
        double pay = 0;
        double rate = 300.0;
        for (String t : th.keySet()) {
            double h = th.get(t) / 60.0;
            double p = h * rate;
            pay += p;
            sb.append(String.format("%-15s %-10.1f \u20B1%-10.2f\n", t, h, p));
        }
        sb.append("\n(Rate: \u20B1300/hr)\n");
        sb.append("Net Profit: \u20B1" + String.format("%.2f", total - pay));
        financialReportArea.setText(sb.toString());
    }

    private JPanel createActionPanel() {
        JPanel main = new JPanel(new BorderLayout());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton btnAssign = new JButton("Assign");
        JButton btnStart = new JButton("Start Job");
        btnStart.setBackground(new Color(173, 216, 230));
        JButton btnComplete = new JButton("Quick Complete");
        btnComplete.setBackground(new Color(144, 238, 144));
        JButton btnEdit = new JButton("Edit Details");
        JButton btnPrint = new JButton("Print");
        
        controls.add(btnAssign); 
        controls.add(btnStart);      
        controls.add(btnComplete);   
        controls.add(btnEdit); 
        controls.add(btnPrint);

        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tfSearch = new JTextField(10);
        JButton btnFilter = new JButton("Search");
        JButton btnReset = new JButton("Reset");
        JButton btnLogout = new JButton("Logout");
        btnLogout.setBackground(new Color(255, 100, 100)); btnLogout.setForeground(Color.WHITE);
        search.add(new JLabel("Search:")); search.add(tfSearch); search.add(btnFilter); search.add(btnReset); search.add(btnLogout);
        main.add(controls, BorderLayout.CENTER); main.add(search, BorderLayout.SOUTH);

        btnAssign.addActionListener(e -> {
            JobOrder j = getSel(); if(j == null) return;
            List<String> availableTechs = allEmployees.stream().filter(emp -> emp.getStatus().equals("AVAILABLE")).map(Employee::getName).collect(Collectors.toList());
            if (availableTechs.isEmpty()) { JOptionPane.showMessageDialog(this, "No Employees AVAILABLE."); return; }
            String selectedTech = (String) JOptionPane.showInputDialog(this, "Select Tech:", "Assign", 3, null, availableTechs.toArray(), availableTechs.get(0));
            if (selectedTech != null) {
                j.assignTechnician(selectedTech);
                allEmployees.stream().filter(emp -> emp.getName().equals(selectedTech)).findFirst().ifPresent(emp -> emp.setStatus("BUSY"));
                refreshAll(); DataManager.saveEmployees(allEmployees);
            }
        });

        btnStart.addActionListener(e -> {
            JobOrder j = getSel();
            if(j != null) {
                if(j.getStatus().equals("SCHEDULED") || j.getStatus().equals("PENDING")) {
                    j.updateStatus("IN_PROGRESS");
                    refreshAll();
                } else {
                    JOptionPane.showMessageDialog(this, "Job must be PENDING or SCHEDULED to start.", "Alert", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        btnComplete.addActionListener(e -> {
            JobOrder j = getSel();
            if(j != null) {
                if(!j.getStatus().equals("COMPLETED")) {
                    String fee = JOptionPane.showInputDialog(this, "Job Done! Enter Service Price (\u20B1):", "0.0");
                    if(fee != null) {
                        try { j.setServiceFee(Double.parseDouble(fee)); } catch(Exception ex) {}
                        String techName = j.getTechnician();
                        allEmployees.stream().filter(emp -> emp.getName().equals(techName)).findFirst().ifPresent(emp -> emp.setStatus("AVAILABLE"));
                        DataManager.saveEmployees(allEmployees);
                        j.updateStatus("COMPLETED");
                        refreshAll();
                    }
                }
            }
        });

        btnEdit.addActionListener(e -> {
            JobOrder j = getSel(); 
            if(j == null) return;
            
            JTextField txtLoc = new JTextField(j.getLocation());
            JTextField txtDesc = new JTextField(j.getDescription());
            JComboBox<String> cbPrio = new JComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH"});
            cbPrio.setSelectedItem(j.getPriority());
            JComboBox<String> cbStat = new JComboBox<>(new String[]{"SCHEDULED", "IN_PROGRESS", "COMPLETED"});
            cbStat.setSelectedItem(j.getStatus());
            JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
            dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "MMM dd, yyyy HH:mm"));
            Date dateVal = Date.from(j.getDateScheduled().atZone(ZoneId.systemDefault()).toInstant());
            dateSpinner.setValue(dateVal);

            JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
            panel.add(new JLabel("Status:"));       panel.add(cbStat);
            panel.add(new JLabel("Location:"));     panel.add(txtLoc);
            panel.add(new JLabel("Description:"));  panel.add(txtDesc);
            panel.add(new JLabel("Priority:"));     panel.add(cbPrio);
            panel.add(new JLabel("Schedule:"));     panel.add(dateSpinner);

            int result = JOptionPane.showConfirmDialog(this, panel, "Edit Ticket Details", JOptionPane.OK_CANCEL_OPTION);

            if(result == JOptionPane.OK_OPTION) {
                j.setLocation(txtLoc.getText());
                j.setDescription(txtDesc.getText());
                j.setPriority((String)cbPrio.getSelectedItem());
                j.setDateScheduled(((Date)dateSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                
                String newStatus = (String) cbStat.getSelectedItem();
                
                if (newStatus.equals("COMPLETED")) {
                    if (!j.getStatus().equals("COMPLETED") || j.getServiceFee() == 0.0) {
                        String fee = JOptionPane.showInputDialog("Enter Service Price (\u20B1):", "0.0");
                        try { j.setServiceFee(Double.parseDouble(fee)); } catch(Exception ex) {}
                    }
                    String techName = j.getTechnician();
                    allEmployees.stream().filter(emp -> emp.getName().equals(techName)).findFirst().ifPresent(emp -> emp.setStatus("AVAILABLE"));
                    DataManager.saveEmployees(allEmployees);
                } 
                j.updateStatus(newStatus); refreshAll(); 
            }
        });

        btnPrint.addActionListener(e -> {
            JobOrder j = getSel(); if(j!=null) {
                JEditorPane p = new JEditorPane("text/html", j.getHtmlSlip());
                if(JOptionPane.showConfirmDialog(this, new JScrollPane(p), "Print?", 2)==0) try{p.print();}catch(Exception ex){}
            }
        });

        btnFilter.addActionListener(e -> {
            TableRowSorter<DefaultTableModel> s = new TableRowSorter<>(activeModel);
            activeTable.setRowSorter(s);
            if(tfSearch.getText().length()>0) s.setRowFilter(RowFilter.regexFilter("(?i)"+tfSearch.getText()));
        });
        btnReset.addActionListener(e -> { tfSearch.setText(""); activeTable.setRowSorter(null); });
        
        btnLogout.addActionListener(e -> { DataManager.saveJobs(allJobs); DataManager.saveEmployees(allEmployees); dispose(); new LoginScreen(); });

        return main;
    }

    private JobOrder getSel() {
        int r = activeTable.getSelectedRow();
        return (r == -1) ? null : allJobs.stream().filter(j -> j.getTicketNumber().equals(activeTable.getValueAt(r, 0))).findFirst().orElse(null);
    }
    
    private JobOrder getArchivedSel() {
        int r = archiveTable.getSelectedRow();
        return (r == -1) ? null : allJobs.stream().filter(j -> j.getTicketNumber().equals(archiveTable.getValueAt(r, 0))).findFirst().orElse(null);
    }

    private void refreshTables() {
        activeModel.setRowCount(0); archiveModel.setRowCount(0);
        allJobs.sort(Comparator.comparing(JobOrder::getDateScheduled));
        for (JobOrder j : allJobs) {
            if (j.getStatus().equals("COMPLETED")) archiveModel.addRow(j.toRowData());
            else activeModel.addRow(j.toRowData());
        }
    }
}
