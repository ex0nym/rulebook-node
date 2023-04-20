package io.exonym.ui;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public abstract class CreatePasswordWindowMouseListener implements MouseListener, ActionListener{

	protected JButton btnOk;
	protected JButton btnCancel;
	protected JPasswordField txtPassword;
	protected JPasswordField txtVerPassword;
	protected JTextField txtUsername;
	
	public JButton getBtnOk() {
		return btnOk;
	}

	public void setBtnOk(JButton btnOk) {
		this.btnOk = btnOk;
	}

	public JButton getBtnCancel() {
		return btnCancel;
	}

	public void setBtnCancel(JButton btnCancel) {
		this.btnCancel = btnCancel;
	}

	public JPasswordField getTxtPassword() {
		return txtPassword;
	}

	public void setTxtPassword(JPasswordField txtPassword) {
		this.txtPassword = txtPassword;
	}

	public JPasswordField getTxtVerPassword() {
		return txtVerPassword;
	}

	public void setTxtVerPassword(JPasswordField txtVerPassword) {
		this.txtVerPassword = txtVerPassword;
	}

	public JTextField getTxtUsername() {
		return txtUsername;
	}

	public void setTxtUsername(JTextField txtUsername) {
		this.txtUsername = txtUsername;
	}
	
}	