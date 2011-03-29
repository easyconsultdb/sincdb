/*
 * Licensed to JumpMind Inc under one or more contributor 
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding 
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU Lesser General Public License (the
 * "License"); you may not use this file except in compliance
 * with the License. 
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see           
 * <http://www.gnu.org/licenses/>.
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.  */


package org.jumpmind.symmetric.extract.csv;

import java.io.Writer;
import java.io.IOException;

import org.jumpmind.symmetric.common.csv.CsvConstants;
import org.jumpmind.symmetric.extract.DataExtractorContext;
import org.jumpmind.symmetric.model.Data;
import org.jumpmind.symmetric.util.CsvUtils;

/**
 * 
 *
 * 
 */
class StreamInsertDataCommand extends AbstractStreamDataCommand {

    public void execute(Writer writer, Data data, String routerId, DataExtractorContext context) throws IOException {
        context.incrementByteCount(CsvUtils.write(writer, CsvConstants.INSERT, DELIMITER, data.getRowData()));
        CsvUtils.writeLineFeed(writer);
        context.incrementDataEventCount();
    }
    
    public boolean isTriggerHistoryRequired() {
        return true;
    }
}