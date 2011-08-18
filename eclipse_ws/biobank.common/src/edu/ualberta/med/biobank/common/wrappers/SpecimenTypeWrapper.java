package edu.ualberta.med.biobank.common.wrappers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ualberta.med.biobank.common.exception.BiobankException;
import edu.ualberta.med.biobank.common.exception.BiobankQueryResultSizeException;
import edu.ualberta.med.biobank.common.peer.SpecimenPeer;
import edu.ualberta.med.biobank.common.peer.SpecimenTypePeer;
import edu.ualberta.med.biobank.common.wrappers.base.SpecimenTypeBaseWrapper;
import edu.ualberta.med.biobank.model.AliquotedSpecimen;
import edu.ualberta.med.biobank.model.SourceSpecimen;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.model.SpecimenType;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class SpecimenTypeWrapper extends SpecimenTypeBaseWrapper {
    private static final String HAS_SPECIMENS_MSG = "Unable to delete specimen type {0}. Specimens of this type exists in storage. Remove all instances before deleting this type.";

    private static final String UNKNOWN_IMPORT_NAME = "Unknown / import";

    public SpecimenTypeWrapper(WritableApplicationService appService,
        SpecimenType wrappedObject) {
        super(appService, wrappedObject);
    }

    public SpecimenTypeWrapper(WritableApplicationService appService) {
        super(appService);
    }

    /**
     * get all sample types in a site for containers which type name contains
     * "typeNameContains" (go recursively inside found containers)
     */
    public static List<SpecimenTypeWrapper> getSpecimenTypeForContainerTypes(
        WritableApplicationService appService, SiteWrapper siteWrapper,
        String typeNameContains) throws ApplicationException {
        List<ContainerTypeWrapper> containerTypes = ContainerTypeWrapper
            .getContainerTypesInSite(appService, siteWrapper, typeNameContains,
                false);
        Set<SpecimenTypeWrapper> SpecimenTypes = new HashSet<SpecimenTypeWrapper>();
        for (ContainerTypeWrapper containerType : containerTypes) {
            SpecimenTypes.addAll(containerType.getSpecimenTypesRecursively());
        }
        return new ArrayList<SpecimenTypeWrapper>(SpecimenTypes);
    }

    /**
     * get all sample types in a site for pallet containers (8*12 size) (go
     * recursively inside found containers)
     */
    public static List<SpecimenTypeWrapper> getSpecimenTypeForPallet96(
        WritableApplicationService appService, SiteWrapper siteWrapper)
        throws ApplicationException {
        List<ContainerTypeWrapper> containerTypes = ContainerTypeWrapper
            .getContainerTypesPallet96(appService, siteWrapper);
        Set<SpecimenTypeWrapper> SpecimenTypes = new HashSet<SpecimenTypeWrapper>();
        for (ContainerTypeWrapper containerType : containerTypes) {
            SpecimenTypes.addAll(containerType.getSpecimenTypesRecursively());
        }
        return new ArrayList<SpecimenTypeWrapper>(SpecimenTypes);
    }

    public static final String ALL_SAMPLE_TYPES_QRY = "from "
        + SpecimenType.class.getName();

    public static List<SpecimenTypeWrapper> getAllSpecimenTypes(
        WritableApplicationService appService, boolean sort)
        throws ApplicationException {
        HQLCriteria c = new HQLCriteria(ALL_SAMPLE_TYPES_QRY);

        List<SpecimenType> SpecimenTypes = appService.query(c);
        List<SpecimenTypeWrapper> list = new ArrayList<SpecimenTypeWrapper>();
        for (SpecimenType type : SpecimenTypes) {
            list.add(new SpecimenTypeWrapper(appService, type));
        }
        if (sort)
            Collections.sort(list);
        return list;
    }

    public static final String ALL_SOURCE_ONLY_SPECIMEN_TYPES_QRY = "from "
        + SpecimenType.class.getName() + " where "
        + SpecimenTypePeer.PARENT_SPECIMEN_TYPE_COLLECTION.getName()
        + ".size = 0";

    public static List<SpecimenTypeWrapper> getAllSourceOnlySpecimenTypes(
        WritableApplicationService appService, boolean sort)
        throws ApplicationException {
        HQLCriteria c = new HQLCriteria(ALL_SOURCE_ONLY_SPECIMEN_TYPES_QRY);

        List<SpecimenType> SpecimenTypes = appService.query(c);
        List<SpecimenTypeWrapper> list = new ArrayList<SpecimenTypeWrapper>();
        for (SpecimenType type : SpecimenTypes) {
            list.add(new SpecimenTypeWrapper(appService, type));
        }
        if (sort)
            Collections.sort(list);
        return list;
    }

    @Override
    public int compareTo(ModelWrapper<SpecimenType> wrapper) {
        if (wrapper instanceof SpecimenTypeWrapper) {
            String name1 = wrappedObject.getName();
            String name2 = wrapper.wrappedObject.getName();
            if (name1 != null && name2 != null) {
                return name1.compareTo(name2);
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return getName();
    }

    private static final String IS_USED_QRY_START = "select count(x) from ";
    private static final String IS_USED_QRY_END = " as x where x.specimenType.id=?";
    private static final Class<?>[] isUsedCheckClasses = new Class[] {
        Specimen.class, SourceSpecimen.class, AliquotedSpecimen.class };

    public boolean isUsed() throws ApplicationException,
        BiobankQueryResultSizeException {
        long usedCount = 0;
        for (Class<?> clazz : isUsedCheckClasses) {
            StringBuilder sb = new StringBuilder(IS_USED_QRY_START).append(
                clazz.getName()).append(IS_USED_QRY_END);
            HQLCriteria c = new HQLCriteria(sb.toString(),
                Arrays.asList(new Object[] { wrappedObject.getId() }));
            usedCount += getCountResult(appService, c);
        }

        return usedCount > 0;
    }

    @Override
    protected void addPersistTasks(TaskList tasks) {
        tasks.add(check().uniqueAndNotNull(SpecimenTypePeer.NAME));
        tasks.add(check().uniqueAndNotNull(SpecimenTypePeer.NAME_SHORT));

        super.addPersistTasks(tasks);
    }

    @Override
    protected void addDeleteTasks(TaskList tasks) {
        String isUsedMsg = MessageFormat.format(HAS_SPECIMENS_MSG, getName());
        tasks.add(check().notUsedBy(Specimen.class, SpecimenPeer.SPECIMEN_TYPE,
            isUsedMsg));

        super.addDeleteTasks(tasks);
    }

    public void checkNameAndShortNameUnique() throws ApplicationException,
        BiobankException {
        checkNoDuplicates(SpecimenType.class, SpecimenTypePeer.NAME.getName(),
            getName(), "A specimen type with name");
        checkNoDuplicates(SpecimenType.class,
            SpecimenTypePeer.NAME_SHORT.getName(), getNameShort(),
            "A specimen type with name short");
    }

    public boolean isUnknownImport() {
        return getName() != null && UNKNOWN_IMPORT_NAME.equals(getName());
    }

    // TODO: remove this override when all persist()-s are like this!
    @Override
    public void persist() throws Exception {
        WrapperTransaction.persist(this, appService);
    }

    @Override
    public void delete() throws Exception {
        WrapperTransaction.delete(this, appService);
    }
}
