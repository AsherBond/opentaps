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
package org.opentaps.common.reporting.jasper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import org.ofbiz.base.util.Debug;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.util.FoundationUtils;

/**
 * JasperReports custom data source class. Provides to engine list
 * of POJO objects as report data source. 
 */
public class JRObjectListDataSource implements JRDataSource {

    private String module = JRObjectListDataSource.class.getName();

    List<? extends Entity> data = null;
    private Iterator<? extends Entity> iterator = null;
    Entity obj = null;

    public JRObjectListDataSource() {}

    public JRObjectListDataSource(List<? extends Entity> data) {
        setObjectList(data);
    }

    public void setObjectList(List<? extends Entity> data) {
        if (data == null)
            throw new IllegalArgumentException();

        this.data = data;
        this.iterator = data.iterator();
    }

    /* (non-Javadoc)
     * @see net.sf.jasperreports.engine.JRDataSource#getFieldValue(net.sf.jasperreports.engine.JRField)
     */
    public Object getFieldValue(JRField field) throws JRException {
        Object value = null;

        if (obj != null) {
            try {
                Method getMethod = obj.getClass().getDeclaredMethod(FoundationUtils.getterName(field.getName()));
                value = getMethod.invoke(obj);
            } catch (FoundationException e) {
                Debug.logError(e.getMessage(), module);
            } catch (SecurityException e) {
                Debug.logError(e.getMessage(), module);
            } catch (NoSuchMethodException e) {
                Debug.logError(e.getMessage(), module);
            } catch (IllegalArgumentException e) {
                Debug.logError(e.getMessage(), module);
            } catch (IllegalAccessException e) {
                Debug.logError(e.getMessage(), module);
            } catch (InvocationTargetException e) {
            }
        }

        return value;
    }

    /* (non-Javadoc)
     * @see net.sf.jasperreports.engine.JRDataSource#next()
     */
    public boolean next() throws JRException {
        boolean hasNext = false;

        if (iterator != null) {
            hasNext = iterator.hasNext();
            if (hasNext)
                obj = iterator.next();
        }

        return hasNext;
    }

}
