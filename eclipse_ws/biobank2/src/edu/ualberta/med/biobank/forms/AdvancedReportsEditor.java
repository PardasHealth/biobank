package edu.ualberta.med.biobank.forms;

import java.awt.Color;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import ar.com.fdvs.dj.core.DynamicJasperHelper;
import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.domain.Style;
import ar.com.fdvs.dj.domain.builders.FastReportBuilder;
import ar.com.fdvs.dj.domain.constants.Border;
import ar.com.fdvs.dj.domain.constants.Transparency;
import ar.com.fdvs.dj.domain.constants.VerticalAlign;
import edu.ualberta.med.biobank.BioBankPlugin;
import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.reports.ReportTreeNode;
import edu.ualberta.med.biobank.common.reports.advanced.HQLField;
import edu.ualberta.med.biobank.common.reports.advanced.QueryTreeNode;
import edu.ualberta.med.biobank.common.reports.advanced.SearchUtils;
import edu.ualberta.med.biobank.forms.input.ReportInput;
import edu.ualberta.med.biobank.reporting.ReportingUtils;
import edu.ualberta.med.biobank.treeview.QueryTree;
import edu.ualberta.med.biobank.views.ReportsView;
import edu.ualberta.med.biobank.widgets.DateTimeWidget;
import edu.ualberta.med.biobank.widgets.infotables.SearchResultsInfoTable;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class AdvancedReportsEditor extends EditorPart {

    public static String ID = "edu.ualberta.med.biobank.editors.AdvancedReportsEditor";

    private Composite top;
    private Composite buttonSection;
    private Composite parameterSection;

    private Button generateButton;
    private Button saveButton;
    private Button printButton;
    private Button exportButton;

    private List<HQLField> fields;
    private ArrayList<Widget> widgetFields;
    private ArrayList<Combo> operatorFields;
    private ArrayList<Label> textLabels;

    private SearchResultsInfoTable reportTable;
    private List<Object> reportData;
    private ReportTreeNode node;

    private QueryTree tree;
    private QueryTreeNode selectedNode;

    private static Map<Class<?>, int[]> columnWidths;

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException {
        setSite(site);
        setInput(input);

        node = ((ReportInput) input).node;

        reportData = new ArrayList<Object>();
        this.setPartName(node.getLabel());

        columnWidths = SearchUtils.getColumnWidths();
        columnWidths = Collections.unmodifiableMap(columnWidths);
    }

    @Override
    public void createPartControl(Composite parent) {

        top = new Composite(parent, SWT.BORDER);
        GridData gdfill = new GridData();
        gdfill.horizontalAlignment = SWT.FILL;
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        top.setLayout(layout);
        top.setLayoutData(gdfill);

        tree = new QueryTree(top, SWT.BORDER, (QueryTreeNode) node.getQuery());
        tree.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                saveFields();
                selectedNode = (QueryTreeNode) ((IStructuredSelection) event
                    .getSelection()).getFirstElement();
                if (selectedNode != null)
                    displayFields(selectedNode);
            }
        });

        GridData gd = new GridData();
        gd.minimumHeight = 600;
        gd.minimumWidth = 300;
        gd.heightHint = 300;
        gd.widthHint = 250;
        tree.getTree().setLayoutData(gd);

        parameterSection = new Composite(top, SWT.NONE);
        GridData pgd = new GridData();
        pgd.horizontalAlignment = SWT.FILL;
        pgd.grabExcessHorizontalSpace = true;
        parameterSection.setLayoutData(pgd);

        buttonSection = new Composite(top, SWT.NONE);
        GridLayout gl = new GridLayout();
        gl.numColumns = 4;
        buttonSection.setLayout(gl);

        generateButton = new Button(buttonSection, SWT.NONE);
        generateButton.setText("Generate");
        generateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                generate();
            }
        });

        saveButton = new Button(buttonSection, SWT.NONE);
        saveButton.setText("Save");
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                saveFields();
                tree.saveTree(Platform.getInstanceLocation().getURL().getPath()
                    + "/saved_reports/", "asd");
            }
        });

        printButton = new Button(buttonSection, SWT.NONE);
        printButton.setImage(BioBankPlugin.getDefault().getImageRegistry().get(
            BioBankPlugin.IMG_PRINTER));
        printButton.setText("Print");
        printButton.setEnabled(false);
        printButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    printTable(false);
                } catch (Exception ex) {
                    BioBankPlugin.openAsyncError(
                        "Error while printing the results", ex);
                }
            }
        });

        exportButton = new Button(buttonSection, SWT.NONE);
        exportButton.setText("Export");
        exportButton.setEnabled(false);
        exportButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    printTable(true);
                } catch (Exception ex) {
                    BioBankPlugin.openAsyncError(
                        "Error while exporting the results", ex);
                }
            }
        });

        createEmptyReportTable();
        top.layout(true, true);

    }

    public void displayFields(QueryTreeNode node) {
        if (parameterSection != null)
            parameterSection.dispose();
        parameterSection = new Composite(top, SWT.NONE);
        GridLayout gl = new GridLayout();
        gl.marginWidth = 0;
        gl.numColumns = 4;
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.TOP;
        parameterSection.setLayout(gl);
        parameterSection.setLayoutData(gd);

        Label headerLabel = new Label(parameterSection, SWT.NONE);
        GridData gdl = new GridData();
        gdl.horizontalSpan = 4;
        headerLabel.setLayoutData(gdl);
        headerLabel.setText(node.getTreePath());

        widgetFields = new ArrayList<Widget>();
        operatorFields = new ArrayList<Combo>();
        textLabels = new ArrayList<Label>();
        fields = node.getFieldData();
        for (HQLField field : fields) {
            drawField(field);
        }
        parameterSection.moveBelow(tree.getTree());
        top.layout(true, true);
    }

    private void drawField(final HQLField field) {
        Label fieldLabel = new Label(parameterSection, SWT.NONE);
        fieldLabel.setText(field.getFname().replaceAll(".name", "") + ":");
        textLabels.add(fieldLabel);
        Combo operatorCombo = new Combo(parameterSection, SWT.READ_ONLY);
        GridData ogd = new GridData();
        ogd.widthHint = 150;
        operatorCombo.setLayoutData(ogd);
        String[] operators = SearchUtils.getOperatorSet(field.getType())
            .toArray(new String[] {});
        operatorCombo.setItems(operators);
        operatorCombo.select(0);
        if (field.getOperator() != null) {
            for (int i = 0; i < operators.length; i++)
                if (operators[i].compareTo(field.getOperator()) == 0) {
                    operatorCombo.select(i);
                    break;
                }
        }
        operatorFields.add(operatorCombo);
        Widget widget;
        GridData wgd = new GridData();
        wgd.widthHint = 200;
        if (field.getType() == Date.class) {
            widget = new DateTimeWidget(parameterSection, SWT.NONE, null);
            wgd.widthHint = 211;
            ((DateTimeWidget) widget).setLayoutData(wgd);
            if (field.getValue() != null)
                ((DateTimeWidget) widget).setDate((Date) field.getValue());
        } else if (field.getType() == String.class) {
            widget = new Text(parameterSection, SWT.BORDER);
            ((Text) widget).setLayoutData(wgd);
            if (field.getValue() != null)
                ((Text) widget).setText((String) field.getValue());
        } else if (field.getType() == Integer.class) {
            widget = new Text(parameterSection, SWT.BORDER);
            ((Text) widget).setLayoutData(wgd);
            if (field.getValue() != null)
                ((Text) widget)
                    .setText(((Integer) field.getValue()).toString());
        } else
            widget = null;

        widgetFields.add(widget);
        Button plusButton = new Button(parameterSection, SWT.NONE);
        plusButton.setImage(BioBankPlugin.getDefault().getImageRegistry().get(
            BioBankPlugin.IMG_ADD));
        plusButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                saveFields();
                HQLField addedField = new HQLField(field);
                selectedNode.insertField(selectedNode.getFieldData().indexOf(
                    field), addedField);
                displayFields(selectedNode);
            }
        });
    }

    private void generate() {

        saveFields();
        final HashMap<String, String> colInfo = SearchUtils
            .getColumnInfo(((QueryTreeNode) node.getQuery()).getNodeInfo()
                .getType());

        IRunnableContext context = new ProgressMonitorDialog(Display
            .getDefault().getActiveShell());
        try {
            context.run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor) {
                    Thread t = new Thread("Querying") {
                        @Override
                        public void run() {
                            try {
                                reportData = SessionManager.getAppService()
                                    .query(
                                        new HQLCriteria(tree
                                            .compileQuery(colInfo.values())));
                                if (reportData.size() >= 1000)
                                    BioBankPlugin
                                        .openAsyncError(
                                            "Size Limit Exceeded",
                                            "Your search criteria is too general. Please try refining your search. Displaying the first 1000 results.");
                            } catch (Exception e) {
                                BioBankPlugin.openAsyncError(
                                    "Error while querying for results", e);
                            }
                        }
                    };
                    monitor.beginTask("Generating Report...",
                        IProgressMonitor.UNKNOWN);
                    t.start();
                    while (true) {
                        if (monitor.isCanceled()) {
                            // TODO t.stop(); we need a safe way to kill query
                            reportData = new ArrayList<Object>();
                            break;
                        } else if (!t.isAlive())
                            break;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            monitor.done();
                            if (!reportData.isEmpty()) {
                                printButton.setEnabled(true);
                                exportButton.setEnabled(true);
                            } else {
                                printButton.setEnabled(false);
                                exportButton.setEnabled(false);
                            }
                            reportTable.dispose();
                            reportTable = new SearchResultsInfoTable(top,
                                reportData, colInfo.keySet().toArray(
                                    new String[] {}), columnWidths
                                    .get(((QueryTreeNode) node.getQuery())
                                        .getNodeInfo().getType()));
                            GridData gd = new GridData();
                            gd.grabExcessHorizontalSpace = true;
                            gd.grabExcessVerticalSpace = true;
                            gd.horizontalSpan = 2;
                            gd.horizontalAlignment = SWT.FILL;
                            gd.verticalAlignment = SWT.FILL;
                            reportTable.setLayoutData(gd);
                            setEnabled(true);
                            top.layout();
                        }

                    });

                }
            });
        } catch (Exception e) {
            BioBankPlugin.openAsyncError("Query Error", e);
        }
    }

    private void createEmptyReportTable() {
        if (reportTable != null) {
            reportTable.dispose();
        }
        reportTable = new SearchResultsInfoTable(top, null,
            new String[] { " " }, new int[] { 500 });
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalSpan = 2;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        reportTable.setLayoutData(gd);
    }

    public void resetSearch() {
        Assert.isNotNull(reportTable);
        createEmptyReportTable();
        reportData = new ArrayList<Object>();

        printButton.setEnabled(false);
        exportButton.setEnabled(false);
    }

    private void setEnabled(boolean enabled) {
        SessionManager.getInstance().getSiteCombo().setEnabled(enabled);
        generateButton.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        printButton.setEnabled(enabled);
        exportButton.setEnabled(enabled);
    }

    public JasperPrint createDynamicReport(String reportName,
        List<Object[]> params, List<String> columnInfo, List<?> list)
        throws Exception {

        FastReportBuilder drb = new FastReportBuilder();
        for (int i = 0; i < columnInfo.size(); i++) {
            drb.addColumn(columnInfo.get(i), columnInfo.get(i), String.class,
                40, false).setPrintBackgroundOnOddRows(true)
                .setUseFullPageWidth(true);
        }

        Map<String, Object> fields = new HashMap<String, Object>();
        String paramString = "";
        for (int i = 0; i < params.size(); i++) {
            paramString += params.get(i)[0] + " : " + params.get(i)[1] + "\n";
        }
        fields.put("title", reportName);
        fields.put("infos", paramString);
        URL reportURL = ReportingUtils.class.getResource("BasicReport.jrxml");
        if (reportURL == null) {
            throw new Exception("No report available with name BasicReport");
        }
        drb.setTemplateFile(reportURL.getFile());
        Style headerStyle = new Style();
        headerStyle.setFont(ReportingUtils.sansSerifBold);
        // headerStyle.setHorizontalAlign(HorizontalAlign.CENTER);
        headerStyle.setBorderBottom(Border.THIN);
        headerStyle.setVerticalAlign(VerticalAlign.MIDDLE);
        headerStyle.setBackgroundColor(Color.LIGHT_GRAY);
        headerStyle.setTransparency(Transparency.OPAQUE);
        Style detailStyle = new Style();
        detailStyle.setFont(ReportingUtils.sansSerif);
        drb.setDefaultStyles(null, null, headerStyle, detailStyle);

        JRDataSource ds = new JRBeanCollectionDataSource(list);
        JasperPrint jp = DynamicJasperHelper.generateJasperPrint(drb.build(),
            new ClassicLayoutManager(), ds, fields);
        return jp;
    }

    protected void saveFields() {
        if (selectedNode != null) {
            List<HQLField> fields = selectedNode.getFieldData();
            for (int i = 0; i < widgetFields.size(); i++) {
                if (widgetFields.get(i) instanceof DateTimeWidget) {
                    fields.get(i).setValue(
                        ((DateTimeWidget) widgetFields.get(i)).getDate());
                    fields.get(i).setOperator(operatorFields.get(i).getText());
                } else if (fields.get(i).getType() == Integer.class) {
                    Integer val;
                    try {
                        val = Integer.parseInt(((Text) widgetFields.get(i))
                            .getText());
                    } catch (NumberFormatException e) {
                        val = null;
                    }
                    fields.get(i).setValue(val);
                    fields.get(i).setOperator(operatorFields.get(i).getText());
                } else {
                    fields.get(i).setValue(
                        ((Text) widgetFields.get(i)).getText());
                    fields.get(i).setOperator(operatorFields.get(i).getText());
                }
            }
        }
    }

    protected void printTable(boolean b) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doSaveAs() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDirty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void setFocus() {
        ReportsView.getTree().setSelection(new StructuredSelection(node));
    }

}
