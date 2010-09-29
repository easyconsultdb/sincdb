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

package org.jumpmind.symmetric.integrate;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jumpmind.symmetric.common.logging.ILog;
import org.jumpmind.symmetric.common.logging.LogFactory;
import org.jumpmind.symmetric.ext.ICacheContext;
import org.jumpmind.symmetric.ext.IExtensionPoint;
import org.jumpmind.symmetric.ext.INodeGroupExtensionPoint;
import org.jumpmind.symmetric.model.DataEventType;

/**
 * 
 */
public class AbstractXmlPublisherExtensionPoint implements IExtensionPoint, INodeGroupExtensionPoint {

    protected final ILog log = LogFactory.getLog(getClass());

    protected final String XML_CACHE = "XML_CACHE_" + this.hashCode();

    private String[] nodeGroups;

    private boolean autoRegister = true;

    protected IPublisher publisher;

    protected Set<String> tableNamesToPublishAsGroup;

    protected String xmlTagNameToUseForGroup = "batch";

    protected List<String> groupByColumnNames;

    protected Format xmlFormat;

    protected ITimeGenerator timeStringGenerator = new ITimeGenerator() {
        public String getTime() {
            return Long.toString(System.currentTimeMillis());
        }
    };

    public AbstractXmlPublisherExtensionPoint() {
        xmlFormat = Format.getCompactFormat();
        xmlFormat.setOmitDeclaration(true);
    }

    protected final static Namespace getXmlNamespace() {
        return Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Element> getXmlCache(ICacheContext ctx) {
        Map<String, Object> cache = ctx.getContextCache();
        Map<String, Element> xmlCache = (Map<String, Element>) cache.get(XML_CACHE);
        if (xmlCache == null) {
            xmlCache = new HashMap<String, Element>();
            cache.put(XML_CACHE, xmlCache);
        }
        return xmlCache;
    }

    @SuppressWarnings("unchecked")
    protected boolean doesXmlExistToPublish(ICacheContext ctx) {
        Map<String, Object> cache = ctx.getContextCache();
        Map<String, StringBuilder> xmlCache = (Map<String, StringBuilder>) cache.get(XML_CACHE);
        return xmlCache != null && xmlCache.size() > 0;
    }

    protected void finalizeXmlAndPublish(ICacheContext ctx) {
        Map<String, Element> ctxCache = getXmlCache(ctx);
        Collection<Element> buffers = ctxCache.values();
        for (Iterator<Element> iterator = buffers.iterator(); iterator.hasNext();) {
            String xml = new XMLOutputter(xmlFormat).outputString(new Document(iterator.next()));
            log.debug("XMLSending", xml);
            iterator.remove();
            publisher.publish(ctx, xml.toString());
        }
    }

    protected void toXmlElement(DataEventType dml, Element xml, String tableName, String[] columnNames, String[] data,
            String[] keyNames, String[] keys) {
        Element row = new Element("row");
        xml.addContent(row);
        row.setAttribute("entity", tableName);
        row.setAttribute("dml", dml.getCode());

        String[] colNames = null;

        if (data == null) {
            colNames = keyNames;
            data = keys;
        } else {
            colNames = columnNames;
        }

        for (int i = 0; i < data.length; i++) {
            String col = colNames[i];
            Element dataElement = new Element("data");
            row.addContent(dataElement);
            dataElement.setAttribute("key", col);
            if (data[i] != null) {
                dataElement.setText(data[i]);
            } else {
                dataElement.setAttribute("nil", "true", getXmlNamespace());
            }
        }
    }

    /**
     * Give the opportunity for the user of this publisher to add in additional
     * attributes. The default implementation adds in the nodeId from the
     * {@link ICacheContext}.
     * 
     * @param ctx
     * @param xml
     *            append XML attributes to this buffer
     */
    protected void addFormattedExtraGroupAttributes(ICacheContext ctx, Element xml) {
        xml.setAttribute("nodeid", ctx.getNodeId());
        if (timeStringGenerator != null) {
            xml.setAttribute("time", timeStringGenerator.getTime());
        }
    }

    protected Element getXmlFromCache(ICacheContext ctx, String[] columnNames, String[] data, String[] keyNames,
            String[] keys) {
        Element xml = null;
        Map<String, Element> ctxCache = getXmlCache(ctx);
        String txId = toXmlGroupId(columnNames, data, keyNames, keys);
        if (txId != null) {
            xml = ctxCache.get(txId);
            if (xml == null) {
                xml = new Element(xmlTagNameToUseForGroup);
                xml.addNamespaceDeclaration(getXmlNamespace());
                xml.setAttribute("id", txId);
                addFormattedExtraGroupAttributes(ctx, xml);
                ctxCache.put(txId, xml);
            }
        }
        return xml;
    }

    protected String toXmlGroupId(String[] columnNames, String[] data, String[] keyNames, String[] keys) {
        if (groupByColumnNames != null) {
            StringBuilder id = new StringBuilder();

            if (keys != null) {
                String[] columns = keyNames;
                for (String col : groupByColumnNames) {
                    int index = ArrayUtils.indexOf(columns, col, 0);
                    if (index >= 0) {
                        id.append(data[index]);
                    } else {
                        id = new StringBuilder();
                        break;
                    }
                }
            }

            if (id.length() == 0) {
                String[] columns = columnNames;
                for (String col : groupByColumnNames) {
                    int index = ArrayUtils.indexOf(columns, col, 0);
                    if (index >= 0) {
                        id.append(data[index]);
                    } else {
                        return null;
                    }
                }
            }

            if (id.length() > 0) {
                return id.toString().replaceAll("-", "");
            }
        } else {
            log
                    .warn("You did not specify 'groupByColumnNames'.  We cannot find any matches in the data to publish as XML if you don't.  You might as well turn off this filter!");
        }
        return null;
    }

    public boolean isAutoRegister() {
        return autoRegister;
    }

    public String[] getNodeGroupIdsToApplyTo() {
        return nodeGroups;
    }

    public void setNodeGroups(String[] nodeGroups) {
        this.nodeGroups = nodeGroups;
    }

    public void setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
    }

    public void setPublisher(IPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Used to populate the time attribute of an XML message.
     */
    public void setTimeStringGenerator(ITimeGenerator timeStringGenerator) {
        this.timeStringGenerator = timeStringGenerator;
    }

    public void setXmlFormat(Format xmlFormat) {
        this.xmlFormat = xmlFormat;
    }

    public void setTableNamesToPublishAsGroup(Set<String> tableNamesToPublishAsGroup) {
        this.tableNamesToPublishAsGroup = tableNamesToPublishAsGroup;
    }

    public void setTableNameToPublish(String tableName) {
        this.tableNamesToPublishAsGroup = new HashSet<String>(1);
        this.tableNamesToPublishAsGroup.add(tableName);
    }

    public void setXmlTagNameToUseForGroup(String xmlTagNameToUseForGroup) {
        this.xmlTagNameToUseForGroup = xmlTagNameToUseForGroup;
    }

    /**
     * This attribute is required. It needs to identify the columns that will be
     * used to key on rows in the specified tables that need to be grouped
     * together in an 'XML batch.'
     */
    public void setGroupByColumnNames(List<String> groupByColumnNames) {
        this.groupByColumnNames = groupByColumnNames;
    }

    public interface ITimeGenerator {
        public String getTime();
    }

}