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

import java.sql.Timestamp;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.entity.GenericEntityException;

import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.source.SyncSource;

/**
 * Represents a processor that knows about syncing records whose remote bean type is <E>
 *  Entitys and Relations
 *  Their mappings to beans
 *  SyncSourceExceptions TODO: does it really have to know this?
 *  
 * ...but does not know about:
 *  Transaction Management
 *  SyncItems
 *  SyncSourceException
 * 
 * Each instance will only be used by one thread.
 * 
 * @author Cameron Smith - Database, Lda - www.database.co.mz *
 */
public interface EntitySyncHandler<E>
{
    //=== initialization ===
    
    public enum MergeStrategy {server_wins, client_wins, merge}

    /**
     * This method should always be called inside a transaction
     * @param syncSource
     * @throws GeneralException 
     */
    public void init(EntitySyncSource syncSource) throws GeneralException;
    
    //=== methods which map directly to MergeableSyncSource methods ===
    
    /**
     * Get the merge strategy configured for this handler, should never be null
     */
    public MergeStrategy getConfiguredMergeStrategy();
    

    /**
     * @see SyncSource.getAllItemKeys
     * @return a sequence of the PKs of Entities which are managed by this handler for the given sync user
     */
    public Iterable<String> getAllKeys();
    
    public Iterable<String> getNewKeys(Timestamp since) throws GenericEntityException;
    
    public Iterable<String> getUpdatedKeys(Timestamp since) throws GenericEntityException;
    
    /**
     * @see SyncSource.getDeletedItemKeys
     * @return a sequence of the PKs of Entities which should be deleted in the client
     * TODO: what about composite PKs? who is responsible for converting to canonical string format?
     *  Probably should return GenericPKs here and let EntitySyncSource convert to SyncItemKeys
     */
    public Iterable<String> getDeletedKeys(Timestamp since) throws GeneralException;
    
    public Iterable<String> getKeysFromTwin(E twin) throws GeneralException;
    
    public E getItemFromId(String key) throws GeneralException;
    
    public void removeItem(String key) throws GeneralException;
    
    /**
     * Update the OT record with the given PK, from the given source Java Bean
     */
    public void updateItem(String key, E source) throws GeneralException;
    
    /**
     * Add the OT record
     * return a partyId to be used as SyncItemKey
     */
    public String addItem(E source) throws GeneralException;

    /**
     * Merge the remoteItem with the data in the entity represented by the given key.
     * 
     * @param key PK of entity to be merged
     * @param source remote bean to be merged
     * @param since when was <code>source</code> last updated?
     * @return true if source was changed, ie remote client needs to re-update with it
     */
    public boolean mergeItem(String key, E source) throws GeneralException;
    
    public E convertDataToBean(SyncItem item)throws GeneralException;
    
    public byte[] convertBeanToData(E item)throws GeneralException;
    
}
