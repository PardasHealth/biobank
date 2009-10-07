package edu.ualberta.med.biobank.common.wrappers;

import edu.ualberta.med.biobank.common.BiobankCheckException;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.SDKQuery;
import gov.nih.nci.system.query.SDKQueryResult;
import gov.nih.nci.system.query.example.DeleteExampleQuery;
import gov.nih.nci.system.query.example.InsertExampleQuery;
import gov.nih.nci.system.query.example.UpdateExampleQuery;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

public abstract class ModelWrapper<E> {

    protected WritableApplicationService appService;

    protected E wrappedObject;

    protected PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(
        this);

    protected HashMap<String, Object> propertiesMap = new HashMap<String, Object>();

    public ModelWrapper(WritableApplicationService appService, E wrappedObject) {
        this.appService = appService;
        this.wrappedObject = wrappedObject;
    }

    /**
     * create a new wrapped object
     */
    public ModelWrapper(WritableApplicationService appService) {
        this.appService = appService;
        try {
            this.wrappedObject = getNewObject();
        } catch (Exception e) {
            throw new RuntimeException(
                "was not able to create new object of type "
                    + getWrappedClass().getName());
        }
    }

    public E getWrappedObject() {
        return wrappedObject;
    }

    public void setWrappedObject(E wrappedObject) {
        this.wrappedObject = wrappedObject;
        propertiesMap.clear();
    }

    public void addPropertyChangeListener(String propertyName,
        PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public boolean isNew() {
        return (getId() == null);
    }

    public Integer getId() {
        Class<?> wrappedClass = wrappedObject.getClass();
        try {
            Method methodGetId = wrappedClass.getMethod("getId");
            return (Integer) methodGetId.invoke(wrappedObject);
        } catch (Exception e) {

        }
        return null;
    }

    public void reload() throws Exception {
        if (!isNew()) {
            E oldValue = wrappedObject;
            wrappedObject = getObjectFromDatabase();
            firePropertyChanges(oldValue, wrappedObject);
        }
        propertiesMap.clear();
    }

    /**
     * return the list of the different properties we want to notify when we
     * call firePropertyChanges
     */
    protected abstract String[] getPropertyChangesNames();

    /**
     * When retrieve the values from the database, need to fire the
     * modifications for the different objects contained in the wrapped object
     */
    private void firePropertyChanges(Object oldWrappedObject,
        Object newWrappedObject) throws Exception {
        String[] memberNames = getPropertyChangesNames();
        if (memberNames == null) {
            throw new Exception("memberNames cannot be null");
        }
        for (String member : memberNames) {
            propertyChangeSupport.firePropertyChange(member, oldWrappedObject,
                newWrappedObject);
        }
    }

    /**
     * using this wrapper id, retrieve the object from the database
     */
    protected E getObjectFromDatabase() throws Exception {
        Class<E> classType = getWrappedClass();
        Constructor<E> constructor = classType.getConstructor();
        Object instance = constructor.newInstance();
        Method setIdMethod = classType.getMethod("setId", Integer.class);
        Integer id = getId();
        setIdMethod.invoke(instance, id);

        List<E> list = appService.search(classType, instance);
        if (list.size() == 0)
            return null;
        if (list.size() == 1) {
            return list.get(0);
        }
        throw new Exception("Found " + list.size() + " objects of type "
            + classType.getName() + " with id=" + id);
    }

    public abstract Class<E> getWrappedClass();

    /**
     * insert or update the object into the database
     */
    @SuppressWarnings("unchecked")
    public void persist() throws BiobankCheckException, Exception {
        persistChecks();
        SDKQuery query;
        if (isNew()) {
            query = new InsertExampleQuery(wrappedObject);
        } else {
            E origObject = getObjectFromDatabase();
            persistDependencies(origObject);
            query = new UpdateExampleQuery(wrappedObject);
        }

        SDKQueryResult result = appService.executeQuery(query);
        wrappedObject = ((E) result.getObjectResult());
        propertiesMap.clear();
    }

    /**
     * should redefine this method if others updates (or deletes) need to be
     * done when this object is update
     */
    @SuppressWarnings("unused")
    protected void persistDependencies(E origObject)
        throws BiobankCheckException, Exception {
    }

    protected abstract void persistChecks() throws BiobankCheckException,
        Exception;

    /**
     * delete the object into the database
     * 
     * @throws ApplicationException
     */
    public void delete() throws BiobankCheckException, Exception {
        if (!isNew()) {
            deleteChecks();
            appService.executeQuery(new DeleteExampleQuery(wrappedObject));
        }
    }

    protected abstract void deleteChecks() throws BiobankCheckException,
        Exception;

    public void reset() throws Exception {
        if (isNew()) {
            E oldValue = wrappedObject;
            wrappedObject = getNewObject();
            firePropertyChanges(oldValue, wrappedObject);
            propertiesMap.clear();
        } else {
            reload();
        }
        propertiesMap.clear();
    }

    protected E getNewObject() throws Exception {
        Constructor<E> constructor = getWrappedClass().getConstructor();
        return constructor.newInstance();
    }

    public void loadAttributes() throws Exception {
        Class<E> classType = getWrappedClass();

        if (classType == null) {
            throw new Exception("wrapped class is null");
        }

        Method[] methods = classType.getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("get")
                && !method.getName().equals("getClass")
                && !method.getReturnType().getName().equals("java.util.Set")) {
                method.invoke(wrappedObject, (Object[]) null);
            }
        }
    }

    /**
     * return true if integrity of this object is ok
     */
    public boolean checkIntegrity() {
        return true;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        Integer id = getId();
        Integer id2 = ((ModelWrapper<?>) object).getId();
        return (id == null && id2 == null) || id.equals(id2);
    }

    /**
     * return the list of all objects of the database of this type
     */
    protected List<E> getAllObjects() throws Exception {
        Class<E> classType = getWrappedClass();
        Constructor<E> constructor = classType.getConstructor();
        Object instance = constructor.newInstance();
        return appService.search(classType, instance);
    }

}