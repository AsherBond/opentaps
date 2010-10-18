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

package org.opentaps.gwt.common.client.lookup;

import com.gwtext.client.data.Record;
import org.opentaps.gwt.common.client.UtilUi;

/**
 * Manage permissions related to entity lookup services.
 * Also used in the GWT widgets to determine what actions can be performed.
 */
public class Permissions {

    private Boolean create;
    private Boolean update;
    private Boolean delete;

    /** The field in the entity that stores the Create permission flag. */
    public static final String CREATE_FIELD_NAME = "__PERM_CREATE";
    /** The field in the entity that stores the Update permission flag. */
    public static final String UPDATE_FIELD_NAME = "__PERM_UPDATE";
    /** The field in the entity that stores the Delete permission flag. */
    public static final String DELETE_FIELD_NAME = "__PERM_DELETE";

    /**
     * Creates a new <code>Permissions</code> instance with <code>null</code> values.
     */
    public Permissions() { }

    /**
     * Creates a new <code>Permissions</code> copying the values from another <code>Permissions</code> instance.
     * @param p the <code>Permissions</code> instance to copy
     */
    public Permissions(Permissions p) {
        this(p.canCreate(), p.canUpdate(), p.canDelete());
    }

    /**
     * Creates a new <code>Permissions</code> setting all values to the same setting.
     * @param global the setting to apply to all permissions
     */
    public Permissions(Boolean global) {
        this(global, global, global);
    }

    /**
     * Creates a new <code>Permissions</code> from a <code>Record</code>.
     * @param record the <code>Record</code> to parse
     */
    public Permissions(Record record) {
        this(canCreate(record), canUpdate(record), canDelete(record));
    }

    /**
     * Creates a new <code>Permissions</code> instance with the given values.
     * @param create a <code>Boolean</code> value
     * @param update a <code>Boolean</code> value
     * @param delete a <code>Boolean</code> value
     */
    public Permissions(Boolean create, Boolean update, Boolean delete) {
        this.create = create;
        this.update = update;
        this.delete = delete;
    }

    /**
     * Merges these base <code>Permissions</code> with a child <code>Permissions</code>, this should be used to merge the global permissions and per entity permissions.
     * The merged value is always the most restrictive of the base and child.
     * @param p the child <code>Permissions</code> value
     * @return a new <code>Permissions</code> instance with the merged values
     */
    public Permissions merge(Permissions p) {
        Permissions merged = new Permissions(this);
        if (p == null) {
            return merged;
        }

        // the base permission prevails unless it is TRUE and the child permission is FALSE.
        merged.setCanCreate(merge(merged.canCreate(), p.canCreate()));
        merged.setCanUpdate(merge(merged.canUpdate(), p.canUpdate()));
        merged.setCanDelete(merge(merged.canDelete(), p.canDelete()));
        return merged;
    }

    private boolean merge(Boolean base, Boolean p) {
        if (p == null) {
            return base;
        }

        if (base == Boolean.FALSE) {
            return false;
        }

        // base == true or null, so p can take precedence
        return p;
    }

    /**
     * Checks if the Create permission is set.
     * @return a <code>Boolean</code> value
     */
    public Boolean canCreate() {
        return create;
    }

    /**
     * Checks if the Update permission is set.
     * @return a <code>Boolean</code> value
     */
    public Boolean canUpdate() {
        return update;
    }

    /**
     * Checks if the Delete permission is set.
     * @return a <code>Boolean</code> value
     */
    public Boolean canDelete() {
        return delete;
    }

    /**
     * Sets the Create permission.
     * @param canCreate a <code>boolean</code> value
     */
    public void setCanCreate(boolean canCreate) {
        create = canCreate;
    }

    /**
     * Sets the Update permission.
     * @param canUpdate a <code>boolean</code> value
     */
    public void setCanUpdate(boolean canUpdate) {
        update = canUpdate;
    }

    /**
     * Sets the Delete permission.
     * @param canDelete a <code>boolean</code> value
     */
    public void setCanDelete(boolean canDelete) {
        delete = canDelete;
    }

    /**
     * Sets the Create permission on the given <code>Record</code>.
     * @param canCreate a <code>boolean</code> value
     * @param record the <code>Record</code> to change
     */
    public static void setCanCreate(boolean canCreate, Record record) {
        record.set(Permissions.CREATE_FIELD_NAME, true);
    }

    /**
     * Sets the Update permission.
     * @param canUpdate a <code>boolean</code> value
     * @param record the <code>Record</code> to change
     */
    public static void setCanUpdate(boolean canUpdate, Record record) {
        record.set(Permissions.UPDATE_FIELD_NAME, true);
    }

    /**
     * Sets the Delete permission.
     * @param canDelete a <code>boolean</code> value
     * @param record the <code>Record</code> to change
     */
    public static void setCanDelete(boolean canDelete, Record record) {
        record.set(Permissions.DELETE_FIELD_NAME, true);
    }

    /**
     * Sets all permissions to the same value.
     * @param global a <code>boolean</code> value
     */
    public void setAll(boolean global) {
        setCanCreate(global);
        setCanUpdate(global);
        setCanDelete(global);
    }

    /**
     * Checks the global permission <code>Record</code> if new records can be created.
     * @param record a <code>Record</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean canCreate(Record record) {
        // sanity check
        if (record == null) {
            return false;
        }
        // should not be a summary row, but in case deny permission
        if (UtilUi.isSummary(record)) {
            return false;
        }
        // check record specific permission
        if (!record.isEmpty(Permissions.CREATE_FIELD_NAME)) {
            return record.getAsBoolean(Permissions.CREATE_FIELD_NAME);
        }
        // else deny by default
        return false;
    }

    /**
     * Checks the global permission <code>Record</code> or if a given <code>Record</code> can be edited.
     * @param record a <code>Record</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean canUpdate(Record record) {
        // sanity check
        if (record == null) {
            return false;
        }
        // summary row are never editable
        if (UtilUi.isSummary(record)) {
            return false;
        }
        // check record specific permission
        if (!record.isEmpty(Permissions.UPDATE_FIELD_NAME)) {
            return record.getAsBoolean(Permissions.UPDATE_FIELD_NAME);
        }
        // else deny by default
        return false;
    }

    /**
     * Checks the global permission <code>Record</code> or if a given <code>Record</code> can be deleted in this grid.
     * @param record a <code>Record</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean canDelete(Record record) {
        // sanity check
        if (record == null) {
            return false;
        }
        // summary row are never deletable
        if (UtilUi.isSummary(record)) {
            return false;
        }
        // check record specific permission
        if (!record.isEmpty(Permissions.DELETE_FIELD_NAME)) {
            return record.getAsBoolean(Permissions.DELETE_FIELD_NAME);
        }
        // else deny by default
        return false;
    }

    /**
     * Checks if the given field name is used for holding Permission data.
     * This can be used to strip client side permissions before submitting data to the server.
     * @param fieldName a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isPermissionField(String fieldName) {
        return (Permissions.DELETE_FIELD_NAME.equals(fieldName) || Permissions.CREATE_FIELD_NAME.equals(fieldName) || Permissions.UPDATE_FIELD_NAME.equals(fieldName));

    }
}
