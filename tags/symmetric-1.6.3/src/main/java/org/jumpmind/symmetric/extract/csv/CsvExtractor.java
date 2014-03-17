/*
 * SymmetricDS is an open source database synchronization solution.
 *   
 * Copyright (C) Chris Henson <chenson42@users.sourceforge.net>
 * Copyright (C) Andrew Wilcox <andrewbwilcox@users.sourceforge.net>
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

package org.jumpmind.symmetric.extract.csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.common.csv.CsvConstants;
import org.jumpmind.symmetric.db.IDbDialect;
import org.jumpmind.symmetric.extract.DataExtractorContext;
import org.jumpmind.symmetric.extract.IDataExtractor;
import org.jumpmind.symmetric.model.Data;
import org.jumpmind.symmetric.model.DataEventType;
import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.model.OutgoingBatch;
import org.jumpmind.symmetric.service.INodeService;
import org.jumpmind.symmetric.service.IParameterService;

public class CsvExtractor implements IDataExtractor {

    private Map<String, IStreamDataCommand> dictionary = null;

    private IParameterService parameterService;

    private IDbDialect dbDialect;

    private INodeService nodeService;

    public void init(BufferedWriter writer, DataExtractorContext context) throws IOException {
        Node nodeIdentity = nodeService.findIdentity();
        String nodeId = (nodeIdentity == null) ? parameterService.getString(ParameterConstants.EXTERNAL_ID)
                : nodeIdentity.getNodeId();
        Util.write(writer, CsvConstants.NODEID, Util.DELIMITER, nodeId);
        writer.newLine();
    }

    public void begin(OutgoingBatch batch, BufferedWriter writer) throws IOException {
        Util.write(writer, CsvConstants.BATCH, Util.DELIMITER, Long.toString(batch.getBatchId()));
        writer.newLine();
        Util.write(writer, CsvConstants.BINARY, Util.DELIMITER, dbDialect.getBinaryEncoding()
                .name());
        writer.newLine();
    }

    public void commit(OutgoingBatch batch, BufferedWriter writer) throws IOException {
        Util.write(writer, CsvConstants.COMMIT, Util.DELIMITER, Long.toString(batch.getBatchId()));
        writer.newLine();
    }

    public void write(BufferedWriter writer, Data data, DataExtractorContext context) throws IOException {
        IStreamDataCommand cmd = dictionary.get(data.getEventType().getCode());
        if (cmd.isTriggerHistoryRequired()) {
            preprocessTable(data, writer, context);
        }
        cmd.execute(writer, data, context);
    }

    /**
     * Writes the table metadata out to a stream only if it hasn't already been
     * written out before
     * 
     * @param tableName
     * @param out
     */
    public void preprocessTable(Data data, BufferedWriter out, DataExtractorContext context) throws IOException {
        if (data.getTriggerHistory() == null) {
            throw new RuntimeException("Missing trigger_hist for table " + data.getTableName()
                    + ": try running syncTriggers() or restarting SymmetricDS");
        }
        String auditKey = Integer.toString(data.getTriggerHistory().getTriggerHistoryId()).intern();
        if (!context.getAuditRecordsWritten().contains(auditKey)) {
            Util.write(out, CsvConstants.TABLE, ", ", data.getTableName());
            out.newLine();
            Util.write(out, CsvConstants.KEYS, ", ", data.getTriggerHistory().getPkColumnNames());
            out.newLine();
            Util.write(out, CsvConstants.COLUMNS, ", ", data.getTriggerHistory().getColumnNames());
            out.newLine();
            context.getAuditRecordsWritten().add(auditKey);
        } else if (!context.isLastTable(data.getTableName())) {
            Util.write(out, CsvConstants.TABLE, ", ", data.getTableName());
            out.newLine();
        }
        if (data.getEventType() == DataEventType.UPDATE && data.getOldData() != null) {
            Util.write(out, CsvConstants.OLD, ", ", data.getOldData());
            out.newLine();
        }
        context.setLastTableName(data.getTableName());
    }

    public void setDictionary(Map<String, IStreamDataCommand> dictionary) {
        this.dictionary = dictionary;
    }

    public void setDbDialect(IDbDialect dbDialect) {
        this.dbDialect = dbDialect;
    }

    public void setParameterService(IParameterService parameterService) {
        this.parameterService = parameterService;
    }

    public void setNodeService(INodeService nodeService) {
        this.nodeService = nodeService;
    }

}