/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 * 
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package org.opentaps.funambol.common;


import java.beans.PropertyEditorSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.funambol.common.pim.common.Property;

/**
 * Create String values from values in a funambol pim Property
 * 
 * @author Cameron Smith - Database, Lda - www.database.co.mz
 */
public class PimPropertyEditor extends PropertyEditorSupport
{
    Log log = LogFactory.getLog(PimPropertyEditor.class);
   
    /**
     * Invert whatever was set via setValue - if it was an Object return a Property, if it was anything else, return a Property
     * 
     * TODO: still not great
     */
    public Object getValue()
    {
        Object value = super.getValue();
        if(value instanceof Property)
        { 
            //return the value - treat COMPLETELY EMPTY String "" as null because FBol always sets this to empty String even when no data
            Object val = ((Property)value).getPropertyValue();
            if(val == null) { return null; }
            else { return StringUtils.isEmpty(val.toString()) ? null : val; }
        }
        else
        { 
            Property valueAsProp = new Property();
            valueAsProp.setPropertyValue(value);
            return valueAsProp;
         }
    }
}
