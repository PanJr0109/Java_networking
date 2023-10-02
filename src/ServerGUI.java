import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;

public class ServerGUI extends JFrame {
    private JLabel systemNameLabel;
    private JComboBox<String> clientComboBox;
    private JTextField pathDirectoryTextField;
    private JButton startButton;
    private JTable statusTable;
    private DefaultTableModel tableModel;
    private List<String> clients;
    private JButton chooseFolderButton;
    private ServerSocket serverSocket;
    private List<Socket> clientSocketList;
    private Map<String, WatchService> watchServiceMap;

    // Map client address to folder path
    private Map<String, String> watchServicePathMap;
    private String selectedClient;


    public ServerGUI() {
        clients = new ArrayList<>();
        clientSocketList = new ArrayList<>();
        watchServiceMap = new HashMap<>();
        initComponents();
    }

    private void initComponents() {
        setTitle("Server");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        systemNameLabel = new JLabel("Monitoring System");
        systemNameLabel.setFont(systemNameLabel.getFont().deriveFont(Font.BOLD, 18f));

        JLabel clientLabel = new JLabel("Danh sách client:");
        clientComboBox = new JComboBox<>();
        clientComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });



        pathDirectoryTextField = new JTextField();

        chooseFolderButton = new JButton("Chọn thư mục");
        chooseFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedClient = (String) clientComboBox.getSelectedItem();
                if (selectedClient != null) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    int result = fileChooser.showOpenDialog(ServerGUI.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedFolder = fileChooser.getSelectedFile();
                        String folderPath = selectedFolder.getAbsolutePath();
                        pathDirectoryTextField.setText(folderPath);
                        // Start monitoring
                        startMonitoring(selectedClient, folderPath);
                    }
                }
            }
        });

        JScrollPane statusScrollPane = new JScrollPane();
        tableModel = new DefaultTableModel();
        statusTable = new JTable(tableModel);
        tableModel.addColumn("STT");
        tableModel.addColumn("Tên thư mục");
        tableModel.addColumn("Hành động");
        tableModel.addColumn("Thời gian");
        tableModel.addColumn("Client");

        statusTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // Cột STT
        statusTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Cột Tên thư mục
        statusTable.getColumnModel().getColumn(2).setPreferredWidth(250); // Cột Hành động
        statusTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Cột Thời gian
        statusTable.getColumnModel().getColumn(4).setPreferredWidth(50); // Cột Client

        statusScrollPane.setViewportView(statusTable);

        startButton = new JButton("Bật server");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServer();
                startButton.setText("Đang bật server...");
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createParallelGroup()
                .addComponent(systemNameLabel, GroupLayout.Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(clientLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clientComboBox, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createSequentialGroup()
                        .addComponent(chooseFolderButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pathDirectoryTextField, GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
                .addComponent(statusScrollPane)
                .addComponent(startButton, GroupLayout.Alignment.CENTER));

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addComponent(systemNameLabel)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(clientLabel)
                        .addComponent(clientComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(chooseFolderButton)
                        .addComponent(pathDirectoryTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(statusScrollPane)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(startButton));

        layout.linkSize(SwingConstants.VERTICAL, chooseFolderButton, pathDirectoryTextField);
    }

    private void startServer() {
        int port = 8080; // Port number for server

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            // Listening for client connections
            Thread acceptClientsThread = new Thread(() -> {
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        clientSocketList.add(clientSocket);

                        // Add client to the ComboBox
                        String clientAddress = clientSocket.getInetAddress().toString();
                        // add port client
                        String portClient = String.valueOf(clientSocket.getPort());
                        clients.add(clientAddress);
                        clientComboBox.addItem("Client: " + portClient);


                        System.out.println("Client connected: " + clientAddress);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });



            acceptClientsThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startMonitoring(String clientAddress, String folderPath) {
        try {
            String clientPort = clientAddress.substring(0);
            Path path = Paths.get(folderPath);
            WatchService watchService = path.getFileSystem().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

            watchServiceMap.put(clientAddress, watchService);


            Thread monitoringThread = new Thread(() -> {
                try {
                    while (true) {
                        WatchKey watchKey = watchService.take();

                        for (WatchEvent<?> event : watchKey.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();

                            if (kind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }

                            WatchEvent<Path> ev = (WatchEvent<Path>) event;
                            Path filename = ev.context();
                            Path changedDirectory = (Path) watchKey.watchable();

                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                System.out.println("Created: " + changedDirectory);
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                System.out.println("Deleted: " + changedDirectory);
                            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                System.out.println("Modified: " + changedDirectory);
                            }

                            String action ;
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                action = "Thêm mới: " + filename.toString();
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                action = "Xoá: " + filename.toString();
                            } else {
                                action = "Chỉnh sửa: " + filename.toString();
                            }
                            // Add event details to the table
                            addEventToTable(changedDirectory.toString(), action, new Date().toString(), clientPort );
                        }

                        if (!watchKey.reset()) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            });
            monitoringThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addEventToTable(String directoryName, String action, String time, String portClient) {
        SwingUtilities.invokeLater(() -> {
            Object[] rowData = {tableModel.getRowCount() + 1, directoryName, action, time, portClient};
            tableModel.addRow(rowData);
        });
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
        });
    }
}
