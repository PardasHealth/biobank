package edu.ualberta.med.biobank.forms.reports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;

import edu.ualberta.med.biobank.BioBankPlugin;
import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.util.DateGroup;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.widgets.DateTimeWidget;

public class AliquotsByStudyClinicDateEditor extends ReportsEditor {

    public static String ID = "edu.ualberta.med.biobank.editors.AliquotsByStudyClinicDateEditor";
    private ComboViewer dateRangeCombo;
    protected DateTimeWidget start;
    protected DateTimeWidget end;
    protected ComboViewer topCombo;

    @Override
    protected int[] getColumnWidths() {
        return new int[] { 100, 100, 100, 100 };
    }

    @Override
    protected String[] getColumnNames() {
        return new String[] {
            "Study",
            "Clinic",
            ((IStructuredSelection) dateRangeCombo.getSelection())
                .getFirstElement().toString(), "Total" };
    }

    @Override
    protected void createOptionSection(Composite parent) {
        dateRangeCombo = widgetCreator.createComboViewer(parent, "Group By",
            Arrays.asList(DateGroup.values()), null);
        dateRangeCombo.getCombo().select(0);
        topCombo = createCustomCombo("Top Container Type", parent);
        start = widgetCreator.createDateTimeWidget(parent,
            "Start Date (Linked)", null, null, null);
        end = widgetCreator.createDateTimeWidget(parent, "End Date (Linked)",
            null, null, null);

    }

    private ComboViewer createCustomCombo(String label, Composite parent) {
        ComboViewer widget = widgetCreator.createComboViewer(parent, label,
            null, null);
        appService = SessionManager.getAppService();
        List<ContainerWrapper> containers = new ArrayList<ContainerWrapper>();
        Set<String> topContainerTypes = new HashSet<String>();
        try {
            // FIXME: uses all sites by default
            List<SiteWrapper> sites = SiteWrapper.getSites(appService);
            for (SiteWrapper site : sites) {
                containers.addAll(site.getTopContainerCollection());
            }
            for (ContainerWrapper c : containers) {
                topContainerTypes.add(c.getContainerType().getNameShort());
            }
        } catch (Exception e) {
            BioBankPlugin.openAsyncError("Error retrieving containers", e);
        }
        widget.setInput(topContainerTypes.toArray(new String[] {}));
        widget.getCombo().select(0);
        return widget;
    }

    @Override
    protected List<Object> getParams() {
        List<Object> params = new ArrayList<Object>();
        params.add(((IStructuredSelection) dateRangeCombo.getSelection())
            .getFirstElement().toString());
        params.add(((IStructuredSelection) topCombo.getSelection())
            .getFirstElement());
        if (start.getDate() == null)
            params.add(new Date(0));
        else
            params.add(start.getDate());
        if (end.getDate() == null)
            params.add(new Date());
        else
            params.add(end.getDate());
        return params;
    }

    @Override
    protected List<String> getParamNames() {
        List<String> param = new ArrayList<String>();
        param.add("Group By");
        param.add("Top Container Type");
        param.add("Start Date (Linked)");
        param.add("End Date (Linked)");
        return param;
    }

}
