package gui;

import java.awt.Color;
import java.net.URL;
import java.util.Locale;

import javax.swing.UIManager;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import configuration.ConfigXML;
import dataAccess.DataAccess;
import businesslogic.BLFacade;
import businesslogic.BLFacadeImplementation;
import businesslogic.BLFactory;

public class ApplicationLauncher {

	public static void main(String[] args) {

		ConfigXML c = ConfigXML.getInstance();
		Locale.setDefault(new Locale(c.getLocale()));

		MainGUI a = new MainGUI("seller3@gmail.com");
		a.setVisible(true);

		try {

			UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");

			BLFacade appFacadeInterface = new BLFactory().getBusinessLogicFactory(c.isBusinessLogicLocal());

			MainGUI.setBussinessLogic(appFacadeInterface);
			// ErreklamatuGUI g=new ErreklamatuGUI();
			// g.setVisible(true);

		} catch (Exception e) {
			a.jLabelSelectOption.setText("Error: " + e.toString());
			a.jLabelSelectOption.setForeground(Color.RED);

			System.out.println("Error in ApplicationLauncher: " + e.toString());
		}

	}

}