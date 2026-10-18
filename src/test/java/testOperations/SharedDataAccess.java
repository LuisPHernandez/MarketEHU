package testOperations;

import dataAccess.DataAccess;


// DataAccess compartido por todas las clases de test con BD.
// Se crea una sola vez por ejecución, de modo que la BD se reinicia una única vez.
public class SharedDataAccess {
	private static final DataAccess INSTANCE = new DataAccess();

	private SharedDataAccess() {
	}

	public static DataAccess get() {
		return INSTANCE;
	}
}
