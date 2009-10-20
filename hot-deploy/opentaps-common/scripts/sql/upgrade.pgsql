/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
 
/*
These scripts are for making table changes from opentaps 1.0 to opentaps 1.4
*/

/*
* Modifying the product average cost table to change its primary keys
* The following statements will do:
*   1. drop PRODUCT_AVERAGE_COST old primary key
*   2. add PRODUCT_AVERAGE_COST_ID column
*   3. set different value to PRODUCT_AVERAGE_COST_ID
*   4. set PRODUCT_AVERAGE_COST_ID as primary key
*/
ALTER TABLE PRODUCT_AVERAGE_COST DROP CONSTRAINT PK_PRODUCT_AVERAGE_COST;
ALTER TABLE PRODUCT_AVERAGE_COST ADD COLUMN PRODUCT_AVERAGE_COST_ID character varying(20);
ALTER TABLE PRODUCT_AVERAGE_COST ALTER COLUMN PRODUCT_AVERAGE_COST_ID SET STORAGE EXTENDED;
CREATE TEMP SEQUENCE rownum START 10000;
UPDATE PRODUCT_AVERAGE_COST SET PRODUCT_AVERAGE_COST_ID=nextval('rownum');
DROP SEQUENCE rownum;
ALTER TABLE PRODUCT_AVERAGE_COST ADD CONSTRAINT PK_PRODUCT_AVERAGE_COST PRIMARY KEY (PRODUCT_AVERAGE_COST_ID);
insert into SEQUENCE_VALUE_ITEM(SEQ_NAME, SEQ_ID) select 'ProductAverageCost', CAST (max(PRODUCT_AVERAGE_COST_ID) as numeric)  from PRODUCT_AVERAGE_COST;
update REQUIREMENT set STATUS_ID = 'REQ_CLOSED' where STATUS_ID in ('REQ_ORDERED', 'REQ_PRODUCED', 'REQ_TRANSFERRED');
