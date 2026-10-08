package testOperations;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import configuration.ConfigXML;
import domain.Salaketa;
import domain.Sale;
import domain.Seller;
import domain.Eskaera;
import domain.Eskaintza;
import domain.Mugimenduak;
import domain.Bidalketa;

public class TestDataAccess {
	protected EntityManager db;
	protected EntityManagerFactory emf;

	ConfigXML c = ConfigXML.getInstance();

	public TestDataAccess() {
		System.out.println("TestDataAccess created");
	}
	
	// Abre una sesión con la misma BD que usa DataAccess (config.xml).
	public void open() {
		String fileName = c.getDbFilename();
		if (c.isDatabaseLocal()) {
			emf = Persistence.createEntityManagerFactory("objectdb:" + fileName);
		} else {
			Map<String, String> properties = new HashMap<String, String>();
			properties.put("javax.persistence.jdbc.user", c.getUser());
			properties.put("javax.persistence.jdbc.password", c.getPassword());
			emf = Persistence.createEntityManagerFactory(
					"objectdb://" + c.getDatabaseNode() + ":" + c.getDatabasePort() + "/" + fileName, properties);
		}
		db = emf.createEntityManager();
		System.out.println("TestDataAccess opened");
	}
	
	// Cierra la sesión para liberar la BD antes de que la use DataAccess.
	public void close() {
		db.close();
		emf.close();
		System.out.println("TestDataAccess closed");
	}
	
	// Guarda un Seller.
	public Seller createSeller(String email, String name, String pass) {
		db.getTransaction().begin();
		Seller seller = new Seller(email, name, pass);
		db.persist(seller);
		db.getTransaction().commit();
		return seller;
	}
	
	// Crea una venta del Seller ownerEmail (siempre debe existir en la BD).
	// Si saleNumber no es null, se fija ese número, si es null, lo genera la BD.
	public Sale createSale(String ownerEmail, String title, String description, int status, float price,
			Date pubDate, Integer saleNumber) {
		db.getTransaction().begin();
		Seller owner = db.find(Seller.class, ownerEmail);
		Sale sale = owner.addSale(title, description, status, price, pubDate, null);
		if (saleNumber != null) {
			sale.setSaleNumber(saleNumber);
		}
		db.persist(sale);
		db.getTransaction().commit();
		return sale;
	}
	
	// Devuelve la venta de la BD, o null si no existe.
	public Sale getSale(Integer saleNumber) {
		return db.find(Sale.class, saleNumber);
	}

	// Elimina todas las denuncias de una venta.
	public void removeSalaketak(Integer saleNumber) {
		Sale sale = db.find(Sale.class, saleNumber);
		if (sale != null) {
			db.getTransaction().begin();
			for (Salaketa salaketa : new ArrayList<Salaketa>(sale.getSalaketak())) {
				sale.removeSalaketa(salaketa);
				db.remove(salaketa);
			}
			db.getTransaction().commit();
		}
	}
	
	// Elimina una venta, sus denuncias y la referencia desde su vendedor.
	public void removeSale(Integer saleNumber) {
		Sale sale = db.find(Sale.class, saleNumber);
		if (sale != null) {
			db.getTransaction().begin();
			for (Salaketa salaketa : new ArrayList<Salaketa>(sale.getSalaketak())) {
				sale.removeSalaketa(salaketa);
				db.remove(salaketa);
			}
			if (sale.getSeller() != null) {
				sale.getSeller().removeSale(sale);
			}
			db.remove(sale);
			db.getTransaction().commit();
		}
	}
	
	// Elimina un Seller y sus ventas.
	public void removeSeller(String email) {
		Seller seller = db.find(Seller.class, email);
		if (seller != null) {
			for (Sale sale : new ArrayList<Sale>(seller.getSales())) {
				removeSale(sale.getSaleNumber());
			}
			db.getTransaction().begin();
			db.remove(seller);
			db.getTransaction().commit();
		}
	}
	
	// Devuelve el número total de denuncias guardadas en la BD.
	public long countSalaketak() {
		return db.createQuery("SELECT COUNT(s) FROM Salaketa s", Long.class).getSingleResult();
	}
	
	//prepara la oferta en bd 
	public Integer[] prepararEskaintza(
	        String buyerEmail,
	        String sellerEmail,
	        float saldo,
	        float precio) {

	    db.getTransaction().begin();

	    try {
	        Seller buyer = new Seller(buyerEmail, "Comprador Test", "123");
	        buyer.setMoney(saldo);

	        Seller seller = new Seller(sellerEmail, "Vendedor Test", "123");

	        Eskaera pedido = new Eskaera(
	                "Bicicleta", "Busco una bicicleta", buyer);

	        Eskaintza oferta = new Eskaintza(
	                precio, "Vendo bicicleta", seller, pedido);

	        pedido.addEskaintza(oferta);

	        db.persist(buyer);
	        db.persist(seller);
	        db.persist(pedido);
	        db.persist(oferta);

	        db.getTransaction().commit();

	        return new Integer[] {
	            pedido.getId(),
	            oferta.getId()
	        };

	    } catch (RuntimeException e) {
	        if (db.getTransaction().isActive()) {
	            db.getTransaction().rollback();
	        }
	        throw e;
	    }
	}
	public void limpiarEskaintza(
	        Integer eskaeraId,
	        Integer eskaintzaId,
	        String buyerEmail,
	        String sellerEmail) {

	    // Si la preparación falló, no tenemos un escenario que limpiar.
	    if (eskaeraId == null || eskaintzaId == null) {
	        return;
	    }

	    db.getTransaction().begin();

	    try {
	        Eskaera pedido = db.find(Eskaera.class, eskaeraId);
	        Eskaintza oferta = db.find(Eskaintza.class, eskaintzaId);
	        Seller buyer = db.find(Seller.class, buyerEmail);
	        Seller seller = db.find(Seller.class, sellerEmail);

	        // Eliminar los movimientos asociados al pedido.
	        if (pedido != null) {
	            for (Mugimenduak movimiento : db.createQuery(
	                    "SELECT m FROM Mugimenduak m WHERE m.eskaera = :pedido",
	                    Mugimenduak.class)
	                    .setParameter("pedido", pedido)
	                    .getResultList()) {

	                if (movimiento.getSeller() != null) {
	                    movimiento.getSeller().getMovements().remove(movimiento);
	                }

	                db.remove(movimiento);
	            }
	        }

	        //  Eliminar las ventas del vendedor exclusivo de esta prueba.
	        if (seller != null) {
	            for (Sale venta : new ArrayList<Sale>(seller.getSales())) {

	                if (venta.getBuyer() != null) {
	                    venta.getBuyer().getPurchasedSales().remove(venta);
	                }

	                seller.removeSale(venta);

	                // Sale tiene cascade=ALL hacia Bidalketa:
	                // al eliminar la venta también se elimina el envío.
	                db.remove(venta);
	            }
	        }

	        // Desvincular y eliminar la oferta.
	        if (pedido != null) {
	            pedido.getEskaintzak().clear();
	        }

	        if (oferta != null) {
	            oferta.setEskaera(null);
	            db.remove(oferta);
	        }

	        // Eliminar el pedido.
	        if (pedido != null) {
	            db.remove(pedido);
	        }

	        // Eliminar los usuarios exclusivos de la prueba.
	        if (buyer != null) {
	            db.remove(buyer);
	        }

	        if (seller != null) {
	            db.remove(seller);
	        }

	        db.getTransaction().commit();

	    } catch (RuntimeException e) {
	        if (db.getTransaction().isActive()) {
	            db.getTransaction().rollback();
	        }
	        throw e;
	    }
	}
	
	public void setPedidoCerrado(Integer id, boolean cerrado) {
	    db.getTransaction().begin();

	    try {
	        Eskaera pedido = db.find(Eskaera.class, id);

	        if (pedido == null) {
	            throw new IllegalArgumentException("El pedido no existe");
	        }

	        pedido.setClosed(cerrado);
	        db.getTransaction().commit();

	    } catch (RuntimeException e) {
	        if (db.getTransaction().isActive()) {
	            db.getTransaction().rollback();
	        }
	        throw e;
	    }
	}

	public Eskaera getEskaera(Integer id) {
	    return db.find(Eskaera.class, id);
	}

	public Seller getSeller(String email) {
	    return db.find(Seller.class, email);
	}

	public long countMovimientosEskaera(Integer id) {
	    return db.createQuery(
	            "SELECT COUNT(m) FROM Mugimenduak m WHERE m.eskaera.id = :id",
	            Long.class)
	            .setParameter("id", id)
	            .getSingleResult();
	}
	public void setSaldo(String email, float saldo) {
	    db.getTransaction().begin();

	    try {
	        Seller seller = db.find(Seller.class, email);

	        if (seller == null) {
	            throw new IllegalArgumentException("El usuario no existe");
	        }

	        seller.setMoney(saldo);
	        db.getTransaction().commit();

	    } catch (RuntimeException e) {
	        if (db.getTransaction().isActive()) {
	            db.getTransaction().rollback();
	        }
	        throw e;
	    }
	}
	public java.util.List<Mugimenduak> getMovimientosEskaera(Integer id) {
	    return db.createQuery(
	            "SELECT m FROM Mugimenduak m WHERE m.eskaera.id = :id",
	            Mugimenduak.class)
	            .setParameter("id", id)
	            .getResultList();
	}
	
	public Integer getIdPedidoInexistente() {
	    int id = -1;

	    while (db.find(Eskaera.class, id) != null) {
	        id--;
	    }

	    return id;
	}
	
	public Integer getIdOfertaInexistente() {
	    int id = -1;

	    while (db.find(Eskaintza.class, id) != null) {
	        id--;
	    }

	    return id;
	}

	public Eskaintza getEskaintza(Integer id) {
	    return db.find(Eskaintza.class, id);
	}

}
