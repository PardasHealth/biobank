package edu.ualberta.med.biobank.action.security;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.criterion.CriteriaSpecification;

import edu.ualberta.med.biobank.action.Action;
import edu.ualberta.med.biobank.action.ActionContext;
import edu.ualberta.med.biobank.action.exception.ActionException;
import edu.ualberta.med.biobank.permission.Permission;
import edu.ualberta.med.biobank.permission.security.RoleManagementPermission;
import edu.ualberta.med.biobank.model.security.Role;

public class RoleGetAllAction implements Action<RoleGetAllOutput> {
    private static final long serialVersionUID = 1L;
    private static final Permission PERMISSION = new RoleManagementPermission();

    @SuppressWarnings("unused")
    private final RoleGetAllInput input;

    public RoleGetAllAction(RoleGetAllInput input) {
        this.input = input;
    }

    @Override
    public boolean isAllowed(ActionContext context) throws ActionException {
        return PERMISSION.isAllowed(context);
    }

    @Override
    public RoleGetAllOutput run(ActionContext context) throws ActionException {
        @SuppressWarnings("unchecked")
        List<Role> results = context.getSession()
            .createCriteria(Role.class)
            .setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY)
            .list();

        SortedSet<Role> roles = new TreeSet<Role>(Role.NAME_COMPARATOR);
        roles.addAll(results);

        return new RoleGetAllOutput(roles);
    }
}
