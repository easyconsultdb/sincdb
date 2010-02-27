package org.jumpmind.symmetric.route;

import java.util.Map;

import javax.annotation.Resource;

import org.jumpmind.symmetric.model.Channel;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/org/jumpmind/symmetric/service/impl/router-service-sql.xml" })
public class RouterDataReaderUnitTest {
    
    @Resource
    Map<String,String> routerServiceSql;
    
    @Test
    public void testOldDataReplacement() {
        RouterDataReader reader = new RouterDataReader(null, 1, routerServiceSql, 1000, null, null, null);
        Channel channel = new Channel();
        Assert.assertTrue(reader.getSql(channel).contains("old_data"));
        Assert.assertFalse(reader.getSql(channel).contains("null"));
        channel.setUseOldDataToRoute(false);
        Assert.assertFalse(reader.getSql(channel).contains("old_data"));
        Assert.assertTrue(reader.getSql(channel).contains("null"));
    }
    
    @Test
    public void testRowDataReplacement() {
        RouterDataReader reader = new RouterDataReader(null, 1, routerServiceSql, 1000, null, null, null);
        Channel channel = new Channel();
        Assert.assertTrue(reader.getSql(channel).contains("row_data"));
        Assert.assertFalse(reader.getSql(channel).contains("null"));
        channel.setUseRowDataToRoute(false);
        Assert.assertFalse(reader.getSql(channel).contains("row_data"));
        Assert.assertTrue(reader.getSql(channel).contains("null"));
    }   
    
    @Test
    public void testOldAndRowDataReplacement() {
        RouterDataReader reader = new RouterDataReader(null, 1, routerServiceSql, 1000, null, null, null);
        Channel channel = new Channel();
        Assert.assertTrue(reader.getSql(channel).contains("row_data"));
        Assert.assertTrue(reader.getSql(channel).contains("old_data"));
        Assert.assertFalse(reader.getSql(channel).contains("null"));
        channel.setUseRowDataToRoute(false);
        channel.setUseOldDataToRoute(false);
        Assert.assertFalse(reader.getSql(channel).contains("row_data"));
        Assert.assertFalse(reader.getSql(channel).contains("old_data"));
        Assert.assertTrue(reader.getSql(channel).contains("null"));
    }  

}
