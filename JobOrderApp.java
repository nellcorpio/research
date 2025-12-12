
package nell;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

// --- 1. The Main Entry Point (Login System) ---
public class JobOrderApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createLoginScreen());
    }

    private static void createLoginScreen() {
        JFrame loginFrame = new JFrame("System Login");
        loginFrame.setSize(350, 200);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(null);
        loginFrame.setLocationRelativeTo(null); // Center on screen

        // UI Components
        JLabel lblUser = new JLabel("Username:");
        lblUser.setBounds(30, 30, 80, 25);
        JTextField txtUser = new JTextField();
        txtUser.setBounds(110, 30, 180, 25);

        JLabel lblPass = new JLabel("Password:");
        lblPass.setBounds(30, 70, 80, 25);
        JPasswordField txtPass = new JPasswordField();
        txtPass.setBounds(110, 70, 180, 25);

        JButton btnLogin = new JButton("Login");
        btnLogin.setBounds(110, 110, 80, 30);
        JButton btnExit = new JButton("Exit");
        btnExit.setBounds(200, 110, 80, 30);

        // Add to frame
        loginFrame.add(lblUser);
        loginFrame.add(txtUser);
        loginFrame.add(lblPass);
        loginFrame.add(txtPass);
        loginFrame.add(btnLogin);
        loginFrame.add(btnExit);

        // --- Button Logic ---
        
        // EXIT BUTTON
        btnExit.addActionListener(e -> System.exit(0));

        // LOGIN BUTTON
        ActionListener loginAction = e -> {
            String username = txtUser.getText();
            String password = new String(txtPass.getPassword());

            // HARDCODED CREDENTIALS (for demonstration)
            if (username.equals("admin") && password.equals("admin123")) {
                loginFrame.dispose(); // Close Login Window
                new MaintenanceDashboard(); // Open Main System
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Invalid Username or Password!", "Login Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        btnLogin.addActionListener(loginAction);
        
        // Allow pressing "Enter" key on password field to login
        txtPass.addActionListener(loginAction);

        loginFrame.setVisible(true);
    }
}

// --- 2. The Main Dashboard (The Job Tracking System) ---
class MaintenanceDashboard {

    private DefaultTableModel tableModel;
    private int jobCounter = 1;

    public MaintenanceDashboard() {
        JFrame frame = new JFrame("Job Order & Repair Tracking System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null); // Center on screen

        // --- TOP PANEL: Create New Request ---
        JPanel createPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        createPanel.setBorder(BorderFactory.createTitledBorder("New Job Order"));
        
        JTextField tfLocation = new JTextField(10);
        JTextField tfDescription = new JTextField(25);
        JButton btnCreate = new JButton("Create Ticket");

        createPanel.add(new JLabel("Location:"));
        createPanel.add(tfLocation);
        createPanel.add(new JLabel("Problem:"));
        createPanel.add(tfDescription);
        createPanel.add(btnCreate);

        // --- CENTER PANEL: Table ---
        String[] columns = {"ID", "Location", "Description", "Technician", "Priority", "Status", "Date Created", "Remarks"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // --- BOTTOM PANEL: Controls ---
        JPanel managementPanel = new JPanel(new GridLayout(2, 1));
        
        // Update Section
        JPanel updateControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        updateControls.setBorder(BorderFactory.createTitledBorder("Manage Selected Job"));

        JTextField tfTech = new JTextField(8);
        JComboBox<String> cbPriority = new JComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH"});
        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"PENDING", "IN_PROGRESS", "COMPLETED"});
        JTextField tfRemarks = new JTextField(15);
        JButton btnUpdate = new JButton("Update");

        updateControls.add(new JLabel("Tech:"));
        updateControls.add(tfTech);
        updateControls.add(new JLabel("Prio:"));
        updateControls.add(cbPriority);
        updateControls.add(new JLabel("Status:"));
        updateControls.add(cbStatus);
        updateControls.add(new JLabel("Rem:"));
        updateControls.add(tfRemarks);
        updateControls.add(btnUpdate);

        // Search Section
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JTextField tfFilter = new JTextField(15);
        JButton btnFilter = new JButton("Filter Location");
        JButton btnLogout = new JButton("Log Out");

        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(tfFilter);
        filterPanel.add(btnFilter);
        filterPanel.add(btnLogout);

        managementPanel.add(updateControls);
        managementPanel.add(filterPanel);

        frame.add(createPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(managementPanel, BorderLayout.SOUTH);

        // --- LOGIC ---

        // Create
        btnCreate.addActionListener(e -> {
            if(tfLocation.getText().isEmpty() || tfDescription.getText().isEmpty()) return;
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
            tableModel.addRow(new Object[]{jobCounter++, tfLocation.getText(), tfDescription.getText(), "Unassigned", "LOW", "PENDING", date, ""});
            tfLocation.setText(""); tfDescription.setText("");
        });

        // Click Row
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    int modelRow = table.convertRowIndexToModel(row);
                    tfTech.setText((String) tableModel.getValueAt(modelRow, 3));
                    cbPriority.setSelectedItem(tableModel.getValueAt(modelRow, 4));
                    cbStatus.setSelectedItem(tableModel.getValueAt(modelRow, 5));
                    tfRemarks.setText((String) tableModel.getValueAt(modelRow, 7));
                }
            }
        });

        // Update
        btnUpdate.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(frame, "Select a row first.");
                return;
            }
            int modelRow = table.convertRowIndexToModel(row);
            tableModel.setValueAt(tfTech.getText(), modelRow, 3);
            tableModel.setValueAt(cbPriority.getSelectedItem(), modelRow, 4);
            tableModel.setValueAt(cbStatus.getSelectedItem(), modelRow, 5);
            tableModel.setValueAt(tfRemarks.getText(), modelRow, 7);
        });

        // Filter
        btnFilter.addActionListener(e -> {
            String text = tfFilter.getText();
            if (text.length() == 0) sorter.setRowFilter(null);
            else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 1));
        });

        // Logout
        btnLogout.addActionListener(e -> {
            frame.dispose(); // Close dashboard
            // Call the main class method to show login again
            JobOrderApp.main(new String[]{}); 
        });

        // Init Dummy Data
        addDummyData();
        frame.setVisible(true);
    }

    private void addDummyData() {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        tableModel.addRow(new Object[]{jobCounter++, "Room 203", "PC will not boot", "Unassigned", "HIGH", "PENDING", date, ""});
        tableModel.addRow(new Object[]{jobCounter++, "HR Dept", "Printer Jam", "Mike", "MEDIUM", "IN_PROGRESS", date, "Parts ordered"});
    }
}
