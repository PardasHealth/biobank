package edu.ualberta.med.biobank.widgets.infotables.entry;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.permission.study.StudyUpdatePermission;
import edu.ualberta.med.biobank.common.wrappers.SourceSpecimenWrapper;
import edu.ualberta.med.biobank.common.wrappers.SpecimenTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.StudyWrapper;
import edu.ualberta.med.biobank.dialogs.PagedDialog.NewListener;
import edu.ualberta.med.biobank.dialogs.StudySourceSpecimenDialog;
import edu.ualberta.med.biobank.gui.common.BgcLogger;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.gui.common.widgets.IInfoTableAddItemListener;
import edu.ualberta.med.biobank.gui.common.widgets.IInfoTableDeleteItemListener;
import edu.ualberta.med.biobank.gui.common.widgets.IInfoTableEditItemListener;
import edu.ualberta.med.biobank.gui.common.widgets.InfoTableEvent;
import edu.ualberta.med.biobank.widgets.infotables.BiobankTableSorter;
import edu.ualberta.med.biobank.widgets.infotables.SourceSpecimenInfoTable;
import gov.nih.nci.system.applicationservice.ApplicationException;

public class SourceSpecimenEntryInfoTable extends SourceSpecimenInfoTable {

    private static BgcLogger LOGGER = BgcLogger
        .getLogger(SourceSpecimenEntryInfoTable.class.getName());

    private List<SpecimenTypeWrapper> availableSpecimenTypes;

    private List<SourceSpecimenWrapper> selectedSourceSpecimens;

    private List<SourceSpecimenWrapper> addedOrModifiedSourceSpecimens;

    private List<SourceSpecimenWrapper> deletedSourceSpecimen;

    private StudyWrapper study;

    private boolean isEditable;

    private boolean isDeletable;

    public SourceSpecimenEntryInfoTable(Composite parent,
        List<SourceSpecimenWrapper> collection) {
        super(parent, collection);
        init();
    }

    private void init() {
        try {
            this.isEditable =
                SessionManager.getAppService().isAllowed(
                    new StudyUpdatePermission(study.getId()));
            this.isDeletable =
                SessionManager.getAppService().isAllowed(
                    new StudyUpdatePermission(study.getId()));

        } catch (ApplicationException e) {
            BgcPlugin.openAsyncError("Permission Error",
                "Unable to retrieve user permissions");
        }
    }

    /**
     * 
     * @param parent a composite control which will be the parent of the new
     *            instance (cannot be null)
     * @param study the study the source specimens belong to.
     */
    public SourceSpecimenEntryInfoTable(Composite parent, StudyWrapper study,
        List<SpecimenTypeWrapper> specimenTypes) {
        super(parent, null);
        this.study = study;
        availableSpecimenTypes = specimenTypes;
        selectedSourceSpecimens = study.getSourceSpecimenCollection(true);
        if (selectedSourceSpecimens == null) {
            selectedSourceSpecimens = new ArrayList<SourceSpecimenWrapper>();
        }
        setList(selectedSourceSpecimens);
        addedOrModifiedSourceSpecimens = new ArrayList<SourceSpecimenWrapper>();
        deletedSourceSpecimen = new ArrayList<SourceSpecimenWrapper>();

        setLayout(new GridLayout(1, false));
        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        addEditSupport();

        try {
            this.isEditable =
                SessionManager.getAppService().isAllowed(
                    new StudyUpdatePermission(study.getId()));
            this.isDeletable =
                SessionManager.getAppService().isAllowed(
                    new StudyUpdatePermission(study.getId()));

        } catch (ApplicationException e) {
            BgcPlugin.openAsyncError("Permission Error",
                "Unable to retrieve user permissions");
        }

    }

    @Override
    protected boolean isEditMode() {
        return true;
    }

    public void addSourceSpecimen() {
        SourceSpecimenWrapper newSourceSpecimen = new SourceSpecimenWrapper(
            SessionManager.getAppService());
        // DO NOT set the study on newSourceSpecimen - if it was done
        // then it would have to be unset if the user presses the Cancel button
        addOrEditStudySourceSpecimen(true, newSourceSpecimen);
    }

    private void addOrEditStudySourceSpecimen(final boolean add,
        final SourceSpecimenWrapper sourceSpecimen) {
        List<SpecimenTypeWrapper> dialogSpecimenTypes = availableSpecimenTypes;
        if (!add) {
            dialogSpecimenTypes.add(sourceSpecimen.getSpecimenType());
        }
        NewListener newListener = null;
        if (add) {
            // only add to the collection when adding and not editing
            newListener = new NewListener() {
                @Override
                public void newAdded(Object spec) {
                    SourceSpecimenWrapper ss = (SourceSpecimenWrapper) spec;
                    availableSpecimenTypes.remove(sourceSpecimen
                        .getSpecimenType());
                    selectedSourceSpecimens.add((SourceSpecimenWrapper) spec);
                    addedOrModifiedSourceSpecimens.add(ss);
                    reloadCollection(selectedSourceSpecimens);
                    notifyListeners();
                }
            };
        }
        StudySourceSpecimenDialog dlg =
            new StudySourceSpecimenDialog(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                sourceSpecimen.getNeedOriginalVolume(), sourceSpecimen
                    .getSpecimenType(), dialogSpecimenTypes, newListener);

        int res = dlg.open();
        if (res == Dialog.OK) {
            sourceSpecimen.setNeedOriginalVolume(dlg.getNeedOriginalVolume());
            sourceSpecimen.setSpecimenType(dlg.getSpecimenType());

            if (!add) {
                reloadCollection(selectedSourceSpecimens);
                notifyListeners();
            }
        }
    }

    private void addEditSupport() {
        if (isEditable) {
            addAddItemListener(new IInfoTableAddItemListener<SourceSpecimenWrapper>() {
                @Override
                public void addItem(InfoTableEvent<SourceSpecimenWrapper> event) {
                    addSourceSpecimen();
                }
            });
        }
        if (isEditable) {
            addEditItemListener(new IInfoTableEditItemListener<SourceSpecimenWrapper>() {
                @Override
                public void editItem(InfoTableEvent<SourceSpecimenWrapper> event) {
                    SourceSpecimenWrapper sourceSpecimen = getSelection();
                    if (sourceSpecimen != null)
                        addOrEditStudySourceSpecimen(false, sourceSpecimen);
                }
            });
        }
        if (isDeletable) {
            addDeleteItemListener(new IInfoTableDeleteItemListener<SourceSpecimenWrapper>() {
                @Override
                public void deleteItem(
                    InfoTableEvent<SourceSpecimenWrapper> event) {
                    SourceSpecimenWrapper sourceSpecimen = getSelection();
                    if (sourceSpecimen != null) {
                        if (!MessageDialog
                            .openConfirm(
                                PlatformUI.getWorkbench()
                                    .getActiveWorkbenchWindow().getShell(),
                                Messages.SourceSpecimenEntryInfoTable_delete_title,
                                Messages.SourceSpecimenEntryInfoTable_delete_question)) {
                            return;
                        }

                        selectedSourceSpecimens.remove(sourceSpecimen);
                        setList(selectedSourceSpecimens);
                        deletedSourceSpecimen.add(sourceSpecimen);
                        availableSpecimenTypes.add(sourceSpecimen
                            .getSpecimenType());
                        notifyListeners();
                    }
                }
            });
        }
    }

    public List<SourceSpecimenWrapper> getAddedOrModifiedSourceSpecimens() {
        return addedOrModifiedSourceSpecimens;
    }

    public List<SourceSpecimenWrapper> getDeletedSourceSpecimens() {
        return deletedSourceSpecimen;
    }

    @Override
    public void reload() {
        selectedSourceSpecimens = study.getSourceSpecimenCollection(true);
        if (selectedSourceSpecimens == null) {
            selectedSourceSpecimens = new ArrayList<SourceSpecimenWrapper>();
        }
        reloadCollection(selectedSourceSpecimens);
        addedOrModifiedSourceSpecimens = new ArrayList<SourceSpecimenWrapper>();
        deletedSourceSpecimen = new ArrayList<SourceSpecimenWrapper>();
        init();
    }

    @SuppressWarnings("serial")
    @Override
    protected BiobankTableSorter getComparator() {
        return new BiobankTableSorter() {
            @Override
            public int compare(Object e1, Object e2) {
                try {
                    TableRowData i1 = getCollectionModelObject(e1);
                    TableRowData i2 = getCollectionModelObject(e2);
                    return super.compare(i1.name, i2.name);
                } catch (Exception e) {
                    return 0;
                }
            }
        };
    }

}
