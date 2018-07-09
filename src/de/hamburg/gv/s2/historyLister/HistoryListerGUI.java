package de.hamburg.gv.s2.historyLister;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.swing.GroupLayout;
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

		JLabel startLabel = new JLabel("Startdatum:");
		Date vonDate = new Date(System.currentTimeMillis() - 3600 * 1000 * 24 * 7);
		vonPicker = new JDatePicker(vonDate);
		JLabel endLabel = new JLabel("Enddatum:");
		Date bisDate = new Date(System.currentTimeMillis());
		bisPicker = new JDatePicker(bisDate);
		JButton b = new JButton("Generieren");
		b.addActionListener(this);

		GroupLayout layout = new GroupLayout(cp);
		cp.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(startLabel)
						.addComponent(endLabel))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(vonPicker)
						.addComponent(bisPicker).addComponent(b)));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(startLabel)
						.addComponent(vonPicker))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(endLabel)
						.addComponent(bisPicker))
				.addComponent(b));

		//this.setPreferredSize(new Dimension(300, 200));
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
			JTextField usernameField = new JTextField(15);
			usernameField.setText("SYSADM5");
			JTextField databaseField = new JTextField(15);
			databaseField.setText("10.62.42.25:6543/verklhhp");

			JPasswordField passwordField = new JPasswordField(15);

			JPanel loginPanel = new JPanel();
			JLabel dbLabel = new JLabel("Datenbank");
			JLabel userLabel = new JLabel("Benutzer");
			JLabel pwLabel = new JLabel("Passwort");

			GroupLayout layout = new GroupLayout(loginPanel);
			loginPanel.setLayout(layout);
			layout.setAutoCreateGaps(true);
			layout.setAutoCreateContainerGaps(true);

			layout.setHorizontalGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(dbLabel)
							.addComponent(userLabel).addComponent(pwLabel))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(databaseField)
							.addComponent(usernameField).addComponent(passwordField)));

			layout.setVerticalGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(dbLabel)
							.addComponent(databaseField))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(userLabel)
							.addComponent(usernameField))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(pwLabel)
							.addComponent(passwordField)));

			int result = JOptionPane.showConfirmDialog(null, loginPanel, "TTSIB-Login", JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				String user = usernameField.getText();
				String passwort = new String(passwordField.getPassword());
				String database = databaseField.getText();

				login = kontroll.connect(database, user, passwort);

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
			JOptionPane.showMessageDialog(this, "Bitte geben Sie ein Start - und ein Enddatum ein!",
					"Fehlerhafte Eingabe", JOptionPane.ERROR_MESSAGE);
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
