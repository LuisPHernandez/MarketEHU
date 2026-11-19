package web.bean;

import businesslogic.BLFacade;
import businesslogic.BLFactory;
import configuration.ConfigXML;

/**
 * Punto único de acceso a la lógica de negocio desde la capa web.
 * Todos los beans comparten la misma instancia de BLFacade.
 */
public final class FacadeBean {

    private static BLFacade businessLogic;

    private FacadeBean() {
    }

    public static synchronized BLFacade getBusinessLogic() {
        if (businessLogic == null) {
            ConfigXML c = ConfigXML.getInstance();
            businessLogic = new BLFactory().getBusinessLogicFactory(c.isBusinessLogicLocal());
        }
        return businessLogic;
    }
}
