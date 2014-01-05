/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class FormatUtilsTest {

    @Test
    public void testReplaceTokens() {
        Assert.assertEquals("test", FormatUtils.replaceTokens("test", null, true));
        Assert.assertEquals("test", FormatUtils.replaceTokens("test", new HashMap<String, String>(), true));
        Map<String, String> params = new HashMap<String, String>();
        params.put("test", "1");
        Assert.assertEquals("test1", FormatUtils.replaceTokens("test$(test)", params, true));
        Assert.assertEquals("test0001", FormatUtils.replaceTokens("test$(test|%04d)", params, true));
    }
    
    @Test
    public void testReplaceCurrentTimestamp() {
        String beforeSql = "insert into sym_node values ('00000', 'test-root-group', '00000', 1, null, null, '2.0', null, null, current_timestamp, null, 0, 0, '00000', 'engine')";
        String afterSql = "insert into sym_node values ('00000', 'test-root-group', '00000', 1, null, null, '2.0', null, null, XXXX, null, 0, 0, '00000', 'engine')";
        Map<String,String> replacementTokens = new HashMap<String, String>();
        replacementTokens.put("current_timestamp", "XXXX");
        Assert.assertEquals(afterSql, FormatUtils.replaceTokens(beforeSql, replacementTokens, false));
        
    }
    
    @Test
    public void testIsWildcardMatch() {
        Assert.assertTrue(FormatUtils.isWildCardMatch("TEST_1", "TEST_*"));
        Assert.assertTrue(FormatUtils.isWildCardMatch("TEST_2", "TEST_*"));
        Assert.assertTrue(FormatUtils.isWildCardMatch("TEST_TEST_TEST", "TEST_*"));
        Assert.assertFalse(FormatUtils.isWildCardMatch("NOT_A_MATCH", "TEST_*"));
        Assert.assertFalse(FormatUtils.isWildCardMatch("NOT_A_MATCH_TEST_1", "TEST_*"));
        Assert.assertTrue(FormatUtils.isWildCardMatch("NOT_A_MATCH_TEST_1", "*TEST*"));
        Assert.assertFalse(FormatUtils.isWildCardMatch("B_A", "*A*B"));
        Assert.assertTrue(FormatUtils.isWildCardMatch("A_B", "*A*B"));        
        Assert.assertFalse(FormatUtils.isWildCardMatch("TEST_NO_MATCH", "TEST_*,!TEST_NO_MATCH"));
    }
}