package edu.ualberta.med.biobank.forms;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.Section;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetInfoAction;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetInfoAction.CEventInfo;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventSaveAction;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventSaveAction.CEventAttrSaveInfo;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventSaveAction.SaveCEventSpecimenInfo;
import edu.ualberta.med.biobank.common.action.collectionEvent.EventAttrInfo;
import edu.ualberta.med.biobank.common.action.patient.PatientGetSimpleCollectionEventInfosAction;
import edu.ualberta.med.biobank.common.action.patient.PatientGetSimpleCollectionEventInfosAction.SimpleCEventInfo;
import edu.ualberta.med.biobank.common.action.patient.PatientNextVisitNumberAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenInfo;
import edu.ualberta.med.biobank.common.action.specimenType.SpecimenTypeGetAllAction;
import edu.ualberta.med.biobank.common.action.study.StudyEventAttrInfo;
import edu.ualberta.med.biobank.common.action.study.StudyGetEventAttrInfoAction;
import edu.ualberta.med.biobank.common.action.study.StudyGetSourceSpecimensAction;
import edu.ualberta.med.biobank.common.formatters.DateFormatter;
import edu.ualberta.med.biobank.common.peer.CollectionEventPeer;
import edu.ualberta.med.biobank.common.wrappers.CollectionEventWrapper;
import edu.ualberta.med.biobank.common.wrappers.CommentWrapper;
import edu.ualberta.med.biobank.common.wrappers.EventAttrTypeEnum;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;
import edu.ualberta.med.biobank.gui.common.widgets.BgcEntryFormWidgetListener;
import edu.ualberta.med.biobank.gui.common.widgets.DateTimeWidget;
import edu.ualberta.med.biobank.gui.common.widgets.MultiSelectEvent;
import edu.ualberta.med.biobank.gui.common.widgets.utils.ComboSelectionUpdate;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.CollectionEvent;
import edu.ualberta.med.biobank.model.Comment;
import edu.ualberta.med.biobank.model.EventAttrCustom;
import edu.ualberta.med.biobank.model.SourceSpecimen;
import edu.ualberta.med.biobank.model.SpecimenType;
import edu.ualberta.med.biobank.treeview.patient.CollectionEventAdapter;
import edu.ualberta.med.biobank.validators.DoubleNumberValidator;
import edu.ualberta.med.biobank.validators.IntegerNumberValidator;
import edu.ualberta.med.biobank.widgets.ComboAndQuantityWidget;
import edu.ualberta.med.biobank.widgets.SelectMultipleWidget;
import edu.ualberta.med.biobank.widgets.infotables.CommentsInfoTable;
import edu.ualberta.med.biobank.widgets.infotables.NewSpecimenInfoTable.ColumnsShown;
import edu.ualberta.med.biobank.widgets.infotables.entry.CEventSpecimenEntryInfoTable;
import edu.ualberta.med.biobank.widgets.utils.GuiUtil;
import gov.nih.nci.system.applicationservice.ApplicationException;

public class CollectionEventEntryForm extends BiobankEntryForm {

    public static final String ID =
        "edu.ualberta.med.biobank.forms.CollectionEventEntryForm"; //$NON-NLS-1$

    public static final String MSG_NEW_PATIENT_VISIT_OK =
        Messages.CollectionEventEntryForm_creation_msg;

    public static final String MSG_PATIENT_VISIT_OK =
        Messages.CollectionEventEntryForm_edition_msg;

    private CollectionEvent ceventCopy;

    private static class FormPvCustomInfo extends EventAttrCustom {
        private Control control;
    }

    private List<FormPvCustomInfo> pvCustomInfoList;

    private BgcEntryFormWidgetListener listener =
        new BgcEntryFormWidgetListener() {
            @Override
            public void selectionChanged(MultiSelectEvent event) {
                setDirty(true);
            }
        };

    private ComboViewer activityStatusComboViewer;

    private CEventSpecimenEntryInfoTable specimensTable;
    private BgcBaseText visitNumberText;

    private DateTimeWidget timeDrawnWidget;

    private List<SpecimenInfo> sourceSpecimens;

    private CEventInfo ceventInfo;

    private CommentsInfoTable commentEntryTable;

    private CommentWrapper comment = new CommentWrapper(
        SessionManager.getAppService());

    Map<Integer, StudyEventAttrInfo> studyAttrInfos;

    private CollectionEventWrapper cevent = new CollectionEventWrapper(
        SessionManager.getAppService());

    @Override
    public void init() throws Exception {
        Assert.isTrue(adapter instanceof CollectionEventAdapter,
            "Invalid editor input: object of type " //$NON-NLS-1$
                + adapter.getClass().getName());

        ceventCopy = new CollectionEvent();
        if (adapter.getId() == null) {
            ceventInfo = new CEventInfo();
            ceventInfo.cevent = new CollectionEvent();
            ceventInfo.cevent.setPatient(((CollectionEventAdapter) adapter)
                .getPatient());
        } else {
            ceventInfo = SessionManager.getAppService().doAction(
                new CollectionEventGetInfoAction(adapter.getId()));
        }
        studyAttrInfos = SessionManager.getAppService().doAction(
            new StudyGetEventAttrInfoAction(ceventInfo.cevent
                .getPatient().getStudy().getId())).getMap();
        copyCEvent();
        // FIXME log edit action?
        // SessionManager.logEdit(cevent);
        String tabName;
        if (adapter.getId() == null) {
            tabName = Messages.CollectionEventEntryForm_title_new;
        } else {
            tabName =
                NLS.bind(Messages.CollectionEventEntryForm_title_edit,
                    ceventCopy.getVisitNumber());
        }

        setPartName(tabName);
    }

    private void copyCEvent() throws Exception {
        // only value created when is a new cevent.
        ceventCopy.setPatient(ceventInfo.cevent.getPatient());
        if (adapter.getId() == null) {
            ceventCopy.setVisitNumber(SessionManager
                .getAppService()
                .doAction(
                    new PatientNextVisitNumberAction(ceventInfo.cevent
                        .getPatient().getId())).getNextVisitNumber());
            ceventCopy.setActivityStatus(ActivityStatus.ACTIVE);
            sourceSpecimens = new ArrayList<SpecimenInfo>();
        } else {
            ceventCopy.setId(ceventInfo.cevent.getId());
            ceventCopy.setVisitNumber(ceventInfo.cevent.getVisitNumber());
            ceventCopy.setActivityStatus(ceventInfo.cevent.getActivityStatus());
            ceventCopy.setComments(ceventInfo.cevent.getComments());
            sourceSpecimens =
                new ArrayList<SpecimenInfo>(ceventInfo.sourceSpecimenInfos);
        }
        cevent.setWrappedObject(ceventCopy);
        comment.setWrappedObject(new Comment());
    }

    @Override
    protected void createFormContent() throws Exception {
        form.setText(Messages.CollectionEventEntryForm_main_title);
        form.setMessage(getOkMessage(), IMessageProvider.NONE);
        page.setLayout(new GridLayout(1, false));
        createMainSection();
        createCommentSection();
        createSpecimensSection();
        if (adapter.getId() == null) {
            setDirty(true);
        }
    }

    private void createCommentSection() {
        Composite client = createSectionWithClient(Messages.Comments_title);
        GridLayout gl = new GridLayout(2, false);

        client.setLayout(gl);
        commentEntryTable =
            new CommentsInfoTable(client, cevent.getCommentCollection(true));
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        commentEntryTable.setLayoutData(gd);
        createBoundWidgetWithLabel(client, BgcBaseText.class, SWT.MULTI,
            Messages.Comments_add, null, comment, "message", null);

    }

    private void createMainSection() throws Exception {
        Composite client = toolkit.createComposite(page);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        client.setLayout(layout);
        client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkit.paintBordersFor(client);

        createReadOnlyLabelledField(client, SWT.NONE,
            Messages.CollectionEventEntryForm_field_study_label, cevent
                .getPatient().getStudy().getName());

        createReadOnlyLabelledField(client, SWT.NONE,
            Messages.CollectionEventEntryForm_field_patient_label, cevent
                .getPatient().getPnumber());

        visitNumberText =
            (BgcBaseText) createBoundWidgetWithLabel(
                client,
                BgcBaseText.class,
                SWT.NONE,
                Messages.CollectionEventEntryForm_field_visitNumber_label,
                null,
                cevent,
                CollectionEventPeer.VISIT_NUMBER.getName(),
                new IntegerNumberValidator(
                    Messages.CollectionEventEntryForm_field_visitNumber_validation_msg,
                    false));

        visitNumberText.addSelectionChangedListener(listener);
        setFirstControl(visitNumberText);

        activityStatusComboViewer =
            createComboViewer(
                client,
                Messages.label_activity,
                ActivityStatus.valuesList(),
                cevent.getActivityStatus(),
                Messages.CollectionEventEntryForm_field_activity_validation_msg,
                new ComboSelectionUpdate() {
                    @Override
                    public void doSelection(Object selectedObject) {
                        if (selectedObject != cevent.getActivityStatus()) {
                            setDirty(true);
                        }
                        cevent
                            .setActivityStatus((ActivityStatus) selectedObject);
                    }
                });

        widgetCreator.createLabel(client,
            Messages.CollectionEventEntryForm_timeDrawn_label);
        timeDrawnWidget =
            new DateTimeWidget(client, SWT.DATE | SWT.TIME, new Date());
        toolkit.adapt(timeDrawnWidget);

        createEventAttrSection(client);
    }

    private void createEventAttrSection(Composite client) throws Exception {
        pvCustomInfoList = new ArrayList<FormPvCustomInfo>();

        for (Entry<Integer, StudyEventAttrInfo> entry : studyAttrInfos
            .entrySet()) {
            FormPvCustomInfo pvCustomInfo = new FormPvCustomInfo();
            pvCustomInfo.setStudyEventAttrId(entry.getValue().attr.getId());
            pvCustomInfo.setLabel(entry.getValue().attr.getGlobalEventAttr()
                .getLabel());
            pvCustomInfo.setType(entry.getValue().type);
            pvCustomInfo.setAllowedValues(entry.getValue()
                .getStudyEventAttrPermissible());
            // FIXME ugly
            EventAttrInfo eventAttrInfo = (adapter.getId() == null)
                ? null : ceventInfo.eventAttrs.get(entry.getKey());
            String origValue = (eventAttrInfo == null)
                ? "" : eventAttrInfo.attr.getValue(); //$NON-NLS-1$ 
            pvCustomInfo.setValue(origValue);
            pvCustomInfo.setOrigValue(origValue);
            pvCustomInfo.control = getControlForLabel(client, pvCustomInfo);
            pvCustomInfoList.add(pvCustomInfo);
        }
    }

    private void createSpecimensSection() {
        Section section =
            createSection(Messages.CollectionEventEntryForm_specimens_title);
        specimensTable =
            new CEventSpecimenEntryInfoTable(section, sourceSpecimens,
                ceventCopy, ColumnsShown.CEVENT_SOURCE_SPECIMENS);
        specimensTable.adaptToToolkit(toolkit, true);
        specimensTable.addSelectionChangedListener(listener);
        try {
            final List<SpecimenType> allSpecimenTypes =
                SessionManager.getAppService()
                    .doAction(new SpecimenTypeGetAllAction()).getList();
            final List<SourceSpecimen> studySourceSpecimens =
                SessionManager
                    .getAppService()
                    .doAction(
                        new StudyGetSourceSpecimensAction(ceventInfo.cevent
                            .getPatient().getStudy().getId())).getList();

            specimensTable.addEditSupport(studySourceSpecimens,
                allSpecimenTypes);
            addSectionToolbar(section,
                Messages.CollectionEventEntryForm_specimens_add_title,
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        form.setFocus();
                        specimensTable.addOrEditSpecimen(true, null,
                            studySourceSpecimens, allSpecimenTypes, ceventCopy,
                            timeDrawnWidget.getDate());
                    }
                });
        } catch (ApplicationException e) {
            BgcPlugin.openAsyncError(
                Messages.CollectionEventEntryForm_specimenstypes_error_msg, e);
        }
        section.setClient(specimensTable);
    }

    private Control getControlForLabel(Composite client,
        FormPvCustomInfo pvCustomInfo) {
        Control control;
        if (EventAttrTypeEnum.NUMBER == pvCustomInfo.getType()) {
            control = createBoundWidgetWithLabel(
                client, BgcBaseText.class, SWT.NONE, pvCustomInfo.getLabel(),
                null, pvCustomInfo, FormPvCustomInfo.VALUE_BIND_STRING,
                new DoubleNumberValidator(
                    Messages.CollectionEventEntryForm_number_validation_msg));
        } else if (EventAttrTypeEnum.TEXT == pvCustomInfo.getType()) {
            control =
                createBoundWidgetWithLabel(client, BgcBaseText.class, SWT.NONE,
                    pvCustomInfo.getLabel(), null, pvCustomInfo,
                    FormPvCustomInfo.VALUE_BIND_STRING, null);
        } else if (EventAttrTypeEnum.DATE_TIME == pvCustomInfo.getType()) {
            control =
                createDateTimeWidget(client, pvCustomInfo.getLabel(),
                    DateFormatter.parseToDateTime(pvCustomInfo.getValue()),
                    null, null);
        } else if (EventAttrTypeEnum.SELECT_SINGLE == pvCustomInfo.getType()) {
            control =
                createBoundWidgetWithLabel(client, Combo.class, SWT.NONE,
                    pvCustomInfo.getLabel(), pvCustomInfo.getAllowedValues(),
                    pvCustomInfo, FormPvCustomInfo.VALUE_BIND_STRING, null);
        } else if (EventAttrTypeEnum.SELECT_MULTIPLE == pvCustomInfo.getType()) {
            widgetCreator.createLabel(client, pvCustomInfo.getLabel());
            SelectMultipleWidget s =
                new SelectMultipleWidget(client, SWT.BORDER,
                    pvCustomInfo.getAllowedValues(), selectionListener);
            s.adaptToToolkit(toolkit, true);
            if (pvCustomInfo.getValue() != null) {
                s.setSelections(pvCustomInfo.getValue().split(
                    FormPvCustomInfo.VALUE_MULTIPLE_SEPARATOR));
            }
            control = s;
        } else {
            Assert.isTrue(false,
                "Invalid pvInfo type: " + pvCustomInfo.getType()); //$NON-NLS-1$
            return null;
        }
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        control.setLayoutData(gd);
        return control;
    }

    private void updateControlForLabel(FormPvCustomInfo pvCustomInfo) {
        if ((EventAttrTypeEnum.NUMBER == pvCustomInfo.getType())
            || (EventAttrTypeEnum.TEXT == pvCustomInfo.getType())
            || (EventAttrTypeEnum.DATE_TIME == pvCustomInfo.getType())
            || (EventAttrTypeEnum.SELECT_SINGLE == pvCustomInfo.getType())) {
            ((BgcBaseText) pvCustomInfo.control).setText(pvCustomInfo
                .getOrigValue());
        } else if (EventAttrTypeEnum.SELECT_MULTIPLE == pvCustomInfo.getType()) {
            ((SelectMultipleWidget) pvCustomInfo.control)
                .setSelections(pvCustomInfo.getOrigValue().split(";"));
        } else {
            Assert.isTrue(false,
                "Invalid pvInfo type: " + pvCustomInfo.getType()); //$NON-NLS-1$
        }
    }

    @Override
    protected String getOkMessage() {
        return (adapter.getId() == null) ? MSG_NEW_PATIENT_VISIT_OK
            : MSG_PATIENT_VISIT_OK;
    }

    @Override
    protected void doBeforeSave() throws Exception {
        for (FormPvCustomInfo combinedPvInfo : pvCustomInfoList) {
            // set the value from the widget
            savePvInfoValueFromControlType(combinedPvInfo);
        }
    }

    @Override
    protected void saveForm() throws Exception {
        List<SaveCEventSpecimenInfo> cevents =
            new ArrayList<CollectionEventSaveAction.SaveCEventSpecimenInfo>();
        for (Object o : specimensTable.getList()) {
            SpecimenInfo specInfo = (SpecimenInfo) o;
            SaveCEventSpecimenInfo ceSpecInfo = new SaveCEventSpecimenInfo();
            // TODO: why is this even being sent to the server, unless comments
            // were modified here and need to be saved?
            // ceSpecInfo.comments = specInfo.specimen.getCommentCollection();
            ceSpecInfo.id = specInfo.specimen.getId();
            ceSpecInfo.inventoryId = specInfo.specimen.getInventoryId();
            ceSpecInfo.quantity = specInfo.specimen.getQuantity();
            ceSpecInfo.specimenTypeId =
                specInfo.specimen.getSpecimenType().getId();
            ceSpecInfo.activityStatus = specInfo.specimen.getActivityStatus();
            ceSpecInfo.createdAt = specInfo.specimen.getCreatedAt();
            ceSpecInfo.centerId =
                SessionManager.getUser().getCurrentWorkingCenter().getId();
            cevents.add(ceSpecInfo);
        }

        List<CEventAttrSaveInfo> ceventAttrList =
            new ArrayList<CollectionEventSaveAction.CEventAttrSaveInfo>();
        for (FormPvCustomInfo combinedPvInfo : pvCustomInfoList) {
            CEventAttrSaveInfo ceventAttr = new CEventAttrSaveInfo();
            ceventAttr.studyEventAttrId = combinedPvInfo.getStudyEventAttrId();
            ceventAttr.value = combinedPvInfo.getValue();
            ceventAttr.type = combinedPvInfo.getType();
            ceventAttrList.add(ceventAttr);
        }

        // save the collection event
        Integer savedCeventId =
            SessionManager
                .getAppService()
                .doAction(
                    new CollectionEventSaveAction(ceventCopy.getId(),
                        ceventCopy.getPatient().getId(), ceventCopy
                            .getVisitNumber(), ceventCopy.getActivityStatus(),
                        comment.getMessage(), cevents, ceventAttrList)).getId();
        PatientGetSimpleCollectionEventInfosAction action =
            new PatientGetSimpleCollectionEventInfosAction(ceventCopy
                .getPatient().getId());
        Map<Integer, SimpleCEventInfo> infos =
            SessionManager.getAppService().doAction(action).getMap();
        ((CollectionEventAdapter) adapter).setValue(infos.get(savedCeventId));
    }

    @Override
    protected boolean openViewAfterSaving() {
        return true;
    }

    private void savePvInfoValueFromControlType(FormPvCustomInfo pvCustomInfo) {
        // for text and combo, the databinding is used
        if (pvCustomInfo.control instanceof DateTimeWidget) {
            pvCustomInfo.setValue(((DateTimeWidget) pvCustomInfo.control)
                .getText());
        } else if (pvCustomInfo.control instanceof ComboAndQuantityWidget) {
            pvCustomInfo
                .setValue(((ComboAndQuantityWidget) pvCustomInfo.control)
                    .getText());
        } else if (pvCustomInfo.control instanceof SelectMultipleWidget) {
            String[] values =
                ((SelectMultipleWidget) pvCustomInfo.control).getSelections();
            pvCustomInfo.setValue(StringUtils.join(values,
                FormPvCustomInfo.VALUE_MULTIPLE_SEPARATOR));
        }
    }

    @Override
    public String getNextOpenedFormId() {
        return CollectionEventViewForm.ID;
    }

    @Override
    public void setValues() throws Exception {
        GuiUtil
            .reset(activityStatusComboViewer, cevent.getActivityStatus());
        specimensTable.reload(sourceSpecimens);
        commentEntryTable.setList(ModelWrapper.wrapModelCollection(
            SessionManager.getAppService(), ceventInfo.cevent.getComments(),
            CommentWrapper.class));
        resetPvCustomInfo();
    }

    private void resetPvCustomInfo() throws Exception {
        for (FormPvCustomInfo pvCustomInfo : pvCustomInfoList) {
            updateControlForLabel(pvCustomInfo);
        }
    }
}
