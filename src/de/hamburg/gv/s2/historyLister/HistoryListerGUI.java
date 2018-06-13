package de.hamburg.gv.s2.historyLister;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.jdatepicker.JDatePicker;

/**
 * 
 */

/**
 * @author timmfl
 *
 */
public class HistoryListerGUI extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;

	private JDatePicker vonPicker, bisPicker;
	private HistoryListerK kontroll;
	private ErgebnisFenster ergebnis;

	public HistoryListerGUI() {
		super("HistoryLister");

		ergebnis = new ErgebnisFenster(this);

		kontroll = new HistoryListerK(ergebnis);

		showLogin();

		Container cp = this.getContentPane();
		cp.setLayout(new GridLayout(5, 1));

		cp.add(new JLabel("Startdatum:"));

		Date vonDate = new Date (System.currentTimeMillis()-3600*1000*24*7);
		vonPicker = new JDatePicker(vonDate);
		cp.add(vonPicker);

		cp.add(new JLabel("Enddatum:"));
		
		Date bisDate = new Date (System.currentTimeMillis());
		bisPicker = new JDatePicker(bisDate);
		cp.add(bisPicker);

		JButton b = new JButton("Generieren");
		b.addActionListener(this);
		cp.add(b);

		this.setPreferredSize(new Dimension(300, 200));
		this.pack();

		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});
		this.setLocationRelativeTo(null);
		this.setVisible(true);

	}

	private void showLogin() {
		boolean login = false;
		do {
			JTextField usernameField = new JTextField(5);
			usernameField.setText("SYSADM5");
			JPasswordField passwordField = new JPasswordField(5);

			JPanel loginPanel = new JPanel();
			loginPanel.setLayout(new GridLayout(2, 2));
			loginPanel.add(new JLabel("Benutzer"));
			loginPanel.add(usernameField);
			loginPanel.add(new JLabel("Passwort"));
			loginPanel.add(passwordField);

			int result = JOptionPane.showConfirmDialog(null, loginPanel, "TTSIB-Login", JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				String user = usernameField.getText();
				String passwort = new String(passwordField.getPassword());

				login = kontroll.connect(user, passwort);

			} else {
				System.exit(0);
			}
		} while (!login);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new HistoryListerGUI();

	}

	private void exit() {
		int result = JOptionPane.showConfirmDialog(this, "Möchten Sie das Programm beenden?", "Programm beenden",
				JOptionPane.YES_NO_OPTION);

		switch (result) {
		case JOptionPane.YES_OPTION:
			kontroll.destroy();
			System.exit(0); // Aktion(en) bei Klicken auf den "Ja-Button"
		}

	}

	public void actionPerformed(ActionEvent e) {
		kontroll.clearData();
		
		Date vonTime = (Date) this.vonPicker.getModel().getValue();
		Date bisTime = (Date) this.bisPicker.getModel().getValue();
		
		if (vonTime == null || bisTime == null) {
			JOptionPane.showMessageDialog(this, "Bitte geben Sie ein Start - und ein Enddatum ein!", "Fehlerhafte Eingabe", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		java.sql.Date von = new java.sql.Date((vonTime).getTime());
		java.sql.Date bis = new java.sql.Date((bisTime).getTime());
		
		kontroll.setInterval(von, bis);

		ergebnis.zeigen();

		Thread t = new Thread(kontroll);
		t.start();

	}

}
