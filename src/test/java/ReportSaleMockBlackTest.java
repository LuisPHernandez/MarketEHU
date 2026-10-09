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

public class ReportSaleMockBlackTest {
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
		
		// Valores por defecto de los parámetros (caso válido)
		userEmail = "lhernandez@ehu.eus";
		saleNumber = 100;
		reason = "motivo";
		
		// Objetos en memoria, el usuario que denuncia, el vendedor y su venta
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

	@Test
	// Caso 1, Clases 1, 3, 5, 7, 9, 11, 13, 15 (todas las válidas)
	// usuario ∈ BD, venta ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason="motivo"
	// Resultado esperado: true y la denuncia queda añadida a la venta
	public void test1() {
		// Llamar al sistema bajo prueba
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
	
	@Test
	// Caso 2, Clase 2 (userEmail == null)
	// usuario ∉ BD (no puede existir un Seller con email null), venta ∈ BD; userEmail=null, saleNumber=100, reason="motivo"
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
		assertTrue(sale.getSalaketak().isEmpty());
	}
	
	@Test
	// Caso 3, Clase 4 (reason == null)
	// usuario ∈ BD, venta ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason=null
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
		assertTrue(sale.getSalaketak().isEmpty());
	}
	
	@Test
	// Caso 4, Clase 6 (saleNumber == null)
	// usuario ∈ BD, venta ∉ BD (no puede existir una venta con saleNumber null); userEmail="lhernandez@ehu.eus", saleNumber=null, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test4() {
		saleNumber = null;
		
		// La BD real lanza esta excepción al buscar con una clave null
		Mockito.when(db.find(Sale.class, saleNumber))
		       .thenThrow(new IllegalArgumentException("Unexpected null argument"));

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
	// Caso 5, Clase 8 (userEmail con formato incorrecto)
	// usuario ∈ BD (email "hola"), venta ∈ BD; userEmail="hola", saleNumber=100, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test5() {
		userEmail = "hola";
		Mockito.when(db.find(Seller.class, userEmail)).thenReturn(new Seller(userEmail, "Seller Test", "123"));

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
	// Caso 5.1, Valor límite de la clase 8: TLD de 1 letra
	// usuario ∈ BD (email "a@ehu.e"), venta ∈ BD; userEmail="a@ehu.e", saleNumber=100, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test5_1() {
		userEmail = "a@ehu.e";
		Mockito.when(db.find(Seller.class, userEmail)).thenReturn(new Seller(userEmail, "Seller Test", "123"));

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
	// Caso 5.2, Valor límite de la clase 7: TLD de 2 letras
	// usuario ∈ BD (email "a@ehu.eu"), venta ∈ BD; userEmail="a@ehu.eu", saleNumber=100, reason="motivo"
	// Resultado esperado: true y la denuncia queda añadida a la venta
	public void test5_2() {
		userEmail = "a@ehu.eu";
		Mockito.when(db.find(Seller.class, userEmail)).thenReturn(new Seller(userEmail, "Seller Test", "123"));

		// Llamar al sistema bajo prueba
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

	@Test
	// Caso 5.3, Valor límite de la clase 8: ninguna @
	// usuario ∈ BD (email "a.eus"), venta ∈ BD; userEmail="a.eus", saleNumber=100, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test5_3() {
		userEmail = "a.eus";
		Mockito.when(db.find(Seller.class, userEmail)).thenReturn(new Seller(userEmail, "Seller Test", "123"));

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
	// Caso 5.4, Valor límite de la clase 8: dos @
	// usuario ∈ BD (email "a@@ehu.eus"), venta ∈ BD; userEmail="a@@ehu.eus", saleNumber=100, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test5_4() {
		userEmail = "a@@ehu.eus";
		Mockito.when(db.find(Seller.class, userEmail)).thenReturn(new Seller(userEmail, "Seller Test", "123"));

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
	// Caso 5.5, Valor límite de la clase 7: una @ y TLD de 3 letras
	// usuario ∈ BD (email "a@ehu.eus"), venta ∈ BD; userEmail="a@ehu.eus", saleNumber=100, reason="motivo"
	// Resultado esperado: true y la denuncia queda añadida a la venta
	public void test5_5() {
		userEmail = "a@ehu.eus";
		Mockito.when(db.find(Seller.class, userEmail)).thenReturn(new Seller(userEmail, "Seller Test", "123"));

		// Llamar al sistema bajo prueba
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

	
	@Test
	// Caso 6, Clase 10 (reason vacío)
	// usuario ∈ BD, venta ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason=""
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
		assertTrue(sale.getSalaketak().isEmpty());
	}
	
	@Test
	// Caso 6.1, Valor límite de la clase 9: reason de longitud 1
	// usuario ∈ BD, venta ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason="a"
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
		assertEquals(1, sale.getSalaketak().size());
		Salaketa denuncia = sale.getSalaketak().get(0);
		assertEquals(reason, denuncia.getReason());
		assertEquals(userEmail, denuncia.getUserEmail());
	}

	@Test
	// Caso 6.2, Valor límite de la clase 9: reason de longitud 2
	// usuario ∈ BD, venta ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason="aa"
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
		assertEquals(1, sale.getSalaketak().size());
		Salaketa denuncia = sale.getSalaketak().get(0);
		assertEquals(reason, denuncia.getReason());
		assertEquals(userEmail, denuncia.getUserEmail());
	}
	
	@Test
	// Caso 7, Clase 12 (saleNumber <= 0)
	// usuario ∈ BD, venta ∈ BD (la venta -10); userEmail="lhernandez@ehu.eus", saleNumber=-10, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test7() {
		saleNumber = -10;
		Sale otherSale = owner.addSale("Raqueta", "raqueta de tenis", 2, 20, new Date(), null);
		otherSale.setSaleNumber(saleNumber);
		Mockito.when(db.find(Sale.class, saleNumber)).thenReturn(otherSale);

		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertFalse(result);

		// Estado de la BD: la venta -10 no tiene denuncias
		assertTrue(otherSale.getSalaketak().isEmpty());
	}

	@Test
	// Caso 7.1, Valor límite de la clase 12: saleNumber = 0
	// usuario ∈ BD, venta ∉ BD (no puede existir una venta con saleNumber 0); userEmail="lhernandez@ehu.eus", saleNumber=0, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test7_1() {
		// En la BD real no puede existir una Sale con saleNumber = 0, ObjectDB interpreta
		// el 0 como "sin número asignado" y genera uno
		saleNumber = 0;

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
	// Caso 7.2, Valor límite de la clase 11: saleNumber = 1
	// usuario ∈ BD, venta ∈ BD (también la venta 1); userEmail="lhernandez@ehu.eus", saleNumber=1, reason="motivo"
	// Resultado esperado: true y la denuncia queda añadida a la venta 1
	public void test7_2() {
		saleNumber = 1;
		Sale otherSale = owner.addSale("Raqueta", "raqueta de tenis", 2, 20, new Date(), null);
		otherSale.setSaleNumber(saleNumber);
		Mockito.when(db.find(Sale.class, saleNumber)).thenReturn(otherSale);

		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertTrue(result);

		// Estado de la BD: la venta 1 tiene 1 denuncia con los datos enviados
		assertEquals(1, otherSale.getSalaketak().size());
		Salaketa denuncia = otherSale.getSalaketak().get(0);
		assertEquals(reason, denuncia.getReason());
		assertEquals(userEmail, denuncia.getUserEmail());
	}

	@Test
	// Caso 7.3, Valor límite de la clase 11: saleNumber = 2
	// usuario ∈ BD, venta ∈ BD (también la venta 2); userEmail="lhernandez@ehu.eus", saleNumber=2, reason="motivo"
	// Resultado esperado: true y la denuncia queda añadida a la venta 2
	public void test7_3() {
		saleNumber = 2;
		Sale otherSale = owner.addSale("Raqueta", "raqueta de tenis", 2, 20, new Date(), null);
		otherSale.setSaleNumber(saleNumber);
		Mockito.when(db.find(Sale.class, saleNumber)).thenReturn(otherSale);

		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertTrue(result);

		// Estado de la BD: la venta 2 tiene 1 denuncia con los datos enviados
		assertEquals(1, otherSale.getSalaketak().size());
		Salaketa denuncia = otherSale.getSalaketak().get(0);
		assertEquals(reason, denuncia.getReason());
		assertEquals(userEmail, denuncia.getUserEmail());
	}
	
	@Test
	// Caso 8, Clase 14 (venta ∉ BD)
	// usuario ∈ BD, venta ∉ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test8() {
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
	// Caso 9, Clase 16 (usuario ∉ BD)
	// usuario ∉ BD, venta ∈ BD; userEmail="lhernandez@ehu.eus", saleNumber=100, reason="motivo"
	// Resultado esperado: false y la venta no cambia
	public void test9() {
		Mockito.when(db.find(Seller.class, userEmail)).thenReturn(null);

		// Llamar al sistema bajo prueba
		sut.open();
		boolean result = sut.reportSale(userEmail, saleNumber, reason);
		sut.close();

		// Salida
		assertFalse(result);

		// Estado de la BD: la venta no tiene denuncias
		assertTrue(sale.getSalaketak().isEmpty());
	}
}
