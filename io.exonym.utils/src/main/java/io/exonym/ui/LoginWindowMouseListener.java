package io.exonym.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPasswordField;
import javax.swing.JTextField;


public abstract class LoginWindowMouseListener implements ActionListener, MouseListener {

	private static final Logger logger = LogManager.getLogger(LoginWindowMouseListener.class);
	protected JButton btnOk;
	protected JButton btnCancel;
	private JPasswordField txtPassword;
	private JTextField txtUsername;
	private JFrame frame; 

	
	protected JButton getBtnOk() {
		return btnOk;
		
	}

	protected void setBtnOk(JButton btnOk) {
		this.btnOk = btnOk;
		
	}

	protected JButton getBtnCancel() {
		return btnCancel;
		
	}

	protected void setBtnCancel(JButton btnCancel) {
		this.btnCancel = btnCancel;
		
	}

	protected JPasswordField getTxtPassword() {
		return txtPassword;
	}

	protected void setTxtPassword(JPasswordField txtPassword) {
		this.txtPassword = txtPassword;
	}

	protected JTextField getTxtUsername() {
		return txtUsername;
	}

	protected void setTxtUsername(JTextField txtUsername) {
		this.txtUsername = txtUsername;
	}

	protected JFrame getFrame() {
		return frame;
		
	}

	protected void setFrame(JFrame frame) {
		this.frame = frame;
		
	}
}
