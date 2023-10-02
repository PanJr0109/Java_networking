import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.Socket;

public class ClientGUI extends JFrame {
    private JTextField portClientField;
    private JTextField portServerField;
    private JTextField ipServerField;
    private JButton connectButton;
    private Socket clientSocket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public ClientGUI() {
        setTitle("Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Tạo các components
        JLabel systemNameLabel = new JLabel("Monitoring System");
        JLabel portClientLabel = new JLabel("Port Client:");
        JLabel portServerLabel = new JLabel("Port Server:");
        JLabel ipServerLabel = new JLabel("IP Server:");

        portClientField = new JTextField();
        portServerField = new JTextField();
        ipServerField = new JTextField();

        // Thiết lập các giá trị mặc định cho các components
        portClientField.setText("Enter port client");
        portServerField.setText("8080");
        ipServerField.setText("localhost");

        connectButton = new JButton("Kết nối");
        connectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Thực hiện các hành động khi nhấn nút Connect
                String portClient = portClientField.getText();
                String portServer = portServerField.getText();
                String ipServer = ipServerField.getText();

                connectButton.setText("Đang kết nối...");
                connectButton.setEnabled(false);


                ConnectToServerTask connectTask = new ConnectToServerTask(ipServer, Integer.parseInt(portServer));
                connectTask.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE == evt.getNewValue()) {
                            try {
                                boolean isConnected = connectTask.get();
                                if (isConnected) {
                                    // Kết nối thành công
                                    JOptionPane.showMessageDialog(null, "Đã kết nối tới server!");
                                    portClientField.setText(String.valueOf(connectTask.getLocalPort()));
                                } else {
                                    // Kết nối thất bại
                                    JOptionPane.showMessageDialog(null, "Kết nối tới server thất bại!");
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            } finally {
                                connectButton.setText("Đang kết nối...");
                                connectButton.setEnabled(true);
                            }
                        }
                    }
                });
                connectTask.execute();
            }
        });

        // Tạo GroupLayout và thiết lập layout cho JFrame
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        // Thiết lập tự động tạo các lỗ rỗng để các thành phần tràn đầy
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        // Thiết lập các thành phần và lề
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(systemNameLabel)
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(portClientLabel)
                                .addComponent(portServerLabel)
                                .addComponent(ipServerLabel))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(portClientField)
                                .addComponent(portServerField)
                                .addComponent(ipServerField)))
                .addComponent(connectButton));

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addComponent(systemNameLabel)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(portClientLabel)
                        .addComponent(portClientField))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(portServerLabel)
                        .addComponent(portServerField))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(ipServerLabel)
                        .addComponent(ipServerField))
                .addComponent(connectButton));

        systemNameLabel.setFont(new Font("Arial", Font.BOLD, 24));
        portClientField.setColumns(20);
        portServerField.setColumns(20);
        ipServerField.setColumns(20);

        pack(); // Điều chỉnh kích thước cửa sổ tự động dựa trên nội dung
        setResizable(false);
        setLocationRelativeTo(null); // Hiển thị cửa sổ giữa màn hình
    }

    private class ConnectToServerTask extends SwingWorker<Boolean, Void> {
        private String ip;
        private int port;
        private int localPort;

        public ConnectToServerTask(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        public int getLocalPort() {
            return localPort;
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            try {
                clientSocket = new Socket(ip, port);
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                writer = new BufferedWriter(new OutputStreamWriter(outputStream));

                // TODO: Xử lý kết nối với server và gửi/nhận dữ liệu
                // gửi port client cho server
                localPort = clientSocket.getLocalPort();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ClientGUI().setVisible(true);
            }
        });
    }
}
