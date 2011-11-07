package edu.ualberta.med.biobank.common.permission.site;

import org.hibernate.Session;

import edu.ualberta.med.biobank.common.action.util.SessionUtil;
import edu.ualberta.med.biobank.common.permission.Permission;
import edu.ualberta.med.biobank.common.permission.PermissionEnum;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.model.User;

public class SiteReadPermission implements Permission {
    private static final long serialVersionUID = 1L;

    private final Integer siteId;

    public SiteReadPermission(Integer siteId) {
        this.siteId = siteId;
    }

    public SiteReadPermission(Site site) {
        this(site.getId());
    }

    @Override
    public boolean isAllowed(User user, Session session) {
        SessionUtil sessionUtil = new SessionUtil(session);
        Site site = sessionUtil.get(Site.class, siteId);
        return PermissionEnum.SITE_READ.isAllowed(user, site);
    }
}
