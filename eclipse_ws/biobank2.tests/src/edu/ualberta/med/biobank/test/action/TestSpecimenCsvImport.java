package edu.ualberta.med.biobank.test.action;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ualberta.med.biobank.common.action.csvimport.SpecimenCsvImportAction;
import edu.ualberta.med.biobank.common.action.exception.CsvImportException;
import edu.ualberta.med.biobank.common.action.exception.CsvImportException.ImportError;
import edu.ualberta.med.biobank.model.AliquotedSpecimen;
import edu.ualberta.med.biobank.model.Center;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.SourceSpecimen;
import edu.ualberta.med.biobank.model.Study;
import edu.ualberta.med.biobank.test.action.csvhelper.SpecimenCsvHelper;

public class TestSpecimenCsvImport extends ActionTest {

    private static Logger log = LoggerFactory
        .getLogger(TestSpecimenCsvImport.class.getName());

    private static final String CSV_NAME = "import_specimens.csv";

    private final SpecimenCsvHelper csvHelper;

    public TestSpecimenCsvImport() {
        super();
        this.csvHelper = new SpecimenCsvHelper();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void noErrorsNoContainers() throws Exception {
        Transaction tx = session.beginTransaction();
        // the site name comes from the CSV file
        Center center = factory.createSite();
        Center clinic = factory.createClinic();
        Study study = factory.createStudy();

        Set<Patient> patients = new HashSet<Patient>();
        patients.add(factory.createPatient());
        patients.add(factory.createPatient());
        patients.add(factory.createPatient());

        Set<SourceSpecimen> sourceSpecimens = new HashSet<SourceSpecimen>();
        sourceSpecimens.add(factory.createSourceSpecimen());
        factory.createSpecimenType();
        sourceSpecimens.add(factory.createSourceSpecimen());
        factory.createSpecimenType();
        sourceSpecimens.add(factory.createSourceSpecimen());

        // create a new specimen type for the aliquoted specimens
        factory.createSpecimenType();
        Set<AliquotedSpecimen> aliquotedSpecimens =
            new HashSet<AliquotedSpecimen>();
        aliquotedSpecimens.add(factory.createAliquotedSpecimen());
        aliquotedSpecimens.add(factory.createAliquotedSpecimen());
        aliquotedSpecimens.add(factory.createAliquotedSpecimen());

        tx.commit();

        try {
            csvHelper.specimensCreateCsv(CSV_NAME, study, clinic, center,
                patients, sourceSpecimens, aliquotedSpecimens);
            SpecimenCsvImportAction importAction =
                new SpecimenCsvImportAction(CSV_NAME);
            exec(importAction);
        } catch (CsvImportException e) {
            showErrorsInLog(e);
            Assert.fail("errors in CVS data: " + e.getMessage());
        }
    }

    @Test
    public void noSourceSpecimensInCsv() throws Exception {
        Transaction tx = session.beginTransaction();
        // the site name comes from the CSV file
        Center center = factory.createSite();
        Center clinic = factory.createClinic();
        Study study = factory.createStudy();

        Set<Patient> patients = new HashSet<Patient>();
        patients.add(factory.createPatient());
        patients.add(factory.createPatient());
        patients.add(factory.createPatient());

        Set<SourceSpecimen> sourceSpecimensEmpty =
            new HashSet<SourceSpecimen>();
        factory.createSpecimenType();
        factory.createSpecimenType();

        // create a new specimen type for the aliquoted specimens
        factory.createSpecimenType();
        Set<AliquotedSpecimen> aliquotedSpecimens =
            new HashSet<AliquotedSpecimen>();
        aliquotedSpecimens.add(factory.createAliquotedSpecimen());
        aliquotedSpecimens.add(factory.createAliquotedSpecimen());
        aliquotedSpecimens.add(factory.createAliquotedSpecimen());

        tx.commit();

        try {
            csvHelper.specimensCreateCsv(CSV_NAME, study, clinic, center,
                patients, sourceSpecimensEmpty, aliquotedSpecimens);
            SpecimenCsvImportAction importAction =
                new SpecimenCsvImportAction(CSV_NAME);
            exec(importAction);
        } catch (CsvImportException e) {
            showErrorsInLog(e);
            Assert.fail("errors in CVS data: " + e.getMessage());
        }
    }

    @Test
    public void missingPatient() {
        Transaction tx = session.beginTransaction();
        // the site name comes from the CSV file
        Center center = factory.createSite();
        Center clinic = factory.createClinic();
        Study study = factory.createStudy();

        Set<Patient> patients = new HashSet<Patient>();
        patients.add(factory.createPatient());
        patients.add(factory.createPatient());
        patients.add(factory.createPatient());

        Set<SourceSpecimen> sourceSpecimens = new HashSet<SourceSpecimen>();
        sourceSpecimens.add(factory.createSourceSpecimen());
        sourceSpecimens.add(factory.createSourceSpecimen());
        sourceSpecimens.add(factory.createSourceSpecimen());

        Set<AliquotedSpecimen> aliquotedSpecimens =
            new HashSet<AliquotedSpecimen>();
        aliquotedSpecimens.add(factory.createAliquotedSpecimen());
        aliquotedSpecimens.add(factory.createAliquotedSpecimen());
        aliquotedSpecimens.add(factory.createAliquotedSpecimen());

        tx.commit();

        try {
            csvHelper.specimensCreateCsv(CSV_NAME, study, clinic, center,
                patients, sourceSpecimens, aliquotedSpecimens);
            SpecimenCsvImportAction importAction =
                new SpecimenCsvImportAction(CSV_NAME);
            exec(importAction);
        } catch (CsvImportException e) {
            Assert.fail("errors in CVS data");
            showErrorsInLog(e);
        } catch (Exception e) {
            Assert.fail("could not import data");
        }

    }

    private void showErrorsInLog(CsvImportException e) {
        for (ImportError ie : e.getErrors()) {
            log.error("ERROR: line no {}: {}", ie.getLineNumber(),
                ie.getMessage());
        }

    }
}
