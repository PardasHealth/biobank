package edu.ualberta.med.biobank.views;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.patient.PatientSearchAction;
import edu.ualberta.med.biobank.common.action.patient.PatientSearchAction.SearchedPatientInfo;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.treeview.AbstractAdapterBase;
import edu.ualberta.med.biobank.treeview.patient.PatientAdapter;
import edu.ualberta.med.biobank.treeview.patient.PatientSearchedNode;

public class CollectionView extends AbstractAdministrationView {

    public static final String ID =
        "edu.ualberta.med.biobank.views.CollectionView"; //$NON-NLS-1$

    private static CollectionView currentInstance;

    private PatientSearchedNode searchedNode;

    private Button radioPnumber;

    public CollectionView() {
        super();
        currentInstance = this;
        SessionManager.addView(this);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        createNodes();
    }

    @Override
    protected void createTreeTextOptions(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        layout.horizontalSpacing = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 0;
        composite.setLayout(layout);

        radioPnumber = new Button(composite, SWT.RADIO);
        radioPnumber.setText(Messages.CollectionView_patient_label);
        radioPnumber.setSelection(true);
    }

    protected void notFound(String text) {
        boolean create =
            BgcPlugin.openConfirm(Messages.CollectionView_patient_error_title,
                Messages.CollectionView_patient_error_msg);
        if (create) {
            SearchedPatientInfo spi = new SearchedPatientInfo();
            spi.patient = new Patient();
            spi.patient.setPnumber(text);
            AbstractAdapterBase adapter = new PatientAdapter(null, spi);
            adapter.openEntryForm();
        }
    }

    protected PatientSearchedNode createSearchedNode() {
        if (searchedNode == null)
            return new PatientSearchedNode(rootNode, 0);
        else
            return searchedNode;
    }

    public static CollectionView getCurrent() {
        return currentInstance;
    }

    public static PatientAdapter getCurrentPatient() {
        AbstractAdapterBase selectedNode = currentInstance.getSelectedNode();
        if (selectedNode != null && selectedNode instanceof PatientAdapter) {
            return (PatientAdapter) selectedNode;
        }
        return null;
    }

    public static void reloadCurrent() {
        if (currentInstance != null) currentInstance.reload();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected String getTreeTextToolTip() {
        return Messages.CollectionView_patient_tooltip;
    }

    public void showSearchedObjectsInTree(Integer patientId, String pnumber,
        boolean searchIfNotFound, boolean doubleClick) {
        List<AbstractAdapterBase> nodeRes =
            rootNode.search(Patient.class, patientId);
        if (nodeRes.size() == 0 && searchIfNotFound) {
            // this is happening when a new patient is created.
            internalSearch(pnumber);
            // searchedNode.performExpand();
            // nodeRes = searchedNode.search(Patient.class, patient.getId());
        }
        if (nodeRes.size() > 0) {
            if (doubleClick) {
                nodeRes.get(0).performDoubleClick();
            }
        }
    }

    @Override
    protected void internalSearch() {
        String text = treeText.getText();

        if (text.trim().isEmpty()) {
            return;
        }

        internalSearch(text);
    }

    protected void internalSearch(String text) {
        try {
            SearchedPatientInfo pinfo =
                SessionManager.getAppService().doAction(
                    new PatientSearchAction(text.trim()));
            if (pinfo == null) {
                notFound(text);
            } else {
                searchedNode.addPatient(pinfo);
                showSearchedObjectsInTree(pinfo.patient.getId(),
                    pinfo.patient.getPnumber(), false, true);
                getTreeViewer().expandToLevel(searchedNode, 3);
            }
        } catch (Exception e) {
            BgcPlugin.openAsyncError(Messages.CollectionView_search_error_msg,
                e);
        }
    }

    private void createNodes() {
        searchedNode = createSearchedNode();
        rootNode.addChild(searchedNode);
        searchedNode.setParent(rootNode);
    }

    public PatientSearchedNode getSearchedNode() {
        return searchedNode;
    }

    @Override
    public void reload() {
        for (AbstractAdapterBase adapter : rootNode.getChildren())
            adapter.rebuild();
        super.reload();
    }

    @Override
    public void clear() {
        searchedNode.clear();
        setSearchFieldsEnablement(false);
    }

    @Override
    protected void createRootNode() {
        createNewRootNode();
    }

}
