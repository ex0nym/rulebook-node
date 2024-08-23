package io.exonym.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Dimension;
import java.awt.Font;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;



public class LoginWindow extends JFrame {

	public static final Font LABEL_FONT = new Font("Tahoma", Font.PLAIN, 12);
	public static final int FRAME_OVERWIDTH = 5;
	public static final int FRAME_OVERHEIGHT = 35;

	private static final long serialVersionUID = 1L;
	private int boxWidth, boxHeight, vPad, hPad;
	private static final Logger logger = LogManager.getLogger(LoginWindow.class);
	private int rows, cols;
	private int width, height; 
	private JButton btnOk, btnCancel;
	private JPasswordField txtPassword;
	private JTextField txtUsername;
	
	public LoginWindow(LoginWindowMouseListener listener) {
		this.boxWidth = 100;
		this.boxHeight= 20;
		this.vPad = 10;
		this.hPad = 10; 
		this.rows = 3; 
		this.cols = 2; 
		this.width = this.cols * this.boxWidth + (this.cols + 1) * this.hPad + FRAME_OVERWIDTH;
		this.height = this.rows * this.boxHeight + (this.rows + 1) * this.vPad + FRAME_OVERHEIGHT;
		build();
		this.setLoginWindowListener(listener);
	
	}
	
	private void build() {
		try {
			JPanel panel = layoutFrame();
			this.add(panel);
			this.setSize(new Dimension(width, height));
			this.setLocationRelativeTo(null);
			this.setResizable(false);
			this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			this.setTitle("Rulebook Node");
			this.getRootPane().setDefaultButton(btnOk);
			this.setVisible(true);
			URL iconURL = this.getClass().getResource("/existence-favicon.png");
			if (iconURL==null){
				logger.warn("The existence icon could not be found.");
				
			} else {
				ImageIcon icon = new ImageIcon(iconURL);
				this.setIconImage(icon.getImage());

			}
		} catch (Exception e) {
			logger.error("Window build error", e);
			
		}
		
	}

	
	protected void setLoginWindowListener(LoginWindowMouseListener listener){
		listener.setBtnCancel(btnCancel);
		listener.setBtnOk(btnOk);
		listener.setTxtPassword(txtPassword);
		listener.setTxtUsername(txtUsername);
		listener.setFrame(this);
		this.btnOk.addMouseListener(listener);
		this.btnOk.addActionListener(listener);
		this.btnCancel.addMouseListener(listener);
		
	}

	
	private JPanel layoutFrame() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(null);
		
		int row1 = vPad * 2 + this.boxHeight;
		int row2 = vPad * 3 + 2 * this.boxHeight;
		int col1 = hPad * 2 + this.boxWidth;
		
		JLabel lblUsername = new JLabel("Username:");
		lblUsername.setBounds(this.hPad, this.vPad, this.boxWidth, this.boxHeight);
		lblUsername.setFont(LABEL_FONT);
		
		JLabel lblPassword = new JLabel("Password:");
		lblPassword.setBounds(this.hPad, row1, this.boxWidth, this.boxHeight);
		lblPassword.setFont(LABEL_FONT);
		
		txtUsername = new JTextField();
		txtUsername.setBounds(col1, this.vPad, this.boxWidth, this.boxHeight);
		txtUsername.setFont(LABEL_FONT);
		txtUsername.setText("de.unique");
		
		txtPassword = new JPasswordField();
		txtPassword.setBounds(col1, row1, this.boxWidth, this.boxHeight);
		txtPassword.setFont(LABEL_FONT);
		txtPassword.setText("eth05pasS");
		
		btnOk = new JButton("OK");
		btnOk.setBounds(col1, row2, this.boxWidth, this.boxHeight);
		btnOk.setFont(LABEL_FONT);
		btnOk.setDefaultCapable(true);
		
		btnCancel = new JButton("Cancel");
		btnCancel.setBounds(this.hPad, row2, this.boxWidth, this.boxHeight);
		btnCancel.setFont(LABEL_FONT);

		mainPanel.add(lblUsername);
		mainPanel.add(lblPassword);
		mainPanel.add(txtUsername);
		mainPanel.add(txtPassword);
		mainPanel.add(btnOk);
		mainPanel.add(btnCancel);	
		
		return mainPanel;
		
	}

	protected JButton getBtnOk() {
		return btnOk;
	}

	protected JButton getBtnCancel() {
		return btnCancel;
	}

	protected JPasswordField getTxtPassword() {
		return txtPassword;
	}

	protected JTextField getTxtUsername() {
		return txtUsername;
	}
}