package edu.ualberta.med.biobank.action.security;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;

import edu.ualberta.med.biobank.action.Action;
import edu.ualberta.med.biobank.action.ActionContext;
import edu.ualberta.med.biobank.action.exception.ActionException;
import edu.ualberta.med.biobank.permission.Permission;
import edu.ualberta.med.biobank.permission.security.UserManagerPermission;
import edu.ualberta.med.biobank.model.security.Group;
import edu.ualberta.med.biobank.model.security.User;

/**
 * Returns a list of {@link Group}-s that the executing user has
 * <em>complete</em> control over.
 * 
 * @author Jonathan Ferland
 */
public class GroupGetAllAction implements Action<GroupGetAllOutput> {
    private static final long serialVersionUID = 1L;
    private static final Permission PERMISSION = new UserManagerPermission();

    public GroupGetAllInput input;

    public GroupGetAllAction(GroupGetAllInput input) {
        this.input = input;
    }

    @Override
    public boolean isAllowed(ActionContext context) throws ActionException {
        return PERMISSION.isAllowed(context);
    }

    @Override
    public GroupGetAllOutput run(ActionContext context)
        throws ActionException {
        @SuppressWarnings("nls")
        Criteria c = context.getSession()
            .createCriteria(Group.class, "g")
            .createAlias("g.memberships", "m", Criteria.LEFT_JOIN)
            .createAlias("g.users", "u", Criteria.LEFT_JOIN)
            .createAlias("m.domain", "d", Criteria.LEFT_JOIN)
            .createAlias("d.centers", "c", Criteria.LEFT_JOIN)
            .createAlias("d.studies", "s", Criteria.LEFT_JOIN)
            .createAlias("m.roles", "r", Criteria.LEFT_JOIN)
            .setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY)
            .addOrder(Order.asc("g.name"));

        User user = context.getUser();
        SortedSet<Group> groups = new TreeSet<Group>(Group.NAME_COMPARATOR);

        @SuppressWarnings("unchecked")
        List<Group> results = c.list();
        for (Group group : results) {
            if (group.isFullyManageable(user)) {
                groups.add(group);
            }
        }

        return new GroupGetAllOutput(groups);
    }
}
