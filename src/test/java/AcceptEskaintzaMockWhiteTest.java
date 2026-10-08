

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import dataAccess.DataAccess;
import domain.Bidalketa;
import domain.Eskaera;
import domain.Eskaintza;
import domain.Mugimenduak;
import domain.Sale;
import domain.Seller;

public class AcceptEskaintzaMockWhiteTest {
	
	private EntityManager db;
	private EntityTransaction transaction;
	private DataAccess dataAccess;
	
	@Before
	public void setUp() {
		db  = mock(EntityManager.class);
		transaction = mock(EntityTransaction.class);
		
		when(db.getTransaction()).thenReturn(transaction);
		
		dataAccess = new DataAccess(db);
	}
	
	//si el pedido no existe debe devolver false.
	@Test
	public void pedidoNoExistente() {
		Eskaintza oferta = new Eskaintza();
		
		when(db.find(Eskaera.class, 999)).thenReturn(null);
		when(db.find(Eskaintza.class,10 )).thenReturn(oferta);
		
		boolean resultado = dataAccess.acceptEskaintza(999,10); 
		
		//comprobaciones
		assertFalse(resultado);
		
		verify(transaction).begin();
		verify(transaction).rollback();
		verify(transaction, never()).commit();
		
		verify(db, never()).persist(any());
		verify(db, never()).merge(any());
	}
	
	@Test
	public void ofertaNoExiste() {
		Eskaera pedido = new Eskaera();
		
		when(db.find(Eskaera.class,1)).thenReturn(pedido);
		when(db.find(Eskaintza.class, 999)).thenReturn(null);
		
		boolean resultado = dataAccess.acceptEskaintza(1, 999);
		
		assertFalse(resultado);
		
		verify(transaction).begin();
		verify(transaction).rollback();
		verify(transaction, never()).commit();
		
		verify(db, never()).persist(any());
		verify(db, never()).merge(any());
	}
	
	@Test
	public void saldoInsuficiente() {
		Seller buyer = new Seller("buyer@test.com", "Comprador","123"); 
		buyer.setMoney(90f);
		
		Seller seller = new Seller("seller@test.com","Vendedor","123");
		
		Eskaera pedido = new Eskaera("Guitarra", "Busco una Guitarra", buyer);
		
		Eskaintza oferta = new Eskaintza(100f, "Vendo Pachon", seller, pedido);
		
		
		when(db.find(Eskaera.class, 1)).thenReturn(pedido);
		when(db.find(Eskaintza.class, 10)).thenReturn(oferta);
		
		boolean resultado = dataAccess.acceptEskaintza(1,10);
		
		assertFalse(resultado);
		assertFalse(pedido.isClosed());
		assertEquals(90f, buyer.getMoney(), 0.001f);
		
		assertTrue(buyer.getPurchasedSales().isEmpty());
		assertTrue(seller.getSales().isEmpty());
		
		verify(transaction).begin();
		verify(transaction).rollback();
		verify(transaction, never()).commit();
		
		verify(db, never()).persist(any());
		verify(db, never()).merge(any());
	}x	
	
	@Test
	public void pedidoCerrado() {
		Eskaera pedido = new Eskaera();
		pedido.setClosed(true);
		
		Eskaintza oferta = new Eskaintza();
		
		when(db.find(Eskaera.class, 1)).thenReturn(pedido);
		when(db.find(Eskaintza.class, 10)).thenReturn(oferta);
		
		boolean resultado = dataAccess.acceptEskaintza(1,10);
		
		 // Comprobaciones.
	    assertFalse(resultado);
	    assertTrue(pedido.isClosed());

	    verify(transaction).begin();
	    verify(transaction).rollback();
	    verify(transaction, never()).commit();

	    verify(db, never()).persist(any());
	    verify(db, never()).merge(any());
		
		 
		
	}
	
	@Test
	public void compraCorrecta() {
		Seller buyer = new Seller("buyer@test.com", "Comprador", "123");
		buyer.setMoney(150f);
		
		Seller seller = new Seller("seller@test.com", "Vendedor", "123");
		
		Eskaera pedido = new Eskaera("Bicicleta","Busco una bicicleta", buyer);
		
		Eskaintza oferta = new Eskaintza(100f,"Vendo bicicleta",seller,pedido);
		
		when(db.find(Eskaera.class, 1)).thenReturn(pedido);
		when(db.find(Eskaintza.class, 10)).thenReturn(oferta);
		
		boolean resultado = dataAccess.acceptEskaintza(1, 10);

		assertTrue(resultado);
		assertEquals(50f, buyer.getMoney(), 0.001f);
		assertTrue(pedido.isClosed());

		assertEquals(1, buyer.getPurchasedSales().size());
		assertEquals(1, seller.getSales().size());
		
		Sale venta = seller.getSales().get(0);

		assertSame(venta, buyer.getPurchasedSales().get(0));
		assertSame(buyer, venta.getBuyer());
		assertSame(seller, venta.getSeller());

		assertEquals("[Eskaera] Bicicleta", venta.getTitle());
		assertEquals("Vendo bicicleta", venta.getDescription());
		assertEquals(100f, venta.getPrice(), 0.001f);

		Bidalketa envio = venta.getBidalketa();

		assertNotNull(envio);
		assertSame(venta, envio.getSale());
		assertEquals("PRESTATZEN", envio.getEgoera());
		
		ArgumentCaptor<Object> captor =
		        ArgumentCaptor.forClass(Object.class);

		// Capturamos los tres objetos enviados a persist().
		verify(db, times(3)).persist(captor.capture());

		Mugimenduak movimiento = null;

		// Buscamos cuál de ellos es el movimiento.
		for (Object objeto : captor.getAllValues()) {
		    if (objeto instanceof Mugimenduak) {
		        movimiento = (Mugimenduak) objeto;
		    }
		}
		
		//comprobaciones

		assertNotNull(movimiento);

		assertEquals("ESKAINTZA_ORDAINKETA", movimiento.getMota());
		assertSame(buyer, movimiento.getSeller());
		assertSame(pedido, movimiento.getEskaera());
		assertSame(oferta, movimiento.getEskaintza());
		assertNotNull(movimiento.getData());
		
		
		verify(db).persist(venta);
		verify(db).persist(envio);
		verify(db, times(3)).persist(any());

		verify(db).merge(buyer);
		verify(db).merge(seller);
		verify(db).merge(pedido);

		verify(transaction).begin();
		verify(transaction).commit();
		verify(transaction, never()).rollback();
	}
	
	@Test
	public void excepcionEnBusqueda() {
	    when(db.find(Eskaera.class, 1))
	            .thenThrow(new RuntimeException("Fallo simulado de BD"));

	
	    boolean resultado = dataAccess.acceptEskaintza(1, 10);

	    // Comprobaciones
	    assertFalse(resultado);

	    verify(transaction).begin();
	    verify(transaction).rollback();
	    verify(transaction, never()).commit();

	    verify(db, never()).persist(any());
	    verify(db, never()).merge(any());
	}
	
	
}
