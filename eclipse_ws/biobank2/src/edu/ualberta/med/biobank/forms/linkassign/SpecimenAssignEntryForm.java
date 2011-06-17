package edu.ualberta.med.biobank.forms.linkassign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

import edu.ualberta.med.biobank.Messages;
import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.peer.ContainerPeer;
import edu.ualberta.med.biobank.common.scanprocess.data.AssignProcessData;
import edu.ualberta.med.biobank.common.scanprocess.data.ProcessData;
import edu.ualberta.med.biobank.common.util.RowColPos;
import edu.ualberta.med.biobank.common.wrappers.ActivityStatusWrapper;
import edu.ualberta.med.biobank.common.wrappers.CollectionEventWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.common.wrappers.SpecimenWrapper;
import edu.ualberta.med.biobank.forms.listener.EnterKeyToNextFieldListener;
import edu.ualberta.med.biobank.gui.common.BiobankGuiCommonPlugin;
import edu.ualberta.med.biobank.gui.common.BiobankLogger;
import edu.ualberta.med.biobank.validators.AbstractValidator;
import edu.ualberta.med.biobank.validators.NonEmptyStringValidator;
import edu.ualberta.med.biobank.validators.StringLengthValidator;
import edu.ualberta.med.biobank.widgets.BiobankText;
import edu.ualberta.med.biobank.widgets.grids.ScanPalletDisplay;
import edu.ualberta.med.biobank.widgets.grids.cell.PalletCell;
import edu.ualberta.med.biobank.widgets.grids.cell.UICellStatus;
import edu.ualberta.med.biobank.widgets.utils.ComboSelectionUpdate;
import edu.ualberta.med.scannerconfig.dmscanlib.ScanCell;
import gov.nih.nci.system.applicationservice.ApplicationException;

public class SpecimenAssignEntryForm extends AbstractLinkAssignEntryForm {

    public static final String ID = "edu.ualberta.med.biobank.forms.SpecimenAssignEntryForm"; //$NON-NLS-1$

    private static BiobankLogger logger = BiobankLogger
        .getLogger(SpecimenAssignEntryForm.class.getName());

    private static Mode mode = Mode.MULTIPLE;

    private static final String INVENTORY_ID_BINDING = "inventoryId-binding"; //$NON-NLS-1$

    private static final String NEW_SINGLE_POSITION_BINDING = "newSinglePosition-binding"; //$NON-NLS-1$

    private static final String OLD_SINGLE_POSITION_BINDING = "oldSinglePosition-binding"; //$NON-NLS-1$

    private static final String PRODUCT_BARCODE_BINDING = "productBarcode-binding"; //$NON-NLS-1$

    private static final String LABEL_BINDING = "label-binding"; //$NON-NLS-1$

    private static final String PALLET_TYPES_BINDING = "palletType-binding"; //$NON-NLS-1$

    protected static boolean useScanner = true;

    // for single specimen assign
    private Button cabinetCheckButton;
    private BiobankText inventoryIdText;
    protected boolean inventoryIdModified;
    private Label oldSinglePositionLabel;
    private BiobankText oldSinglePositionText;
    private Label oldSinglePositionCheckLabel;
    private AbstractValidator oldSinglePositionCheckValidator;
    private BiobankText oldSinglePositionCheckText;
    private Label newSinglePositionLabel;
    private StringLengthValidator newSinglePositionValidator;
    private BiobankText newSinglePositionText;
    protected boolean positionTextModified;
    private BiobankText singleTypeText;
    private BiobankText singleCollectionDateText;

    // for multiple specimens assign
    private ContainerWrapper currentMultipleContainer;
    protected boolean palletproductBarcodeTextModified;
    private NonEmptyStringValidator productBarcodeValidator;
    protected boolean isModifyingMultipleFields;
    private NonEmptyStringValidator palletLabelValidator;
    private BiobankText palletPositionText;
    protected boolean useNewProductBarcode;
    private ContainerWrapper containerToRemove;
    private ComboViewer palletTypesViewer;
    protected boolean palletPositionTextModified;
    private List<ContainerTypeWrapper> palletContainerTypes;
    private BiobankText palletproductBarcodeText;
    private boolean saveEvenIfMissing;
    private boolean isFakeScanLinkedOnly;
    private Button fakeScanLinkedOnlyButton;
    private Composite multipleOptionsFields;
    private Composite fakeScanComposite;
    private Button useScannerButton;
    private Label palletproductBarcodeLabel;
    private boolean isNewMultipleContainer;
    private boolean checkingMultipleContainerPosition;

    private boolean initWithProduct = false;

    @Override
    protected void init() throws Exception {
        super.init();
        setCanLaunchScan(true);
        currentMultipleContainer = new ContainerWrapper(appService);
        initPalletValues();
    }

    /**
     * Multiple. initialize pallet
     */
    private void initPalletValues() {
        try {
            currentMultipleContainer.initObjectWith(new ContainerWrapper(
                appService));
            currentMultipleContainer.reset();
            currentMultipleContainer.setActivityStatus(ActivityStatusWrapper
                .getActiveActivityStatus(appService));
            currentMultipleContainer.setSite(SessionManager.getUser()
                .getCurrentWorkingSite());
        } catch (Exception e) {
            logger.error("Error while reseting pallet values", e); //$NON-NLS-1$
        }
    }

    @Override
    protected String getActivityTitle() {
        return Messages.getString("SpecimenAssign.activity.title"); //$NON-NLS-1$
    }

    @Override
    public BiobankLogger getErrorLogger() {
        return logger;
    }

    @Override
    protected String getFormTitle() {
        return Messages.getString("SpecimenAssign.form.title"); //$NON-NLS-1$
    }

    @Override
    protected boolean isSingleMode() {
        return mode.isSingleMode();
    }

    @Override
    protected void setMode(Mode m) {
        mode = m;
    }

    @Override
    protected void setFirstControl(Mode mode) {
        if (mode.isSingleMode())
            setFirstControl(inventoryIdText);
        else
            setFirstControl(useScannerButton);
    }

    @Override
    protected String getOkMessage() {
        return Messages.getString("SpecimenAssign.okmessage"); //$NON-NLS-1$
    }

    @Override
    public String getNextOpenedFormID() {
        return ID;
    }

    @Override
    protected void createCommonFields(Composite commonFieldsComposite) {
        BiobankText siteLabel = createReadOnlyLabelledField(
            commonFieldsComposite, SWT.NONE,
            Messages.getString("SpecimenAssign.site.label")); //$NON-NLS-1$
        siteLabel.setText(SessionManager.getUser().getCurrentWorkingCenter()
            .getNameShort());
    }

    @Override
    protected int getLeftSectionWidth() {
        return 450;
    }

    @Override
    protected void createSingleFields(Composite parent) {
        Composite fieldsComposite = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        fieldsComposite.setLayout(layout);
        toolkit.paintBordersFor(fieldsComposite);
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        fieldsComposite.setLayoutData(gd);

        // check box to say it is a cabinet specimen or not
        Label cabinetCheckButtonLabel = widgetCreator.createLabel(
            fieldsComposite,
            Messages.getString("SpecimenAssign.single.cabinet.check.label")); //$NON-NLS-1$
        cabinetCheckButton = toolkit.createButton(fieldsComposite,
            "", SWT.CHECK); //$NON-NLS-1$
        cabinetCheckButton.setToolTipText(Messages
            .getString("SpecimenAssign.single.cabinet.check.tooltip")); //$NON-NLS-1$
        cabinetCheckButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (cabinetCheckButton.getSelection()) {
                    if (inventoryIdText.getText().length() == 4) {
                        // compatibility with old cabinet specimens imported
                        // 4 letters specimens are now C+4letters
                        inventoryIdText
                            .setText("C" + inventoryIdText.getText()); //$NON-NLS-1$
                        focusControl(inventoryIdText);
                    }
                }
            }
        });
        // this check box is there only for cbsr : specimens imported from
        // cabinet from the old database where 4 letters. A 'C' has been added
        // to them to make them different from the freezer specimen with exactly
        // the same inventory id
        boolean cbsrCenter = SessionManager.getUser().isCBSRCenter();
        widgetCreator.showWidget(cabinetCheckButtonLabel, cbsrCenter);
        widgetCreator.showWidget(cabinetCheckButton, cbsrCenter);

        // inventoryID
        inventoryIdText = (BiobankText) createBoundWidgetWithLabel(
            fieldsComposite,
            BiobankText.class,
            SWT.NONE,
            Messages.getString("SpecimenAssign.single.inventoryId.label"), new String[0], //$NON-NLS-1$
            singleSpecimen,
            "inventoryId", //$NON-NLS-1$
            new NonEmptyStringValidator(Messages
                .getString("SpecimenAssign.single.inventoryId.validator.msg")), //$NON-NLS-1$
            INVENTORY_ID_BINDING);
        inventoryIdText.addKeyListener(textFieldKeyListener);
        inventoryIdText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (inventoryIdModified)
                    try {
                        retrieveSingleSpecimenData();
                    } catch (Exception ex) {
                        BiobankGuiCommonPlugin.openError(
                            "Move - specimen error", ex); //$NON-NLS-1$
                        focusControl(inventoryIdText);
                    }
                inventoryIdModified = false;
            }
        });
        inventoryIdText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                inventoryIdModified = true;
                displaySinglePositions(false);
                canSaveSingleSpecimen.setValue(false);
            }
        });

        singleTypeText = (BiobankText) createLabelledWidget(fieldsComposite,
            BiobankText.class, SWT.NONE,
            Messages.getString("SpecimenAssign.single.type.label"));
        singleTypeText.setEnabled(false);

        singleCollectionDateText = (BiobankText) createLabelledWidget(
            fieldsComposite, BiobankText.class, SWT.NONE,
            Messages.getString("SpecimenAssign.single.collection.date.label"));
        singleCollectionDateText.setEnabled(false);

        createSinglePositionFields(fieldsComposite);
    }

    /**
     * 
     * Single assign. Search the specimen, if find it, display related
     * information
     */
    protected void retrieveSingleSpecimenData() throws Exception {
        String inventoryId = inventoryIdText.getText();
        singleTypeText.setText("");
        singleCollectionDateText.setText("");
        if (inventoryId.isEmpty()) {
            return;
        }
        if (cabinetCheckButton.getSelection() && inventoryId.length() == 4) {
            // compatibility with old cabinet specimens imported
            // 4 letters specimens are now C+4letters
            inventoryId = "C" + inventoryId; //$NON-NLS-1$
        }
        reset();
        singleSpecimen.setInventoryId(inventoryId);
        inventoryIdText.setText(inventoryId);
        oldSinglePositionCheckText.setText("?"); //$NON-NLS-1$

        appendLog(Messages.getString(
            "SpecimenAssign.single.activitylog.gettingInfoId", //$NON-NLS-1$
            singleSpecimen.getInventoryId()));
        SpecimenWrapper foundSpecimen = SpecimenWrapper.getSpecimen(appService,
            singleSpecimen.getInventoryId());
        if (foundSpecimen == null) {
            throw new Exception(Messages.getString(
                "SpecimenAssign.single.inventoryId.error", //$NON-NLS-1$
                singleSpecimen.getInventoryId()));
        }
        singleSpecimen.initObjectWith(foundSpecimen);
        if (singleSpecimen.isUsedInDispatch()) {
            throw new Exception(
                Messages
                    .getString("SpecimenAssign.single.specimen.transit.error")); //$NON-NLS-1$
        }
        singleTypeText.setText(singleSpecimen.getSpecimenType().getNameShort());
        singleCollectionDateText.setText(singleSpecimen.getTopSpecimen()
            .getFormattedCreatedAt());
        String positionString = singleSpecimen.getPositionString(true, false);
        if (positionString == null) {
            displayOldSingleFields(false);
            positionString = Messages.getString("SpecimenAssign.position.none"); //$NON-NLS-1$
            focusControl(newSinglePositionText);
        } else {
            displayOldSingleFields(true);
            oldSinglePositionCheckText.setText(oldSinglePositionCheckText
                .getText());
            focusControl(oldSinglePositionCheckText);
        }
        oldSinglePositionText.setText(positionString);
        appendLog(Messages.getString(
            "SpecimenAssign.single.activitylog.specimenInfo", //$NON-NLS-1$
            singleSpecimen.getInventoryId(), positionString));
        canSaveSingleSpecimen.setValue(true);
    }

    /**
     * Single assign: Some fields will be displayed only if the specimen has
     * already a position
     */
    private void createSinglePositionFields(Composite fieldsComposite) {
        // for move mode: display old position retrieved from database
        oldSinglePositionLabel = widgetCreator.createLabel(fieldsComposite,
            Messages.getString("SpecimenAssign.single.old.position.label")); //$NON-NLS-1$
        oldSinglePositionText = (BiobankText) widgetCreator.createBoundWidget(
            fieldsComposite, BiobankText.class, SWT.NONE,
            oldSinglePositionLabel, new String[0], null, null);
        oldSinglePositionText.setEnabled(false);
        oldSinglePositionText
            .addKeyListener(EnterKeyToNextFieldListener.INSTANCE);

        // for move mode: field to enter old position. Check needed to be sure
        // nothing is wrong with the specimen
        oldSinglePositionCheckLabel = widgetCreator.createLabel(
            fieldsComposite, Messages
                .getString("SpecimenAssign.single.old.position.check.label")); //$NON-NLS-1$
        oldSinglePositionCheckValidator = new AbstractValidator(
            Messages
                .getString("SpecimenAssign.single.old.position.check.validation.msg")) { //$NON-NLS-1$
            @Override
            public IStatus validate(Object value) {
                if (value != null && !(value instanceof String)) {
                    throw new RuntimeException(
                        "Not supposed to be called for non-strings."); //$NON-NLS-1$
                }

                if (value != null) {
                    String s = (String) value;
                    if (s.equals(oldSinglePositionText.getText())) {
                        hideDecoration();
                        return Status.OK_STATUS;
                    }
                }
                showDecoration();
                return ValidationStatus.error(errorMessage);
            }
        };
        oldSinglePositionCheckText = (BiobankText) widgetCreator
            .createBoundWidget(fieldsComposite, BiobankText.class, SWT.NONE,
                oldSinglePositionCheckLabel, new String[0], new WritableValue(
                    "", String.class), oldSinglePositionCheckValidator, //$NON-NLS-1$
                OLD_SINGLE_POSITION_BINDING);
        oldSinglePositionCheckText
            .addKeyListener(EnterKeyToNextFieldListener.INSTANCE);

        // for all modes: position to be assigned to the specimen
        newSinglePositionLabel = widgetCreator.createLabel(fieldsComposite,
            Messages.getString("SpecimenAssign.single.position.label")); //$NON-NLS-1$
        newSinglePositionValidator = new StringLengthValidator(4,
            Messages.getString("SpecimenAssign.single.position.validationMsg")); //$NON-NLS-1$
        displayOldSingleFields(false);
        newSinglePositionText = (BiobankText) widgetCreator.createBoundWidget(
            fieldsComposite, BiobankText.class, SWT.NONE,
            newSinglePositionLabel, new String[0], new WritableValue("", //$NON-NLS-1$
                String.class), newSinglePositionValidator,
            NEW_SINGLE_POSITION_BINDING);
        newSinglePositionText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (positionTextModified
                    && newSinglePositionValidator
                        .validate(newSinglePositionText.getText()) == Status.OK_STATUS) {
                    BusyIndicator.showWhile(PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell().getDisplay(),
                        new Runnable() {
                            @Override
                            public void run() {
                                initContainersFromPosition(
                                    newSinglePositionText, false, null);
                                checkPositionAndSpecimen(inventoryIdText,
                                    newSinglePositionText);
                            }
                        });
                }
                positionTextModified = false;
            }
        });
        newSinglePositionText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                positionTextModified = true;
                displaySinglePositions(false);
                canSaveSingleSpecimen.setValue(false);
            }
        });
        newSinglePositionText
            .addKeyListener(EnterKeyToNextFieldListener.INSTANCE);
        displayOldSingleFields(false);
    }

    /**
     * Single assign: show or hide old positions fields
     */
    private void displayOldSingleFields(boolean displayOld) {
        widgetCreator.showWidget(oldSinglePositionLabel, displayOld);
        widgetCreator.showWidget(oldSinglePositionText, displayOld);
        widgetCreator.showWidget(oldSinglePositionCheckLabel, displayOld);
        widgetCreator.showWidget(oldSinglePositionCheckText, displayOld);
        if (displayOld) {
            newSinglePositionLabel.setText(Messages
                .getString("SpecimenAssign.single.new.position.label") //$NON-NLS-1$
                + ":"); //$NON-NLS-1$
        } else {
            newSinglePositionLabel.setText(Messages
                .getString("SpecimenAssign.single.position.label") //$NON-NLS-1$
                + ":"); //$NON-NLS-1$
            oldSinglePositionCheckText.setText(oldSinglePositionText.getText());
        }
        page.layout(true, true);
    }

    @Override
    protected void createMultipleFields(Composite parent)
        throws ApplicationException {
        multipleOptionsFields = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        multipleOptionsFields.setLayout(layout);
        toolkit.paintBordersFor(multipleOptionsFields);
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        multipleOptionsFields.setLayoutData(gd);

        productBarcodeValidator = new NonEmptyStringValidator(
            Messages
                .getString("SpecimenAssign.multiple.productBarcode.validationMsg"));//$NON-NLS-1$
        palletLabelValidator = new NonEmptyStringValidator(
            Messages
                .getString("SpecimenAssign.multiple.palletLabel.validationMsg"));//$NON-NLS-1$

        widgetCreator.createLabel(multipleOptionsFields,
            Messages.getString("SpecimenAssign.useScanner.check.label")); //$NON-NLS-1$
        useScannerButton = toolkit.createButton(multipleOptionsFields,
            "", SWT.CHECK); //$NON-NLS-1$
        useScannerButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setUseScanner(useScannerButton.getSelection());
            }
        });

        palletproductBarcodeLabel = widgetCreator.createLabel(
            multipleOptionsFields,
            Messages.getString("SpecimenAssign.multiple.productBarcode.label")); //$NON-NLS-1$)
        palletproductBarcodeText = (BiobankText) createBoundWidget(
            multipleOptionsFields, BiobankText.class, SWT.NONE,
            palletproductBarcodeLabel, null, currentMultipleContainer,
            ContainerPeer.PRODUCT_BARCODE.getName(), productBarcodeValidator,
            PRODUCT_BARCODE_BINDING);
        palletproductBarcodeText.addKeyListener(textFieldKeyListener);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        palletproductBarcodeText.setLayoutData(gd);

        palletproductBarcodeText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (palletproductBarcodeTextModified
                    && productBarcodeValidator.validate(
                        currentMultipleContainer.getProductBarcode()).equals(
                        Status.OK_STATUS)) {
                    boolean ok = checkMultipleScanBarcode();
                    setCanLaunchScan(ok);
                    if (!ok)
                        focusControl(palletproductBarcodeText);
                }
                palletproductBarcodeTextModified = false;
            }
        });
        palletproductBarcodeText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (!checkingMultipleContainerPosition) {
                    palletproductBarcodeTextModified = true;
                    palletTypesViewer.setInput(null);
                    currentMultipleContainer.setContainerType(null);
                    palletPositionText.setEnabled(true);
                    palletPositionText.setText(""); //$NON-NLS-1$
                }
            }
        });

        palletPositionText = (BiobankText) createBoundWidgetWithLabel(
            multipleOptionsFields,
            BiobankText.class,
            SWT.NONE,
            Messages.getString("SpecimenAssign.multiple.palletLabel.label"), null, //$NON-NLS-1$
            currentMultipleContainer, ContainerPeer.LABEL.getName(),
            palletLabelValidator, LABEL_BINDING);
        palletPositionText.addKeyListener(EnterKeyToNextFieldListener.INSTANCE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        palletPositionText.setLayoutData(gd);
        palletPositionText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (palletPositionText.isEnabled()
                    && palletPositionTextModified
                    && palletLabelValidator.validate(
                        currentMultipleContainer.getLabel()).equals(
                        Status.OK_STATUS)) {
                    BusyIndicator.showWhile(Display.getDefault(),
                        new Runnable() {
                            @Override
                            public void run() {
                                boolean ok = initWithProduct
                                    || checkMultipleContainerPosition();
                                setCanLaunchScan(ok);
                                initCellsWithContainer(currentMultipleContainer);
                                if (!ok) {
                                    focusControl(palletPositionText);
                                    showOnlyPallet(true);
                                }
                                palletPositionTextModified = false;
                            }
                        });
                }
                palletPositionTextModified = false;
            }
        });
        palletPositionText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (!isModifyingMultipleFields) {
                    palletPositionTextModified = true;
                    palletTypesViewer.setInput(null);
                    currentMultipleContainer.setContainerType(null);
                }
            }
        });

        createPalletTypesViewer(multipleOptionsFields);

        createPlateToScanField(multipleOptionsFields);

        createScanButton(parent);
    }

    private boolean checkMultipleContainerPosition() {
        checkingMultipleContainerPosition = true;
        initContainersFromPosition(palletPositionText, true, null);
        if (parentContainers == null)
            return false;
        try {
            ContainerWrapper parent = parentContainers.get(0);
            ContainerWrapper containerAtPosition = parent
                .getChildByLabel(currentMultipleContainer.getLabel());
            List<ContainerTypeWrapper> possibleTypes = null;
            ContainerTypeWrapper typeSelection = null;
            boolean enableCombo = true;
            if (containerAtPosition == null) {
                // free position for the container
                parent.addChild(
                    currentMultipleContainer.getLabel().replaceAll(
                        parent.getLabel(), ""), currentMultipleContainer); //$NON-NLS-1$
                possibleTypes = getPossibleTypes(parent.getContainerType()
                    .getChildContainerTypeCollection());
                if (possibleTypes.size() == 1) {
                    typeSelection = possibleTypes.get(0);
                }
            } else {
                String barcodeAtPosition = containerAtPosition
                    .getProductBarcode();
                if (barcodeAtPosition != null && !barcodeAtPosition.isEmpty()) {
                    if (!barcodeAtPosition.equals(currentMultipleContainer
                        .getProductBarcode())) {
                        BiobankGuiCommonPlugin
                            .openError(
                                Messages
                                    .getString("SpecimenAssign.multiple.dialog.positionUsed.error.title"), //$NON-NLS-1$
                                Messages
                                    .getString(
                                        "SpecimenAssign.multiple.dialog.positionUsed.error.msg", //$NON-NLS-1$
                                        barcodeAtPosition,
                                        currentMultipleContainer.getSite()
                                            .getNameShort())); //$NON-NLS-1$
                        appendLog(Messages
                            .getString(
                                "SpecimenAssign.multiple.activitylog.pallet.positionUsedMsg", //$NON-NLS-1$
                                barcodeAtPosition, currentMultipleContainer
                                    .getLabel(), currentMultipleContainer
                                    .getSite().getNameShort()));
                        return false;
                    }
                } else {
                    enableCombo = false;
                    // not barcode before: use barcode entered by user
                    if (containerAtPosition.hasSpecimens()) {
                        // Position already physically used but no barcode was
                        // set (old database compatibility)
                        appendLog(Messages
                            .getString(
                                "SpecimenAssign.multiple.activitylog.pallet.positionUsedWithNoProductBarcode", //$NON-NLS-1$
                                currentMultipleContainer.getLabel(),
                                containerAtPosition.getContainerType()
                                    .getName(), currentMultipleContainer
                                    .getProductBarcode()));
                    } else if (containerAtPosition.getContainerType()
                        .getSpecimenTypeCollection().size() > 0) {
                        // Position initialised but not physically used
                        appendLog(Messages
                            .getString(
                                "SpecimenAssign.multiple.activitylog.pallet.positionInitialized", //$NON-NLS-1$
                                currentMultipleContainer.getLabel(),
                                containerAtPosition.getContainerType()
                                    .getName()));
                    } else {
                        BiobankGuiCommonPlugin.openError("Error",
                            "Container found but can't hold specimens");
                        return false;
                    }
                }
                String newBarcode = currentMultipleContainer
                    .getProductBarcode();
                typeSelection = containerAtPosition.getContainerType();
                possibleTypes = getPossibleTypes(Arrays.asList(typeSelection));
                currentMultipleContainer.initObjectWith(containerAtPosition);
                currentMultipleContainer.reset();
                containerAtPosition.reload();
                if (newBarcode != null) {
                    palletproductBarcodeText.setText(newBarcode);
                }
            }
            palletTypesViewer.getCombo().setEnabled(enableCombo);
            palletTypesViewer.setInput(possibleTypes);
            if (possibleTypes.size() == 0) {
                BiobankGuiCommonPlugin
                    .openAsyncError(
                        Messages
                            .getString("SpecimenAssignEntryForm.pallet.96.error.title"), //$NON-NLS-1$
                        Messages
                            .getString("SpecimenAssignEntryForm.pallet.96.error.msg")); //$NON-NLS-1$ 
                typeSelection = null;
                return false;
            }
            if (typeSelection == null)
                palletTypesViewer.getCombo().deselectAll();
            else
                palletTypesViewer.setSelection(new StructuredSelection(
                    typeSelection));
        } catch (Exception ex) {
            BiobankGuiCommonPlugin.openError(Messages
                .getString("SpecimenAssign.multiple.validation.error.title"), //$NON-NLS-1$
                ex);
            appendLog(Messages.getString(
                "SpecimenAssign.multiple.activitylog.error", //$NON-NLS-1$
                ex.getMessage()));
            return false;
        } finally {
            checkingMultipleContainerPosition = false;
        }
        return true;
    }

    /**
     * is use scanner, want only 8*12 pallets. Also check the container type can
     * hold specimens
     */
    private List<ContainerTypeWrapper> getPossibleTypes(
        List<ContainerTypeWrapper> childContainerTypeCollection) {
        List<ContainerTypeWrapper> palletTypes = new ArrayList<ContainerTypeWrapper>();
        for (ContainerTypeWrapper type : childContainerTypeCollection) {
            if (type.getSpecimenTypeCollection().size() > 0
                && (!useScanner || type.isPallet96()))
                palletTypes.add(type);
        }
        return palletTypes;
    }

    protected boolean checkMultipleScanBarcode() {
        try {
            initWithProduct = false;
            ContainerWrapper palletFoundWithProductBarcode = ContainerWrapper
                .getContainerWithProductBarcodeInSite(appService,
                    currentMultipleContainer.getSite(),
                    currentMultipleContainer.getProductBarcode());
            isNewMultipleContainer = palletFoundWithProductBarcode == null;
            if (palletFoundWithProductBarcode != null) {
                // a container with this barcode exists
                if (!palletFoundWithProductBarcode.isPallet96()) {
                    BiobankGuiCommonPlugin
                        .openAsyncError(
                            Messages
                                .getString("SpecimenAssign.multiple.validation.error.title"), //$NON-NLS-1$
                            Messages
                                .getString("SpecimenAssign.barcode.notPallet.error.msg")); //$NON-NLS-1$
                    return false;
                }
                if (!palletPositionText.getText().isEmpty()
                    && !palletPositionText.getText().equals(
                        palletFoundWithProductBarcode.getLabel())) {
                    // a label was entered but is different from the one set to
                    // the pallet retrieved
                    BiobankGuiCommonPlugin
                        .openAsyncError(
                            Messages
                                .getString("SpecimenAssign.multiple.validation.error.title"), //$NON-NLS-1$
                            Messages
                                .getString(
                                    "SpecimenAssign.barcode.exists.different.position.error.msg", //$NON-NLS-1$
                                    palletFoundWithProductBarcode
                                        .getProductBarcode(),
                                    palletFoundWithProductBarcode
                                        .getFullInfoLabel()));
                    return false;
                }
                currentMultipleContainer
                    .initObjectWith(palletFoundWithProductBarcode);
                currentMultipleContainer.reset();
                initWithProduct = true;

                // display the type, which can't be modified.
                palletTypesViewer.getCombo().setEnabled(false);
                palletTypesViewer.setInput(Arrays
                    .asList(palletFoundWithProductBarcode.getContainerType()));
                palletTypesViewer.setSelection(new StructuredSelection(
                    palletFoundWithProductBarcode.getContainerType()));
                appendLog(Messages
                    .getString(
                        "SpecimenAssign.multiple.activitylog.pallet.productBarcode.exists", //$NON-NLS-1$
                        currentMultipleContainer.getProductBarcode(),
                        palletFoundWithProductBarcode.getLabel(),
                        currentMultipleContainer.getSite().getNameShort(),
                        palletFoundWithProductBarcode.getContainerType()
                            .getName()));
                // can't modify the position if exists already
                palletPositionText.setEnabled(false);
                focusPlateToScan();
            }
        } catch (Exception ex) {
            BiobankGuiCommonPlugin
                .openError(
                    Messages
                        .getString("SpecimenAssign.multiple.validation.error.title"), ex); //$NON-NLS-1$
            appendLog(Messages.getString(
                "SpecimenAssign.multiple.activitylog.error", //$NON-NLS-1$
                ex.getMessage()));
            return false;
        }
        return true;
    }

    @Override
    protected void defaultInitialisation() {
        super.defaultInitialisation();
        useScannerButton.setSelection(useScanner);
        setUseScanner(useScanner);
    }

    @Override
    protected void setUseScanner(boolean use) {
        useScanner = use;
        showPlateToScanField(use && !mode.isSingleMode());
        widgetCreator.showWidget(scanButton, use);
        widgetCreator.showWidget(palletproductBarcodeLabel, use);
        widgetCreator.showWidget(palletproductBarcodeText, use);
        widgetCreator.setBinding(PRODUCT_BARCODE_BINDING, use);
        if (fakeScanComposite != null)
            widgetCreator.showWidget(fakeScanComposite, use);
        showScanTubeAloneSwitch(use);
        if (palletTypesViewer != null) {
            palletTypesViewer.setInput(null);
            palletTypesViewer.getCombo().deselectAll();
        }
        if (use) {
            if (isScanTubeAloneMode())
                // want to deactivate it at first in scan mode
                toggleScanTubeAloneMode();
        } else {
            palletproductBarcodeText.setText(""); //$NON-NLS-1$
            currentMultipleContainer.setContainerType(null);
            setScanHasBeenLaunched(true);
            setScanValid(true);
            if (!isScanTubeAloneMode())
                // want to activate tube alone mode if do not use the scanner
                toggleScanTubeAloneMode();

        }
        try {
            reset();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        super.setUseScanner(use);
        page.layout(true, true);
        checkPalletContainerTypes();
    }

    /**
     * assign multiple: list of container types
     */
    private void createPalletTypesViewer(Composite parent)
        throws ApplicationException {
        initPalletContainerTypes();
        palletTypesViewer = widgetCreator.createComboViewer(parent, Messages
            .getString("SpecimenAssign.multiple.palletType.label"), //$NON-NLS-1$
            null, null, Messages
                .getString("SpecimenAssign.multiple.palletType.validationMsg"), //$NON-NLS-1$
            true, PALLET_TYPES_BINDING, new ComboSelectionUpdate() {
                @Override
                public void doSelection(Object selectedObject) {
                    currentMultipleContainer
                        .setContainerType((ContainerTypeWrapper) selectedObject);
                    palletTypesViewer.getCombo().setFocus();
                    if (!useScanner)
                        displayPalletPositions();
                }
            });
    }

    /**
     * Multiple assign. Initialise list of possible pallets (8*12)
     */
    private void initPalletContainerTypes() throws ApplicationException {
        palletContainerTypes = ContainerTypeWrapper.getContainerTypesPallet96(
            appService, SessionManager.getUser().getCurrentWorkingSite());
    }

    private void checkPalletContainerTypes() {
        if (!isSingleMode() && useScanner && palletContainerTypes.size() == 0) {
            BiobankGuiCommonPlugin
                .openAsyncError(
                    Messages
                        .getString("SpecimenAssign.multiple.dialog.noPalletFoundError.title"), //$NON-NLS-1$
                    Messages
                        .getString("SpecimenAssign.multiple.dialog.noPalletFoundError.msg" //$NON-NLS-1$
                        ));
        }
    }

    @Override
    protected void enableFields(boolean enable) {
        super.enableFields(enable);
        multipleOptionsFields.setEnabled(enable);
    }

    @Override
    protected boolean fieldsValid() {
        if (mode.isSingleMode())
            return true;
        IStructuredSelection selection = (IStructuredSelection) palletTypesViewer
            .getSelection();
        return isPlateValid()
            && (!useScanner || productBarcodeValidator.validate(
                palletproductBarcodeText.getText()).equals(Status.OK_STATUS))
            && palletLabelValidator.validate(palletPositionText.getText())
                .equals(Status.OK_STATUS) && selection.size() > 0;
    }

    @Override
    protected void saveForm() throws Exception {
        // FIXME might need to use batch query

        if (mode.isSingleMode())
            saveSingleSpecimen();
        else
            saveMultipleSpecimens();
        setFinished(false);
    }

    private void saveMultipleSpecimens() throws Exception {
        if (saveEvenIfMissing) {
            if (containerToRemove != null) {
                containerToRemove.delete();
            }
            currentMultipleContainer.persist();
            String productBarcode = currentMultipleContainer
                .getProductBarcode();
            String containerType = currentMultipleContainer.getContainerType()
                .getName();
            String palletLabel = currentMultipleContainer.getLabel();
            String siteName = currentMultipleContainer.getSite().getNameShort();
            if (isNewMultipleContainer)
                appendLog(Messages.getString(
                    "SpecimenAssign.multiple.activitylog.pallet.added", //$NON-NLS-1$
                    productBarcode, containerType, palletLabel, siteName));
            int totalNb = 0;
            StringBuffer sb = new StringBuffer(
                Messages
                    .getString("SpecimenAssign.multiple.activilylog.save.start")); //$NON-NLS-1$
            try {
                Map<RowColPos, PalletCell> cells = getCells();
                for (Entry<RowColPos, PalletCell> entry : cells.entrySet()) {
                    RowColPos rcp = entry.getKey();
                    PalletCell cell = entry.getValue();
                    if (cell != null
                        && (cell.getStatus() == UICellStatus.NEW || cell
                            .getStatus() == UICellStatus.MOVED)) {
                        SpecimenWrapper specimen = cell.getSpecimen();
                        if (specimen != null) {
                            specimen.setPosition(rcp);
                            specimen.setParent(currentMultipleContainer);
                            specimen.persist();
                            String posStr = specimen.getPositionString(true,
                                false);
                            if (posStr == null) {
                                posStr = Messages
                                    .getString("SpecimenAssign.position.none"); //$NON-NLS-1$
                            }
                            computeActivityLogMessage(sb, cell, specimen,
                                posStr);
                            totalNb++;
                        }
                    }
                }
            } catch (Exception ex) {
                setScanHasBeenLaunched(false, true);
                throw ex;
            }
            appendLog(sb.toString());
            appendLog(Messages.getString(
                "SpecimenAssign.multiple.activitylog.save.summary", //$NON-NLS-1$ 
                totalNb, currentMultipleContainer.getLabel(),
                currentMultipleContainer.getSite().getNameShort()));
            setFinished(false);
        }
    }

    private void computeActivityLogMessage(StringBuffer sb, PalletCell cell,
        SpecimenWrapper specimen, String posStr) {
        CollectionEventWrapper visit = specimen.getCollectionEvent();
        sb.append(Messages.getString(
            "SpecimenAssign.multiple.activitylog.specimen.assigned", //$NON-NLS-1$
            posStr, currentMultipleContainer.getSite().getNameShort(), cell
                .getValue(), specimen.getSpecimenType().getName(), visit
                .getPatient().getPnumber(), visit.getVisitNumber()));
    }

    private void saveSingleSpecimen() throws Exception {
        singleSpecimen.persist();
    }

    @Override
    protected ProcessData getProcessData() {
        return new AssignProcessData(currentMultipleContainer);
    }

    @Override
    public void onReset() throws Exception {
        super.onReset();
        parentContainers = null;
        // resultShownValue.setValue(Boolean.FALSE);
        // the 2 following lines are needed. The validator won't update if don't
        // do that (why ?)
        inventoryIdText.setText("**"); //$NON-NLS-1$ 
        inventoryIdText.setText(""); //$NON-NLS-1$
        singleTypeText.setText(""); //$NON-NLS-1$ 
        singleCollectionDateText.setText(""); //$NON-NLS-1$ 
        oldSinglePositionText.setText(""); //$NON-NLS-1$
        oldSinglePositionCheckText.setText(""); //$NON-NLS-1$
        newSinglePositionText.setText(""); //$NON-NLS-1$
        displayOldSingleFields(false);

        showOnlyPallet(true);
        form.layout(true, true);
        if (!mode.isSingleMode()) {
            palletproductBarcodeText.setFocus();
        }

        singleSpecimen.reset(); // reset internal values
        setDirty(false);
        initWithProduct = false;
        if (mode.isSingleMode())
            focusControl(inventoryIdText);
        else if (useScanner)
            focusControl(palletproductBarcodeText);
        else
            focusControl(palletPositionText);
    }

    @Override
    public void reset(boolean resetAll) {
        super.reset(resetAll);
        String productBarcode = ""; //$NON-NLS-1$
        String label = ""; //$NON-NLS-1$
        ContainerTypeWrapper type = null;

        if (!resetAll) { // keep fields values
            productBarcode = palletproductBarcodeText.getText();
            label = palletPositionText.getText();
            type = currentMultipleContainer.getContainerType();
        } else {
            if (palletTypesViewer != null) {
                palletTypesViewer.setInput(null);
                palletTypesViewer.getCombo().deselectAll();
            }
            removeRescanMode();
            freezerWidget.setSelection(null);
            hotelWidget.setSelection(null);
            palletWidget.setCells(null);
        }
        setScanHasBeenLaunched(isSingleMode() || !useScanner);
        initPalletValues();

        palletproductBarcodeText.setText(productBarcode);
        productBarcodeValidator.validate(productBarcode);
        palletPositionText.setText(label);
        palletLabelValidator.validate(label);
        currentMultipleContainer.setContainerType(type);
        if (resetAll) {
            setDirty(false);
            useNewProductBarcode = false;
        }
        canSaveSingleSpecimen.setValue(!isSingleMode());
    }

    @Override
    protected void setBindings(boolean isSingleMode) {
        widgetCreator.setBinding(INVENTORY_ID_BINDING, isSingleMode);
        widgetCreator.setBinding(OLD_SINGLE_POSITION_BINDING, isSingleMode);
        oldSinglePositionCheckText.setText("?"); //$NON-NLS-1$
        widgetCreator.setBinding(NEW_SINGLE_POSITION_BINDING, isSingleMode);
        widgetCreator.setBinding(PRODUCT_BARCODE_BINDING, !isSingleMode
            && useScanner);
        widgetCreator.setBinding(LABEL_BINDING, !isSingleMode);
        widgetCreator.setBinding(PALLET_TYPES_BINDING, !isSingleMode);
        super.setBindings(isSingleMode);
        setScanHasBeenLaunched(isSingleMode || !useScanner);
        checkPalletContainerTypes();
        setCanLaunchScan(true);
    }

    @Override
    protected void showModeComposite(Mode mode) {
        boolean single = mode.isSingleMode();
        if (single)
            setFirstControl(inventoryIdText);
        else if (useScanner)
            setFirstControl(palletproductBarcodeText);
        else
            setFirstControl(palletPositionText);
        try {
            reset();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        super.showModeComposite(mode);
    }

    /**
     * Multiple assign
     */
    @Override
    protected boolean canScanTubeAlone(PalletCell cell) {
        return super.canScanTubeAlone(cell)
            || cell.getStatus() == UICellStatus.MISSING;
    }

    /**
     * Multiple assign
     */
    @Override
    protected void launchScanAndProcessResult() {
        super.launchScanAndProcessResult();
        page.layout(true, true);
        book.reflow(true);
        cancelConfirmWidget.setFocus();
    }

    /**
     * Multiple assign
     */
    @Override
    protected void beforeScanThreadStart() {
        showOnlyPallet(false, false);
        currentMultipleContainer.setSite(SessionManager.getUser()
            .getCurrentWorkingSite());
        currentMultipleContainer
            .setContainerType((ContainerTypeWrapper) ((IStructuredSelection) palletTypesViewer
                .getSelection()).getFirstElement());
        isFakeScanLinkedOnly = fakeScanLinkedOnlyButton != null
            && fakeScanLinkedOnlyButton.getSelection();
    }

    /**
     * Multiple assign
     */
    @Override
    protected void afterScanAndProcess(Integer rowOnly) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                cancelConfirmWidget.setFocus();
                displayPalletPositions();
                palletWidget.setCells(getCells());
                setDirty(true);
                setRescanMode();
                page.layout(true, true);
                form.reflow(true);
            }
        });
    }

    /**
     * Multiple assign
     */
    protected void displayPalletPositions() {
        if (currentMultipleContainer.hasParentContainer()) {
            ContainerWrapper hotelContainer = currentMultipleContainer
                .getParentContainer();
            ContainerWrapper freezerContainer = hotelContainer
                .getParentContainer();

            if (freezerContainer != null) {
                freezerLabel.setText(freezerContainer.getFullInfoLabel());
                freezerWidget.setContainerType(freezerContainer
                    .getContainerType());
                freezerWidget
                    .setSelection(hotelContainer.getPositionAsRowCol());
                freezerWidget.redraw();
            }

            hotelLabel.setText(hotelContainer.getFullInfoLabel());
            hotelWidget.setContainerType(hotelContainer.getContainerType());
            hotelWidget.setSelection(currentMultipleContainer
                .getPositionAsRowCol());
            hotelWidget.redraw();

            palletLabel.setText(currentMultipleContainer.getLabel());

            palletWidget.setContainerType(
                currentMultipleContainer.getContainerType(),
                ScanPalletDisplay.SAMPLE_WIDTH);
            palletWidget.setCells(getCells());

            showOnlyPallet(false);

            widgetCreator.showWidget(freezerLabel, freezerContainer != null);
            widgetCreator.showWidget(freezerWidget, freezerContainer != null);
            page.layout(true, true);
        }
    }

    /**
     * Multiple assign
     */
    @Override
    protected Map<RowColPos, PalletCell> getFakeScanCells() throws Exception {
        if (currentMultipleContainer.hasSpecimens()) {
            Map<RowColPos, PalletCell> palletScanned = new HashMap<RowColPos, PalletCell>();
            for (RowColPos pos : currentMultipleContainer.getSpecimens()
                .keySet()) {
                if (pos.row != 0 && pos.col != 2) {
                    palletScanned.put(pos,
                        new PalletCell(new ScanCell(pos.row, pos.col,
                            currentMultipleContainer.getSpecimens().get(pos)
                                .getInventoryId())));
                }
            }
            return palletScanned;
        } else {
            if (isFakeScanLinkedOnly) {
                return PalletCell.getRandomSpecimensNotAssigned(appService,
                    currentMultipleContainer.getSite().getId());
            }
            return PalletCell.getRandomSpecimensAlreadyAssigned(appService,
                currentMultipleContainer.getSite().getId());
        }
    }

    @Override
    protected void doBeforeSave() throws Exception {
        if (!mode.isSingleMode()) {
            saveEvenIfMissing = true;
            if (currentScanState == UICellStatus.MISSING) {
                boolean save = BiobankGuiCommonPlugin
                    .openConfirm(
                        Messages
                            .getString("SpecimenAssign.multiple.dialog.reallySave.title"), //$NON-NLS-1$
                        Messages
                            .getString("SpecimenAssign.multiple.dialog.saveWithMissing.msg")); //$NON-NLS-1$
                if (!save) {
                    setDirty(true);
                    saveEvenIfMissing = false;
                }
            }
        }
    }

    /**
     * Multiple assign
     */
    @Override
    protected void createFakeOptions(Composite fieldsComposite) {
        fakeScanComposite = toolkit.createComposite(fieldsComposite);
        fakeScanComposite.setLayout(new GridLayout());
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        fakeScanComposite.setLayoutData(gd);
        fakeScanLinkedOnlyButton = toolkit.createButton(fakeScanComposite,
            "Select linked only specimens", SWT.RADIO); //$NON-NLS-1$
        fakeScanLinkedOnlyButton.setSelection(true);
        toolkit.createButton(fakeScanComposite,
            "Select linked and assigned specimens", SWT.RADIO); //$NON-NLS-1$
    }

    @Override
    protected Mode initialisationMode() {
        return mode;
    }

    @Override
    protected Composite getFocusedComposite(boolean single) {
        if (single)
            return inventoryIdText;
        if (useScanner)
            return palletproductBarcodeText;
        return palletPositionText;
    }

    @Override
    protected boolean needPlate() {
        return useScanner;
    }

}