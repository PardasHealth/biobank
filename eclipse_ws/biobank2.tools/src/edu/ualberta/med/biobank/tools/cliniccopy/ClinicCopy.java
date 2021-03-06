package edu.ualberta.med.biobank.tools.cliniccopy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.ualberta.med.biobank.client.util.ServiceConnection;
import edu.ualberta.med.biobank.common.wrappers.AddressWrapper;
import edu.ualberta.med.biobank.common.wrappers.ClinicWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContactWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.common.wrappers.StudyWrapper;
import edu.ualberta.med.biobank.common.wrappers.util.WrapperUtil;
import edu.ualberta.med.biobank.model.Clinic;
import edu.ualberta.med.biobank.server.applicationservice.BiobankApplicationService;
import edu.ualberta.med.biobank.tools.GenericAppArgs;
import edu.ualberta.med.biobank.tools.bbpdbconsent.BbpdbConsent;
import edu.ualberta.med.biobank.tools.utils.HostUrl;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

/**
 * See USAGE constant below for a description of this class.
 *
 * This class fixes copies clinics from one BioBank database to another.
 *
 */
@Deprecated
@SuppressWarnings({ "unused", "nls" })
public class ClinicCopy {

    // @formatter:off
    private static String USAGE =
        "Usage: bbpdbconsent [options]\n\n"
            + "Used to copy clinic information from the training server to the\n"
            + "CBSR server for the REFINE clinics.\n\n"
            + "Options\n"
            + "  -H, --host       hostname for BioBank server and MySQL server\n"
            + "  -p, --port       port number for BioBank server\n"
            + "  -u, --user       user name to log into BioBank server\n"
            + "  -w, --password   password to log into BioBank server\n"
            + "  -v, --verbose    shows verbose output\n"
            + "  -h, --help       shows this text\n"; //$NON-NLS-1$
    // @formatter:on

    private static final Logger LOGGER = Logger.getLogger(BbpdbConsent.class
        .getName());

    private static final String[] CLINICS_TO_COPY = { "200 - Spokane WA",
        "201 - Erie PA", "202 - Cherry Hill NJ", "203 - West Des Moines IA",
        "204 - Evansville IN", "205 - Southfield MI", "206 - Nashville TN",
        "207 - Amarillo TX", "300 - Oulu FI" };

    public static final String CLINICS_QUERY = "select clinics from "
        + Clinic.class.getName() + " clinics"
        + " inner join clinics.contacts contacts"
        + " inner join contacts.studies studies"
        + " where clinics.nameShort = ?";

    private final BiobankApplicationService tsAppService;

    private final BiobankApplicationService appService;

    private Map<String, ClinicWrapper> clinicsOnTest;

    private Map<String, ClinicWrapper> clinicsOnProd;

    private SiteWrapper cbsrSiteOnProd;

    private StudyWrapper refineStudyOnProd;

    public static void main(String[] argv) {
        try {
            GenericAppArgs args = new GenericAppArgs(argv);
            if (args.helpOption()) {
                System.out.println(USAGE);
                System.exit(0);
            } else if (args.error) {
                System.out.println(args.errorMsg + "\n" + USAGE);
                System.exit(-1);
            }
            new ClinicCopy(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ClinicCopy(GenericAppArgs appArgs) throws Exception {
        tsAppService = ServiceConnection.getAppService("https://cbsr-training.med.ualberta.ca/biobank",
                                                       appArgs.userOption(),
                                                       appArgs.passwordOption());

        String hostUrl = HostUrl.getHostUrl(appArgs.hostOption(), appArgs.portOption());

        appService = ServiceConnection.getAppService(hostUrl,
                                                     appArgs.userOption(),
                                                     appArgs.passwordOption());

        refineStudyOnProd = null;
        for (StudyWrapper study : StudyWrapper.getAllStudies(appService)) {
            LOGGER.info("study name: " + study.getNameShort());
            if (study.getNameShort().equals("REFINE")) {
                refineStudyOnProd = study;
            }
        }

        if (refineStudyOnProd == null) {
            throw new Exception(
                "could not find REFINE study on production server");
        }

        addClinics(tsAppService);
        addContacts();
    }

    @Deprecated
    private void addClinics(BiobankApplicationService tsAppService2)
        throws Exception {
        clinicsOnTest = new HashMap<String, ClinicWrapper>();
        clinicsOnProd = new HashMap<String, ClinicWrapper>();
        for (String clinicNameShort : CLINICS_TO_COPY) {
            ClinicWrapper clinic = getClinic(tsAppService, clinicNameShort);
            clinicsOnTest.put(clinicNameShort, clinic);

            ClinicWrapper clinicOnProd = getClinic(appService, clinicNameShort);

            if (clinicOnProd != null) {
                LOGGER.error("clinic already exists: " + clinicNameShort);
            } else {
                AddressWrapper newAddress = new AddressWrapper(appService);
                newAddress.setStreet1(clinic.getAddress().getStreet1());
                newAddress.setStreet2(clinic.getAddress().getStreet2());
                newAddress.setCity(clinic.getAddress().getCity());
                newAddress.setProvince(clinic.getAddress().getProvince());
                newAddress.setPostalCode(clinic.getAddress().getPostalCode());
                newAddress.setEmailAddress(clinic.getAddress()
                    .getEmailAddress());
                newAddress.setPhoneNumber(clinic.getAddress().getPhoneNumber());
                newAddress.setFaxNumber(clinic.getAddress().getFaxNumber());
                newAddress.setCountry(clinic.getAddress().getCountry());
                newAddress.persist();
                newAddress.reload();

                ClinicWrapper newClinic = new ClinicWrapper(appService);
                newClinic.setName(clinic.getName());
                newClinic.setNameShort(clinic.getNameShort());
                // newClinic.setComment(clinic.getComment());
                newClinic.setAddress(newAddress);
                newClinic.setActivityStatus(clinic.getActivityStatus());
                newClinic.persist();
                newClinic.reload();

                clinicsOnProd.put(clinicNameShort, newClinic);

                LOGGER.info("added clinic " + clinicNameShort);
            }
        }
    }

    private void addContacts() throws Exception {
        ArrayList<ContactWrapper> newContacts = new ArrayList<ContactWrapper>();

        for (ClinicWrapper clinic : clinicsOnTest.values()) {
            for (ContactWrapper contact : clinic.getContactCollection(false)) {
                ContactWrapper newContact = new ContactWrapper(appService);
                newContact.setName(contact.getName());
                newContact.setTitle(contact.getTitle());
                newContact.setMobileNumber(contact.getMobileNumber());
                newContact.setFaxNumber(contact.getFaxNumber());
                newContact.setEmailAddress(contact.getEmailAddress());
                newContact.setPagerNumber(contact.getPagerNumber());
                newContact.setOfficeNumber(contact.getOfficeNumber());
                newContact.setClinic(clinicsOnProd.get(clinic.getNameShort()));
                newContact.persist();
                newContact.reload();

                newContacts.add(newContact);

                LOGGER.info("added contact " + newContact.getName()
                    + " for clinic " + newContact.getClinic().getNameShort());
            }
        }

        refineStudyOnProd.addToContactCollection(newContacts);
        refineStudyOnProd.persist();

    }

    private ClinicWrapper getClinic(BiobankApplicationService appService,
        String nameShort) throws Exception {
        List<Clinic> rawList = appService.query(new HQLCriteria(CLINICS_QUERY,
            Arrays.asList(new Object[] { nameShort })));
        if ((rawList == null) || rawList.isEmpty()) {
            return null;
        }
        if (rawList.size() > 1) {
            throw new Exception("more than one clinic with name short "
                + nameShort);
        }
        return WrapperUtil.wrapModel(appService, rawList.get(0),
            ClinicWrapper.class);
    }
}
