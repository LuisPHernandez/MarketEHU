package businesslogic;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import configuration.ConfigXML;
import dataAccess.DataAccess;

/**
 * Factoría que centraliza la obtención del objeto de la lógica de negocio.
 */
public class BLFactory {

    /**
     * Crea la implementación de la lógica de negocio indicada.
     *
     * @param isLocal true para un objeto local, false para un objeto remoto
     * @return el objeto de la lógica de negocio
     */
    public BLFacade getBusinessLogicFactory(boolean isLocal) {
        if (isLocal) {
            return createLocalBusinessLogic();
        }
        return createRemoteBusinessLogic();
    }

    private BLFacade createLocalBusinessLogic() {
        DataAccess da = new DataAccess();
        return new BLFacadeImplementation(da);
    }

    private BLFacade createRemoteBusinessLogic() {
        ConfigXML c = ConfigXML.getInstance();
        String serviceName = "http://" + c.getBusinessLogicNode() + ":" + c.getBusinessLogicPort() + "/ws/"
                + c.getBusinessLogicName() + "?wsdl";

        try {
            URL url = new URL(serviceName);

            QName qname = new QName("http://businessLogic/", "BLFacadeImplementationService");

            Service service = Service.create(url, qname);
            return service.getPort(BLFacade.class);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("URL del servicio incorrecta: " + serviceName, e);
        }
    }
}
