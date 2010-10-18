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
package org.opentaps.gwt.warehouse.inventory.client.form;

import java.util.Date;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.BaseFormPanel;

import com.google.gwt.user.client.Window;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.data.ArrayReader;
import com.gwtext.client.data.DateFieldDef;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.FloatFieldDef;
import com.gwtext.client.data.MemoryProxy;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.SimpleStore;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.widgets.form.FormPanel;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.ColumnModel;
import com.gwtext.client.widgets.grid.GridPanel;
import com.gwtext.client.widgets.layout.HorizontalLayout;
import com.gwtext.client.widgets.layout.VerticalLayout;

public class TraceInventoryApp {

    public BaseFormPanel mainPanel;
    private Panel findPanel;
    private GridPanel itemsPanel;
    private GridPanel inPanel;
    private GridPanel outPanel;

    RecordDef itemRecordDef = new RecordDef(  
            new FieldDef[]{  
                    new StringFieldDef("itemSeqId"),
                    new StringFieldDef("name"),
                    new FloatFieldDef("quantity")  
            }  
    );  

    RecordDef inventoryItemDef = new RecordDef(  
            new FieldDef[]{  
                    new StringFieldDef("itemId"),
                    new StringFieldDef("name"),
                    new FloatFieldDef("quantity"),
                    new DateFieldDef("received"),
                    new DateFieldDef("issued")
            }  
    );  

    public TraceInventoryApp() {
        super();

        mainPanel = new BaseFormPanel();

        mainPanel.setLayout(new VerticalLayout(UtilUi.CONTAINERS_VERTICAL_SPACING));
        mainPanel.setTitle("Tracking Inventory Movements");
        mainPanel.setBorder(true);
        mainPanel.setPaddings(10);
        mainPanel.setUrl("https://llll:1111/");

        findPanel = new Panel();
        findPanel.setLayout(new HorizontalLayout(30));

        Store docTypeStore = new SimpleStore(
                new String[] {"documentType", "docTypeDesc"}, 
                new String[][] {
                new String[] {"lotId", "Lot"}
                /*
                new String[] {"inventoryItemId", "Inventory Item"},
                new String[] {"orderId", "Order"}
                */
        });
        docTypeStore.load();

        ComboBox ctrlDocTypes = new ComboBox("ID", "documentType");
        ctrlDocTypes.setStore(docTypeStore);
        ctrlDocTypes.setFieldLabel("ID");
        ctrlDocTypes.setDisplayField("docTypeDesc");
        ctrlDocTypes.setValueField("docType");
        ctrlDocTypes.setHiddenName("docType");
        ctrlDocTypes.setMode(ComboBox.LOCAL);
        ctrlDocTypes.setTriggerAction(ComboBox.ALL);
        ctrlDocTypes.setSelectOnFocus(true);
        findPanel.add(ctrlDocTypes);

        TextField ctlrId = new TextField("documentId");
        ctlrId.setWidth(140);
        findPanel.add(ctlrId);

        Button ctrlFind = new Button("Find");
        ctrlFind.addListener(new ButtonListenerAdapter() {
            @Override
            public void onClick(Button button, EventObject e) {
                TraceInventoryApp.this.getMainPanel().getForm().submit();
            }
        });
        findPanel.add(ctrlFind);

        // add items list
        itemsPanel = new GridPanel();
        Object[][] data = new Object[][] {
                new Object[] {"00001", "Item 1", new Double(5.5)},
                new Object[] {"00002", "Item 2", new Double(6.6)}
        };
        MemoryProxy proxy = new MemoryProxy(data);

        ArrayReader reader = new ArrayReader(itemRecordDef);  
        Store store = new Store(proxy, reader);  
        store.load();  
        itemsPanel.setStore(store);

        ColumnConfig[] columns = new ColumnConfig[]{
                new ColumnConfig("Seq Number", "itemSeqId", 160),
                new ColumnConfig("Name", "name", 600, true, null, "name"),
                new ColumnConfig("Quantity", "quantity", 60),
        };
        ColumnModel columnModel = new ColumnModel(columns);
        itemsPanel.setColumnModel(columnModel);

        itemsPanel.setFrame(false);
        itemsPanel.setStripeRows(true);
        itemsPanel.setAutoExpandColumn("name");

        itemsPanel.setWidth(UtilUi.LIST_CONTAINER_WIDTH);
        itemsPanel.setTitle("Items");

        // add source list
        inPanel = new GridPanel();
        MemoryProxy proxy1 = new MemoryProxy(new Object[][] {
                new Object[] {"00001", "Inventory Item 1", new Double(5.5), new Date(), new Date()},
                new Object[] {"00002", "Inventory Item 2", new Double(6.6), new Date(), new Date()},
                new Object[] {"00003", "Inventory Item 3", new Double(6.6), new Date(), new Date()}
        });

        ArrayReader reader1 = new ArrayReader(inventoryItemDef);  
        Store store1 = new Store(proxy1, reader1);  
        store1.load();  
        inPanel.setStore(store1);

        ColumnConfig[] columns1 = new ColumnConfig[]{
                new ColumnConfig("Id", "itemId", 160),
                new ColumnConfig("Name", "name", 600, true, null, "name"),
                new ColumnConfig("Quantity", "quantity", 60),
                new ColumnConfig("Received", "received", 60),
                new ColumnConfig("Issued", "issued", 60),
        };
        ColumnModel columnModel1 = new ColumnModel(columns1);
        inPanel.setColumnModel(columnModel1);

        inPanel.setFrame(false);
        inPanel.setStripeRows(true);
        inPanel.setAutoExpandColumn("name");

        inPanel.setWidth(UtilUi.LIST_CONTAINER_WIDTH);
        inPanel.setTitle("Received from");

        // add target list
        outPanel = new GridPanel();
        MemoryProxy proxy2 = new MemoryProxy(new Object[][] {
                new Object[] {"00004", "Inventory Item 4", new Double(5.5), new Date(), new Date()},
                new Object[] {"00005", "Inventory Item 5", new Double(6.6), new Date(), new Date()},
                new Object[] {"00006", "Inventory Item 6", new Double(6.6), new Date(), new Date()}
        });

        ArrayReader reader2 = new ArrayReader(inventoryItemDef);  
        Store store2 = new Store(proxy2, reader2);  
        store1.load();  
        outPanel.setStore(store2);

        ColumnConfig[] columns2 = new ColumnConfig[]{
                new ColumnConfig("Id", "itemId", 160),
                new ColumnConfig("Name", "name", 600, true, null, "name"),
                new ColumnConfig("Quantity", "quantity", 60),
                new ColumnConfig("Received", "received", 60),
                new ColumnConfig("Issued", "issued", 60),
        };
        ColumnModel columnModel2 = new ColumnModel(columns2);
        outPanel.setColumnModel(columnModel2);

        outPanel.setFrame(false);
        outPanel.setStripeRows(true);
        outPanel.setAutoExpandColumn("name");

        outPanel.setWidth(UtilUi.LIST_CONTAINER_WIDTH);
        outPanel.setTitle("Issued to");

        mainPanel.add(findPanel);

        //mainPanel.add(itemsPanel);
        mainPanel.add(inPanel);
        mainPanel.add(outPanel);

    }

    /**
     * @return
     */
    public FormPanel getMainPanel() {
        return mainPanel;
    }

}
