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

package org.opentaps.common.pagination;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.opentaps.common.builder.ListBuilder;
import org.opentaps.common.builder.ListBuilderException;

/**
 * The Paginator is a session object which manages a PaginationState and a ListBuilder.
 * It is responsible for interpreting actions such as sorting a list, getting a page and
 * saving the state.  This is a generic object designed to be called from generic requests.
 *
 * The idea is that the developer only needs to worry about defining a ListBuilder for the
 * Paginator.  There are conveniences for creating ListBuilders, which simlify
 * the whole thing even more.
 *
 * @author Leon Torres (leon@opensourcestrategies.com)
 */
public class Paginator implements HttpSessionBindingListener {

    public static final String module = PaginatorFactory.class.getName();

    protected String paginatorName;
    protected PaginationState state;
    protected ListBuilder listBuilder;
    protected Map params; // parameters that are passed to each pagination request as specified in the varargs for <@paginate> macro
    protected boolean isFormlet;
    protected long cachedSize = -1; // cached size for external calls to the get size functions
    protected boolean renderExcelButton;

    // disallow direct constructors
    protected Paginator() {}

    /**
     * Build a basic paginator.
     */
    public Paginator(
    	String paginatorName,
    	ListBuilder listBuilder,
    	Map params,
    	GenericValue userLogin,
    	String applicationName,
    	boolean rememberPage,
    	boolean rememberOrderBy,
    	boolean renderExcelButton
    ) {
        this.paginatorName = paginatorName;
        this.listBuilder = listBuilder;
        setParams(params);
        isFormlet = false;
        state = new PaginationState(paginatorName, userLogin, applicationName, rememberPage, rememberOrderBy);
        this.renderExcelButton = renderExcelButton;
    }

    public String getPaginatorName() {
        return paginatorName;
    }

    public String getApplicationName() {
        return state.getApplicationName();
    }

    public ListBuilder getListBuilder() {
        return listBuilder;
    }

    public Map getParams() {
        return params;
    }

    /** Change the list builder and clean up any resources used by old list builder. */
    public void setListBuilder(ListBuilder listBuilder) {
        if (this.listBuilder != null) {
            this.listBuilder.close();
        }
        this.listBuilder = listBuilder;
    }

    public boolean hasPageBuilder() {
        return listBuilder.hasPageBuilder();
    }

    public boolean isFormlet() {
        return isFormlet;
    }

    public void setIsFormlet(boolean isFormlet) {
        this.isFormlet = isFormlet;
    }

    public void setParams(Map params) {
        if (params != null && params.size() > 0) {
            this.params = params;
        } else {
            this.params = FastMap.newInstance();
        }
    }

    public void setRememberPage(boolean rememberPage) {
        state.setRememberPage(rememberPage);
    }

    public void setRememberOrderBy(boolean rememberOrderBy) {
        state.setRememberOrderBy(rememberOrderBy);
    }

    public void setRenderExcelButton(boolean renderExcelButton) {
        this.renderExcelButton = renderExcelButton;
    }

    public boolean getRenderExcelButton() {
        return renderExcelButton;
    }

    /** Outside consumers should use this function to get the size. */
    public long getListSize() {
        if (cachedSize == -1) {
            try {
                cachedSize = listBuilder.getListSize();
                listBuilder.close();
            } catch (ListBuilderException e) {
                Debug.logError(e, e.getMessage(), module);
            }
        }
        return cachedSize;
    }

    public long getViewSize() {
        return state.getViewSize();
    }

    public void setViewSize(long viewSize) {
        state.setViewSize(viewSize);
    }

    public boolean getViewAll() {
        return state.getViewAll();
    }

    public void setViewAll(boolean viewAll) {
        state.setViewAll(viewAll);
    }

    public void toggleViewAll() {
        state.toggleViewAll();
    }

    public long getCursorIndex() {
        return state.getCursorIndex();
    }

    public void setCursorIndex(long index) {
        state.setCursorIndex(index);
    }

    public List getOrderBy() {
        return state.getOrderBy();
    }

    public String getOrderByString() {
        return PaginationState.serializeOrderBy(state.getOrderBy());
    }

    // this should work as long as the cursor never reaches the last element
    public long getPageNumber() {
        if (state.getViewAll()) return 1;
        return 1 + state.getCursorIndex() / state.getViewSize();
    }

    public long getTotalPages() {
        long size = getListSize();
        if (size == 0 || state.getViewAll()) return 1;
        return (long) Math.ceil((double) size / (double) state.getViewSize());
    }

    /** Free up resources when removed from session or session expires. */
    public void valueUnbound(HttpSessionBindingEvent event) {
        if (listBuilder != null) listBuilder.close();
        listBuilder = null;
        state = null;
    }

    /** Unimplemented */
    public void valueBound(HttpSessionBindingEvent event) {
    }


    /*************************************************************************/
    /**                                                                     **/
    /**           Functions to fetch Pages and change State                 **/
    /**                                                                     **/
    /*************************************************************************/


    private long getLastPageCursorIndex() {
        try {
            if (listBuilder.getListSize() == 0 || state.getViewAll()) return 0;

            // handle case where list would fit exactly in the viewport
            if ((listBuilder.getListSize() % state.getViewSize()) == 0) {
                return listBuilder.getListSize() - state.getViewSize();
            }
            // all other cases rely on integer division droping the remainder
            return state.getViewSize() * (listBuilder.getListSize()/state.getViewSize());
        } catch (ListBuilderException e) {
            Debug.logError(e, e.getMessage(), module);
            return 0;
        }
    }

    // pattern is to set and save state first, which is enforced by this function
    private List getPage(long cursorIndex) throws ListBuilderException {
        if (state.getViewAll()) {
            return listBuilder.getCompleteList();
        }

        cachedSize = listBuilder.getListSize();
        if (cursorIndex < 0) {
            cursorIndex = 0;
        }
        if (cursorIndex >= cachedSize) {
            cursorIndex = getLastPageCursorIndex();
        }
        if (cursorIndex != state.getCursorIndex()) {
            state.setCursorIndex(cursorIndex);
            state.save();
        }
        return listBuilder.build(state.getViewSize(), state.getCursorIndex());
    }

    public List getFirstPage() throws ListBuilderException {
        return getPage(0);
    }

    /** Gets the last page, which has a cursor position an integer multiple of the viewSize. */
    public List getLastPage() throws ListBuilderException {
        return getPage(getLastPageCursorIndex());
    }

    public List getCurrentPage() throws ListBuilderException {
        return getPage(state.getCursorIndex());
    }

    public List getNextPage() throws ListBuilderException {
        return getPage(state.getCursorIndex() + state.getViewSize());
    }

    public List getPreviousPage() throws ListBuilderException {
        return getPage(state.getCursorIndex() - state.getViewSize());
    }

    public List getPageNumber(long page) throws ListBuilderException {
        return getPage(state.getViewSize() * (page - 1));
    }

    /** Changes the size of the viewport and sents the state back to the first page. */
    public void changeViewSize(long delta) {
        if (getListSize() == 0) return;
        if (delta == 0) return;
        long newViewSize = state.getViewSize() + delta;
        if (newViewSize <= 0) return;

        state.setCursorIndex(0);
        state.setViewSize(newViewSize);
        state.save();
    }

    /** Changes the order and sets the state back to the first page. */
    public void changeOrderBy(List orderBy) {
        if (getListSize() == 0) return;
        if (orderBy == null && state.getOrderBy() == null) return;
        if (orderBy != null && orderBy.equals(state.getOrderBy())) {
            reverseOrderBy(orderBy);
            return;
        }
        internalChangeOrderBy(orderBy);
    }

    /** Changes the order and sets the state back to the first page.
        Using the given reverse orderBy list instead of computing from the normal orderBy.
     */
    public void changeOrderBy(List orderBy, List orderByReverse) {
        if (getListSize() == 0) return;
        if (orderBy == null && state.getOrderBy() == null) return;
        if (orderBy != null && orderBy.equals(state.getOrderBy())) {
            internalChangeOrderBy(orderByReverse);
            return;
        }
        internalChangeOrderBy(orderBy);
    }

    /** Reverses the order of the sort by adding or removing DESC to the first parameter. */
    public void reverseOrderBy(List orderBy) {
        if (getListSize() == 0) return;
        if (orderBy.size() == 0) return;
        String first = (String) orderBy.get(0);
        int index = first.indexOf(" DESC");
        if (index == -1) {
            orderBy.set(0, first + " DESC");
        } else {
            orderBy.set(0, first.substring(0, index));
        }
        internalChangeOrderBy(orderBy);
    }

    private void internalChangeOrderBy(List orderBy) {
        state.setOrderBy(orderBy);
        state.setCursorIndex(0);
        state.save();
        listBuilder.changeOrderBy(orderBy);
    }
}
