/*
 * SymmetricDS is an open source database synchronization solution.
 *   
 * Copyright (C) Eric Long <erilong@users.sourceforge.net>,
 *               Chris Henson <chenson42@users.sourceforge.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.jumpmind.symmetric.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jumpmind.symmetric.common.TestConstants;
import org.jumpmind.symmetric.common.csv.CsvConstants;
import org.jumpmind.symmetric.ext.TestDataLoaderFilter;
import org.jumpmind.symmetric.load.AbstractDataLoaderTest;
import org.jumpmind.symmetric.load.csv.CsvLoader;
import org.jumpmind.symmetric.model.IncomingBatch;
import org.jumpmind.symmetric.model.IncomingBatchHistory;
import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.transport.internal.InternalIncomingTransport;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.csvreader.CsvWriter;

public class DataLoaderServiceTest extends AbstractDataLoaderTest {

    protected Node client = new Node(TestConstants.TEST_CLIENT_EXTERNAL_ID, null, null);;

    public DataLoaderServiceTest() {
    }

    public DataLoaderServiceTest(String dbType) {
        super(dbType);
    }

    protected Level setLoggingLevelForTest(Level level) {
        Level old = Logger.getLogger(DataLoaderService.class).getLevel();
        Logger.getLogger(DataLoaderService.class).setLevel(level);
        Logger.getLogger(CsvLoader.class).setLevel(level);
        return old;
    }

    /**
     * Make sure the symmetric engine is initialized.  @BeforeMethod was 
     * used intentionally to make sure the engine is initialized right before the 
     * tests run.  @BeforeTest ran too early for the MultiDatabaseTest.
     */
    @BeforeMethod(groups="continuous")
    public void init() {
        getSymmetricEngine();
    }
    
    @Test(groups = {"continuous"})
    public void testStatistics() throws Exception {
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] updateValues = new String[11];
        updateValues[0] = updateValues[10] = getNextId();
        updateValues[2] = updateValues[4] = "required string";
        String[] insertValues = (String[]) ArrayUtils.subarray(updateValues, 0, updateValues.length - 1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        writeTable(writer, TEST_TABLE, TEST_KEYS, TEST_COLUMNS);
        String nextBatchId = getNextBatchId();
        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId });

        // Update becomes fallback insert
        writer.write(CsvConstants.UPDATE);
        writer.writeRecord(updateValues, true);

        // Insert becomes fallback update
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);

        // Insert becomes fallback update
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);

        // Clean insert
        insertValues[0] = getNextId();
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);

        // Delete affects no rows
        writer.writeRecord(new String[] { CsvConstants.DELETE, getNextId() }, true);
        writer.writeRecord(new String[] { CsvConstants.DELETE, getNextId() }, true);
        writer.writeRecord(new String[] { CsvConstants.DELETE, getNextId() }, true);

        // Failing statement
        insertValues[0] = getNextId();
        insertValues[5] = "i am an invalid date";
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);

        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId });
        writer.close();
        load(out);

        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId,
                TestConstants.TEST_CLIENT_EXTERNAL_ID);
        Assert.assertEquals(list.size(), 1, "Wrong number of history. " + printDatabase());
        IncomingBatchHistory history = list.get(0);
        Assert.assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status. "
                + printDatabase());
        Assert.assertNotNull(history.getStartTime(), "Start time cannot be null. " + printDatabase());
        Assert.assertNotNull(history.getEndTime(), "End time cannot be null. " + printDatabase());
        Assert.assertEquals(history.getFailedRowNumber(), 8, "Wrong failed row number. " + printDatabase());
        Assert.assertEquals(history.getByteCount(), 317, "Wrong byte count. " + printDatabase());
        Assert.assertEquals(history.getStatementCount(), 8, "Wrong statement count. " + printDatabase());
        Assert.assertEquals(history.getFallbackInsertCount(), 1, "Wrong fallback insert count. "
                + printDatabase());
        Assert.assertEquals(history.getFallbackUpdateCount(), 2, "Wrong fallback update count. "
                + printDatabase());
        Assert.assertEquals(history.getMissingDeleteCount(), 3, "Wrong missing delete count. "
                + printDatabase());
        setLoggingLevelForTest(old);
    }

    @Test(groups = {"continuous"})
    public void testUpdateCollision() throws Exception {       
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] insertValues = new String[11];
        insertValues[0] = getNextId();
        insertValues[2] = insertValues[4] = "inserted row for testUpdateCollision";

        String[] updateValues = new String[11];
        updateValues[0] = getId();
        updateValues[10] = getNextId();
        updateValues[2] = updateValues[4] = "update will become an insert that violates PK";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        writeTable(writer, TEST_TABLE, TEST_KEYS, TEST_COLUMNS);

        String nextBatchId = getNextBatchId();

        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId });

        // This insert will be OK
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);

        // Update becomes fallback insert, and then violate the primary key
        writer.write(CsvConstants.UPDATE);
        writer.writeRecord(updateValues, true);

        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId });
        writer.close();
        load(out);

        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId,
                TestConstants.TEST_CLIENT_EXTERNAL_ID);
        Assert.assertEquals(list.size(), 1, "Wrong number of history");

        load(out);

        IncomingBatchHistory history = list.get(0);
        Assert.assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status");

        list = getIncomingBatchService().findIncomingBatchHistory(batchId,
                TestConstants.TEST_CLIENT_EXTERNAL_ID);
        Assert.assertEquals(list.size(), 2, "Wrong number of history");

        history = list.get(0);
        Assert.assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status");

        history = list.get(1);
        Assert.assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status");
        setLoggingLevelForTest(old);
    }

    @Test(groups = {"continuous"})
    public void testSqlStatistics() throws Exception {
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] insertValues = new String[10];
        insertValues[2] = insertValues[4] = "sql stat test";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        String nextBatchId = getNextBatchId();
        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId });
        writeTable(writer, TEST_TABLE, TEST_KEYS, TEST_COLUMNS);

        // Clean insert
        String firstId = getNextId();
        insertValues[0] = firstId;
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);

        // Clean insert
        String secondId = getNextId();
        insertValues[0] = secondId;
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);

        String thirdId = getNextId();
        insertValues[0] = thirdId;
        insertValues[2] = "This is a very long string that will fail upon insert into the database.";
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);

        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId });
        writer.close();
        load(out);

        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId,
                TestConstants.TEST_CLIENT_EXTERNAL_ID);
        Assert.assertEquals(list.size(), 1, "Wrong number of history");
        IncomingBatchHistory history = list.get(0);
        Assert.assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status. "
                + printDatabase());
        Assert.assertNotNull(history.getStartTime(), "Start time cannot be null. " + printDatabase());
        Assert.assertNotNull(history.getEndTime(), "End time cannot be null. " + printDatabase());
        Assert.assertEquals(history.getFailedRowNumber(), 3, "Wrong failed row number. " + printDatabase());
        Assert.assertEquals(history.getByteCount(), 374, "Wrong byte count. " + printDatabase());
        Assert.assertEquals(history.getStatementCount(), 3, "Wrong statement count. " + printDatabase());
        Assert.assertEquals(history.getFallbackInsertCount(), 0, "Wrong fallback insert count. "
                + printDatabase());
        Assert.assertEquals(history.getFallbackUpdateCount(), 1, "Wrong fallback update count. "
                + printDatabase());
        Assert.assertEquals(history.getMissingDeleteCount(), 0, "Wrong missing delete count. "
                + printDatabase());
        Assert.assertNotNull(history.getSqlState(), "Sql state should not be null. " + printDatabase());
        Assert.assertNotNull(history.getSqlMessage(), "Sql message should not be null. " + printDatabase());
        setLoggingLevelForTest(old);
    }

    @Test(groups = {"continuous"})
    public void testSkippingResentBatch() throws Exception {
        String[] values = { getNextId(), "resend string", "resend string not null", "resend char",
                "resend char not null", "2007-01-25 00:00:00.0", "2007-01-25 01:01:01.0", "0", "7", "10.10" };
        getNextBatchId();
        for (int i = 0; i < 7; i++) {
            batchId--;
            testSimple(CsvConstants.INSERT, values, values);
            IncomingBatchHistory.Status expectedStatus = IncomingBatchHistory.Status.OK;
            long expectedCount = 1;
            if (i > 0) {
                expectedStatus = IncomingBatchHistory.Status.SK;
                expectedCount = 0;
            }
            Assert.assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID),
                    IncomingBatch.Status.OK, "Wrong status");
            List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(
                    batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
            Assert.assertEquals(list.size(), i + 1, "Wrong number of history");
            IncomingBatchHistory history = list.get(i);
            Assert.assertEquals(history.getStatus(), expectedStatus, "Wrong status");
            Assert.assertEquals(history.getFailedRowNumber(), 0, "Wrong failed row number");
            Assert.assertEquals(history.getStatementCount(), expectedCount, "Wrong statement count");
            Assert.assertEquals(history.getFallbackInsertCount(), 0, "Wrong fallback insert count");
            Assert.assertEquals(history.getFallbackUpdateCount(), 0, "Wrong fallback update count");
            // pause to make sure we get a different start time on the incoming batch history
            Thread.sleep(10);
        }
    }

    @Test(groups = {"continuous"})
    public void testErrorWhileSkip() throws Exception {
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] values = { getNextId(), "string2", "string not null2", "char2", "char not null2",
                "2007-01-02 00:00:00.0", "2007-02-03 04:05:06.0", "0", "47", "67.89" };

        testSimple(CsvConstants.INSERT, values, values);
        Assert.assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID),
                IncomingBatch.Status.OK, "Wrong status");
        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId,
                TestConstants.TEST_CLIENT_EXTERNAL_ID);
        Assert.assertEquals(list.size(), 1, "Wrong number of history");
        IncomingBatchHistory history = list.get(0);
        Assert.assertEquals(history.getStatus(), IncomingBatchHistory.Status.OK, "Wrong status");
        Assert.assertEquals(history.getFailedRowNumber(), 0, "Wrong failed row number");
        Assert.assertEquals(history.getStatementCount(), 1, "Wrong statement count");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        writer.writeRecord(new String[] { CsvConstants.BATCH, getBatchId() });
        writer.writeRecord(new String[] { CsvConstants.TABLE, TEST_TABLE });
        writer.write("UnknownTokenWithinBatch");
        writer.writeRecord(new String[] { CsvConstants.COMMIT, getBatchId() });
        writer.close();
        // Pause a moment to guarantee our history comes back in time order
        Thread.sleep(10);
        load(out);
        Assert.assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID),
                IncomingBatch.Status.OK, "Wrong status");
        list = getIncomingBatchService().findIncomingBatchHistory(batchId,
                TestConstants.TEST_CLIENT_EXTERNAL_ID);
        Assert.assertEquals(list.size(), 2, "Wrong number of history");
        history = list.get(1);
        Assert.assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status");
        Assert.assertEquals(history.getFailedRowNumber(), 0, "Wrong failed row number");
        Assert.assertEquals(history.getStatementCount(), 0, "Wrong statement count");
        setLoggingLevelForTest(old);
    }

    @Test(groups = {"continuous"})
    public void testErrorWhileParsing() throws Exception {
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] values = { getNextId(), "should not reach database", "string not null", "char",
                "char not null", "2007-01-02", "2007-02-03 04:05:06.0", "0", "47", "67.89" };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        writer.write("UnknownTokenOutsideBatch");
        String nextBatchId = getNextBatchId();
        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId });
        writer.writeRecord(new String[] { CsvConstants.TABLE, TEST_TABLE });
        writeTable(writer, TEST_TABLE, TEST_KEYS, TEST_COLUMNS);
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(values, true);
        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId });
        writer.close();
        load(out);
        Assert.assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID), null,
                "Wrong status");
        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId,
                TestConstants.TEST_CLIENT_EXTERNAL_ID);
        Assert.assertEquals(list.size(), 0, "Wrong number of history");
        setLoggingLevelForTest(old);
    }

    @Test(groups = {"continuous"})
    public void testErrorThenSuccessBatch() throws Exception {
        Logger.getLogger(DataLoaderServiceTest.class).warn("testErrorThenSuccessBatch");
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] values = { getNextId(), "This string is too large and will cause the statement to fail",
                "string not null2", "char2", "char not null2", "2007-01-02 00:00:00.0",
                "2007-02-03 04:05:06.0", "0", "47", "67.89" };
        getNextBatchId();
        int retries = 3;
        for (int i = 0; i < retries; i++) {
            batchId--;
            testSimple(CsvConstants.INSERT, values, null);
            Assert.assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID),
                    IncomingBatch.Status.ER, "Wrong status");
            List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(
                    batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
            Assert.assertEquals(list.size(), i + 1, "Wrong number of history. " + printDatabase());
            IncomingBatchHistory history = list.get(i);
            Assert.assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status. "
                    + printDatabase());
            Assert.assertEquals(history.getFailedRowNumber(), 1, "Wrong failed row number. "
                    + printDatabase());
            Assert.assertEquals(history.getStatementCount(), 1, "Wrong statement count. " + printDatabase());
            // pause to make sure we get a different start time on the incoming batch history
            Thread.sleep(10);
        }

        batchId--;
        values[1] = "A smaller string that will succeed";
        testSimple(CsvConstants.INSERT, values, values);
        Assert.assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID),
                IncomingBatch.Status.OK, "Wrong status. " + printDatabase());
        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId,
                TestConstants.TEST_CLIENT_EXTERNAL_ID);
        Assert.assertEquals(list.size(), retries + 1, "Wrong number of history. " + printDatabase());
        IncomingBatchHistory history = list.get(retries);
        Assert.assertEquals(history.getStatus(), IncomingBatchHistory.Status.OK, "Wrong status. "
                + printDatabase());
        Assert.assertEquals(history.getFailedRowNumber(), 0, "Wrong failed row number. " + printDatabase());
        Assert.assertEquals(history.getStatementCount(), 1, "Wrong statement count. " + printDatabase());
        setLoggingLevelForTest(old);
    }

    @Test(groups = {"continuous"})
    public void testMultipleBatch() throws Exception {
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] values = { getNextId(), "string", "string not null2", "char2", "char not null2",
                "2007-01-02 00:00:00.0", "2007-02-03 04:05:06.0", "0", "47", "67.89" };
        String[] values2 = { getNextId(), "This string is too large and will cause the statement to fail",
                "string not null2", "char2", "char not null2", "2007-01-02 00:00:00.0",
                "2007-02-03 04:05:06.0", "0", "47", "67.89" };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        writeTable(writer, TEST_TABLE, TEST_KEYS, TEST_COLUMNS);

        String nextBatchId = getNextBatchId();
        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId });
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(values, true);
        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId });

        String nextBatchId2 = getNextBatchId();
        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId2 });
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(values2, true);
        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId2 });

        writer.close();
        load(out);
        assertTestTableEquals(values[0], values);
        assertTestTableEquals(values2[0], null);

        Assert.assertEquals(findIncomingBatchStatus(Integer.parseInt(nextBatchId),
                TestConstants.TEST_CLIENT_EXTERNAL_ID), IncomingBatch.Status.OK, "Wrong status. "
                + printDatabase());
        Assert.assertEquals(findIncomingBatchStatus(Integer.parseInt(nextBatchId2),
                TestConstants.TEST_CLIENT_EXTERNAL_ID), IncomingBatch.Status.ER, "Wrong status. "
                + printDatabase());
        setLoggingLevelForTest(old);
    }

    protected void load(ByteArrayOutputStream out) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        getTransportManager().setIncomingTransport(new InternalIncomingTransport(in));
        getDataLoaderService().loadData(client, null);
    }

    protected IncomingBatch.Status findIncomingBatchStatus(int batchId, String nodeId) {
        IncomingBatch batch = getIncomingBatchService().findIncomingBatch(batchId, nodeId);
        IncomingBatch.Status status = null;
        if (batch != null) {
            status = batch.getStatus();
        }
        return status;
    }
    
    @Test(groups="continuous",dependsOnMethods="testMultipleBatch")
    public void testAutoRegisteredExtensionPoint() {
        TestDataLoaderFilter registeredFilter = (TestDataLoaderFilter)getBeanFactory().getBean("registeredDataFilter");
        TestDataLoaderFilter unRegisteredFilter = (TestDataLoaderFilter)getBeanFactory().getBean("unRegisteredDataFilter");
        Assert.assertTrue(registeredFilter.getNumberOfTimesCalled() > 0);
        Assert.assertTrue(unRegisteredFilter.getNumberOfTimesCalled() == 0);
    }

}
