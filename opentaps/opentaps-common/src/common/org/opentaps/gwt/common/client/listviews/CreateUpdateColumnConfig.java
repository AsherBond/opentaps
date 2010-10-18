/*
 * Copyright (c) Open Source Strategies, Inc.
 *
 * Opentaps is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opentaps is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opentaps.gwt.common.client.listviews;

import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.util.Format;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.Renderer;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.lookup.Permissions;

/**
 * A <code>ColumnConfig</code> that contains update / remove buttons for the current record.
 */
public class CreateUpdateColumnConfig extends ColumnConfig implements Renderer {

    // a width that fits both update / create button widths
    private static final int WIDTH = 70;
    private static final int ICON_WIDTH = 30;
    private boolean useIcons = true;

    /**
     * Creates a new <code>CreateUpdateColumnConfig</code>.
     * @param idIndex the data index (the field name of the Store associated with the Grid) containing the ID field, the button will be a create button if the id is <code>null</code>, else it will be an update button
     */
    public CreateUpdateColumnConfig(String idIndex) {
        this(idIndex, true);
    }

    /**
     * Creates a new <code>CreateUpdateColumnConfig</code>.
     * @param idIndex the data index (the field name of the Store associated with the Grid) containing the ID field, the button will be a create button if the id is <code>null</code>, else it will be an update button
     * @param useIcons if <code>true</code> use icons instead of text buttons
     */
    public CreateUpdateColumnConfig(String idIndex, boolean useIcons) {
        super("&nbsp;", idIndex);
        setRenderer(this);
        setSortable(false);
        this.useIcons = useIcons;
        if (useIcons) {
            setWidth(ICON_WIDTH);
        } else {
            setWidth(WIDTH);
        }
        setFixed(true);
    }

    /**
     * The implementation of the <code>Renderer</code> interface that produce the content of the cell.
     * @param value the value of the current record for this cell
     * @param cellMetadata a <code>CellMetadata</code> value
     * @param record the current <code>Record</code> value
     * @param rowIndex the row index of the current record
     * @param colNum the column index of this cell
     * @param store a <code>Store</code> value
     * @return the cell content as an HTML string
     */
    public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
        // check if the record can be updated
        if (UtilUi.isSummary(record)) {
            return null;
        }

        String buttonLabel;
        String icon;
        if (value == null) {
            buttonLabel = UtilUi.MSG.commonCreate();
            icon = UtilUi.ICON_SAVE;
        } else {
            // do not render the save button if the record cannot be updated
            if (!Permissions.canUpdate(record)) {
                return null;
            }

            buttonLabel = UtilUi.MSG.commonUpdate();
            icon = UtilUi.ICON_SAVE;
        }

        if (useIcons) {
            return Format.format("<img alt=\"{0}\" src=\"{1}\" />", buttonLabel, icon);
        } else {
            return Format.format("<span class=\"buttontext\">{0}</span>", buttonLabel);
        }
    }

}
