package io.exonym.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class CreatePasswordWindow extends JFrame {

	private static final Logger logger = LogManager.getLogger(CreatePasswordWindow.class);
	private int boxWidth, boxHeight, vPad, hPad;
	private int rows, cols;
	private int width, height; 
	private JButton btnOk, btnCancel;
	private JPasswordField txtPassword;
	private JPasswordField txtVerPassword;
	private JTextField txtUsername;
	
	public CreatePasswordWindow() {
		this.boxWidth = 100;
		this.boxHeight= 20;
		this.vPad = 10;
		this.hPad = 10; 
		this.rows = 4; 
		this.cols = 2; 
		this.width = this.cols * this.boxWidth + (this.cols + 1) * this.hPad + LoginWindow.FRAME_OVERWIDTH;
		this.height = this.rows * this.boxHeight + (this.rows + 1) * this.vPad + LoginWindow.FRAME_OVERHEIGHT;
		build();
	
	}
	
	private void build() {
		try {
			JPanel panel = layoutFrame();
			this.add(panel);
			this.setSize(new Dimension(width, height));
			this.setLocationRelativeTo(null);
			this.setResizable(false);
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.setTitle("Rulebook Node");
			this.getRootPane().setDefaultButton(btnOk);
			this.setVisible(true);

		} catch (Exception e) {
			logger.error("Window build error", e);
			
		}
	}

	
	public void setLoginWindowListener(CreatePasswordWindowMouseListener listener){
		listener.setBtnCancel(btnCancel);
		listener.setBtnOk(btnOk);
		listener.setTxtPassword(txtPassword);
		listener.setTxtUsername(txtUsername);
		listener.setTxtVerPassword(txtVerPassword);
		this.btnOk.addMouseListener(listener);
		this.btnOk.addActionListener(listener);
		this.btnCancel.addMouseListener(listener);
		
	}

	
	private JPanel layoutFrame() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(null);
		
		int row1 = vPad * 2 + this.boxHeight;
		int row2 = vPad * 3 + 2 * this.boxHeight;
		int row3 = vPad * 4 + 3 * this.boxHeight;
		int col1 = hPad * 2 + this.boxWidth;
		
		JLabel lblUsername = new JLabel("Username:");
		lblUsername.setBounds(this.hPad, this.vPad, this.boxWidth, this.boxHeight);
		lblUsername.setFont(LoginWindow.LABEL_FONT);
		
		JLabel lblPassword = new JLabel("Password:");
		lblPassword.setBounds(this.hPad, row1, this.boxWidth, this.boxHeight);
		lblPassword.setFont(LoginWindow.LABEL_FONT);
		
		JLabel lblVerPassword = new JLabel("Verify Password:");
		lblVerPassword.setBounds(this.hPad, row2, this.boxWidth, this.boxHeight);
		lblVerPassword.setFont(LoginWindow.LABEL_FONT);
		
		txtUsername = new JTextField();
		txtUsername.setBounds(col1, this.vPad, this.boxWidth, this.boxHeight);
		txtUsername.setFont(LoginWindow.LABEL_FONT);
		
		txtPassword = new JPasswordField();
		txtPassword.setBounds(col1, row1, this.boxWidth, this.boxHeight);
		txtPassword.setFont(LoginWindow.LABEL_FONT);
		
		txtVerPassword = new JPasswordField();
		txtVerPassword.setBounds(col1, row2, this.boxWidth, this.boxHeight);
		txtVerPassword.setFont(LoginWindow.LABEL_FONT);
		
		btnOk = new JButton("OK");
		btnOk.setBounds(col1, row3, this.boxWidth, this.boxHeight);
		btnOk.setFont(LoginWindow.LABEL_FONT);
		btnOk.setDefaultCapable(true);
		
		btnCancel = new JButton("Cancel");
		btnCancel.setBounds(this.hPad, row3, this.boxWidth, this.boxHeight);
		btnCancel.setFont(LoginWindow.LABEL_FONT);

		mainPanel.add(lblUsername);
		mainPanel.add(lblPassword);
		mainPanel.add(txtUsername);
		mainPanel.add(txtPassword);
		mainPanel.add(lblVerPassword);
		mainPanel.add(txtVerPassword);
		mainPanel.add(btnOk);
		mainPanel.add(btnCancel);	
		
		return mainPanel;
			
	}

	public JButton getBtnOk() {
		return btnOk;
	}

	public JButton getBtnCancel() {
		return btnCancel;
	}

	public JPasswordField getTxtPassword() {
		return txtPassword;
	}

	public JTextField getTxtUsername() {
		return txtUsername;
	}
	
}