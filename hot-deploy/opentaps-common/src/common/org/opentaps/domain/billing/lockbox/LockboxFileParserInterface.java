/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package org.opentaps.domain.billing.lockbox;

import java.util.List;

/**
 * Lockbox file parser.
 */
public interface LockboxFileParserInterface {

    /**
     * Parse the data.
     * @param data a <code>String</code> containing the text file to import
     * @throws LockboxFileParserException if an error occurs
     */
    public void parse(String data) throws LockboxFileParserException;

    /**
     * Parse the data.
     * @param lines an array of data strings representing the lockbox file
     * @throws LockboxFileParserException if an error occurs
     */
    public void parse(String[] lines) throws LockboxFileParserException;

    /**
     * Gets the parsed list of <code>LockboxBatch</code>.
     * Note that the primary key has to be completed before storage.
     * @return the parsed list of <code>LockboxBatch</code>
     */
    public List<LockboxBatch> getLockboxBatches();

    /**
     * Gets the parsed list of <code>LockboxBatchItem</code>.
     * Note that the primary key has to be completed before storage.
     * @return the parsed list of <code>LockboxBatchItem</code>
     */
    public List<LockboxBatchItem> getLockboxBatchItems();

    /**
     * Gets the parsed list of <code>LockboxBatchItemDetail</code>.
     * Note that the primary key has to be completed before storage.
     * @return the parsed list of <code>LockboxBatchItemDetail</code>
     */
    public List<LockboxBatchItemDetail> getLockboxBatchItemDetails();
}
