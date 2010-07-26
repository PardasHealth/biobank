package edu.ualberta.med.biobank.server.logging.logger;

import java.util.Date;
import java.util.Map;

import edu.ualberta.med.biobank.model.Log;
import edu.ualberta.med.biobank.model.Shipment;
import edu.ualberta.med.biobank.model.Study;

public class ShipmentStateLogger extends BiobankObjectStateLogger {

    protected ShipmentStateLogger() {
    }

    @Override
    protected Log getLogObject(Object obj, Map<String, Object> statesMap) {
        if (obj instanceof Shipment) {
            Log log = new Log();
            String details = "";
            Date dateReceived = (Date) statesMap.get("dateReceived");
            Study study = (Study) statesMap.get("study");
            log.setSite(study.getSite().getNameShort());
            if (dateReceived != null) {
                details = "Received:" + dateTimeFormatter.format(dateReceived);
            }
            String waybill = (String) statesMap.get("waybill");
            if (waybill != null) {
                details += " - Waybill:" + waybill;
            }
            log.setDetails(details);
            log.setType("Shipment");
            return log;
        }
        return null;
    }

}
