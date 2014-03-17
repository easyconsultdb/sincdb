package org.jumpmind.symmetric.io.data.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.jumpmind.db.util.BinaryEncoding;
import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.io.data.Batch;
import org.jumpmind.symmetric.io.data.Batch.BatchType;
import org.jumpmind.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDataReader {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected long logDebugAndCountBytes(String[] tokens) {
        long bytesRead = 0;
        if (tokens != null) {
            StringBuilder debugBuffer = log.isDebugEnabled() ? new StringBuilder() : null;
            for (String token : tokens) {
                bytesRead += token != null ? token.length() : 0;
                if (debugBuffer != null) {
                    if (token != null) {
                        String tokenTrimmed = FormatUtils.abbreviateForLogging(token);
                        debugBuffer.append(tokenTrimmed);
                    } else {
                        debugBuffer.append("<null>");
                    }
                    debugBuffer.append(",");
                }
            }
            if (debugBuffer != null && debugBuffer.length() > 1) {
                log.debug("CSV parsed: {}", debugBuffer.substring(0, debugBuffer.length() - 1));
            }
        }
        return bytesRead;
    }
    

    protected static Batch toBatch(BinaryEncoding binaryEncoding) {
        return new Batch(BatchType.LOAD, Batch.UNKNOWN_BATCH_ID, "default", binaryEncoding, null,
                null, true);
    }

    protected static Reader toReader(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader in = new InputStreamReader(fis, "UTF-8");
            return new BufferedReader(in);
        } catch (IOException ex) {
            throw new IoException(ex);
        }
    }

    protected static Reader toReader(InputStream is) {
        try {
            return new BufferedReader(new InputStreamReader(is, "UTF-8"));
        } catch (IOException ex) {
            throw new IoException(ex);
        }
    }
}