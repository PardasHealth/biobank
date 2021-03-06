package edu.ualberta.med.biobank.widgets.infotables;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.common.util.StringUtil;
import edu.ualberta.med.biobank.common.wrappers.LogWrapper;
import edu.ualberta.med.biobank.gui.common.widgets.AbstractInfoTableWidget;
import edu.ualberta.med.biobank.gui.common.widgets.BgcLabelProvider;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.model.User;

public class LoggingInfoTable extends ReportTableWidget<LogWrapper> {
    public static final I18n i18n = I18nFactory.getI18n(LoggingInfoTable.class);

    @SuppressWarnings("nls")
    private static final String[] HEADINGS = new String[] {
        Site.NAME.singular().toString(),
        User.NAME.singular().toString(),
        i18n.tr("Date"),
        i18n.tr("Action"),
        i18n.tr("Type"),
        Patient.PropertyName.PNUMBER.toString(),
        Specimen.PropertyName.INVENTORY_ID.toString(),
        i18n.tr("Location"),
        i18n.tr("Details") };

    private static final int PAGE_SIZE_ROWS = 20;

    public LoggingInfoTable(Composite parent, List<LogWrapper> collection) {
        super(parent, collection, HEADINGS, PAGE_SIZE_ROWS);
    }

    private static class TableRowData {
        String center;
        String user;
        String date;
        String action;
        String type;
        String patientNumber;
        String inventoryId;
        String positionLabel;
        String details;

        @SuppressWarnings("nls")
        @Override
        public String toString() {
            return StringUtils.join(new String[] { center, user, date, action,
                type, patientNumber, inventoryId, positionLabel, details },
                "\t");
        }
    }

    @Override
    public BgcLabelProvider getLabelProvider() {
        return new BgcLabelProvider() {
            @Override
            public String getColumnText(Object element, int columnIndex) {
                TableRowData item =
                    getCollectionModelObject((LogWrapper) element);
                if (item == null) {
                    if (columnIndex == 0) {
                        return AbstractInfoTableWidget.LOADING;
                    }
                    return StringUtil.EMPTY_STRING;
                }
                switch (columnIndex) {
                case 0:
                    return item.center;
                case 1:
                    return item.user;
                case 2:
                    return item.date;
                case 3:
                    return item.action;
                case 4:
                    return item.type;
                case 5:
                    return item.patientNumber;
                case 6:
                    return item.inventoryId;
                case 7:
                    return item.positionLabel;
                case 8:
                    return item.details;
                default:
                    return StringUtil.EMPTY_STRING;
                }
            }
        };
    }

    public TableRowData getCollectionModelObject(LogWrapper logQuery) {
        TableRowData info = new TableRowData();
        info.center = logQuery.getCenter();
        info.user = logQuery.getUsername();
        info.action = logQuery.getAction();
        info.type = logQuery.getType();
        info.positionLabel = logQuery.getLocationLabel();
        info.patientNumber = logQuery.getPatientNumber();
        info.inventoryId = logQuery.getInventoryId();
        info.details = logQuery.getDetails();

        Date logQueryDate = logQuery.getCreatedAt();
        if (logQueryDate != null) {
            @SuppressWarnings("nls")
            SimpleDateFormat dateTimeSecond =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            info.date = dateTimeSecond.format(logQueryDate);
        } else {
            info.date = null;
        }

        return info;
    }

    public Table getTable() {
        return tableViewer.getTable();
    }

}