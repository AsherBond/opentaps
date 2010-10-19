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

package org.opentaps.gwt.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * The GwtFunction used to read gwt function from *.properties and generate CommonMessages.
 */
public class GwtLabelDefinition {

    /**
     * The label key.
     * Ex: "commonWelcome"
     */
    private String key;

    /**
     * The origin label key.
     * Ex: "CommonWelcome"
     */
    private String originKey;
    /**
     * The label text string.
     * Ex: "Welcome {0} {1}."
     */
    private String text;

    /**
     * The <code>List</code> of parameter names.
     * Ex: <code>[ "firstName", "lastName" ]</code>
     */
    private List<String> parameters = new ArrayList<String>();

    /**
     * the source properties file of label.
     * Ex: "opentapsUiLabels.properties"
     */
    private String propertiesFile;
    /**
     * Gets the label text string.
     * @return the label text string
     */
    public String getText() {
        return text;
    }
    /**
     * Gets the source properties file of label.
     * @return the source properties file of label
     */
    public String getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * Gets the default label text string. Used in the generated message interface as the text supplied to the <code>Default</code> annotation.
     * @return the default label text string
     */
    public String getDefaultText() {
        return text.replace("\"", "\\\"");
    }

    /**
     * Gets the label key. Used in the generated message interface as the method name.
     * @return the label key
     */
    public String getKey() {
        return key;
    }
    
    /**
     * Gets the origin label key. Used in the generated message interface as the method name.
     * @return the label key
     */
    public String getOriginKey() {
        return originKey;
    }

    /**
     * Gets the <code>List</code> of parameters. Used in the generated message interface as the parameters name.
     * @return the <code>List</code> of parameters
     */
    public List<String> getParameters() {
        return parameters;
    }

    /**
     * Checks if this label definition matches the signature of the given label definition.
     * This is used to compare localized labels with the default labels, to make sure that the localized label
     *  will work with the generated interface.
     * @param reference a <code>GwtLabelDefinition</code> value
     * @return a <code>boolean</code> value
     */
    public boolean matchesSignatureOf(GwtLabelDefinition reference) {
        return parameters.containsAll(reference.getParameters()) && (reference.getParameters().size() == parameters.size());
    }

    /**
     * Creates a simple label definition.
     *
     * @param propertiesFile the source properties file of label
     * @param key the label key
     * @param text the label text
     */
    public GwtLabelDefinition(String propertiesFile, String key, String originKey, String text) {
        this.propertiesFile = propertiesFile;
        this.key = key;
        this.originKey = originKey;
        this.text = text;
    }

    /**
     * Creates a parameterized label definition.
     *
     * @param propertiesFile the source properties file of label
     * @param key the label key
     * @param text the label text
     * @param parameters the <code>List</code> of parameter names
     */
    public GwtLabelDefinition(String propertiesFile, String key, String text, String originKey, List<String> parameters) {
        this.propertiesFile = propertiesFile;
        this.key = key;
        this.text = text;
        this.parameters = parameters;
        this.originKey = originKey;
    }
}
