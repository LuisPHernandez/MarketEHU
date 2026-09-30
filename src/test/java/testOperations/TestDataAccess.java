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
}
