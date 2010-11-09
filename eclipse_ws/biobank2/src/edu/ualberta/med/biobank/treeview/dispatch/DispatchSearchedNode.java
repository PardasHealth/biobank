package edu.ualberta.med.biobank.treeview.dispatch;

import java.util.Date;

import org.eclipse.core.runtime.Assert;

import edu.ualberta.med.biobank.common.wrappers.DispatchWrapper;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.treeview.AbstractSearchedNode;
import edu.ualberta.med.biobank.treeview.AdapterBase;

public class DispatchSearchedNode extends AbstractSearchedNode {

    public DispatchSearchedNode(AdapterBase parent, int id) {
        super(parent, id, true);
    }

    @Override
    protected AdapterBase createChildNode(ModelWrapper<?> child) {
        Assert.isTrue(child instanceof DispatchWrapper);
        return new DispatchAdapter(this, (DispatchWrapper) child);
    }

    @Override
    protected AdapterBase createChildNode() {
        return new DispatchAdapter(this, null);
    }

    @Override
    protected boolean isParentTo(ModelWrapper<?> parent, ModelWrapper<?> child) {
        if (child instanceof DispatchWrapper) {
            return parent.equals(((DispatchWrapper) child).getSender());
        }
        return false;
    }

    @Override
    public AdapterBase search(Object searchedObject) {
        if (searchedObject instanceof Date) {
            Date date = (Date) searchedObject;
            return getChild((int) date.getTime());
        } else if (searchedObject instanceof DispatchWrapper) {
            return getChild((ModelWrapper<?>) searchedObject, true);
        }
        return searchChildren(searchedObject);
    }

    @Override
    protected void addNode(ModelWrapper<?> wrapper) {
        DispatchAdapter ship = new DispatchAdapter(this,
            (DispatchWrapper) wrapper);
        ship.setParent(this);
        addChild(ship);
    }

}