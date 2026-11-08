package gui;

import java.util.List;
import javax.swing.table.AbstractTableModel;
import domain.Sale;
import domain.Seller;

public class SellerAdapter extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    private final List<Sale> sales;
    // datos del Adaptee (Seller)
    private final String[] columnNames = { "Title", "description", "Price" };

    public SellerAdapter(Seller selller){
        this.sales = selller.getSales(); // se guarda al seller  y todas sus sales
    }

    // 1 fila por cada sale
    @Override
    public int getRowCount() {
        return sales.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        Sale s = sales.get(row);
        switch (col) {
            case 0:
                return s.getTitle();
            case 1:
                return s.getDescription();
            case 2:
                return s.getPrice();
            default:
                return null;
        }
    }
}