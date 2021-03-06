package edu.ualberta.med.biobank.common.wrappers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.ualberta.med.biobank.common.wrappers.base.RoleBaseWrapper;
import edu.ualberta.med.biobank.model.PermissionEnum;
import edu.ualberta.med.biobank.model.Role;
import edu.ualberta.med.biobank.server.applicationservice.BiobankApplicationService;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class RoleWrapper extends RoleBaseWrapper {

    public RoleWrapper(WritableApplicationService appService) {
        super(appService);
    }

    public RoleWrapper(WritableApplicationService appService, Role wrappedObject) {
        super(appService, wrappedObject);
    }

    @Override
    public int compareTo(ModelWrapper<Role> role2) {
        if (role2 instanceof RoleWrapper) {
            String name1 = getName();
            String name2 = ((RoleWrapper) role2).getName();

            if (name1 == null || name2 == null)
                return 0;
            return name1.compareTo(name2);
        }
        return 0;
    }

    private static final String ALL_ROLES_QRY = " from " + Role.class.getName(); //$NON-NLS-1$

    public static List<RoleWrapper> getAllRoles(
        BiobankApplicationService appService) throws ApplicationException {
        HQLCriteria criteria = new HQLCriteria(ALL_ROLES_QRY,
            new ArrayList<Object>());

        List<Role> roles = appService.query(criteria);
        return ModelWrapper.wrapModelCollection(appService, roles,
            RoleWrapper.class);
    }

    /**
     * Duplicate this role. The resulting role is not saved yet in the database
     */
    public RoleWrapper duplicate() {
        RoleWrapper newRole = new RoleWrapper(appService);
        newRole.setName(getName());
        newRole.addToPermissionCollection(getPermissionCollection());
        return newRole;
    }

    public void addToPermissionCollection(
        Collection<PermissionEnum> addedPermissions) {
        wrappedObject.getPermissions().addAll(addedPermissions);
    }

    public void removeFromPermissionCollection(
        List<PermissionEnum> removedPermissions) {
        wrappedObject.getPermissions().removeAll(removedPermissions);
    }

    public Collection<PermissionEnum> getPermissionCollection() {
        return wrappedObject.getPermissions();
    }
}
