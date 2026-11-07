package businesslogic;

import java.util.List;
import java.util.NoSuchElementException;

import domain.Sale;

/**
 * Iterador extendido sobre una lista de ventas.
 */
public class ExtendedIteratorSales implements ExtendedIterator<Sale> {
	private List<Sale> sales;
	private int position;

	public ExtendedIteratorSales(List<Sale> sales) {
		this.sales = sales;
		this.position = 0;
	}

	public boolean hasNext() {
		return position < sales.size();
	}

	public Sale next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return sales.get(position++);
	}

	public boolean hasPrevious() {
		return position >= 0;
	}

	public Sale previous() {
		if (!hasPrevious()) {
			throw new NoSuchElementException();
		}
		return sales.get(position--);
	}

	public void goFirst() {
		position = 0;
	}

	public void goLast() {
		position = sales.size() - 1;
	}
}
