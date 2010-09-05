/*
 * Copyright (c) opentaps Group LLC
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
package org.opentaps.gwt.common.webapp.client;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.HttpProxy;
import com.gwtext.client.data.JsonReader;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.data.event.StoreListenerAdapter;
import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.config.OpentapsConfig;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.client.lookup.configuration.WebAppLookupConfiguration;

/**
 * the Entry point classes of Webapps top Menu.
 */
public class WebAppsMenu extends BaseEntry {
    private static final String WEBAPPS_MENU_ID = "webAppsMenu";
    private static final String MODULE = WebAppsMenu.class.getName();
    private static int DISPLAY_MENU_ITEMS = 5;
    private static final RecordDef WEB_APPS_RECORD_DEF = new RecordDef(
            new FieldDef[]{
                new StringFieldDef(WebAppLookupConfiguration.OUT_APPLICATION_ID),
                new StringFieldDef(WebAppLookupConfiguration.OUT_LINK_URL),
                new StringFieldDef(WebAppLookupConfiguration.OUT_SEQUENCE_NUM),
                new StringFieldDef(WebAppLookupConfiguration.OUT_SHORT_NAME)
            }
    );

    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found
     */
    public void onModuleLoad() {
        OpentapsConfig config = new OpentapsConfig();
        if (config.getShowTopNavMenu() && RootPanel.get(WEBAPPS_MENU_ID) != null) {
            init();
        }
    }

    protected void init() {
        final HttpProxy dataProxy = new HttpProxy(WebAppLookupConfiguration.URL_FIND_WEB_APPS);
        JsonReader reader = new JsonReader(WEB_APPS_RECORD_DEF);
        reader.setRoot(UtilLookup.JSON_ROOT);
        reader.setId(UtilLookup.JSON_ID);
        reader.setTotalProperty(UtilLookup.JSON_TOTAL);
        Store store = new Store(dataProxy, reader, true);
        store.addStoreListener(new StoreListenerAdapter() {
                @Override public void onLoad(Store store, Record[] records) {
                    onStoreLoad(store, records);
                }

                @Override public void onLoadException(Throwable error) {
                    onStoreLoadError(error);
                }
            });
        store.load();
    }

    protected void onStoreLoad(Store store, Record[] records) {

        // Top level menu
        MenuBar menuTop = new MenuBar();
        menuTop.addStyleName("topMenu");

        // More menu - vertical=true
        MenuBar menuMore = new MenuBar(true);

        for (int i = 0; i < records.length; i++) {
            Record record = records[i];
            String applicationId = record.getAsString(WebAppLookupConfiguration.OUT_APPLICATION_ID);
            String shortName = record.getAsString(WebAppLookupConfiguration.OUT_SHORT_NAME);
            // in case the app has no shortName set, display the applicationId
            if (shortName == null) {
                shortName = applicationId;
            }
            final String linkUrl = record.getAsString(WebAppLookupConfiguration.OUT_LINK_URL);
            if (applicationId != null) {
                Command command = new Command() { public void execute() { } };
                String menuStyle = "";
                String menuText = "";
                if (linkUrl != null && !"".equals(linkUrl)) {
                    menuStyle = i <= DISPLAY_MENU_ITEMS ? "linkMenu" : "textMenu";
                    menuText =  "<a href=\"" + linkUrl + "\">" + shortName + "</a>";
                } else {
                    menuStyle = "textMenu";
                    menuText = shortName;
                }

                MenuItem menuItem = new MenuItem(menuText, true, command);
                menuItem.addStyleName(menuStyle);
                if (i <= DISPLAY_MENU_ITEMS) {
                    // The top 5 should be shown above logo.
                    menuTop.addItem(menuItem);
                } else {
                    // The rest show be under "More"
                    menuMore.addItem(menuItem);
                }
            }
        }
        // add "more" second level menu for contain more link
        if (records.length > DISPLAY_MENU_ITEMS + 1) {
            menuTop.addItem("<span class=\"moreMenu\">more</span>", true, menuMore);
        }
        // add menu to the root panel.
        RootPanel.get(WEBAPPS_MENU_ID).add(menuTop);
    }

    protected void onStoreLoadError(Throwable error) {
        UtilUi.logError("Store load error [" + error + "] for: " + UtilUi.toString(this), MODULE, "onStoreLoadError");
    }
}
