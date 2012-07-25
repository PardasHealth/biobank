package edu.ualberta.med.biobank.permission.specimenType;

import edu.ualberta.med.biobank.action.ActionContext;
import edu.ualberta.med.biobank.permission.Permission;
import edu.ualberta.med.biobank.model.PermissionEnum;

public class SpecimenTypeUpdatePermission implements Permission {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean isAllowed(ActionContext context) {
        return PermissionEnum.SPECIMEN_TYPE_UPDATE.isAllowed(context.getUser());
    }
}