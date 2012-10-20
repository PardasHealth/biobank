package edu.ualberta.med.biobank.action.security;

import java.util.Set;

import edu.ualberta.med.biobank.CommonBundle;
import edu.ualberta.med.biobank.action.Action;
import edu.ualberta.med.biobank.action.ActionContext;
import edu.ualberta.med.biobank.action.IdResult;
import edu.ualberta.med.biobank.action.exception.ActionException;
import edu.ualberta.med.biobank.permission.Permission;
import edu.ualberta.med.biobank.permission.security.UserManagerPermission;
import edu.ualberta.med.biobank.i18n.Bundle;
import edu.ualberta.med.biobank.i18n.LString;
import edu.ualberta.med.biobank.i18n.LocalizedException;
import edu.ualberta.med.biobank.model.security.Domain;
import edu.ualberta.med.biobank.model.security.Group;
import edu.ualberta.med.biobank.model.security.Membership;
import edu.ualberta.med.biobank.model.security.User;
import edu.ualberta.med.biobank.util.SetDiff;
import edu.ualberta.med.biobank.util.SetDiff.Pair;

public class GroupSaveAction implements Action<IdResult> {
    private static final long serialVersionUID = 1L;
    private static final Bundle bundle = new CommonBundle();
    private static final Permission PERMISSION = new UserManagerPermission();

    @SuppressWarnings("nls")
    public static final LString INADEQUATE_PERMISSIONS_ERRMSG =
        bundle.tr("You do not have adequate permissions to save this group.")
            .format();

    private final GroupSaveInput input;

    public GroupSaveAction(GroupSaveInput input) {
        this.input = input;
    }

    @Override
    public boolean isAllowed(ActionContext context) throws ActionException {
        return PERMISSION.isAllowed(context);
    }

    @Override
    public IdResult run(ActionContext context) throws ActionException {
        User executingUser = context.getUser();
        Group group = context.get(Group.class, input.getGroupId(), new Group());

        checkFullyManageable(group, executingUser);

        group.setName(input.getName());
        group.setName(input.getName());

        Set<User> users = context.load(User.class, input.getUserIds());
        group.getUsers().clear();
        group.getUsers().addAll(users);

        setMemberships(context, group);

        checkFullyManageable(group, executingUser);

        context.getSession().saveOrUpdate(group);

        return new IdResult(group.getId());
    }

    private void checkFullyManageable(Group group, User executingUser) {
        if (!group.isFullyManageable(executingUser)) {
            throw new LocalizedException(INADEQUATE_PERMISSIONS_ERRMSG);
        }
    }

    private void setMemberships(ActionContext context, Group group) {
        SetDiff<Membership> diff = new SetDiff<Membership>(
            group.getMemberships(), input.getMemberships());

        for (Membership m : diff.getRemovals()) {
            group.getMemberships().remove(m);
            context.getSession().delete(m);
        }

        for (Membership m : diff.getAdditions()) {
            group.getMemberships().add(m);
            m.setPrincipal(group);
        }

        for (Pair<Membership> pair : diff.getIntersection()) {
            Membership oldM = pair.getOld();
            Membership newM = pair.getNew();

            oldM.getPermissions().clear();
            oldM.getPermissions().addAll(newM.getPermissions());

            oldM.getRoles().clear();
            oldM.getRoles().addAll(newM.getRoles());

            oldM.setUserManager(newM.isUserManager());
            oldM.setEveryPermission(newM.isEveryPermission());

            // TODO: throw away old domain, copy into new? Shorter.
            Domain newD = newM.getDomain();
            Domain oldD = oldM.getDomain();

            oldD.getCenters().clear();
            oldD.getCenters().addAll(newD.getCenters());
            oldD.setAllCenters(newD.isAllCenters());

            oldD.getStudies().clear();
            oldD.getStudies().addAll(newD.getStudies());
            oldD.setAllStudies(newD.isAllStudies());
        }

        for (Membership m : group.getMemberships()) {
            m.reducePermissions();
        }
    }
}
