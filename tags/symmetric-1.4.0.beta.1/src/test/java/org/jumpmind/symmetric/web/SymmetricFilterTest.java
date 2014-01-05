/*
 * SymmetricDS is an open source database synchronization solution.
 *   
 * Copyright (C) Keith Naas <knaas@users.sourceforge.net>
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

package org.jumpmind.symmetric.web;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.AbstractDatabaseTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * This simply makes sure the SymmetricFilter is setup correctly.
 * 
 */
public class SymmetricFilterTest extends AbstractDatabaseTest {

    private static final String BAD_SECURITY_TOKEN = "2";
    private static final String BAD_NODE_ID = "2";

    protected ConfigurableWebApplicationContext applicationContext;
    protected ServletContext servletContext;

    @BeforeMethod(alwaysRun = true)
    protected void springTestContextBeforeTestMethod(Method method) throws Exception {
        servletContext = new MockServletContext();
        applicationContext = new XmlWebApplicationContext();
        applicationContext.setParent(getSymmetricEngine().getApplicationContext());
        applicationContext.setServletContext(servletContext);
        applicationContext.setConfigLocations(new String[0]);
        applicationContext.refresh();

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
    }

    @DataProvider(name = "authenticationFilterForbiddenParams")
    public Object[][] authenticationFilterForbiddenParams() {
        final Map<String, String> emptyAuthentication = new HashMap<String, String>();
        emptyAuthentication.put(WebConstants.SECURITY_TOKEN, "");
        emptyAuthentication.put(WebConstants.NODE_ID, "");

        return new Object[][] { { "GET", "/ack", null }, { "GET", "/ack/", null }, { "GET", "/ack/more", null },
                { "GET", "/ack?name=value", null }, { "GET", "/ack?name=value&name=value", null },
                { "GET", String.format("/ack?%s=1&%s=2", WebConstants.SECURITY_TOKEN, WebConstants.NODE_ID), null },
                { "GET", "/ack", emptyAuthentication }, { "PUT", "/ack", null }, { "POST", "/ack", null },
                { "DELETE", "/ack", null }, { "TRACE", "/ack", null }, { "OPTIONS", "/ack", null },
                { "HEAD", "/ack", null }, { "GET", "/pull", null }, { "GET", "/pull/", null },
                { "GET", "/pull/more", null }, { "GET", "/pull?name=value", null },
                { "GET", "/pull?name=value&name=value", null },
                { "GET", String.format("/pull?%s=1&%s=2", WebConstants.SECURITY_TOKEN, WebConstants.NODE_ID), null },
                { "GET", "/pull", emptyAuthentication }, { "PUT", "/pull", null }, { "POST", "/pull", null },
                { "DELETE", "/pull", null }, { "TRACE", "/pull", null }, { "OPTIONS", "/pull", null },
                { "HEAD", "/pull", null }, { "GET", "/push", null }, { "GET", "/push/", null },
                { "GET", "/push/more", null }, { "GET", "/push?name=value", null },
                { "GET", "/push?name=value&name=value", null },
                { "GET", String.format("/push?%s=1&%s=2", WebConstants.SECURITY_TOKEN, WebConstants.NODE_ID), null },
                { "GET", "/push", emptyAuthentication }, { "PUT", "/push", null }, { "POST", "/push", null },
                { "DELETE", "/push", null }, { "TRACE", "/push", null }, { "OPTIONS", "/push", null },
                { "HEAD", "/push", null }, };
    }

    @Test(groups = "continuous", dataProvider = "authenticationFilterForbiddenParams")
    public void testAuthenticationFilterForbidden(String method, String uri, Map<String, String> parameters)
            throws Exception {

        final SymmetricFilter filter = new SymmetricFilter();
        filter.init(new MockFilterConfig(servletContext));
        final MockHttpServletRequest request = createMockHttpServletRequest(method, uri, parameters);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_FORBIDDEN);
        filter.destroy();
    }

    @DataProvider(name = "authenticationFilterRegistrationRequiredParams")
    public Object[][] authenticationFilterRegistrationRequiredParams() {
        final Map<String, String> badAuthentication = new HashMap<String, String>();
        badAuthentication.put(WebConstants.SECURITY_TOKEN, BAD_SECURITY_TOKEN);
        badAuthentication.put(WebConstants.NODE_ID, BAD_NODE_ID);
        return new Object[][] { { "GET", "/ack", badAuthentication }, { "GET", "/pull", badAuthentication },
                { "GET", "/push", badAuthentication }, };
    }

    @Test(groups = "continuous", dataProvider = "authenticationFilterRegistrationRequiredParams")
    public void testAuthenticationFilterRegistrationRequired(String method, String uri, Map<String, String> parameters)
            throws Exception {
        final SymmetricFilter filter = new SymmetricFilter();
        filter.init(new MockFilterConfig(servletContext));

        final MockHttpServletRequest request = createMockHttpServletRequest(method, uri, parameters);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        Assert.assertEquals(response.getStatus(), WebConstants.REGISTRATION_REQUIRED);
        filter.destroy();
    }

    @DataProvider(name = "authenticationFilterParams")
    public Object[][] authenticationFilterParams() {
        final Map<String, String> goodAuthentication = new HashMap<String, String>();
        goodAuthentication.put(WebConstants.SECURITY_TOKEN, MockNodeService.GOOD_SECURITY_TOKEN);
        goodAuthentication.put(WebConstants.NODE_ID, MockNodeService.GOOD_NODE_ID);
        return new Object[][] { { "GET", "/ack", goodAuthentication }, { "POST", "/ack", goodAuthentication },
                { "PUT", "/ack", goodAuthentication }, { "DELETE", "/ack", goodAuthentication },
                { "TRACE", "/ack", goodAuthentication }, { "OPTIONS", "/ack", goodAuthentication },
                { "HEAD", "/ack", goodAuthentication }, { "GET", "/pull", goodAuthentication },
                { "POST", "/pull", goodAuthentication }, { "PUT", "/pull", goodAuthentication },
                { "DELETE", "/pull", goodAuthentication }, { "TRACE", "/pull", goodAuthentication },
                { "OPTIONS", "/pull", goodAuthentication }, { "HEAD", "/pull", goodAuthentication },
                { "GET", "/push", goodAuthentication }, { "POST", "/push", goodAuthentication },
                { "PUT", "/push", goodAuthentication }, { "DELETE", "/push", goodAuthentication },
                { "TRACE", "/push", goodAuthentication }, { "OPTIONS", "/push", goodAuthentication },
                { "HEAD", "/push", goodAuthentication } };
    }

    @Test(groups = "continuous", dataProvider = "authenticationFilterParams")
    public void testAuthenticationFilter(String method, String uri, Map<String, String> parameters) throws Exception {

        final SymmetricFilter filter = new SymmetricFilter();
        filter.init(new MockFilterConfig(servletContext));

        final MockHttpServletRequest request = createMockHttpServletRequest(method, uri, parameters);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_FORBIDDEN);
        filter.destroy();
    }

    private MockHttpServletRequest createMockHttpServletRequest(String method, String uri,
            Map<String, String> parameters) {
        final String[] uriParts = StringUtils.split(uri, "?");
        final MockHttpServletRequest request = new MockHttpServletRequest(servletContext, method, uriParts[0]);
        if (uriParts.length > 1) {
            request.setQueryString(uriParts[1]);
        }
        if (parameters != null) {
            request.setParameters(parameters);
        }
        return request;
    }
}