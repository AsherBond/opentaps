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
package org.opentaps.gwt.common.client.form;

import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.Window;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.layout.FitLayout;
import com.gwtext.client.widgets.layout.HorizontalLayout;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.BaseFormPanel;
import org.opentaps.gwt.common.client.listviews.SearchResultsListView;
import org.opentaps.gwt.common.client.lookup.configuration.SearchLookupConfiguration;

/**
 * A generic search form.
 * This contains a simple text input and a search button.
 * Results are presented in a popup window using the given <code>SearchResultsListView</code>
 *  instance (which does the application specific formatting of the results).
 */
public class SearchForm extends BaseFormPanel {

    private final Window win;
    private final TextField searchInput;
    private SearchResultsListView results;

    private static final int RESULT_HEIGHT = 600;
    private static final int RESULT_WIDTH = 800;

    /**
     * Default constructor.
     * @param resultsListView the instance of the results list view to use
     */
    public SearchForm(SearchResultsListView resultsListView) {
        super();
        setBorder(false);
        setHideLabels(true);

        // using an inner panel to customize the layout
        Panel innerPanel = new Panel();
        innerPanel.setBorder(false);
        innerPanel.setLayout(new HorizontalLayout(5));

        searchInput = new TextField();
        searchInput.setName(SearchLookupConfiguration.IN_QUERY);
        searchInput.setWidth(200);  // width of search input box
        setFieldListeners(searchInput);
        innerPanel.add(searchInput);

        Button submitButton = makeStandardSubmitButton(UtilUi.MSG.search());
        innerPanel.add(submitButton);

        add(innerPanel);

        win = new Window(UtilUi.MSG.searchResults());
        win.setModal(false);
        win.setResizable(true);
        win.setMinHeight(RESULT_HEIGHT);
        win.setLayout(new FitLayout());
        win.setCloseAction(Window.HIDE);

        results = resultsListView;
        results.setFrame(false);
        results.setAutoHeight(false);
        results.setCollapsible(false);
        results.setHeader(false);
        results.setBorder(false);
        results.setWidth(RESULT_WIDTH);
        win.add(results);
    }

    @Override public void submit() {
        search();
    }

    private void search() {
        if (win.getHeight() < RESULT_HEIGHT) {
            win.setHeight(RESULT_HEIGHT);
        }
        win.show();
        win.center();
        results.search(searchInput.getText());
    }

}
