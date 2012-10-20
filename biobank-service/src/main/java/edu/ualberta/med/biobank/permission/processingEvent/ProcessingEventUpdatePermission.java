package edu.ualberta.med.biobank.permission.processingEvent;

import edu.ualberta.med.biobank.action.ActionContext;
import edu.ualberta.med.biobank.permission.Permission;
import edu.ualberta.med.biobank.model.center.ProcessingEvent;
import edu.ualberta.med.biobank.model.type.PermissionEnum;

public class ProcessingEventUpdatePermission implements Permission {
    private static final long serialVersionUID = 1L;
    private Integer peId;

    public ProcessingEventUpdatePermission(Integer peId) {
        this.peId = peId;
    }

    @Override
    public boolean isAllowed(ActionContext context) {
        ProcessingEvent pe = context.load(ProcessingEvent.class, peId);
        return PermissionEnum.PROCESSING_EVENT_UPDATE
            .isAllowed(context.getUser(), pe.getCenter());
    }
}
