package net.java.visualvm.modules.glassfish.ui;

import java.util.Vector;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class StatsTable extends JTable {

    private String selectedRowRef = null;
    {
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public StatsTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
    }

    public StatsTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
    }

    public StatsTable(int numRows, int numColumns) {
        super(numRows, numColumns);
    }

    public StatsTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
    }

    public StatsTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
    }

    public StatsTable(TableModel dm) {
        super(dm);
    }

    public StatsTable() {
        super();
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        selectedRowRef = getSelectedRowID();
        super.tableChanged(e);
        setSelectedRowByID(selectedRowRef);
    }

    private String getSelectedRowID() {
        int rc = getRowCount();
        int sr = getSelectedRow();
        if (getRowCount() == 0 || getSelectedRow() <= 0) {
            return null;
        }

        return getValueAt(getSelectedRow(), 0).toString();
    }

    private void setSelectedRowByID(String id) {
        if (id == null) {
            return;
        }
        for (int i = 0; i < getRowCount(); i++) {
            if (getValueAt(i, 0).equals(id)) {
                setRowSelectionInterval(i, i);
                return;
            }
        }
    }
}
