/* This file is part of VoltDB.
 * Copyright (C) 2008-2010 VoltDB L.L.C.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.voltdb.regressionsuites;

import java.io.IOException;
import org.voltdb.*;
import org.voltdb.VoltTable.ColumnInfo;
import org.voltdb.client.*;
import org.voltdb.compiler.VoltProjectBuilder;
import org.voltdb.elt.*;

/**
 *  End to end ELT tests using the RawProcessor and the ELSinkServer.
 *
 *  Note, this test reuses the TestSQLTypesSuite schema and procedures.
 *  Each table in that schema, to the extent the DDL is supported by the
 *  DB, really needs an ELT round trip test.
 */

public class TestELTSuite extends RegressionSuite {

    /** Shove a table name and pkey in front of row data */
    private Object[] convertValsToParams(String tableName, final int i,
                                         final Object[] rowdata)
    {
        final Object[] params = new Object[rowdata.length + 2];
        params[0] = tableName;
        params[1] = i;
        for (int ii=0; ii < rowdata.length; ++ii)
            params[ii+2] = rowdata[ii];
        return params;
    }

    /** Push pkey into expected row data */
    private Object[] convertValsToRow(final int i, final Object[] rowdata) {
        final Object[] row = new Object[rowdata.length + 1];
        row[0] = i;
        for (int ii=0; ii < rowdata.length; ++ii)
            row[ii+1] = rowdata[ii];
        return row;
    }

    private void quiesce(final Client client)
    throws ProcCallException, NoConnectionsException, IOException
    {
        client.drain();
        client.callProcedure("@Quiesce");
    }

    private void quiesceAndVerify(final Client client, ELTestClient tester)
    throws ProcCallException, IOException
    {
        quiesce(client);
        tester.work();
        assertTrue(tester.allRowsVerified());
    }

    private void quiesceAndVerifyFalse(final Client client, ELTestClient tester)
    throws ProcCallException, IOException
    {
        quiesce(client);
        tester.work();
        assertFalse(tester.allRowsVerified());
    }

    /**
     * Verify safe startup (we can connect clients and poll empty tables)
     */
    public void testELTSafeStartup() throws IOException, ProcCallException, InterruptedException
    {
        ELTestClient tester = new ELTestClient();
        tester.connectToELServer();

        final Client client = getClient();
        quiesceAndVerify(client, tester);
    }

    /**
     * Sends ten tuples to an EL enabled VoltServer and verifies the receipt
     * of those tuples after a quiesce (shutdown). Base case.
     */
    public void testELTRoundTripPersistentTable() throws IOException, ProcCallException, InterruptedException
    {
        ELTestClient tester = new ELTestClient();
        tester.connectToELServer();

        final Client client = getClient();
        for (int i=0; i < 10; i++) {
            final Object[] rowdata = TestSQLTypesSuite.m_midValues;
            tester.addRow("ALLOW_NULLS", i, convertValsToRow(i, rowdata));

            final Object[] params = convertValsToParams("ALLOW_NULLS", i, rowdata);
            client.callProcedure("Insert", params);
        }
        quiesceAndVerify(client, tester);
    }

    public void testELTLocalServerTooMany() throws IOException, ProcCallException, InterruptedException
    {
        ELTestClient tester = new ELTestClient();
        tester.connectToELServer();

        final Client client = getClient();
        for (int i=0; i < 10; i++) {
            final Object[] rowdata = TestSQLTypesSuite.m_midValues;
            final Object[] params = convertValsToParams("ALLOW_NULLS", i, rowdata);
            client.callProcedure("Insert", params);
        }
        quiesceAndVerifyFalse(client, tester);
    }

    public void testELTLocalServerTooMany2() throws IOException, ProcCallException, InterruptedException
    {
        ELTestClient tester = new ELTestClient();
        tester.connectToELServer();

        final Client client = getClient();
        for (int i=0; i < 10; i++) {
            final Object[] rowdata = TestSQLTypesSuite.m_midValues;
            // register only even rows with tester
            if ((i % 2) == 0)
            {
                tester.addRow("ALLOW_NULLS", i, convertValsToRow(i, rowdata));
            }
            final Object[] params = convertValsToParams("ALLOW_NULLS", i, rowdata);
            client.callProcedure("Insert", params);
        }
        quiesceAndVerifyFalse(client, tester);
    }

    /** Verify test infrastructure fails a test that sends too few rows */
    public void testELTLocalServerTooFew() throws IOException, ProcCallException, InterruptedException
    {
        ELTestClient tester = new ELTestClient();
        tester.connectToELServer();
        final Client client = getClient();

        for (int i=0; i < 10; i++) {
            final Object[] rowdata = TestSQLTypesSuite.m_midValues;
            tester.addRow("ALLOW_NULLS", i, convertValsToRow(i, rowdata));
            final Object[] params = convertValsToParams("ALLOW_NULLS", i, rowdata);
            // Only do the first 7 inserts
            if (i < 7)
            {
                client.callProcedure("Insert", params);
            }
        }
        quiesceAndVerifyFalse(client, tester);
    }

    /** Verify test infrastructure fails a test that sends mismatched data */
    public void testELTLocalServerBadData() throws IOException, ProcCallException, InterruptedException
    {
        ELTestClient tester = new ELTestClient();
        tester.connectToELServer();
        final Client client = getClient();

        for (int i=0; i < 10; i++) {
            final Object[] rowdata = TestSQLTypesSuite.m_midValues;
            // add wrong pkeys on purpose!
            tester.addRow("ALLOW_NULLS", i + 10, convertValsToRow(i+10, rowdata));
            final Object[] params = convertValsToParams("ALLOW_NULLS", i, rowdata);
            client.callProcedure("Insert", params);
        }
        quiesceAndVerifyFalse(client, tester);
    }

    /**
     * Sends ten tuples to an EL enabled VoltServer and verifies the receipt
     * of those tuples after a quiesce (shutdown). Base case.
     */
    public void testELTRoundTripStreamedTable() throws IOException, ProcCallException, InterruptedException
    {
        ELTestClient tester = new ELTestClient();
        tester.connectToELServer();
        final Client client = getClient();

        for (int i=0; i < 10; i++) {
            final Object[] rowdata = TestSQLTypesSuite.m_midValues;
            tester.addRow("NO_NULLS", i, convertValsToRow(i, rowdata));
            final Object[] params = convertValsToParams("NO_NULLS", i, rowdata);
            client.callProcedure("Insert", params);
        }
        quiesceAndVerify(client, tester);
    }

    /** Test that a table w/o ELT enabled does not produce ELT content */
    public void testThatTablesOptIn() throws IOException, ProcCallException, InterruptedException
    {
        ELTestClient tester = new ELTestClient();
        tester.connectToELServer();
        final Client client = getClient();

        Object params[] = new Object[TestSQLTypesSuite.COLS + 2];
        params[0] = "WITH_DEFAULTS";  // this table should not produce ELT output

        // populate the row data
        for (int i=0; i < TestSQLTypesSuite.COLS; ++i) {
            params[i+2] = TestSQLTypesSuite.m_midValues[i];
        }

        for (int i=0; i < 10; i++) {
            params[1] = i; // pkey
            // do NOT add row to TupleVerfier as none should be produced
            client.callProcedure("Insert", params);
        }
        quiesceAndVerify(client, tester);
    }


    class RollbackCallback implements ProcedureCallback {
        @Override
        public void clientCallback(ClientResponse clientResponse) {
            if (clientResponse.getStatus() != ClientResponse.USER_ABORT) {
                fail();
            }
        }
    }

    /*
     * Sends many tuples to an EL enabled VoltServer and verifies the receipt
     * of each in the EL stream. Some procedures rollback (after a real insert).
     * Tests that streams are correct in the face of rollback.
     */
    public void testELTRollback() throws IOException, ProcCallException, InterruptedException {
        ELTestClient tester = new ELTestClient();
        tester.connectToELServer();
        final Client client = getClient();

        double rollbackPerc = 0.15;
        double random = Math.random(); // initializes the generator

        // eltxxx: should pick more random data
        final Object[] rowdata = TestSQLTypesSuite.m_midValues;

        // roughly 10k rows is a full buffer it seems
        for (int pkey=0; pkey < 175000; pkey++) {
            if ((pkey % 1000) == 0) {
                System.out.println("Rollback test added " + pkey + " rows");
            }
            final Object[] params = convertValsToParams("ALLOW_NULLS", pkey, rowdata);
            random = Math.random();
            if (random <= rollbackPerc) {
                // note - do not update the el verifier as this rollsback
                boolean done;
                do {
                    done = client.callProcedure(new RollbackCallback(), "RollbackInsert", params);
                    if (done == false) {
                        client.backpressureBarrier();
                    }
                } while (!done);
            }
            else {
                tester.addRow("ALLOW_NULLS", pkey, convertValsToRow(pkey, rowdata));
                // the sync call back isn't synchronous if it isn't explicitly blocked on...
                boolean done;
                do {
                    done = client.callProcedure(new NullCallback(), "Insert", params);
                    if (done == false) {
                        client.backpressureBarrier();
                    }
                } while (!done);
            }
        }
        client.drain();
        quiesceAndVerify(client, tester);
    }

    private VoltTable createLoadTableTable(boolean addToVerifier, ELTestClient tester) {

        VoltTable loadTable = new VoltTable(new ColumnInfo("PKEY", VoltType.INTEGER),
          new ColumnInfo(TestSQLTypesSuite.m_columnNames[0], TestSQLTypesSuite.m_types[0]),
          new ColumnInfo(TestSQLTypesSuite.m_columnNames[1], TestSQLTypesSuite.m_types[1]),
          new ColumnInfo(TestSQLTypesSuite.m_columnNames[2], TestSQLTypesSuite.m_types[2]),
          new ColumnInfo(TestSQLTypesSuite.m_columnNames[3], TestSQLTypesSuite.m_types[3]),
          new ColumnInfo(TestSQLTypesSuite.m_columnNames[4], TestSQLTypesSuite.m_types[4]),
          new ColumnInfo(TestSQLTypesSuite.m_columnNames[5], TestSQLTypesSuite.m_types[5]),
          new ColumnInfo(TestSQLTypesSuite.m_columnNames[6], TestSQLTypesSuite.m_types[6]),
          new ColumnInfo(TestSQLTypesSuite.m_columnNames[7], TestSQLTypesSuite.m_types[7]),
          new ColumnInfo(TestSQLTypesSuite.m_columnNames[8], TestSQLTypesSuite.m_types[8]),
          new ColumnInfo(TestSQLTypesSuite.m_columnNames[9], TestSQLTypesSuite.m_types[9]),
          new ColumnInfo(TestSQLTypesSuite.m_columnNames[10], TestSQLTypesSuite.m_types[10]));

        for (int i=0; i < 100; i++) {
            if (addToVerifier) {
                tester.addRow("ALLOW_NULLS", i, convertValsToRow(i, TestSQLTypesSuite.m_midValues));
            }
            loadTable.addRow(convertValsToRow(i, TestSQLTypesSuite.m_midValues));
        }
        return loadTable;
    }

    /*
     * Verify that allowELT = no is obeyed for @LoadMultipartitionTable
     */
    public void testLoadMultiPartitionTableELTOff() throws IOException, ProcCallException
    {
        // allow ELT is off. no rows added to the verifier
        ELTestClient tester = new ELTestClient();
        tester.connectToELServer();
        VoltTable loadTable = createLoadTableTable(false, tester);
        Client client = getClient();
        client.callProcedure("@LoadMultipartitionTable", "ALLOW_NULLS", loadTable, 0);
        quiesceAndVerify(client, tester);
    }

    /*
     * Verify that allowELT = yes is obeyed for @LoadMultipartitionTable
     */
    public void testLoadMultiPartitionTableELTOn() throws IOException, ProcCallException
    {
        // allow ELT is on. rows added to the verifier
        ELTestClient tester = new ELTestClient();
        tester.connectToELServer();
        VoltTable loadTable = createLoadTableTable(true, tester);
        Client client = getClient();
        client.callProcedure("@LoadMultipartitionTable", "ALLOW_NULLS", loadTable, 1);
        quiesceAndVerify(client, tester);
    }

    /*
     * Verify that allowELT = yes is obeyed for @LoadMultipartitionTable
     */
    public void testLoadMultiPartitionTableELTOn2() throws IOException, ProcCallException
    {
        // allow ELT is on but table is not opted in to ELT.
        ELTestClient tester = new ELTestClient();
        tester.connectToELServer();
        VoltTable loadTable = createLoadTableTable(false, tester);
        Client client = getClient();
        client.callProcedure("@LoadMultipartitionTable", "WITH_DEFAULTS", loadTable, 1);
        quiesceAndVerify(client, tester);
    }

    /*
     * Verify that planner rejects updates to append-only tables
     */
    public void testELTUpdateAppendOnly() throws IOException {
        final Client client = getClient();
        boolean passed = false;
        try {
            client.callProcedure("@AdHoc", "Update NO_NULLS SET A_TINYINT=0 WHERE PKEY=0;");
        }
        catch (ProcCallException e) {
            if (e.getMessage().contains("Illegal to update an export-only table.")) {
                passed = true;
            }
        }
        assertTrue(passed);
    }

    /*
     * Verify that planner rejects reads of append-only tables.
     */
    public void testELTSelectAppendOnly() throws IOException {
        final Client client = getClient();
        boolean passed = false;
        try {
            client.callProcedure("@AdHoc", "Select PKEY from NO_NULLS WHERE PKEY=0;");
        }
        catch (ProcCallException e) {
            if (e.getMessage().contains("Illegal to read an export-only table.")) {
                passed = true;
            }
        }
        assertTrue(passed);
    }

    /*
     *  Verify that planner rejects deletes of append-only tables
     */
    public void testELTDeleteAppendOnly() throws IOException {
        final Client client = getClient();
        boolean passed = false;
        try {
            client.callProcedure("@AdHoc", "DELETE from NO_NULLS WHERE PKEY=0;");
        }
        catch (ProcCallException e) {
            if (e.getMessage().contains("Illegal to delete from an export-only table.")) {
                passed = true;
            }
        }
        assertTrue(passed);
    }


    /*
     * Test suite boilerplate
     */
    static final Class<?>[] PROCEDURES = {
        org.voltdb.regressionsuites.sqltypesprocs.Insert.class,
        org.voltdb.regressionsuites.sqltypesprocs.RollbackInsert.class
    };

    public TestELTSuite(final String name) {
        super(name);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.runClasses(TestOrderBySuite.class);
    }

    static public junit.framework.Test suite()
    {
        final MultiConfigSuiteBuilder builder =
            new MultiConfigSuiteBuilder(TestELTSuite.class);

        final VoltProjectBuilder project = new VoltProjectBuilder();
        project.addSchema(TestELTSuite.class.getResource("sqltypessuite-ddl.sql"));
        // add the connector/processor (name, host, port)
        // and the exportable tables (name, export-only)
        project.addELT("org.voltdb.elt.processors.RawProcessor", true /*enabled*/);
        // "WITH_DEFAULTS" is a non-elt'd persistent table
        project.addELTTable("ALLOW_NULLS", false);   // persistent table
        project.addELTTable("NO_NULLS", true);  // streamed table
        // and then project builder as normal
        project.addPartitionInfo("NO_NULLS", "PKEY");
        project.addPartitionInfo("ALLOW_NULLS", "PKEY");
        project.addPartitionInfo("WITH_DEFAULTS", "PKEY");
        project.addPartitionInfo("WITH_NULL_DEFAULTS", "PKEY");
        project.addPartitionInfo("EXPRESSIONS_WITH_NULLS", "PKEY");
        project.addPartitionInfo("EXPRESSIONS_NO_NULLS", "PKEY");
        project.addPartitionInfo("JUMBO_ROW", "PKEY");
        project.addProcedures(PROCEDURES);

        // JNI
        final VoltServerConfig config =
            new LocalSingleProcessServer("elt-ddl.jar", 2,
                                         BackendTarget.NATIVE_EE_JNI);

        config.compile(project);
        builder.addServerConfig(config);

        return builder;
    }

}
