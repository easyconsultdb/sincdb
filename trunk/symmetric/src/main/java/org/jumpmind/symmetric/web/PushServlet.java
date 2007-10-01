package org.jumpmind.symmetric.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.symmetric.common.Constants;
import org.jumpmind.symmetric.service.IDataLoaderService;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * The default download rate is 20k/sec. This change be changed via the servlet
 * param <code>kbs-rate</code>
 * 
 * @author awilcox
 *
 */
public class PushServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Log logger = LogFactory.getLog(PushServlet.class);

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (logger.isDebugEnabled()) {
            logger.debug("Push request received from "
                    + req.getParameter(WebConstants.NODE_ID));
        }

        ApplicationContext ctx = WebApplicationContextUtils
                .getWebApplicationContext(getServletContext());
        IDataLoaderService service = (IDataLoaderService) ctx
                .getBean(Constants.DATALOADER_SERVICE);

        InputStream is = null;

        if (logger.isDebugEnabled()) {
            StringBuilder b = new StringBuilder();
            BufferedReader reader = req.getReader();
            String line = null;
            do {
                line = reader.readLine();
                if (line != null) {
                    b.append(line);
                    b.append("\n");
                }
            } while (line != null);

            logger.debug("Received: " + b);
            is = new ByteArrayInputStream(b.toString().getBytes());
        } else {
            is = req.getInputStream();
        }

        OutputStream out = getOutputstream(resp);
        service.loadData(is, out);
        out.flush();

        if (logger.isDebugEnabled()) {
            logger.debug("Done with push request from "
                    + req.getParameter(WebConstants.NODE_ID));
        }
    }

    private OutputStream getOutputstream(HttpServletResponse resp)
            throws IOException {
        //        // TODO get the pull rate per client
        //        String param = getInitParameter("kbs-rate");
        //        int rate = 20;
        //
        //        if (param != null) {
        //            rate = Integer.parseInt(param);
        //        }
        //        
        //        MeteredOutputStream out = new MeteredOutputStream(resp
        //                .getOutputStream(), MeteredOutputStream.KB * rate);

        return resp.getOutputStream();

    }

}
