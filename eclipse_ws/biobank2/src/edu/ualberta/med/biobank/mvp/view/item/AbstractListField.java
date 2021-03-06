package edu.ualberta.med.biobank.mvp.view.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;

import edu.ualberta.med.biobank.mvp.event.ui.ListChangeEvent;
import edu.ualberta.med.biobank.mvp.event.ui.ListChangeHandler;
import edu.ualberta.med.biobank.mvp.user.ui.ListField;

public abstract class AbstractListField<E> extends AbstractValidationField
    implements ListField<E> {
    private final HandlerManager handlerManager = new HandlerManager(this);
    private final List<E> list = new ArrayList<E>();
    private final List<E> unmodifiableList = Collections.unmodifiableList(list);

    @Override
    public HandlerRegistration addListChangeHandler(ListChangeHandler<E> handler) {
        return handlerManager.addHandler(ListChangeEvent.getType(), handler);
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        handlerManager.fireEvent(event);
    }

    @Override
    public List<E> asUnmodifiableList() {
        return unmodifiableList;
    }

    @Override
    public final void setElements(Collection<? extends E> elements) {
        setElements(elements, false);
    }

    @Override
    public void setElements(Collection<? extends E> elements, boolean fireEvents) {
        setElements(elements, fireEvents, true);
    }

    protected void setElementsInternal(Collection<? extends E> elements) {
        setElements(elements, true, false);
    }

    /**
     * Update the GUI's value to match {@link #asUnmodifiableList()}. Do so
     * without firing any events.
     */
    protected abstract void updateGui();

    private synchronized void setElements(Collection<? extends E> elements,
        boolean fireEvents, boolean updateGui) {
        list.clear();
        list.addAll(elements);

        if (updateGui) {
            updateGui();
        }

        if (fireEvents) {
            fireEvent(new ListChangeEvent<E>(this));
        }
    }
}
