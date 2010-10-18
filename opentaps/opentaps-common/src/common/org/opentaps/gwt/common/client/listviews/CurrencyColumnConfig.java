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

import java.math.BigDecimal;

import com.google.gwt.i18n.client.NumberFormat;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.Renderer;
import org.opentaps.gwt.common.client.UtilUi;

/**
 * A ColumnConfig that displays a currency amount.
 * Can be set by giving both the amount column and the currency code column, or by setting
 *  a static currency code.
 */
public class CurrencyColumnConfig extends ColumnConfig implements Renderer {

    private static final String MODULE = CurrencyColumnConfig.class.getName();

    private String currencyCode;
    private String currencyIndex;

    /**
     * Create a new CurrencyColumnConfig.
     * This will use the default currency code according to the user locale (normally that is NOT wanted)
     *  unless the currency is set with <code>setCurrencyCode()</code>.
     * @param header the column header
     * @param dataIndex the data index (the field name of the Store associated with the Grid containing the amount)
     */
    public CurrencyColumnConfig(String header, String dataIndex) {
        this(header, null, dataIndex);
    }

    /**
     * Create a new CurrencyColumnConfig.
     *
     * @param header the column header
     * @param currencyIndex the currency index (the field name of the Store associated with the Grid containing the currency code)
     * @param dataIndex the data index (the field name of the Store associated with the Grid containing the amount)
     */
    public CurrencyColumnConfig(String header, String currencyIndex, String dataIndex) {
        super(header, dataIndex);
        this.currencyIndex = currencyIndex;
        this.setRenderer(this);
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
        if (value == null) {
            return "";
        }
        String amount = (String) value;

        NumberFormat fmt = null;
        String currency = currencyCode;
        if (currencyIndex != null) {
            currencyCode = record.getAsString(currencyIndex);
        }
        if (currencyCode == null) {
            fmt = NumberFormat.getCurrencyFormat();
        } else {
            try {
                fmt = NumberFormat.getCurrencyFormat(currencyCode);
            } catch (Exception e) {
                // Note: there is a bug in getCurrencyFormat and it does not work by looking at the properties file
                // but is limited to 4 hard coded currencies
                UtilUi.logWarning("Cannot set currency format with currency code [" + currencyCode + "] " + e.getMessage(), MODULE, "render");
                // manually build the format and use the currency code as the currency symbol
                // this pattern is the currency pattern but with the currency symbol removed
                fmt = NumberFormat.getFormat("#,##0.00;(#,##0.00)");
                return currencyCode + " " + fmt.format(new BigDecimal(amount).doubleValue());
            }
        }
        return fmt.format(new BigDecimal(amount).doubleValue());
    }

    /**
     * Sets the currency code to user.
     * @param currencyCode a 3 chars <code>String</code> value
     */
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
