package edu.ualberta.med.biobank.common.wrappers;

import edu.ualberta.med.biobank.common.wrappers.base.AliquotedSpecimenBaseWrapper;
import edu.ualberta.med.biobank.model.AliquotedSpecimen;
import gov.nih.nci.system.applicationservice.WritableApplicationService;

public class AliquotedSpecimenWrapper extends AliquotedSpecimenBaseWrapper {

    public AliquotedSpecimenWrapper(WritableApplicationService appService,
        AliquotedSpecimen wrappedObject) {
        super(appService, wrappedObject);
    }

    public AliquotedSpecimenWrapper(WritableApplicationService appService) {
        super(appService);
    }

    @Override
    public int compareTo(ModelWrapper<AliquotedSpecimen> wrapper) {
        if (wrapper instanceof AliquotedSpecimenWrapper) {
            String name1 = wrappedObject.getSpecimenType().getName();
            String name2 = wrapper.wrappedObject.getSpecimenType().getName();
            if (name1 != null && name2 != null) {
                return name1.compareTo(name2);
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return new StringBuilder(getSpecimenType().getName()).append("/") //$NON-NLS-1$
            .append(getQuantity()).append("/").append(getActivityStatus()) //$NON-NLS-1$
            .toString();
    }
}
