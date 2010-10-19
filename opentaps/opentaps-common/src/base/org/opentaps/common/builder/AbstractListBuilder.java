package org.opentaps.common.builder;

import java.util.List;
import java.lang.reflect.UndeclaredThrowableException;

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

/**
 * Defines list builder methods that only need one implementation.
 * All ListBuilders should at least extend this.
 */
public abstract class AbstractListBuilder implements ListBuilder {

    public static final String module = AbstractListBuilder.class.getName();

    protected PageBuilder pageBuilder = null;

    public boolean hasPageBuilder() {
        return (pageBuilder != null);
    }

    public void setPageBuilder(PageBuilder pageBuilder) {
        this.pageBuilder = pageBuilder;
    }

    public PageBuilder getPageBuilder() {
        return pageBuilder;
    }

    public List build(long viewSize, long cursorIndex) throws ListBuilderException {
        List page = getPartialList(viewSize, cursorIndex);
        if (pageBuilder != null) {
            try {
                return pageBuilder.build(page);
            } catch (IllegalArgumentException e) { // report brain-dead uses of this throwable in the OFBiz framework
                throw new ListBuilderException(e);
            } catch (UndeclaredThrowableException e) { // report bsh.EvalError and other things
                throw new ListBuilderException(e.getCause());
            } catch (Exception e) { // translate remaining exceptions to ListBuilderException, which will cause it to be reported to screen
                throw new ListBuilderException(e);
            }
        }
        return page;
    }

    public List getCompleteList() throws ListBuilderException {
        if (isDeterministic()) {
            return getPartialList(getListSize(), 0);
        } else {
            throw new ListBuilderException("Attempted to call getCompleteList() on a non-deterministic list builder ["+this.getClass().getName()+"].  This builder should override the method.");
        }
    }
}
