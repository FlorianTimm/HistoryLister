package de.hamburg.gv.s2.historyLister;

public interface HistoryStringListener {
	public void showTextLine(String zeile);
	public void complete(String komplett);
	public void clean();
	public void setRowCount(int rowCount);
	public void setRow(int row);
}
