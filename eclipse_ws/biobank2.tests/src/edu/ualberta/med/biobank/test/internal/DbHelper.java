package edu.ualberta.med.biobank.test.internal;

import edu.ualberta.med.biobank.common.wrappers.ClinicWrapper;
import edu.ualberta.med.biobank.common.wrappers.CollectionEventWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.common.wrappers.DispatchWrapper;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.common.wrappers.PatientWrapper;
import edu.ualberta.med.biobank.common.wrappers.ProcessingEventWrapper;
import edu.ualberta.med.biobank.common.wrappers.RequestWrapper;
import edu.ualberta.med.biobank.common.wrappers.ResearchGroupWrapper;
import edu.ualberta.med.biobank.common.wrappers.SpecimenWrapper;
import edu.ualberta.med.biobank.common.wrappers.StudyWrapper;
import gov.nih.nci.system.applicationservice.WritableApplicationService;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.Assert;

@Deprecated
public class DbHelper {

    protected static WritableApplicationService appService;

    protected static Random r = new Random();

    public static void setAppService(WritableApplicationService appService) {
        Assert.assertNotNull("appService is null", appService);
        DbHelper.appService = appService;
    }

    public static <T> T chooseRandomlyInList(List<T> list) {
        if (list.size() == 1) {
            return list.get(0);
        }
        if (list.size() > 1) {
            int pos = r.nextInt(list.size());
            return list.get(pos);
        }
        return null;
    }

    public static void deleteContainers(Collection<ContainerWrapper> containers)
        throws Exception {
        Assert.assertNotNull("appService is null", appService);
        if ((containers == null) || (containers.size() == 0))
            return;

        for (ContainerWrapper container : containers) {
            container.reload();
            if (container.hasChildren()) {
                deleteContainers(container.getChildren().values());
            }
            if (container.hasSpecimens()) {
                deleteFromList(container.getSpecimens().values());
            }
            container.reload();
            container.delete();
        }
    }

    public static void deleteDispatches(Collection<DispatchWrapper> dispaches)
        throws Exception {
        Assert.assertNotNull("appService is null", appService);
        if ((dispaches == null) || dispaches.isEmpty())
            return;

        for (DispatchWrapper dispatch : dispaches) {
            dispatch.reload();
            deleteFromList(dispatch.getDispatchSpecimenCollection(false));
            dispatch.reload();
            dispatch.delete();
        }
    }

    public static void deleteCollectionEvents(
        List<CollectionEventWrapper> cevents) throws Exception {
        for (CollectionEventWrapper ce : cevents) {
            if (ce.isNew())
                continue;
            ce.reload();
            // cannot use all here! otherwise you delete twice
            deleteSpecimensAndChildren(ce.getOriginalSpecimenCollection(false));
            ce.reload();
            ce.delete();
        }
    }

    private static void deleteSpecimensAndChildren(
        List<SpecimenWrapper> specimenCollection) throws Exception {
        for (SpecimenWrapper child : specimenCollection) {
            deleteSpecimensAndChildren(child.getChildSpecimenCollection(false));
            child.reload();
            child.delete();
        }
    }

    public static void deletePatients(List<PatientWrapper> patients)
        throws Exception {
        Assert.assertNotNull("appService is null", appService);
        if (patients == null)
            return;

        // visites liees au ship avec patient de la visit non lie au shipment
        for (PatientWrapper patient : patients) {
            patient.reload();
            deleteCollectionEvents(patient.getCollectionEventCollection(false));
            patient.reload();
            patient.delete();
        }
    }

    public static void deleteProcessingEvents(
        List<ProcessingEventWrapper> pevents) throws Exception {
        for (ProcessingEventWrapper pevent : pevents) {
            pevent.reload();
            for (SpecimenWrapper spc : pevent.getSpecimenCollection(false)) {
                spc.delete();
            }
            pevent.reload();
            pevent.delete();
        }
    }

    public static void deleteClinics(List<ClinicWrapper> clinics)
        throws Exception {
        Assert.assertNotNull("appService is null", appService);
        for (ClinicWrapper clinic : clinics) {
            clinic.reload();
            deleteFromList(clinic.getOriginInfoCollection(false));

            List<StudyWrapper> studies = clinic.getStudyCollection();
            for (StudyWrapper study : studies) {
                List<PatientWrapper> patients = study
                    .getPatientCollection(false);
                for (PatientWrapper patient : patients) {
                    List<CollectionEventWrapper> collectionEvents = patient
                        .getCollectionEventCollection(false);
                    for (CollectionEventWrapper collectionEvent : collectionEvents) {
                        deleteFromList(collectionEvent
                            .getAllSpecimenCollection(false));
                    }
                    deleteFromList(collectionEvents);
                }
                deleteFromList(patients);
            }
            deleteFromList(studies);
            clinic.delete();
        }
    }

    public static void deleteResearchGroups(
        List<ResearchGroupWrapper> researchGroups) throws Exception {
        deleteFromList(researchGroups);
    }

    public static void deleteFromList(Collection<? extends ModelWrapper<?>> list)
        throws Exception {
        if (list == null)
            return;

        for (ModelWrapper<?> object : list) {
            object.reload();
            object.delete();
        }
    }

    public static void deleteRequests(List<RequestWrapper> createdRequests)
        throws Exception {
        for (RequestWrapper r : createdRequests) {
            deleteFromList(r.getRequestSpecimenCollection(false));
        }
        deleteFromList(createdRequests);
    }

}
