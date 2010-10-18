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

package com.opensourcestrategies.financials.invoice;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.base.util.UtilNumber;

import java.math.BigDecimal;

/**
 * InvoiceWithOutstandingBalance - Wrapper class for the Invoice entity with the amount of unapplied payments and
 * whether the invoice is past due or not.  This is better than just using another Map.
 * 
 * The get methods seem to be required by Freemarker.
 *
 * @author     <a href="mailto:nate@natereed.com">Nate Reed</a> 
 * @author     <a href="mailto:sichen@opensourcestrategies.com">Nate Reed</a> 
 * @version    $ $
 * @since      
 */

public @Deprecated class InvoiceWithOutstandingBalance {

    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

    public GenericValue invoice;
    public BigDecimal amount;
    public boolean isPastDue;
    public BigDecimal interestCharged = BigDecimal.ZERO.setScale(decimals, rounding);
    
    public InvoiceWithOutstandingBalance(GenericValue invoice, BigDecimal amount, boolean isPastDue, BigDecimal interestCharged) {
        // tbd: check that invoice is an "Invoice" object and throw an IllegalArgumentException if not
        this.invoice = invoice;    
        this.amount = amount;
        this.isPastDue = isPastDue;
        if (interestCharged != null) {
            this.interestCharged = interestCharged;
        }
    }
    
    public InvoiceWithOutstandingBalance(GenericValue invoice, BigDecimal amount, boolean isPastDue) {
        this(invoice, amount, isPastDue, null);
    }    
    
    public boolean isPastDue() {
        return this.isPastDue;
    }
    
    public BigDecimal getAmount() {
        return this.amount;
    }
    
    public GenericValue getInvoice() {
        return this.invoice;
    }

    public BigDecimal getInterestCharged() {
        return this.interestCharged;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("InvoiceWithOutstandingBalance=");
        result.append("[decimals," + decimals + "]");
        result.append("[rounding," + rounding + "]");
        result.append("[invoice," + invoice + "]");
        result.append("[amount," + amount + "]");
        result.append("[isPastDue," + isPastDue + "]");
        result.append("[interestCharged," + interestCharged + "]");
        return result.toString();
    }
}
