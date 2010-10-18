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

package org.opentaps.financials.domain.billing.lockbox;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import org.ofbiz.base.crypto.HashCrypt;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.domain.billing.lockbox.LockboxBatch;
import org.opentaps.domain.billing.lockbox.LockboxBatchItem;
import org.opentaps.domain.billing.lockbox.LockboxBatchItemDetail;
import org.opentaps.domain.billing.lockbox.LockboxFileParserException;
import org.opentaps.domain.billing.lockbox.LockboxFileParserInterface;


/** {@inheritDoc} */
public class LockboxFileParser implements LockboxFileParserInterface {

    private static final String MODULE = LockboxFileParser.class.getName();

    private static final String DATE_FORMAT = "yyMMdd";
    private static final String TIME_FORMAT = "HHmm";
    private static final String DATE_TIME_FORMAT = DATE_FORMAT + TIME_FORMAT;
    private static final int LINE_LENGTH = 80;

    private List<LockboxBatch> lockboxBatches;
    private List<LockboxBatchItem> lockboxBatchItems;
    private List<LockboxBatchItemDetail> lockboxBatchItemDetails;

    private HeaderLine headerLine;
    private ServiceHeaderLine serviceHeaderLine;
    private ServiceTotalLine serviceTotalLine;
    private DestinationTrailerLine destinationTrailerLine;

    private List<Batch> batches;
    private Batch currentBatch;
    private Map<String, String> accountAndRoutingNumbers;

    private String lockboxFileHash;

    /**
     * Default constructor.
     */
    public LockboxFileParser() { }

    /**
     * Gets the parsed <code>HeaderLine</code>, only after <code>parse()</code>.
     * @return a <code>HeaderLine</code> value
     */
    public HeaderLine getHeaderLine() {
        return headerLine;
    }

    /**
     * Gets the parsed <code>ServiceHeaderLine</code>, only after <code>parse()</code>.
     * @return a <code>ServiceHeaderLine</code> value
     */
    public ServiceHeaderLine getServiceHeaderLine() {
        return serviceHeaderLine;
    }

    /**
     * Gets the parsed <code>ServiceTotalLine</code>, only after <code>parse()</code>.
     * @return a <code>ServiceTotalLine</code> value
     */
    public ServiceTotalLine getServiceTotalLine() {
        return serviceTotalLine;
    }

    /**
     * Gets the parsed <code>DestinationTrailerLine</code>, only after <code>parse()</code>.
     * @return a <code>DestinationTrailerLine</code> value
     */
    public DestinationTrailerLine getDestinationTrailerLine() {
        return destinationTrailerLine;
    }

    /**
     * Gets the parsed <code>Batch</code>, only after <code>parse()</code>.
     * @return a list of <code>Batch</code> value
     */
    public List<Batch> getBatches() {
        return batches;
    }

    /**
     * Gets the parsed account and routing numbers, only after <code>parse()</code>.
     * Those numbers are collected from all the lines that contain them both.
     * @return a <code>Map</code> of <code>accountNumber</code> to <code>routingNumber</code>
     */
    public Map<String, String> getAccountAndRoutingNumbers() {
        return accountAndRoutingNumbers;
    }

    /** {@inheritDoc} */
    public void parse(String data) throws LockboxFileParserException {
        // this is used to test for duplicate import
        lockboxFileHash = HashCrypt.getDigestHash(data);
        parse(data.replaceAll("\r", "").split("\n"));
    }

    /** {@inheritDoc} */
    public void parse(String[] lines) throws LockboxFileParserException {
        batches = new ArrayList<Batch>();

        if (UtilValidate.isEmpty(lockboxFileHash)) {
            throw new LockboxFileParserException("There are no valid hash of original file.");
        }

        // collect the routing numbers from DetailHeaderLine and DetailLine
        accountAndRoutingNumbers = new HashMap<String, String>();

        // reads the data line by line, and populate the batches list
        for (String line : lines) {
            readNextLine(line);
        }

        // end of data, check that the last batch was closed properly
        if (currentBatch != null) {
            throw new LockboxFileParserException("Unexpected end of data, the last batch was not closed.");
        }

        lockboxBatches = new ArrayList<LockboxBatch>();
        lockboxBatchItems = new ArrayList<LockboxBatchItem>();
        lockboxBatchItemDetails = new ArrayList<LockboxBatchItemDetail>();

        // track the total amount of all batches
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Batch batch : batches) {

            DetailHeaderLine header = batch.getDetailHeaderLine();

            LockboxBatch lockboxBatch = new LockboxBatch();
            String id = header.getBatchNumber();
            lockboxBatch.setLockboxBatchId(id);
            lockboxBatch.setBatchId(id);
            lockboxBatch.setDatetimeEntered(headerLine.getDateTime());
            lockboxBatch.setBatchCount(batch.getBatchTotalLine().getCount());
            lockboxBatch.setBatchAmount(batch.getBatchTotalLine().getAmount());
            lockboxBatch.setOutstandingAmount(batch.getBatchTotalLine().getAmount());
            lockboxBatch.setFileHashMark(lockboxFileHash);
            lockboxBatches.add(lockboxBatch);

            // track the total amount of the checks in this batch
            BigDecimal checkTotalAmount = BigDecimal.ZERO;

            Debug.logInfo("Created LockboxBatch: " + lockboxBatch, MODULE);

            List<String> usedCheckIds = FastList.newInstance();

            for (BatchItem item : batch.getItems()) {

                DetailLine detail = item.getDetailLine();
                // the account number is stored in the destination field
                accountAndRoutingNumbers.put(detail.getDestination(), detail.getRtNumber());

                LockboxBatchItem lockboxBatchItem = new LockboxBatchItem();
                lockboxBatchItem.setLockboxBatchId(id);
                lockboxBatchItem.setItemSeqId(detail.getItemNumber());
                lockboxBatchItem.setPaymentDate(detail.getDate());
                lockboxBatchItem.setCheckNumber(detail.getCheckNumber());
                lockboxBatchItem.setCheckAmount(detail.getAmount());
                lockboxBatchItem.setRoutingNumber(detail.getRtNumber());
                lockboxBatchItem.setAccountNumber(detail.getDestination());
                // test if the check id is unique for the batch
                if (usedCheckIds.contains(lockboxBatchItem.getItemSeqId())) {
                    throw new LockboxFileParserException("Check ID [" + lockboxBatchItem.getItemSeqId() + "] occurs in batch more than once.");
                }
                lockboxBatchItems.add(lockboxBatchItem);
                usedCheckIds.add(lockboxBatchItem.getItemSeqId());

                List<String> usedDetailIds = FastList.newInstance();

                // sum the checks total
                checkTotalAmount = checkTotalAmount.add(lockboxBatchItem.getCheckAmount());

                Debug.logInfo("Created LockboxBatchItem: " + lockboxBatchItem, MODULE);

                for (OverflowLine overflow : item.getItems()) {
                    LockboxBatchItemDetail lockboxBatchItemDetail = new LockboxBatchItemDetail();
                    lockboxBatchItemDetail.setLockboxBatchId(id);
                    lockboxBatchItemDetail.setItemSeqId(overflow.getItemNumber());
                    lockboxBatchItemDetail.setDetailSeqId(overflow.getSequence());
                    lockboxBatchItemDetail.setInvoiceNumber(overflow.getInvoiceNumber());
                    lockboxBatchItemDetail.setInvoiceAmount(overflow.getInvoiceAmount());
                    lockboxBatchItemDetail.setCustomerId(overflow.getCustomerId());
                    // test if item detail id is unique for a check
                    if (usedDetailIds.contains(lockboxBatchItemDetail.getDetailSeqId())) {
                        throw new LockboxFileParserException("Check number [" + lockboxBatchItem.getCheckNumber() + "] has payment applications with the same sequence id.");
                    }
                    lockboxBatchItemDetails.add(lockboxBatchItemDetail);
                    usedDetailIds.add(lockboxBatchItemDetail.getDetailSeqId());

                    Debug.logInfo("Created LockboxBatchItemDetail: " + lockboxBatchItemDetail, MODULE);
                }
            }

            // end of batch, validate the checks total against the batch total
            if (checkTotalAmount.compareTo(lockboxBatch.getBatchAmount()) != 0) {
                throw new LockboxFileParserException("Mismatch between batch total [" + lockboxBatch.getBatchAmount() + "] and actual checks total [" + checkTotalAmount + "] for batch ID [" + lockboxBatch.getBatchId() + "].");
            }

            // sum the total of all batches
            totalAmount = totalAmount.add(checkTotalAmount);
        }

        // end of file parsing, check total against the service total line
        if (totalAmount.compareTo(serviceTotalLine.getAmount()) != 0) {
            throw new LockboxFileParserException("Mismatch between the service total line amount [" + serviceTotalLine.getAmount() + "] and actual total [" + totalAmount + "]");
        }
    }

    /** {@inheritDoc} */
    public List<LockboxBatch> getLockboxBatches() {
        return lockboxBatches;
    }

    /** {@inheritDoc} */
    public List<LockboxBatchItem> getLockboxBatchItems() {
        return lockboxBatchItems;
    }

    /** {@inheritDoc} */
    public List<LockboxBatchItemDetail> getLockboxBatchItemDetails() {
        return lockboxBatchItemDetails;
    }

    private void readNextLine(String line) throws LockboxFileParserException {

        Debug.logInfo("* " + line, MODULE);

        // check line validity
        if (!isValidLine(line)) {
            throw new LockboxFileParserException("Line is invalid: [" + line + "]");
        }

        // there is not a header line yet we are just starting to parse the file
        if (headerLine == null) {
            headerLine = new HeaderLine(line);
            return;
        }
        // else a new header line is illegal
        if (HeaderLine.isLine(line)) {
            throw new LockboxFileParserException("Unexpected new header line found.");
        }

        if (destinationTrailerLine != null) {
            throw new LockboxFileParserException("Unexpected new line found, already read the final line.");
        }

        // a service header should be the second line
        if (ServiceHeaderLine.isLine(line)) {
            // no batch should be started yet
            if (currentBatch != null) {
                throw new LockboxFileParserException("Unexpected service header line at this point.");
            }
            serviceHeaderLine = new ServiceHeaderLine(line);
            return;
        }
        // a detail header line starts a new batch
        if (DetailHeaderLine.isLine(line)) {
            // no batch should be processing
            if (currentBatch != null) {
                throw new LockboxFileParserException("Unexpected detail header line at this point, cannot start a new batch before closing the previous one.");
            }

            currentBatch = newBatch(new DetailHeaderLine(line));
            // set the headers
            currentBatch.setHeaders(headerLine, serviceHeaderLine);
            return;
        }
        // a detail line, for an open batch
        if (DetailLine.isLine(line)) {
            // must have an open batch
            if (currentBatch == null) {
                throw new LockboxFileParserException("Unexpected detail line at this point, no batch in progress.");
            }
            currentBatch.addDetail(new DetailLine(line));
            return;
        }
        // an overflow for an open batch with an item
        if (OverflowLine.isLine(line)) {
            // must have an open batch
            if (currentBatch == null) {
                throw new LockboxFileParserException("Unexpected overflow line at this point, no batch in progress.");
            }
            currentBatch.addOverflow(new OverflowLine(line));
            return;
        }
        // a batch total closes an open batch
        if (BatchTotalLine.isLine(line)) {
            // must have an open batch
            if (currentBatch == null) {
                throw new LockboxFileParserException("Unexpected batch total line at this point, no batch in progress.");
            }
            currentBatch.endBatch(new BatchTotalLine(line));
            batches.add(currentBatch);
            currentBatch = null;
            return;
        }

        // a service total
        if (ServiceTotalLine.isLine(line)) {
            // no batch should be open
            if (currentBatch != null) {
                throw new LockboxFileParserException("Unexpected service total line at this point, a batch is in progress.");
            }
            serviceTotalLine = new ServiceTotalLine(line);
            return;
        }
        // a destination trailer
        if (DestinationTrailerLine.isLine(line)) {
            // no batch should be open
            if (currentBatch != null) {
                throw new LockboxFileParserException("Unexpected destination trailer line at this point, a batch is in progress.");
            }
            destinationTrailerLine = new DestinationTrailerLine(line);
            return;
        }

    }

    private Batch newBatch(DetailHeaderLine line) throws LockboxFileParserException {
        // check the batch number id is not already used
        String batchNumber = line.getBatchNumber();
        for (Batch b : batches) {
            if (b.getDetailHeaderLine().getBatchNumber().equals(batchNumber)) {
                throw new LockboxFileParserException("Unexpected new batch, has same batch number [" + batchNumber + "] as an already parsed batch.");
            }
        }

        return new Batch(line);
    }

    /**
     * Converts a date from the lockbox format to a <code>Timestamp</code>.
     * The format expected is YYMMDD.
     * @param dateString a <code>String</code> value
     * @return a <code>Timestamp</code> value
     * @throws LockboxFileParserException if an error occurs
     */
    private static Timestamp readDate(String dateString) throws LockboxFileParserException {
        if (dateString.length() != DATE_FORMAT.length()) {
            throw new LockboxFileParserException("The given date string is invalid: [" + dateString + "].");
        }
        // this format would depend of the source
        // note the rule for abbreviated year is done by adjusting dates to be within 80 years before and 20 years after the time, see SimpleDateFormat javadoc
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        Date date;
        try {
            date = format.parse(dateString);
        } catch (ParseException e) {
            throw new LockboxFileParserException(e);
        }

        return new Timestamp(date.getTime());
    }

    /**
     * Converts a date time from the lockbox format to a <code>Timestamp</code>.
     * The format expected is YYMMDD for the date and HHMM for the time.
     * @param dateString a <code>String</code> value
     * @param timeString a <code>String</code> value
     * @return a <code>Timestamp</code> value
     * @throws LockboxFileParserException if an error occurs
     */
    private static Timestamp readDateTime(String dateString, String timeString) throws LockboxFileParserException {
        if (dateString.length() != DATE_FORMAT.length()) {
            throw new LockboxFileParserException("The given date string is invalid: [" + dateString + "].");
        }
        if (timeString.length() != TIME_FORMAT.length()) {
            throw new LockboxFileParserException("The given time string is invalid: [" + timeString + "].");
        }
        // this format would depend of the source
        // note the rule for abbreviated year is done by adjusting dates to be within 80 years before and 20 years after the time, see SimpleDateFormat javadoc
        SimpleDateFormat format = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.US);
        Date date;
        try {
            date = format.parse(dateString + timeString);
        } catch (ParseException e) {
            throw new LockboxFileParserException(e);
        }

        return new Timestamp(date.getTime());
    }

    private static BigDecimal readCents(String value) throws LockboxFileParserException {
        return readBigDecimal(value).movePointLeft(2);
    }

    private static BigDecimal readBigDecimal(String value) throws LockboxFileParserException {
        String str = value.trim();
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            throw new LockboxFileParserException("Could not parse the string [" + str + "] into a BigDecimal: " + e.getMessage());
        }
    }

    private static Long readLong(String value) throws LockboxFileParserException {
        String str = value.trim();
        try {
            return Long.valueOf(str);
        } catch (NumberFormatException e) {
            throw new LockboxFileParserException("Could not parse the string [" + str + "] into a Long: " + e.getMessage());
        }
    }

    private static String readString(String value) {
        String str = value.trim();
        // clean leading zero used in padding the field
        char[] chars = str.toCharArray();
        int index = 0;
        for (; index < str.length(); index++) {
            if (chars[index] != '0') {
                break;
            }
        }
        if (index == 0) {
            return str;
        } else {
            return str.substring(index);
        }
    }

    private static Boolean isValidLine(String line) {
        // check the line length is according to specifications
        Debug.logInfo("line length = " + line.length(), MODULE);
        return line.length() == LINE_LENGTH;
    }

    /**
     * Gets the hash (digest) of the parsed data.
     * This is used to test for duplicate imports.
     * @return the hash (digest) of the parsed data
     */
    public String getHash() {
        return lockboxFileHash;
    }

    private static class Batch {

        private HeaderLine headerLine;
        private ServiceHeaderLine serviceHeaderLine;
        private DetailHeaderLine detailHeaderLine;
        private BatchTotalLine batchTotalLine;

        private List<BatchItem> items;
        private BatchItem currentItem;

        public Batch(DetailHeaderLine detailHeaderLine) {
            this.detailHeaderLine = detailHeaderLine;
            this.items = new ArrayList<BatchItem>();
        }

        public void setHeaders(HeaderLine headerLine, ServiceHeaderLine serviceHeaderLine) {
            this.headerLine = headerLine;
            this.serviceHeaderLine = serviceHeaderLine;
        }

        public void endBatch(BatchTotalLine batchTotalLine) {
            this.batchTotalLine = batchTotalLine;
        }

        public void addDetail(DetailLine detailLine) throws LockboxFileParserException {
            if (!detailHeaderLine.getBatchNumber().equals(detailLine.getBatchNumber())) {
                throw new LockboxFileParserException("Unexpected detail line, current batch number is [" + detailHeaderLine.getBatchNumber() + "] doesn't match the detail line batch number [" + detailLine.getBatchNumber() + "].");
            }

            currentItem = new BatchItem(detailLine);
            items.add(currentItem);
        }

        public void addOverflow(OverflowLine overflowLine) throws LockboxFileParserException {
            if (currentItem == null) {
                throw new LockboxFileParserException("Unexpected overflow line at this point, no batch item in progress.");
            }
            if (!detailHeaderLine.getBatchNumber().equals(overflowLine.getBatchNumber())) {
                throw new LockboxFileParserException("Unexpected overflow line, current batch number is [" + detailHeaderLine.getBatchNumber() + "] doesn't match the overflow line batch number [" + overflowLine.getBatchNumber() + "].");
            }

            currentItem.addOverflow(overflowLine);
        }

        public HeaderLine getHeaderLine() {
            return headerLine;
        }

        public ServiceHeaderLine getServiceHeaderLine() {
            return serviceHeaderLine;
        }

        public DetailHeaderLine getDetailHeaderLine() {
            return detailHeaderLine;
        }

        public BatchTotalLine getBatchTotalLine() {
            return batchTotalLine;
        }

        public List<BatchItem> getItems() {
            return items;
        }
    }

    private static class BatchItem {

        private DetailLine detailLine;
        private List<OverflowLine> items;

        public BatchItem(DetailLine detailLine) {
            this.detailLine = detailLine;
            this.items = new ArrayList<OverflowLine>();
        }

        public void addOverflow(OverflowLine overflowLine) {
            items.add(overflowLine);
        }

        public DetailLine getDetailLine() {
            return detailLine;
        }

        public List<OverflowLine> getItems() {
            return items;
        }
    }

    /**
     * Represents the Header line and its structure.
     */
    private static class HeaderLine {

        public static final char ID = '1';
        private static final int PRIORITY_START = 1;
        private static final int PRIORITY_END = 3;
        private static final int COMPANY_NAME_START = 3;
        private static final int COMPANY_NAME_END = 13;
        private static final int RT_NUMBER_START = 13;
        private static final int RT_NUMBER_END = 23;
        private static final int DATE_START = 23;
        private static final int DATE_END = 29;
        private static final int TIME_START = 29;
        private static final int TIME_END = 33;

        private Timestamp dateTime;
        private String priority;
        private String companyName;
        private String rtNumber;

        public HeaderLine(String line) throws LockboxFileParserException {
            if (!isLine(line)) {
                throw new LockboxFileParserException("The line [" + line + "] is not a header line, it should start with [" + ID + "]");
            }

            // parse relevant information from this line
            String date = line.substring(DATE_START, DATE_END);
            String time = line.substring(TIME_START, TIME_END);

            // convert the date and time from their string format to a Timestamp
            priority = line.substring(PRIORITY_START, PRIORITY_END);
            companyName = line.substring(COMPANY_NAME_START, COMPANY_NAME_END);
            rtNumber = line.substring(RT_NUMBER_START, RT_NUMBER_END);
            dateTime = readDateTime(date, time);
        }

        public static Boolean isLine(String line) {
            return ID == line.charAt(0);
        }

        public String getPriority() {
            return priority;
        }

        public String getCompanyName() {
            return companyName;
        }

        public String getRtNumber() {
            return rtNumber;
        }

        public Timestamp getDateTime() {
            return dateTime;
        }
    }

    /**
     * Represents the Service Header line and its structure.
     */
    private static class ServiceHeaderLine {

        public static final char ID = '2';
        private static final int COMPANY_NAME_START = 1;
        private static final int COMPANY_NAME_END = 11;
        private static final int RT_NUMBER_START = 11;
        private static final int RT_NUMBER_END = 21;

        private String companyName;
        private String rtNumber;

        public ServiceHeaderLine(String line) throws LockboxFileParserException {
            if (!isLine(line)) {
                throw new LockboxFileParserException("The line [" + line + "] is not a service header line, it should start with [" + ID + "]");
            }

            // parse relevant information from this line
            companyName = line.substring(COMPANY_NAME_START, COMPANY_NAME_END);
            rtNumber = line.substring(RT_NUMBER_START, RT_NUMBER_END);
        }

        public static Boolean isLine(String line) {
            return ID == line.charAt(0);
        }

        public String getCompanyName() {
            return companyName;
        }

        public String getRtNumber() {
            return rtNumber;
        }
    }

    /**
     * Represents the Detail Header line (the start of a batch) and its structure.
     */
    private static class DetailHeaderLine {

        public static final char ID = '5';
        private static final int BATCH_NUMBER_START = 1;
        private static final int BATCH_NUMBER_END = 4;
        private static final int ITEM_NUMBER_START = 4;
        private static final int ITEM_NUMBER_END = 7;
        private static final int LOCKBOX_NUMBER_START = 7;
        private static final int LOCKBOX_NUMBER_END = 14;
        private static final int DATE_START = 14;
        private static final int DATE_END = 20;
        private static final int COMPANY_NAME_START = 20;
        private static final int COMPANY_NAME_END = 30;
        private static final int RT_NUMBER_START = 30;
        private static final int RT_NUMBER_END = 40;

        private String batchNumber;
        private String itemNumber;
        private String lockboxNumber;
        private Timestamp date;
        private String companyName;
        private String rtNumber;

        public DetailHeaderLine(String line) throws LockboxFileParserException {
            if (!isLine(line)) {
                throw new LockboxFileParserException("The line [" + line + "] is not a detail header line, it should start with [" + ID + "]");
            }

            // parse relevant information from this line
            batchNumber = line.substring(BATCH_NUMBER_START, BATCH_NUMBER_END);
            itemNumber = line.substring(ITEM_NUMBER_START, ITEM_NUMBER_END);
            lockboxNumber = line.substring(LOCKBOX_NUMBER_START, LOCKBOX_NUMBER_END);
            date = readDate(line.substring(DATE_START, DATE_END));
            companyName = line.substring(COMPANY_NAME_START, COMPANY_NAME_END);
            rtNumber = line.substring(RT_NUMBER_START, RT_NUMBER_END);
        }

        public static Boolean isLine(String line) {
            return ID == line.charAt(0);
        }

        public Timestamp getDate() {
            return date;
        }

        public String getBatchNumber() {
            return batchNumber;
        }

        public String getItemNumber() {
            return itemNumber;
        }

        public String getLockboxNumber() {
            return lockboxNumber;
        }

        public String getCompanyName() {
            return companyName;
        }

        public String getRtNumber() {
            return rtNumber;
        }
    }

    /**
     * Represents the Detail line (a check) and its structure.
     */
    private static class DetailLine {

        public static final char ID = '6';
        private static final int BATCH_NUMBER_START = 1;
        private static final int BATCH_NUMBER_END = 4;
        private static final int ITEM_NUMBER_START = 4;
        private static final int ITEM_NUMBER_END = 7;
        private static final int LOCKBOX_NUMBER_START = 7;
        private static final int LOCKBOX_NUMBER_END = 14;
        private static final int DATE_START = 14;
        private static final int DATE_END = 20;
        private static final int DESTINATION_START = 20;
        private static final int DESTINATION_END = 30;
        private static final int RT_NUMBER_START = 30;
        private static final int RT_NUMBER_END = 40;
        private static final int AMOUNT_START = 40;
        private static final int AMOUNT_END = 50;
        /*private static final int PAYER_RT_START = 50;
        private static final int PAYER_RT_END = 58;
        private static final int PAYER_ACCOUNT_START = 58;
        private static final int PAYER_ACCOUNT_END = 68;*/
        private static final int CHECK_START = 68;
        private static final int CHECK_END = 80;

        private String batchNumber;
        private String itemNumber;
        private String lockboxNumber;
        private Timestamp date;
        private String destination;
        private String rtNumber;
        private String checkNumber;
        private BigDecimal amount;

        public DetailLine(String line) throws LockboxFileParserException {
            if (!isLine(line)) {
                throw new LockboxFileParserException("The line [" + line + "] is not a detail line, it should start with [" + ID + "]");
            }

            // parse relevant information from this line
            batchNumber = line.substring(BATCH_NUMBER_START, BATCH_NUMBER_END);
            itemNumber = line.substring(ITEM_NUMBER_START, ITEM_NUMBER_END);
            lockboxNumber = line.substring(LOCKBOX_NUMBER_START, LOCKBOX_NUMBER_END);
            date = readDate(line.substring(DATE_START, DATE_END));
            destination = line.substring(DESTINATION_START, DESTINATION_END);
            rtNumber = line.substring(RT_NUMBER_START, RT_NUMBER_END);
            checkNumber = readString(line.substring(CHECK_START, CHECK_END));
            amount = readCents(line.substring(AMOUNT_START, AMOUNT_END));
        }

        public static Boolean isLine(String line) {
            return ID == line.charAt(0);
        }

        public Timestamp getDate() {
            return date;
        }

        public String getBatchNumber() {
            return batchNumber;
        }

        public String getItemNumber() {
            return itemNumber;
        }

        public String getLockboxNumber() {
            return lockboxNumber;
        }

        public String getDestination() {
            return destination;
        }

        public String getRtNumber() {
            return rtNumber;
        }

        public String getCheckNumber() {
            return checkNumber;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    /**
     * Represents the Overflow line (a payment application to an invoice) and its structure.
     */
    private static class OverflowLine {

        public static final char ID = '4';
        private static final int BATCH_NUMBER_START = 1;
        private static final int BATCH_NUMBER_END = 4;
        private static final int ITEM_NUMBER_START = 4;
        private static final int ITEM_NUMBER_END = 7;
        private static final int TYPE_START = 7;
        private static final int TYPE_END = 8;
        private static final int SEQUENCE_START = 8;
        private static final int SEQUENCE_END = 10;
        private static final int INVOICE_NUMBER_START = 10;
        private static final int INVOICE_NUMBER_END = 20;
        private static final int INVOICE_AMOUNT_START = 20;
        private static final int INVOICE_AMOUNT_END = 30;
        private static final int CUSTOMER_START = 60;
        private static final int CUSTOMER_END = 70;

        private String batchNumber;
        private String itemNumber;
        private String type;
        private String sequence;
        private String invoiceNumber;
        private BigDecimal invoiceAmount;
        private String customerId;

        public OverflowLine(String line) throws LockboxFileParserException {
            if (!isLine(line)) {
                throw new LockboxFileParserException("The line [" + line + "] is not an overflow line, it should start with [" + ID + "]");
            }

            // parse relevant information from this line
            batchNumber = line.substring(BATCH_NUMBER_START, BATCH_NUMBER_END);
            itemNumber = line.substring(ITEM_NUMBER_START, ITEM_NUMBER_END);
            type = line.substring(TYPE_START, TYPE_END);
            sequence = line.substring(SEQUENCE_START, SEQUENCE_END);
            invoiceNumber = readString(line.substring(INVOICE_NUMBER_START, INVOICE_NUMBER_END));
            invoiceAmount = readCents(line.substring(INVOICE_AMOUNT_START, INVOICE_AMOUNT_END));
            customerId = readString(line.substring(CUSTOMER_START, CUSTOMER_END));
        }

        public static Boolean isLine(String line) {
            return ID == line.charAt(0);
        }

        public String getBatchNumber() {
            return batchNumber;
        }

        public String getItemNumber() {
            return itemNumber;
        }

        public String getType() {
            return type;
        }

        public String getSequence() {
            return sequence;
        }

        public String getInvoiceNumber() {
            return invoiceNumber;
        }

        public BigDecimal getInvoiceAmount() {
            return invoiceAmount;
        }

        public String getCustomerId() {
            return customerId;
        }
    }

    /**
     * Represents the Batch Total Header line (the end of a batch) and its structure.
     */
    private static class BatchTotalLine {

        public static final char ID = '7';
        private static final int BATCH_NUMBER_START = 1;
        private static final int BATCH_NUMBER_END = 4;
        private static final int ITEM_NUMBER_START = 4;
        private static final int ITEM_NUMBER_END = 7;
        private static final int LOCKBOX_NUMBER_START = 7;
        private static final int LOCKBOX_NUMBER_END = 14;
        private static final int DATE_START = 14;
        private static final int DATE_END = 20;
        private static final int COUNT_START = 20;
        private static final int COUNT_END = 23;
        private static final int AMOUNT_START = 23;
        private static final int AMOUNT_END = 33;

        private String batchNumber;
        private String itemNumber;
        private String lockboxNumber;
        private Timestamp date;
        private Long count;
        private BigDecimal amount;

        public BatchTotalLine(String line) throws LockboxFileParserException {
            if (!isLine(line)) {
                throw new LockboxFileParserException("The line [" + line + "] is not a batch total line, it should start with [" + ID + "]");
            }

            // parse relevant information from this line
            batchNumber = line.substring(BATCH_NUMBER_START, BATCH_NUMBER_END);
            itemNumber = line.substring(ITEM_NUMBER_START, ITEM_NUMBER_END);
            lockboxNumber = line.substring(LOCKBOX_NUMBER_START, LOCKBOX_NUMBER_END);
            date = readDate(line.substring(DATE_START, DATE_END));
            count = readLong(line.substring(COUNT_START, COUNT_END));
            amount = readCents(line.substring(AMOUNT_START, AMOUNT_END));
        }

        public static Boolean isLine(String line) {
            return ID == line.charAt(0);
        }

        public Timestamp getDate() {
            return date;
        }

        public String getBatchNumber() {
            return batchNumber;
        }

        public String getItemNumber() {
            return itemNumber;
        }

        public String getLockboxNumber() {
            return lockboxNumber;
        }

        public Long getCount() {
            return count;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    /**
     * Represents the Service Total line (at the end of the file) and its structure.
     */
    private static class ServiceTotalLine {

        public static final char ID = '8';
        private static final int LOCKBOX_NUMBER_START = 7;
        private static final int LOCKBOX_NUMBER_END = 14;
        private static final int DATE_START = 14;
        private static final int DATE_END = 20;
        private static final int REMITTANCE_START = 20;
        private static final int REMITTANCE_END = 23;
        private static final int REMITTANCE_AMOUNT_START = 23;
        private static final int REMITTANCE_AMOUNT_END = 33;

        private String lockboxNumber;
        private Timestamp date;
        private Long count;
        private BigDecimal amount;

        public ServiceTotalLine(String line) throws LockboxFileParserException {
            if (!isLine(line)) {
                throw new LockboxFileParserException("The line [" + line + "] is not a service total line, it should start with [" + ID + "]");
            }

            // parse relevant information from this line
            lockboxNumber = line.substring(LOCKBOX_NUMBER_START, LOCKBOX_NUMBER_END);
            date = readDate(line.substring(DATE_START, DATE_END));
            count = readLong(line.substring(REMITTANCE_START, REMITTANCE_END));
            amount = readCents(line.substring(REMITTANCE_AMOUNT_START, REMITTANCE_AMOUNT_END));
        }

        public static Boolean isLine(String line) {
            return ID == line.charAt(0);
        }

        public String getLockboxNumber() {
            return lockboxNumber;
        }

        public Timestamp getDate() {
            return date;
        }

        public Long getCount() {
            return count;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    /**
     * Represents the Destination Trailer line (last line of the file) and its structure.
     */
    private static class DestinationTrailerLine {

        public static final char ID = '9';
        private static final int RECORD_COUNT_START = 1;
        private static final int RECORD_COUNT_END = 7;

        private Long count;

        public DestinationTrailerLine(String line) throws LockboxFileParserException {
            if (!isLine(line)) {
                throw new LockboxFileParserException("The line [" + line + "] is not a destination trailer line, it should start with [" + ID + "]");
            }

            // parse relevant information from this line
            count = readLong(line.substring(RECORD_COUNT_START, RECORD_COUNT_END));
        }

        public static Boolean isLine(String line) {
            return ID == line.charAt(0);
        }

        public Long getCount() {
            return count;
        }
    }
}
