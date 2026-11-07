package businesslogic;

import java.util.Iterator;

/**
 * Iterador extendido que además de recorrer hacia delante, permite
 * recorrer hacia atrás y posicionarse en el primer o el último elemento.
 */
public interface ExtendedIterator<T> extends Iterator<T> {
	// return the actual element and go to the previous
	public T previous();

	// true if there is a previous element
	public boolean hasPrevious();

	// It is placed in the first element
	public void goFirst();

	// It is placed in the last element
	public void goLast();
}
