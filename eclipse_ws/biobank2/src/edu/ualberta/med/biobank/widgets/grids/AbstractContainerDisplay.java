package edu.ualberta.med.biobank.widgets.grids;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ualberta.med.biobank.common.util.StringUtil;
import edu.ualberta.med.biobank.common.wrappers.ContainerLabelingSchemeWrapper;
import edu.ualberta.med.biobank.model.Container;
import edu.ualberta.med.biobank.model.ContainerLabelingScheme;
import edu.ualberta.med.biobank.model.ContainerType;
import edu.ualberta.med.biobank.model.type.LabelingLayout;
import edu.ualberta.med.biobank.model.util.RowColPos;
import edu.ualberta.med.biobank.widgets.grids.well.AbstractUIWell;
import edu.ualberta.med.biobank.widgets.grids.well.UICellStatus;

/**
 * This class is there to give a common parent class to grid container widgets and drawers widgets
 */
public abstract class AbstractContainerDisplay {

    private static Logger log = LoggerFactory.getLogger(AbstractContainerDisplay.class.getName());

    // FIXME: remove this field
    protected Container container;

    // FIXME: remove this field
    protected ContainerType containerType;

    /**
     * true if we want the container to display full info in each box displayed
     */
    protected boolean displayFullInfoString = false;

    /**
     * max width this container will have : used to calculate cells width
     */
    protected int maxWidth = -1;

    /**
     * max height this container will have : used to calculate cells height
     */
    protected int maxHeight = -1;

    protected List<UICellStatus> legendStatus;

    protected final String containerLabel;

    /**
     * Default labelling scheme to SBS.
     */
    private int childLabelingSchemeId = 1;

    protected abstract Image updateGridImage(ContainerDisplayWidget containerDisplayWidget);

    protected abstract Point computeSize(int wHint, int hHint, boolean changed);

    protected abstract Rectangle getGridSize();

    protected RowColPos storageSize = new RowColPos(0, 0);

    private LabelingLayout labelingLayout = LabelingLayout.VERTICAL;

    public AbstractContainerDisplay(String containerLabel) {
        this.containerLabel = containerLabel;
    }

    @SuppressWarnings("nls")
    public AbstractUIWell getObjectAtCoordinates(
        Map<RowColPos, ? extends AbstractUIWell> cells,
        int x,
        int y) {

        if (cells == null) {
            throw new IllegalStateException("cells is null");
        }

        if (!cells.isEmpty()) {
            RowColPos rcp = getPositionAtCoordinates(x, y);
            if (rcp != null) {
                return cells.get(rcp);
            }
        }
        return null;
    }

    public abstract RowColPos getPositionAtCoordinates(int x, int y);

    public void initLegend(List<UICellStatus> status) {
        this.legendStatus = status;
    }

    public void setContainerType(ContainerType type) {
        this.containerType = type;
    }

    /**
     * Get the text to write inside the cell. This default implementation use the cell position and
     * the containerType.
     */
    @SuppressWarnings("unused")
    protected String getDefaultTextForBox(
        Map<RowColPos, ? extends AbstractUIWell> cells,
        int indexRow,
        int indexCol) {
        RowColPos rowcol = new RowColPos(indexRow, indexCol);
        String parentLabel = StringUtil.EMPTY_STRING;
        if (displayFullInfoString && container != null) {
            parentLabel = container.getLabel();
        }
        if (containerType != null) {
            return parentLabel + containerType.getPositionString(rowcol);
        }

        if (childLabelingSchemeId > 0) {
            return ContainerLabelingScheme.getPositionString(
                rowcol,
                childLabelingSchemeId,
                storageSize.getRow(),
                storageSize.getCol(),
                labelingLayout);
        }
        return StringUtil.EMPTY_STRING;
    }

    public void setStorageSize(int rows, int columns) {
        storageSize = new RowColPos(rows, columns);
    }

    public Point getSizeToApply() {
        return null;
    }

    /**
     * Modify dimensions of the grid. maxWidth and maxHeight are used to calculate the size of the
     * cells
     * 
     * @param maxWidth max width the grid should have
     * @param maxHeight max height the grid should have
     */
    @SuppressWarnings("nls")
    public void setDisplaySize(int maxWidth, int maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        log.debug("setDisplaySize: maxWidth: {}, maxHeight: {}", maxWidth, maxHeight);
    }

    public void setLabelingScheme(ContainerLabelingSchemeWrapper childLabelingScheme) {
        if (childLabelingScheme != null) {
            this.childLabelingSchemeId = childLabelingScheme.getId();
        } else {
            this.childLabelingSchemeId = 1;
        }
    }

    public void setLabelingSchemeId(int id) {
        this.childLabelingSchemeId = id;
    }

    public void setLabelingLayout(LabelingLayout labelingLayout) {
        this.labelingLayout = labelingLayout;
    }

}
