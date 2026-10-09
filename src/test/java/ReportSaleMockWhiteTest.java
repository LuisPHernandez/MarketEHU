import static org.junit.Assert.*;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import dataAccess.DataAccess;
import domain.Salaketa;
import domain.Sale;
import domain.Seller;

public class ReportSaleMockWhiteTest {
	// Sistema bajo prueba
	private DataAccess sut;

	// Simulación de la BD
	private MockedStatic<Persistence> persistenceMock;
	@Mock
	private EntityManagerFactory entityManagerFactory;
	@Mock
	private EntityManager db;
	@Mock
	private EntityTransaction et;

	// Datos del contexto
	private String userEmail;
	private Integer saleNumber;
	private String reason;
	private Seller seller;
	private Seller owner;
	private Sale sale;

	@Before
	public void setUp() {
		// Crear los mocks
		MockitoAnnotations.openMocks(this);
		persistenceMock = Mockito.mockStatic(Persistence.class);
		persistenceMock.when(
		    () -> Persistence.createEntityManagerFactory(Mockito.any())
		).thenReturn(entityManagerFactory);
		Mockito.when(entityManagerFactory.createEntityManager())
		       .thenReturn(db);
		Mockito.when(db.getTransaction())
		       .thenReturn(et);

		// Crear el SUT con la BD simulada
		sut = new DataAccess(db);

		// Valores por defecto de los parámetros (caso 5, camino que llega al return true)
		userEmail = "lhernandez@ehu.eus";
		saleNumber = 100;
		reason = "motivo";

		// Objetos en memoria: el usuario que denuncia, el vendedor y su venta
		seller = new Seller(userEmail, "Seller Test", "123");
		owner = new Seller("vendedor@ehu.eus", "Vendedor Test", "123");
		sale = owner.addSale("Balón", "balón de fútbol", 2, 10, new Date(), null);
		sale.setSaleNumber(saleNumber);

		// Estado por defecto de la BD: usuario ∈ BD, vendedor ∈ BD y venta ∈ BD
		Mockito.when(db.find(Seller.class, userEmail)).thenReturn(seller);
		Mockito.when(db.find(Seller.class, owner.getEmail())).thenReturn(owner);
		Mockito.when(db.find(Sale.class, saleNumber)).thenReturn(sale);
	}

	@After
	public void tearDown() {
		persistenceMock.close();
	}
	
	// @Test
	// // Caso 1, Camino 1-try2(T)-15-20-End
	// // Condición: db.find(...) lanza una excepción (saleNumber == null)
	// // u ∈ BD, s ∉ BD (no puede existir una venta con saleNumber null); userEmail="lhernandez@ehu.eus", saleNumber=null, reason="motivo"
	// // Resultado esperado: false y la BD no cambia
	// public void test1() {
	// 	// La BD real lanza esta excepción al buscar con una clave null
	// 	saleNumber = null;
	// 	Mockito.when(db.find(Sale.class, saleNumber))
	// 	       .thenThrow(new IllegalArgumentException("Unexpected null argument"));

	// 	// Llamar al sistema bajo prueba
	// 	sut.open();
	// 	boolean result = sut.reportSale(userEmail, saleNumber, reason);
	// 	sut.close();

	// 	// Salida
	// 	assertFalse(result);

	// 	// Estado de la BD: no se ha añadido ninguna denuncia (la única venta de la BD no tiene denuncias)
	// 	assertTrue(sale.getSalaketak().isEmpty());
	// }

	@Test
	// Caso 2, Camino 1-try2(F)-3-5-if6.1(T)-7-9-End
	// Condición: la venta no existe (sale == null)
	// u ∈ BD, s ∉ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason="motivo"
	// Resultado esperado: false y la BD no cambia
	public void test2() {
		Mockito.when(db.find(Sale.class, saleNumber)).thenReturn(null);

		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertFalse(result);

		// Estado de la BD: no se ha añadido ninguna denuncia (la única venta de la BD no tiene denuncias)
		assertTrue(sale.getSalaketak().isEmpty());
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
		assertTrue(sale.getSalaketak().isEmpty());
	}

	@Test
	// Caso 5, Camino 1-try2(F)-3-5-if6.1(F)-if6.2(F)-if6.3(F)-10-14-End
	// Condición: la venta existe y el motivo no es null ni vacío (sale != null && reason != null && !reason.isEmpty())
	// u ∈ BD, s ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason="motivo"
	// Resultado esperado: true y se asocian el motivo y el email del usuario a la venta
	public void test5() {
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertTrue(result);

		// Estado de la BD: la venta tiene 1 denuncia con los datos enviados
		assertEquals(1, sale.getSalaketak().size());
		Salaketa denuncia = sale.getSalaketak().get(0);
		assertEquals(reason, denuncia.getReason());
		assertEquals(userEmail, denuncia.getUserEmail());
	}

}
