package edu.ualberta.med.biobank.test.action;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolationException;

import org.junit.Assert;
import org.junit.Test;

import edu.ualberta.med.biobank.common.action.exception.ActionException;
import edu.ualberta.med.biobank.common.action.specimenType.SpecimenTypeDeleteAction;
import edu.ualberta.med.biobank.common.action.specimenType.SpecimenTypeGetAllAction;
import edu.ualberta.med.biobank.common.action.specimenType.SpecimenTypeSaveAction;
import edu.ualberta.med.biobank.common.action.specimenType.SpecimenTypesGetForContainerTypesAction;
import edu.ualberta.med.biobank.model.Capacity;
import edu.ualberta.med.biobank.model.ContainerType;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.model.SpecimenType;

public class TestSpecimenType extends TestAction {

    // private static Logger log = LoggerFactory
    // .getLogger(TestSpecimenBatchOp.class.getName());

    @Test
    public void getAction() {
        session.beginTransaction();

        // create two parent specimen types and three child specimen types,
        // the two parent specimen types each have the three child ones as children
        Set<SpecimenType> parentSpcTypes = new HashSet<SpecimenType>();
        parentSpcTypes.add(factory.createSpecimenType());
        parentSpcTypes.add(factory.createSpecimenType());

        Set<SpecimenType> childSpcTypes = new HashSet<SpecimenType>();
        childSpcTypes.add(factory.createSpecimenType());
        childSpcTypes.add(factory.createSpecimenType());
        childSpcTypes.add(factory.createSpecimenType());

        for (SpecimenType parentSpcType : parentSpcTypes) {
            parentSpcType.getChildSpecimenTypes().addAll(childSpcTypes);
        }

        session.getTransaction().commit();

        // specimen types are global, it's possible that other specimen types are in the
        // database before the test was started
        List<SpecimenType> actionResult = exec(new SpecimenTypeGetAllAction()).getList();
        for (SpecimenType spcType : parentSpcTypes) {
            Assert.assertTrue(actionResult.contains(spcType));
        }
        for (SpecimenType spcType : childSpcTypes) {
            Assert.assertTrue(actionResult.contains(spcType));
        }

        for (SpecimenType actionResultSpcType : actionResult) {
            if (parentSpcTypes.contains(actionResultSpcType)) {
                Assert.assertEquals(actionResultSpcType.getChildSpecimenTypes().size(),
                    childSpcTypes.size());
            }
        }
    }

    @Test
    public void addChildTypes() {
        session.beginTransaction();

        // create two parent specimen types and three child specimen types,
        // the two parent specimen types each have the three child ones as children
        SpecimenType parentSpcType = factory.createSpecimenType();

        Set<SpecimenType> childSpcTypes = new HashSet<SpecimenType>();
        childSpcTypes.add(factory.createSpecimenType());
        childSpcTypes.add(factory.createSpecimenType());
        childSpcTypes.add(factory.createSpecimenType());

        session.getTransaction().commit();

        Set<Integer> childSpecimenTypeIds = new HashSet<Integer>();
        for (SpecimenType spcType : childSpcTypes) {
            childSpecimenTypeIds.add(spcType.getId());
        }

        // add the child types to the parent
        SpecimenTypeSaveAction saveAction = new SpecimenTypeSaveAction(parentSpcType.getName(),
            parentSpcType.getNameShort());
        saveAction.setId(parentSpcType.getId());
        saveAction.setChildSpecimenTypeIds(childSpecimenTypeIds);
        exec(saveAction);

        session.clear();
        SpecimenType specimenType = (SpecimenType) session.load(
            SpecimenType.class, parentSpcType.getId());

        Assert.assertEquals(childSpcTypes.size(), specimenType.getChildSpecimenTypes().size());
        Assert.assertTrue(specimenType.getChildSpecimenTypes().containsAll(childSpcTypes));

    }

    @Test
    public void removeChildTypes() {
        session.beginTransaction();

        // create two parent specimen types and three child specimen types,
        // the two parent specimen types each have the three child ones as children
        Set<SpecimenType> parentSpcTypes = new HashSet<SpecimenType>();
        parentSpcTypes.add(factory.createSpecimenType());
        parentSpcTypes.add(factory.createSpecimenType());

        Set<SpecimenType> childSpcTypes = new HashSet<SpecimenType>();
        childSpcTypes.add(factory.createSpecimenType());
        childSpcTypes.add(factory.createSpecimenType());
        childSpcTypes.add(factory.createSpecimenType());

        for (SpecimenType parentSpcType : parentSpcTypes) {
            parentSpcType.getChildSpecimenTypes().addAll(childSpcTypes);
        }

        session.getTransaction().commit();

        // delete the child types from the first parent
        SpecimenType parentSpcType = parentSpcTypes.iterator().next();
        SpecimenTypeSaveAction saveAction = new SpecimenTypeSaveAction(parentSpcType.getName(),
            parentSpcType.getNameShort());
        saveAction.setId(parentSpcType.getId());
        exec(saveAction);

        session.clear();
        SpecimenType specimenType = (SpecimenType) session.load(
            SpecimenType.class, parentSpcType.getId());

        Assert.assertEquals(0, specimenType.getChildSpecimenTypes().size());

        // delete the child types from the second parent
        parentSpcTypes.remove(parentSpcType);
        parentSpcType = parentSpcTypes.iterator().next();
        saveAction = new SpecimenTypeSaveAction(parentSpcType.getName(),
            parentSpcType.getNameShort());
        saveAction.setId(parentSpcType.getId());
        exec(saveAction);

        session.clear();
        specimenType = (SpecimenType) session.load(SpecimenType.class, parentSpcType.getId());

        Assert.assertEquals(0, specimenType.getChildSpecimenTypes().size());
    }

    @Test
    public void deleteChildType() {
        session.beginTransaction();

        Set<SpecimenType> parentSpcTypes = new HashSet<SpecimenType>();
        parentSpcTypes.add(factory.createSpecimenType());
        parentSpcTypes.add(factory.createSpecimenType());

        SpecimenType childSpcType = factory.createSpecimenType();

        for (SpecimenType parentSpcType : parentSpcTypes) {
            parentSpcType.getChildSpecimenTypes().add(childSpcType);
        }

        session.getTransaction().commit();

        // attempt to delete the child type
        try {
            exec(new SpecimenTypeDeleteAction(childSpcType));
            Assert.fail("should not be allowed to delete child specimen type");
        } catch (ConstraintViolationException e) {
            // do nothing
        }

        // remove child type from parents
        for (SpecimenType parentSpcType : parentSpcTypes) {
            SpecimenTypeSaveAction saveAction = new SpecimenTypeSaveAction(parentSpcType.getName(),
                parentSpcType.getNameShort());
            saveAction.setId(parentSpcType.getId());
            exec(saveAction);
        }

        // check that child type was removed properly from former parents
        session.clear();
        for (SpecimenType parentSpcType : parentSpcTypes) {
            SpecimenType specimenType = (SpecimenType) session.load(SpecimenType.class,
                parentSpcType.getId());
            Assert.assertEquals(0, specimenType.getChildSpecimenTypes().size());
        }

        // now delete the child type
        exec(new SpecimenTypeDeleteAction(childSpcType));
    }

    @Test
    public void deleteParentType() {
        session.beginTransaction();

        SpecimenType parentSpcType = factory.createSpecimenType();
        SpecimenType childSpcType = factory.createSpecimenType();
        parentSpcType.getChildSpecimenTypes().add(childSpcType);

        session.getTransaction().commit();

        // attempt to remove parent type
        try {
            exec(new SpecimenTypeDeleteAction(parentSpcType));
            Assert.fail("should not be allowed to delete parent specimen type with children");
        } catch (ConstraintViolationException e) {
            // do nothing
        }

        // remove child type from parent
        SpecimenTypeSaveAction saveAction = new SpecimenTypeSaveAction(parentSpcType.getName(),
            parentSpcType.getNameShort());
        saveAction.setId(parentSpcType.getId());
        exec(saveAction);

        // remove parent type
        exec(new SpecimenTypeDeleteAction(parentSpcType));
    }

    @Test
    public void mangleTypes() {
        int size = exec(new SpecimenTypeGetAllAction()).getList().size();

        String name = getMethodNameR();
        final Integer typeId = exec(new SpecimenTypeSaveAction(name, name)).getId();

        Assert.assertEquals(size + 1, exec(new SpecimenTypeGetAllAction()).getList().size());

        exec(new SpecimenTypeDeleteAction(
            (SpecimenType) session.load(SpecimenType.class, typeId)));

        Assert.assertEquals(size, exec(new SpecimenTypeGetAllAction()).getList().size());
    }

    @Test
    public void specimenTypesForContainerTypes() {
        session.beginTransaction();
        Set<SpecimenType> setA = new HashSet<SpecimenType>();
        setA.add(factory.createSpecimenType());
        setA.add(factory.createSpecimenType());

        Set<SpecimenType> setB = new HashSet<SpecimenType>();
        setA.add(factory.createSpecimenType());
        setA.add(factory.createSpecimenType());

        ContainerType ctype1 = factory.createContainerType();
        Capacity capacity = new Capacity();
        capacity.setRowCapacity(5);
        capacity.setColCapacity(5);
        ctype1.setCapacity(capacity);
        ctype1.getSpecimenTypes().addAll(setA);

        ContainerType ctype2 = factory.createContainerType();
        capacity = new Capacity();
        capacity.setRowCapacity(10);
        capacity.setColCapacity(10);
        ctype2.setCapacity(capacity);
        ctype2.getSpecimenTypes().addAll(setB);

        ContainerType ctype3 = factory.createContainerType();
        capacity = new Capacity();
        capacity.setRowCapacity(8);
        capacity.setColCapacity(12);
        ctype3.setCapacity(capacity);
        session.getTransaction().commit();

        Set<Capacity> capacities = new HashSet<Capacity>();
        capacities.add(ctype1.getCapacity());
        capacities.add(ctype2.getCapacity());

        List<SpecimenType> specimenTypes = exec(
            new SpecimenTypesGetForContainerTypesAction(ctype1.getSite(), capacities)).getList();

        Assert.assertEquals(setA.size() + setB.size(), specimenTypes.size());
        Assert.assertTrue(specimenTypes.containsAll(setA));
        Assert.assertTrue(specimenTypes.containsAll(setB));

        capacities = new HashSet<Capacity>();
        capacities.add(ctype1.getCapacity());
        capacities.add(ctype3.getCapacity());

        specimenTypes = exec(new SpecimenTypesGetForContainerTypesAction(
            ctype1.getSite(), capacities)).getList();

        Assert.assertEquals(setA.size(), specimenTypes.size());
        Assert.assertTrue(specimenTypes.containsAll(setA));

        // test for empty list
        capacities = new HashSet<Capacity>();
        capacities.add(ctype3.getCapacity());

        specimenTypes = exec(new SpecimenTypesGetForContainerTypesAction(
            ctype3.getSite(), capacities)).getList();
        Assert.assertEquals(0, specimenTypes.size());
    }

    @Test
    public void specimenTypesForContainerTypesEmptyCapacity() {
        session.beginTransaction();
        Site site = factory.createSite();
        session.getTransaction().commit();

        // test empty capacity
        Set<Capacity> capacities = new HashSet<Capacity>();

        try {
            exec(new SpecimenTypesGetForContainerTypesAction(site, capacities)).getList();
            Assert.fail("should not be allowed run action without capacities");
        } catch (ActionException e) {
            // do nothing
        }

    }
}
