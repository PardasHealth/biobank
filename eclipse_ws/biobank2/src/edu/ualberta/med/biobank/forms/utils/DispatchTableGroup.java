package edu.ualberta.med.biobank.forms.utils;

import java.util.ArrayList;
import java.util.List;

import edu.ualberta.med.biobank.common.wrappers.DispatchAliquotWrapper;
import edu.ualberta.med.biobank.common.wrappers.DispatchWrapper;

public enum DispatchTableGroup {
    ADDED("Added") {
        @Override
        public List<DispatchAliquotWrapper> getChildren(DispatchWrapper shipment) {
            return shipment.getNonProcessedDispatchAliquotCollection();
        }
    },
    NON_PROCESSED("Non Processed") {
        @Override
        public List<DispatchAliquotWrapper> getChildren(DispatchWrapper shipment) {
            return shipment.getNonProcessedDispatchAliquotCollection();
        }
    },
    RECEIVED("Received") {
        @Override
        public List<DispatchAliquotWrapper> getChildren(DispatchWrapper shipment) {
            return shipment.getReceivedDispatchAliquots();
        }
    },
    EXTRA("Extra") {
        @Override
        public List<DispatchAliquotWrapper> getChildren(DispatchWrapper shipment) {
            return shipment.getExtraDispatchAliquots();
        }
    },
    MISSING("Missing") {
        @Override
        public List<DispatchAliquotWrapper> getChildren(DispatchWrapper shipment) {
            return shipment.getMissingDispatchAliquots();
        }
    };

    private String label;

    private DispatchTableGroup(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public String getTitle(DispatchWrapper ship) {
        return label + " (" + getChildren(ship).size() + ")";
    }

    public abstract List<DispatchAliquotWrapper> getChildren(
        DispatchWrapper shipment);

    public static Object findParent(DispatchAliquotWrapper dsa) {
        for (DispatchTableGroup tg : values()) {
            if (tg.getChildren(dsa.getDispatch()).contains(dsa)) {
                return tg;
            }
        }
        return null;
    }

    public static List<DispatchTableGroup> getGroupsForShipment(
        DispatchWrapper ship) {
        List<DispatchTableGroup> groups = new ArrayList<DispatchTableGroup>();
        if (ship.isInCreationState()) {
            groups.add(ADDED);
        } else {
            groups.add(NON_PROCESSED);
        }
        if (ship.hasBeenReceived()) {
            groups.add(RECEIVED);
            groups.add(EXTRA);
        }
        if (ship.hasBeenReceived() || ship.isInTransitState()) {
            groups.add(MISSING);
        }
        return groups;
    }
}