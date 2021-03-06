package edu.ualberta.med.biobank.common.action.center;

import edu.ualberta.med.biobank.common.action.Action;
import edu.ualberta.med.biobank.common.action.ActionContext;
import edu.ualberta.med.biobank.common.action.EmptyResult;
import edu.ualberta.med.biobank.common.action.exception.ActionException;
import edu.ualberta.med.biobank.model.Center;

public abstract class CenterDeleteAction implements Action<EmptyResult> {
    private static final long serialVersionUID = 1L;

    protected final Integer centerId;

    public CenterDeleteAction(Center center) {
        this.centerId = center.getId();
    }

    public EmptyResult run(ActionContext context, Center center)
        throws ActionException {
        context.getSession().delete(center);
        return new EmptyResult();
    }
}
