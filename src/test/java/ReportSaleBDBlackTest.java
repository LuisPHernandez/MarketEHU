import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dataAccess.DataAccess;
import domain.Salaketa;
import domain.Sale;
import testOperations.TestDataAccess;

public class ReportSaleBDBlackTest {
	// Sistema bajo prueba
	private static DataAccess sut = new DataAccess();

	// Operaciones auxiliares
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
		// Valores por defecto de los parámetros (caso válido)
		userEmail = DEFAULT_USER_EMAIL;
		saleNumber = DEFAULT_SALE_NUMBER;
		reason = "motivo";

		// Estado por defecto de la BD, vendedor ∈ BD, usuario ∈ BD y venta 100 ∈ BD
		testDA.open();
		testDA.createSeller(OWNER_EMAIL, "Vendedor Test", "123");
		testDA.createSeller(DEFAULT_USER_EMAIL, "Seller Test", "123");
		testDA.createSale(OWNER_EMAIL, "Balón", "balón de fútbol", 2, 10, new Date(), DEFAULT_SALE_NUMBER);
		testDA.close();
	}

	@After
	public void tearDown() {
		// Restaurar la BD a estado inicial
		testDA.open();
		if (userEmail != null && !userEmail.equals(DEFAULT_USER_EMAIL)) {
			testDA.removeSeller(userEmail);
		}
		testDA.removeSeller(DEFAULT_USER_EMAIL);
		testDA.removeSeller(OWNER_EMAIL);
		testDA.removeSalaketak(1);
		testDA.removeSalaketak(2);
		testDA.close();
	}

	@Test
	// Caso 1, Clases 1, 3, 5, 7, 9, 11, 13, 15 (todas las válidas)
	// usuario ∈ BD, venta ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100,
	// reason="motivo"
	// Resultado esperado: true y la denuncia queda añadida a la venta
	public void test1() {
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

	@Test
	// Caso 2, Clase 2 (userEmail == null)
	// usuario ∉ BD (no puede existir un Seller con email null), venta ∈ BD;
	// userEmail=null, saleNumber=100, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test2() {
		userEmail = null;

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
	// Caso 3, Clase 4 (reason == null)
	// usuario ∈ BD, venta ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100,
	// reason=null
	// Resultado esperado: false y la venta no cambia
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
	// Caso 4, Clase 6 (saleNumber == null)
	// usuario ∈ BD, venta ∉ BD (no puede existir una venta con saleNumber null);
	// userEmail="lhernandez@ehu.eus", saleNumber=null, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test4() {
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
	// Caso 5, Clase 8 (userEmail con formato incorrecto)
	// usuario ∈ BD (email "hola"), venta ∈ BD; userEmail="hola", saleNumber=100,
	// reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test5() {
		userEmail = "hola";
		testDA.open();
		testDA.createSeller(userEmail, "Seller Test", "123");
		testDA.close();

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
	// Caso 5.1, Valor límite de la clase 8: TLD de 1 letra
	// usuario ∈ BD (email "a@ehu.e"), venta ∈ BD; userEmail="a@ehu.e",
	// saleNumber=100, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test5_1() {
		userEmail = "a@ehu.e";
		testDA.open();
		testDA.createSeller(userEmail, "Seller Test", "123");
		testDA.close();

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
	// Caso 5.2, Valor límite de la clase 7: TLD de 2 letras
	// usuario ∈ BD (email "a@ehu.eu"), venta ∈ BD; userEmail="a@ehu.eu",
	// saleNumber=100, reason="motivo"
	// Resultado esperado: true y la denuncia queda añadida a la venta
	public void test5_2() {
		userEmail = "a@ehu.eu";
		testDA.open();
		testDA.createSeller(userEmail, "Seller Test", "123");
		testDA.close();

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

	@Test
	// Caso 5.3, Valor límite de la clase 8: ninguna @
	// usuario ∈ BD (email "a.eus"), venta ∈ BD; userEmail="a.eus", saleNumber=100,
	// reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test5_3() {
		userEmail = "a.eus";
		testDA.open();
		testDA.createSeller(userEmail, "Seller Test", "123");
		testDA.close();

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
	// Caso 5.4, Valor límite de la clase 8: dos @
	// usuario ∈ BD (email "a@@ehu.eus"), venta ∈ BD; userEmail="a@@ehu.eus",
	// saleNumber=100, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test5_4() {
		userEmail = "a@@ehu.eus";
		testDA.open();
		testDA.createSeller(userEmail, "Seller Test", "123");
		testDA.close();

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
	// Caso 5.5, Valor límite de la clase 7: una @ y TLD de 3 letras
	// usuario ∈ BD (email "a@ehu.eus"), venta ∈ BD; userEmail="a@ehu.eus",
	// saleNumber=100, reason="motivo"
	// Resultado esperado: true y la denuncia queda añadida a la venta
	public void test5_5() {
		userEmail = "a@ehu.eus";
		testDA.open();
		testDA.createSeller(userEmail, "Seller Test", "123");
		testDA.close();

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

	@Test
	// Caso 6, Clase 10 (reason vacío)
	// usuario ∈ BD, venta ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100,
	// reason=""
	// Resultado esperado: false y la venta no cambia
	public void test6() {
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
	// Caso 6.1, Valor límite de la clase 9: reason de longitud 1
	// usuario ∈ BD, venta ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100,
	// reason="a"
	// Resultado esperado: true y la denuncia queda añadida a la venta
	public void test6_1() {
		reason = "a";

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

	@Test
	// Caso 6.2, Valor límite de la clase 9: reason de longitud 2
	// usuario ∈ BD, venta ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100,
	// reason="aa"
	// Resultado esperado: true y la denuncia queda añadida a la venta
	public void test6_2() {
		reason = "aa";

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

	@Test
	// Caso 7, Clase 12 (saleNumber <= 0)
	// usuario ∈ BD, venta ∈ BD (la venta -10); userEmail="lhernandez@ehu.eus",
	// saleNumber=-10, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test7() {
		saleNumber = -10;
		testDA.open();
		testDA.createSale(OWNER_EMAIL, "Raqueta", "raqueta de tenis", 2, 20, new Date(), saleNumber);
		testDA.close();

		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertFalse(result);

		// Estado de la BD: la venta -10 no tiene denuncias
		testDA.open();
		Sale sale = testDA.getSale(saleNumber);
		testDA.close();
		assertTrue(sale.getSalaketak().isEmpty());
	}

	@Test
	// Caso 7.1, Valor límite de la clase 12: saleNumber = 0
	// usuario ∈ BD, venta ∉ BD (no puede existir una venta con saleNumber 0);
	// userEmail="lhernandez@ehu.eus", saleNumber=0, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test7_1() {
		// En la BD no puede existir una Sale con saleNumber = 0: ObjectDB interpreta
		// el 0 como "sin número asignado" y genera uno
		saleNumber = 0;
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
	// Caso 7.2, Valor límite de la clase 11: saleNumber = 1
	// usuario ∈ BD, venta ∈ BD (la venta 1 de los datos iniciales);
	// userEmail="lhernandez@ehu.eus", saleNumber=1, reason="motivo"
	// Resultado esperado: true y la denuncia queda añadida a la venta 1
	public void test7_2() {
		saleNumber = 1;

		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertTrue(result);

		// Estado de la BD: la venta 1 tiene 1 denuncia con los datos enviados
		testDA.open();
		Sale sale = testDA.getSale(saleNumber);
		testDA.close();
		assertEquals(1, sale.getSalaketak().size());
		Salaketa denuncia = sale.getSalaketak().get(0);
		assertEquals(reason, denuncia.getReason());
		assertEquals(userEmail, denuncia.getUserEmail());
	}

	@Test
	// Caso 7.3, Valor límite de la clase 11: saleNumber = 2
	// usuario ∈ BD, venta ∈ BD (la venta 2 de los datos iniciales);
	// userEmail="lhernandez@ehu.eus", saleNumber=2, reason="motivo"
	// Resultado esperado: true y la denuncia queda añadida a la venta 2
	public void test7_3() {
		saleNumber = 2;

		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertTrue(result);

		// Estado de la BD: la venta 2 tiene 1 denuncia con los datos enviados
		testDA.open();
		Sale sale = testDA.getSale(saleNumber);
		testDA.close();
		assertEquals(1, sale.getSalaketak().size());
		Salaketa denuncia = sale.getSalaketak().get(0);
		assertEquals(reason, denuncia.getReason());
		assertEquals(userEmail, denuncia.getUserEmail());
	}

	@Test
	// Caso 8, Clase 14 (venta ∉ BD)
	// usuario ∈ BD, venta ∉ BD; userEmail="lhernandez@ehu.eus", saleNumber=100,
	// reason="motivo"
	// Resultado esperado: false y la BD no cambia
	public void test8() {
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
	// Caso 9, Clase 16 (usuario ∉ BD)
	// usuario ∉ BD, venta ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100,
	// reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test9() {
		// Preparar: el usuario que denuncia no está en la BD
		testDA.open();
		testDA.removeSeller(DEFAULT_USER_EMAIL);
		testDA.close();

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
}
