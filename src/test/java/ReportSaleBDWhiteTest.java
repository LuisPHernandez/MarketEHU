import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dataAccess.DataAccess;
import domain.Salaketa;
import domain.Sale;
import testOperations.TestDataAccess;

public class ReportSaleBDWhiteTest {
	// Sistema bajo prueba (al crearlo, la BD se reinicia con los datos iniciales)
	private static DataAccess sut = new DataAccess();

	// Operaciones auxiliares para preparar y limpiar la BD
	private static TestDataAccess testDA = new TestDataAccess();

	// Objetos que crean los tests en la BD
	private static final String OWNER_EMAIL = "vendedor@ehu.eus";
	private static final String DEFAULT_USER_EMAIL = "lhernandez@ehu.eus";
	private static final Integer DEFAULT_SALE_NUMBER = 100;

	// Parámetros del caso
	private String userEmail;
	private Integer saleNumber;
	private String reason;

	@Before
	public void setUp() {
		// Valores por defecto de los parámetros (caso 5, camino que llega al return true)
		userEmail = DEFAULT_USER_EMAIL;
		saleNumber = DEFAULT_SALE_NUMBER;
		reason = "motivo";

		// Estado por defecto de la BD: vendedor ∈ BD, u ∈ BD y s ∈ BD
		testDA.open();
		testDA.createSeller(OWNER_EMAIL, "Vendedor Test", "123");
		testDA.createSeller(DEFAULT_USER_EMAIL, "Seller Test", "123");
		testDA.createSale(OWNER_EMAIL, "Balón", "balón de fútbol", 2, 10, new Date(), DEFAULT_SALE_NUMBER);
		testDA.close();
	}

	@After
	public void tearDown() {
		// Restaurar la BD: eliminar lo creado (el vendedor se elimina con su venta y sus denuncias)
		testDA.open();
		testDA.removeSeller(DEFAULT_USER_EMAIL);
		testDA.removeSeller(OWNER_EMAIL);
		testDA.close();
	}
	

	@Test 
	// Caso 1, Camino 1-try2(T)-15-20-End
	// Condición: db.find(...) lanza una excepción (saleNumber == null)
	// u ∈ BD, s ∉ BD (no puede existir una venta con saleNumber null); userEmail="lhernandez@ehu.eus", saleNumber=null, reason="motivo"
	// Resultado esperado: false y la BD no cambia
	public void test1() {
		saleNumber = null;
		testDA.open();
		long salaketakBefore = testDA.countSalaketak();
		testDA.close();

		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertFalse(result);

		// Estado de la BD: no se ha añadido ninguna denuncia
		testDA.open();
		long salaketakAfter = testDA.countSalaketak();
		testDA.close();
		assertEquals(salaketakBefore, salaketakAfter);
	}

	@Test
	// Caso 2, Camino 1-try2(F)-3-5-if6.1(T)-7-9-End
	// Condición: la venta no existe (sale == null)
	// u ∈ BD, s ∉ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason="motivo"
	// Resultado esperado: false y la BD no cambia
	public void test2() {
		// Preparar: la venta 100 no está en la BD
		testDA.open();
		testDA.removeSale(DEFAULT_SALE_NUMBER);
		long salaketakBefore = testDA.countSalaketak();
		testDA.close();

		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertFalse(result);

		// Estado de la BD: no se ha añadido ninguna denuncia
		testDA.open();
		long salaketakAfter = testDA.countSalaketak();
		testDA.close();
		assertEquals(salaketakBefore, salaketakAfter);
	}

	@Test
	// Caso 3, Camino 1-try2(F)-3-5-if6.1(F)-if6.2(T)-7-9-End
	// Condición: la venta existe y el motivo es null (sale != null && reason == null)
	// u ∈ BD, s ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason=null
	// Resultado esperado: false y la BD no cambia
	public void test3() {
		reason = null;

		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertFalse(result);

		// Estado de la BD: la venta no tiene denuncias
		testDA.open();
		Sale sale = testDA.getSale(DEFAULT_SALE_NUMBER);
		testDA.close();
		assertTrue(sale.getSalaketak().isEmpty());
	}

	@Test
	// Caso 4, Camino 1-try2(F)-3-5-if6.1(F)-if6.2(F)-if6.3(T)-7-9-End
	// Condición: la venta existe, el motivo no es null y es vacío (sale != null && reason != null && reason.isEmpty())
	// u ∈ BD, s ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason=""
	// Resultado esperado: false y la BD no cambia
	public void test4() {
		reason = "";

		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertFalse(result);

		// Estado de la BD: la venta no tiene denuncias
		testDA.open();
		Sale sale = testDA.getSale(DEFAULT_SALE_NUMBER);
		testDA.close();
		assertTrue(sale.getSalaketak().isEmpty());
	}

	@Test
	// Caso 5, Camino 1-try2(F)-3-5-if6.1(F)-if6.2(F)-if6.3(F)-10-14-End
	// Condición: la venta existe y el motivo no es null ni vacío (sale != null && reason != null && !reason.isEmpty())
	// u ∈ BD, s ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason="motivo"
	// Resultado esperado: true y se asocian el motivo y el email del usuario a la venta
	public void test5() {
		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertTrue(result);

		// Estado de la BD: la venta tiene 1 denuncia con los datos enviados
		testDA.open();
		Sale sale = testDA.getSale(DEFAULT_SALE_NUMBER);
		testDA.close();
		assertEquals(1, sale.getSalaketak().size());
		Salaketa denuncia = sale.getSalaketak().get(0);
		assertEquals(reason, denuncia.getReason());
		assertEquals(userEmail, denuncia.getUserEmail());
	}
}
