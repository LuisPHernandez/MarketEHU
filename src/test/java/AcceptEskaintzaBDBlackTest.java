import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dataAccess.DataAccess;
import domain.Bidalketa;
import domain.Eskaera;
import domain.Eskaintza;
import domain.Mugimenduak;
import domain.Sale;
import domain.Seller;
import testOperations.TestDataAccess;

public class AcceptEskaintzaBDBlackTest {
    // Sistema bajo prueba.
    private static DataAccess sut = new DataAccess();

    // Operaciones auxiliares para preparar, consultar y limpiar la BD.
    private static TestDataAccess testDA = new TestDataAccess();

    // Usuarios creados exclusivamente para estas pruebas.
    private static final String SELLER_EMAIL = "seller@test.com";
    private static final String DEFAULT_USER_EMAIL = "buyer@test.com";

    // Valores iniciales del caso válido.
    private static final float DEFAULT_BALANCE = 150f;
    private static final float DEFAULT_PRICE = 100f;

    // IDs generados al guardar el pedido y la oferta.
    private Integer eskaeraId;
    private Integer eskaintzaId;
      
    @Before
    public void setUp() {
        testDA.open();

        try {
            Integer[] ids = testDA.prepararEskaintza(
                    DEFAULT_USER_EMAIL,
                    SELLER_EMAIL,
                    DEFAULT_BALANCE,
                    DEFAULT_PRICE);

            eskaeraId = ids[0];
            eskaintzaId = ids[1];

        } finally {
            testDA.close();
        }
    }
    
    @After
    public void tearDown() {
        if (eskaeraId == null || eskaintzaId == null) {
            return;
        }

        testDA.open();

        try {
            testDA.limpiarEskaintza(
                    eskaeraId,
                    eskaintzaId,
                    DEFAULT_USER_EMAIL,
                    SELLER_EMAIL);
        } finally {
            testDA.close();
        }
    }
    
    @Test
    public void pedidoCerrado() {
        // PREPARAR
        testDA.open();
        try {
            testDA.setPedidoCerrado(eskaeraId, true);
        } finally {
            testDA.close();
        }

        //sobre la BD 
        boolean resultado;

        sut.open();
        try {
            resultado = sut.acceptEskaintza(eskaeraId, eskaintzaId);
        } finally {
            sut.close();
        }

        // verificar salida FALSE
        assertFalse(resultado);

        //comprobar el estado guardado en una nueva sesión.
        testDA.open();
        try {
            Eskaera pedido = testDA.getEskaera(eskaeraId);
            Seller buyer = testDA.getSeller(DEFAULT_USER_EMAIL);
            Seller seller = testDA.getSeller(SELLER_EMAIL);

            assertNotNull(pedido);
            assertNotNull(buyer);
            assertNotNull(seller);

            assertTrue(pedido.isClosed());
            assertEquals(DEFAULT_BALANCE, buyer.getMoney(), 0.001f);
            assertEquals(0f, seller.getMoney(), 0.001f);

            assertTrue(buyer.getPurchasedSales().isEmpty());
            assertTrue(seller.getSales().isEmpty());

            assertEquals(0L, testDA.countMovimientosEskaera(eskaeraId));

        } finally {
            testDA.close();
        }
    }
    
    
    @Test
    public void saldoInsuficiente() {
    	testDA.open();
    	try {
    		testDA.setSaldo(DEFAULT_USER_EMAIL,90f);
    	}finally {
    		testDA.close();
    	}
    	
    	boolean resultado;
    	
    	sut.open();
    	try{
    		resultado = sut.acceptEskaintza(eskaeraId,eskaintzaId);
    	}finally {
    		sut.close();
    	}
    	//verificamos que no fuera aceptada
    	assertFalse(resultado);
    	
    	  testDA.open();
          try {
              Eskaera pedido = testDA.getEskaera(eskaeraId);
              Seller buyer = testDA.getSeller(DEFAULT_USER_EMAIL);
              Seller seller = testDA.getSeller(SELLER_EMAIL);

              assertNotNull(pedido);
              assertNotNull(buyer);
              assertNotNull(seller);

              assertFalse(pedido.isClosed());
              assertEquals(90f, buyer.getMoney(), 0.001f);
              assertEquals(0f, seller.getMoney(), 0.001f);

              assertTrue(buyer.getPurchasedSales().isEmpty());
              assertTrue(seller.getSales().isEmpty());

              assertEquals(0L, testDA.countMovimientosEskaera(eskaeraId));

          } finally {
              testDA.close();
       }
    }
    @Test
    public void compraCorrecta() {
        //el @Before ya dejó el escenario válido.
     
        boolean resultado;

        sut.open();
        try {
            resultado = sut.acceptEskaintza(eskaeraId, eskaintzaId);
        } finally {
            sut.close();
        }

        assertTrue(resultado);

        // 
        testDA.open();
        try {
            Eskaera pedido = testDA.getEskaera(eskaeraId);
            Seller buyer = testDA.getSeller(DEFAULT_USER_EMAIL);
            Seller seller = testDA.getSeller(SELLER_EMAIL);

            assertNotNull(pedido);
            assertNotNull(buyer);
            assertNotNull(seller);

            // Saldo y estado del pedido.
            assertEquals(50f, buyer.getMoney(), 0.001f);
            assertTrue(pedido.isClosed());

            // El vendedor todavía no recibe el dinero en este método.
            assertEquals(0f, seller.getMoney(), 0.001f);

            // Venta y relaciones.
            assertEquals(1, seller.getSales().size());
            assertEquals(1, buyer.getPurchasedSales().size());

            Sale venta = seller.getSales().get(0);

            assertNotNull(venta.getSaleNumber());
            assertEquals(
                    venta.getSaleNumber(),
                    buyer.getPurchasedSales().get(0).getSaleNumber());

            assertNotNull(venta.getBuyer());
            assertNotNull(venta.getSeller());
            assertEquals(DEFAULT_USER_EMAIL, venta.getBuyer().getEmail());
            assertEquals(SELLER_EMAIL, venta.getSeller().getEmail());

            assertEquals("[Eskaera] Bicicleta", venta.getTitle());
            assertEquals("Vendo bicicleta", venta.getDescription());
            assertEquals(DEFAULT_PRICE, venta.getPrice(), 0.001f);
            assertEquals(1, venta.getStatus());
            assertNotNull(venta.getPubDate());

            // Envío.
            Bidalketa envio = venta.getBidalketa();

            assertNotNull(envio);
            assertNotNull(envio.getId());
            assertEquals("PRESTATZEN", envio.getEgoera());
            assertNotNull(envio.getSale());
            assertEquals(
                    venta.getSaleNumber(),
                    envio.getSale().getSaleNumber());

            // Movimiento de pago.
            java.util.List<Mugimenduak> movimientos =
                    testDA.getMovimientosEskaera(eskaeraId);

            assertEquals(1, movimientos.size());

            Mugimenduak movimiento = movimientos.get(0);

            assertEquals("ESKAINTZA_ORDAINKETA", movimiento.getMota());
            assertNotNull(movimiento.getData());
            assertNotNull(movimiento.getSeller());
            assertNotNull(movimiento.getEskaera());
            assertNotNull(movimiento.getEskaintza());

            assertEquals(
                    DEFAULT_USER_EMAIL,
                    movimiento.getSeller().getEmail());
            assertEquals(eskaeraId, movimiento.getEskaera().getId());
            assertEquals(eskaintzaId, movimiento.getEskaintza().getId());

        } finally {
            testDA.close();
        }
    }
    @Test
    public void pedidoNoExiste() {
        Integer idInexistente;

        //obtener un ID que no existe.
        testDA.open();
        try {
            idInexistente = testDA.getIdPedidoInexistente();
            assertNull(testDA.getEskaera(idInexistente));
        } finally {
            testDA.close();
        }

        //pedido inexistente y oferta existente.
        boolean resultado;

        sut.open();
        try {
            resultado = sut.acceptEskaintza(idInexistente, eskaintzaId);
        } finally {
            sut.close();
        }

        assertFalse(resultado);

        //los datos preparados permanecen sin cambios.
        testDA.open();
        try {
            assertNull(testDA.getEskaera(idInexistente));

            Eskaera pedido = testDA.getEskaera(eskaeraId);
            Seller buyer = testDA.getSeller(DEFAULT_USER_EMAIL);
            Seller seller = testDA.getSeller(SELLER_EMAIL);

            assertNotNull(pedido);
            assertNotNull(buyer);
            assertNotNull(seller);

            assertFalse(pedido.isClosed());
            assertEquals(DEFAULT_BALANCE, buyer.getMoney(), 0.001f);
            assertEquals(0f, seller.getMoney(), 0.001f);

            assertTrue(buyer.getPurchasedSales().isEmpty());
            assertTrue(seller.getSales().isEmpty());

            assertEquals(0L, testDA.countMovimientosEskaera(eskaeraId));

        } finally {
            testDA.close();
        }
    }
    
    @Test
    public void ofertaNoExiste() {
        Integer idInexistente;

        //obtener un ID que no existe.
        testDA.open();
        try {
            idInexistente = testDA.getIdOfertaInexistente();
            assertNull(testDA.getEskaintza(idInexistente));
        } finally {
            testDA.close();
        }

        //oferta inexistente y pedidov existente.
        boolean resultado;

        sut.open();
        try {
            resultado = sut.acceptEskaintza(eskaeraId, idInexistente);
        } finally {
            sut.close();
        }

        assertFalse(resultado);

        //los datos preparados permanecen sin cambios.
        testDA.open();
        try {
            assertNull(testDA.getEskaintza(idInexistente));

            Eskaera pedido = testDA.getEskaera(eskaeraId);
            Seller buyer = testDA.getSeller(DEFAULT_USER_EMAIL);
            Seller seller = testDA.getSeller(SELLER_EMAIL);

            assertNotNull(pedido);
            assertNotNull(buyer);
            assertNotNull(seller);

            assertFalse(pedido.isClosed());
            assertEquals(DEFAULT_BALANCE, buyer.getMoney(), 0.001f);
            assertEquals(0f, seller.getMoney(), 0.001f);

            assertTrue(buyer.getPurchasedSales().isEmpty());
            assertTrue(seller.getSales().isEmpty());

            assertEquals(0L, testDA.countMovimientosEskaera(eskaeraId));

        } finally {
            testDA.close();
        }
    }
    
    @Test
    public void pedidoIdNulo() {
        boolean resultado;

        // buscar con una clave nula provoca la excepción.
        sut.open();
        try {
            resultado = sut.acceptEskaintza(null, eskaintzaId);
        } finally {
            sut.close();
        }

        // El método debe capturar la excepción y devolver false.
        assertFalse(resultado);

        //verificar que los datos preparados no han cambiado.
        testDA.open();
        try {
            Eskaera pedido = testDA.getEskaera(eskaeraId);
            Seller buyer = testDA.getSeller(DEFAULT_USER_EMAIL);
            Seller seller = testDA.getSeller(SELLER_EMAIL);

            assertNotNull(pedido);
            assertNotNull(buyer);
            assertNotNull(seller);

            assertFalse(pedido.isClosed());
            assertEquals(DEFAULT_BALANCE, buyer.getMoney(), 0.001f);
            assertEquals(0f, seller.getMoney(), 0.001f);

            assertTrue(buyer.getPurchasedSales().isEmpty());
            assertTrue(seller.getSales().isEmpty());

            assertEquals(0L, testDA.countMovimientosEskaera(eskaeraId));

        } finally {
            testDA.close();
        }
    }
    
    @Test
    public void ofertaIdNulo() {
        boolean resultado;

        // Ejecutar con pedido existente e ID de oferta nulo.
        sut.open();
        try {
            resultado = sut.acceptEskaintza(eskaeraId, null);
        } finally {
            sut.close();
        }

        assertFalse(resultado);

        // Consultar de nuevo la BD para comprobar que no hay cambios.
        testDA.open();
        try {
            Eskaera pedido = testDA.getEskaera(eskaeraId);
            Eskaintza oferta = testDA.getEskaintza(eskaintzaId);
            Seller buyer = testDA.getSeller(DEFAULT_USER_EMAIL);
            Seller seller = testDA.getSeller(SELLER_EMAIL);

            assertNotNull(pedido);
            assertNotNull(oferta);
            assertNotNull(buyer);
            assertNotNull(seller);

            assertFalse(pedido.isClosed());
            assertEquals(DEFAULT_BALANCE, buyer.getMoney(), 0.001f);
            assertEquals(DEFAULT_PRICE, oferta.getPrice(), 0.001f);
            assertEquals(0f, seller.getMoney(), 0.001f);

            assertTrue(buyer.getPurchasedSales().isEmpty());
            assertTrue(seller.getSales().isEmpty());

            assertEquals(0L, testDA.countMovimientosEskaera(eskaeraId));

        } finally {
            testDA.close();
        }
    }
    
    //pruebas valores limites
    
    @Test
    public void saldoJustoPorDebajo() {
    	testDA.open();
    	try {
    		testDA.setSaldo(DEFAULT_USER_EMAIL,99.99f);
    	}finally {
    		testDA.close();
    	}
    	
    	boolean resultado;
    	
    	sut.open();
    	try{
    		resultado = sut.acceptEskaintza(eskaeraId,eskaintzaId);
    	}finally {
    		sut.close();
    	}
    	//verificamos que no fuera aceptada
    	assertFalse(resultado);
    	
    	  testDA.open();
          try {
              Eskaera pedido = testDA.getEskaera(eskaeraId);
              Seller buyer = testDA.getSeller(DEFAULT_USER_EMAIL);
              Seller seller = testDA.getSeller(SELLER_EMAIL);

              assertNotNull(pedido);
              assertNotNull(buyer);
              assertNotNull(seller);

              assertFalse(pedido.isClosed());
              assertEquals(99.99f, buyer.getMoney(), 0.001f);
              assertEquals(0f, seller.getMoney(), 0.001f);

              assertTrue(buyer.getPurchasedSales().isEmpty());
              assertTrue(seller.getSales().isEmpty());

              assertEquals(0L, testDA.countMovimientosEskaera(eskaeraId));

          } finally {
              testDA.close();
       }
    }
    
    @Test
    public void saldoExactoAlPrecio() {
        testDA.open();
        try {
            testDA.setSaldo(DEFAULT_USER_EMAIL, 100f);
        } finally {
            testDA.close();
        }

        boolean resultado;

        sut.open();
        try {
            resultado = sut.acceptEskaintza(eskaeraId, eskaintzaId);
        } finally {
            sut.close();
        }

        assertTrue(resultado);

        testDA.open();
        try {
            Seller buyer = testDA.getSeller(DEFAULT_USER_EMAIL);
            Eskaera pedido = testDA.getEskaera(eskaeraId);

            assertNotNull(buyer);
            assertNotNull(pedido);
            assertEquals(0f, buyer.getMoney(), 0.001f);
            assertTrue(pedido.isClosed());
        } finally {
            testDA.close();
        }
    }
    
    @Test
    public void saldoJustoPorEncima() {
        testDA.open();
        try {
            testDA.setSaldo(DEFAULT_USER_EMAIL, 100.01f);
        } finally {
            testDA.close();
        }

        boolean resultado;

        sut.open();
        try {
            resultado = sut.acceptEskaintza(eskaeraId, eskaintzaId);
        } finally {
            sut.close();
        }

        assertTrue(resultado);

        testDA.open();
        try {
            Seller buyer = testDA.getSeller(DEFAULT_USER_EMAIL);
            Eskaera pedido = testDA.getEskaera(eskaeraId);

            assertNotNull(buyer);
            assertNotNull(pedido);
            assertEquals(0.01f, buyer.getMoney(), 0.001f);
            assertTrue(pedido.isClosed());
        } finally {
            testDA.close();
        }
    }
    
}
