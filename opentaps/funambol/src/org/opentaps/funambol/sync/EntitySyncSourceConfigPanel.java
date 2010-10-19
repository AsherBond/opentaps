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
package org.opentaps.funambol.sync;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang.StringUtils;

import com.funambol.admin.AdminException;
import com.funambol.admin.ui.SourceManagementPanel;
import com.funambol.framework.engine.source.ContentType;
import com.funambol.framework.engine.source.SyncSourceInfo;

/**
 * This class implements the configuration panel for an EntitySyncSource
 *
 * TODO: should a GUI thing like this really be here?
 */
public class EntitySyncSourceConfigPanel extends SourceManagementPanel implements Serializable
{
    // --------------------------------------------------------------- Constants

    /**
     * Allowed characters for name and uri
     */
    public static final String NAME_ALLOWED_CHARS
    = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-_.";

    // ------------------------------------------------------------ Private data
    /** label for the panel's name */
    private JLabel panelName = new JLabel();

    /** border to evidence the title of the panel */
    private TitledBorder  titledBorder1;

    private JLabel           nameLabel          = new JLabel()     ;
    private JTextField       nameValue          = new JTextField() ;
    private JLabel           typeLabel          = new JLabel()     ;
    private JLabel           sourceUriLabel     = new JLabel()     ;
    private JTextField       sourceUriValue     = new JTextField() ;

    private JButton          confirmButton      = new JButton()    ;

    private EntitySyncSource  syncSource         = null             ;

    // ------------------------------------------------------------ Constructors

    /**
     * Creates a new EntitySyncSourceConfigPanel instance
     */
    public EntitySyncSourceConfigPanel() {
        init();
    }

    // ----------------------------------------------------------- Private methods

    /**
     * Create the panel
     * @throws Exception if error occures during creation of the panel
     */
    private void init(){
        // set layout
        this.setLayout(null);

        // set properties of label, position and border
        //  referred to the title of the panel
        titledBorder1 = new TitledBorder("");

        panelName.setFont(titlePanelFont);
        panelName.setText("Edit Dummy SyncSource");
        panelName.setBounds(new Rectangle(14, 5, 316, 28));
        panelName.setAlignmentX(SwingConstants.CENTER);
        panelName.setBorder(titledBorder1);

        sourceUriLabel.setText("Source URI: ");
        sourceUriLabel.setFont(defaultFont);
        sourceUriLabel.setBounds(new Rectangle(14, 60, 150, 18));
        sourceUriValue.setFont(defaultFont);
        sourceUriValue.setBounds(new Rectangle(170, 60, 350, 18));

        nameLabel.setText("Name: ");
        nameLabel.setFont(defaultFont);
        nameLabel.setBounds(new Rectangle(14, 90, 150, 18));
        nameValue.setFont(defaultFont);
        nameValue.setBounds(new Rectangle(170, 90, 350, 18));

        typeLabel.setText("Type: text/vcard");
        typeLabel.setFont(defaultFont);
        typeLabel.setBounds(new Rectangle(14, 120, 150, 18));

        confirmButton.setFont(defaultFont);
        confirmButton.setText("Add");
        confirmButton.setBounds(170, 200, 70, 25);

        confirmButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event ) {
                try {
                    validateValues();
                    getValues();
                    if (getState() == STATE_INSERT) {
                        EntitySyncSourceConfigPanel.this.actionPerformed(new ActionEvent(EntitySyncSourceConfigPanel.this, ACTION_EVENT_INSERT, event.getActionCommand()));
                    } else {
                        EntitySyncSourceConfigPanel.this.actionPerformed(new ActionEvent(EntitySyncSourceConfigPanel.this, ACTION_EVENT_UPDATE, event.getActionCommand()));
                    }
                } catch (Exception e) {
                    notifyError(new AdminException(e.getMessage()));
                }
            }
        });

        // add all components to the panel
        this.add(panelName        , null);
        this.add(nameLabel        , null);
        this.add(nameValue        , null);
        this.add(typeLabel        , null);
        this.add(sourceUriLabel   , null);
        this.add(sourceUriValue   , null);
        this.add(confirmButton    , null);
    }

    /**
     * Loads the given syncSource showing the name, uri and type in the panel's
     * fields.
     *
     * @param syncSource the SyncSource instance
     */
    public void updateForm() {
         if (!(getSyncSource() instanceof EntitySyncSource)) {
          notifyError(
              new AdminException(
                  "This is not an EntitySyncSource! Unable to process SyncSource values."
              )
          );
          return;
        }
        if (getState() == STATE_INSERT) {
          confirmButton.setText("Add");
        } else if (getState() == STATE_UPDATE) {
          confirmButton.setText("Save");
        }

        this.syncSource = (EntitySyncSource) getSyncSource();

        sourceUriValue.setText(syncSource.getSourceURI() );
        nameValue.setText     (syncSource.getName()      );
        
        if (this.syncSource.getSourceURI() != null) {
            sourceUriValue.setEditable(false);
        }
    }

    // ----------------------------------------------------------- Private methods
    /**
     * Checks if the values provided by the user are all valid. In caso of errors,
     * a IllegalArgumentException is thrown.
     *
     * @throws IllegalArgumentException if:
     *         <ul>
     *         <li>name, uri, type or directory are empty (null or zero-length)
     *         <li>the types list length does not match the versions list length
     *         </ul>
     */
    private void validateValues() throws IllegalArgumentException {
        String value = null;

        value = nameValue.getText();
        if (StringUtils.isEmpty(value)) {
            throw new
            IllegalArgumentException(
            "Field 'Name' cannot be empty. Please provide a SyncSource name.");
        }

        if (!StringUtils.containsOnly(value, NAME_ALLOWED_CHARS.toCharArray())) {
            throw new
            IllegalArgumentException(
            "Only the following characters are allowed for field 'Name': \n" + NAME_ALLOWED_CHARS);
        }

        value = sourceUriValue.getText();
        if (StringUtils.isEmpty(value)) {
            throw new
            IllegalArgumentException(
            "Field 'Source URI' cannot be empty. Please provide a SyncSource URI.");
        }
    }

    /**
     * Set syncSource properties with the values provided by the user.
     */
    private void getValues()
    {
        syncSource.setSourceURI(sourceUriValue.getText().trim());
        syncSource.setName     (nameValue.getText().trim());

        syncSource.setInfo(new SyncSourceInfo(new ContentType[] { new ContentType("text/vcard", "1.1") }, 0));
    }

}
