package org.jumpmind.db.sql;

import java.io.InputStreamReader;

import junit.framework.Assert;

import org.junit.Test;

public class SqlScriptReaderTest {

    @Test
    public void testReadScript() throws Exception {
        SqlScriptReader reader = new SqlScriptReader(new InputStreamReader(getClass().getResourceAsStream("/test-script-1.sql")));
        Assert.assertEquals("select * from test", reader.readSqlStatement());
        Assert.assertEquals("insert into test (one, two, three) values('1','1','2')", reader.readSqlStatement());
        for (int i = 0; i < 4; i++) {            
           Assert.assertEquals("delete from test where one='1'", reader.readSqlStatement());
        }
        Assert.assertEquals("update test\n  set one = '1', two = '2'\nwhere one = 'one'", reader.readSqlStatement());
        Assert.assertNull(reader.readSqlStatement());
        reader.close();
    }
}