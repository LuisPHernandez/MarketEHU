package gui;

import businesslogic.BLFacade;
import businesslogic.BLFactory;
import domain.Seller;

public class AdapterMain {
    public static void main(String[] args) {
        boolean isLocal = true;
        BLFacade blFacade = new BLFactory().getBusinessLogicFactory(isLocal);
        Seller s = blFacade.isLogged("Aitor Fernandez", "aurrera");
        SellerTable st = new SellerTable(s); // el enunciado pone "SallerTable": es una errata
        st.setVisible(true);
    }
}
