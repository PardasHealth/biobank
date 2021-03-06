package edu.ualberta.med.biobank.common.permission.clinic;

import edu.ualberta.med.biobank.common.action.ActionContext;
import edu.ualberta.med.biobank.common.permission.Permission;
import edu.ualberta.med.biobank.model.PermissionEnum;

public class ClinicCreatePermission implements Permission {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean isAllowed(ActionContext context) {
        return PermissionEnum.CLINIC_CREATE.isAllowed(context.getUser());
    }

}