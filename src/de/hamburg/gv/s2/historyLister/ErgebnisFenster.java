package de.hamburg.gv.s2.historyLister;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ErgebnisFenster extends JDialog implements HistoryStringListener {
	private static final long serialVersionUID = 1L;
	private JTextArea jta;
	private JProgressBar jpg;
	
	
	public ErgebnisFenster(JFrame owner) {
		super(owner);
		Container cp = this.getContentPane();
		cp.setLayout(new BorderLayout());
		jpg = new JProgressBar();
		jta = new JTextArea();
		JScrollPane jsp = new JScrollPane(jta);
		cp.add(jpg, BorderLayout.NORTH);
		cp.add(jsp, BorderLayout.CENTER);
		
		this.setPreferredSize(new Dimension(800, 400));
		this.pack();
	}
	
	
	public void zeigen() {
		this.setVisible(true);
	}

	@Override
	public void showTextLine(String zeile) {
		jta.append(zeile);
		jta.setCaretPosition(jta.getText().length());
	}

	@Override
	public void complete(String komplett) {
		jta.setText(komplett);
	}


	@Override
	public void clean() {
		jta.setText("");
	}


	@Override
	public void setRowCount(int rowCount) {
		jpg.setMaximum(rowCount);
	}


	@Override
	public void setRow(int row) {
		jpg.setValue(row);
	}
}
