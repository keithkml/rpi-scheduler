/*
 *  Copyright (c) 2004, The University Scheduler Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *  - Neither the name of the University Scheduler Project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 */

package edu.rpi.scheduler.ui;

import sun.io.Converters;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * Format a LogRecord into a standard XML format.
 * <p>
 * The DTD specification is provided as Appendix A to the
 * Java Logging APIs specification.
 * <p>
 * The XMLFormatter can be used with arbitrary character encodings,
 * but it is recommended that it normally be used with UTF-8.  The
 * character encoding can be set on the output Handler.
 *
 * @version 1.24, 01/12/04
 * @since 1.4
 */

public class MyXMLFormatter extends Formatter {
    private LogManager manager = LogManager.getLogManager();
    private final String lineSeparator = System.getProperty("line.separator");

    // Append a two digit number.
    private void a2(StringBuffer sb, int x) {
        if (x < 10) {
            sb.append('0');
        }
        sb.append(x);
    }

    // Append the time and date in ISO 8601 format
    private void appendISO8601(StringBuffer sb, long millis) {
        Date date = new Date(millis);
        sb.append(date.getYear() + 1900);
        sb.append('-');
        a2(sb, date.getMonth() + 1);
        sb.append('-');
        a2(sb, date.getDate());
        sb.append('T');
        a2(sb, date.getHours());
        sb.append(':');
        a2(sb, date.getMinutes());
        sb.append(':');
        a2(sb, date.getSeconds());
    }

    // Append to the given StringBuffer an escaped version of the
    // given text string where XML special characters have been escaped.
    // For a null string we append "<null>"
    private void escape(StringBuffer sb, String text) {
        if (text == null) {
            text = "<null>";
        }
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '<') {
                sb.append("&lt;");
            } else if (ch == '>') {
                sb.append("&gt;");
            } else if (ch == '&') {
                sb.append("&amp;");
            } else {
                sb.append(ch);
            }
        }
    }

    /**
     * Format the given message to XML.
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public String format(LogRecord record) {
        StringBuffer sb = new StringBuffer(500);
        sb.append("<record>");
        sb.append(lineSeparator);

        sb.append("  <date>");
        appendISO8601(sb, record.getMillis());
        sb.append("</date>");
        sb.append(lineSeparator);

        sb.append("  <millis>");
        sb.append(record.getMillis());
        sb.append("</millis>");
        sb.append(lineSeparator);

        sb.append("  <sequence>");
        sb.append(record.getSequenceNumber());
        sb.append("</sequence>");
        sb.append(lineSeparator);

        String name = record.getLoggerName();
        if (name != null) {
            sb.append("  <logger>");
            escape(sb, name);
            sb.append("</logger>");
            sb.append(lineSeparator);
        }

        sb.append("  <level>");
        escape(sb, record.getLevel().toString());
        sb.append("</level>");
        sb.append(lineSeparator);

        if (record.getSourceClassName() != null) {
            sb.append("  <class>");
            escape(sb, record.getSourceClassName());
            sb.append("</class>");
            sb.append(lineSeparator);
        }

        if (record.getSourceMethodName() != null) {
            sb.append("  <method>");
            escape(sb, record.getSourceMethodName());
            sb.append("</method>");
            sb.append(lineSeparator);
        }

        sb.append("  <thread>");
        sb.append(record.getThreadID());
        sb.append("</thread>");
        sb.append(lineSeparator);

        if (record.getMessage() != null) {
            // Format the message string and its accompanying parameters.
            String message = formatMessage(record);
            sb.append("  <message>");
            escape(sb, message);
            sb.append("</message>");
            sb.append(lineSeparator);
        }

        // If the message is being localized, output the key, resource
        // bundle name, and params.
        ResourceBundle bundle = record.getResourceBundle();
        try {
            if (bundle != null
                    && bundle.getString(record.getMessage()) != null) {
                sb.append("  <key>");
                escape(sb, record.getMessage());
                sb.append("</key>");
                sb.append(lineSeparator);
                sb.append("  <catalog>");
                escape(sb, record.getResourceBundleName());
                sb.append("</catalog>");
                sb.append(lineSeparator);
            }
        } catch (Exception ex) {
            // The message is not in the catalog.  Drop through.
        }

        Object parameters[] = record.getParameters();
        //  Check to see if the parameter was not a messagetext format
        //  or was not null or empty
        if (parameters != null && parameters.length != 0
                && record.getMessage().indexOf("{") == -1) {
            for (int i = 0; i < parameters.length; i++) {
                sb.append("  <param>");
                try {
                    escape(sb, parameters[i].toString());
                } catch (Exception ex) {
                    sb.append("???");
                }
                sb.append("</param>");
                sb.append(lineSeparator);
            }
        }

        Throwable th = record.getThrown();
        while (th != null) {
            // Report on the state of the throwable.
            writeExceptionInfo(sb, th);
            th = th.getCause();
        }

        sb.append("</record>");
        sb.append(lineSeparator);
        return sb.toString();
    }

    private void writeExceptionInfo(StringBuffer sb, Throwable th) {
        sb.append("  <exception>");
        sb.append(lineSeparator);
        sb.append("    <message>");
        escape(sb, th.toString());
        sb.append("</message>");
        sb.append(lineSeparator);
        StackTraceElement trace[] = th.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement frame = trace[i];
            sb.append("    <frame>");
            sb.append(lineSeparator);
            sb.append("      <class>");
            escape(sb, frame.getClassName());
            sb.append("</class>");
            sb.append(lineSeparator);
            sb.append("      <method>");
            escape(sb, frame.getMethodName());
            sb.append("</method>");
            sb.append(lineSeparator);
            // Check for a line number.
            if (frame.getLineNumber() >= 0) {
                sb.append("      <line>");
                sb.append(frame.getLineNumber());
                sb.append("</line>");
                sb.append(lineSeparator);
            }
            sb.append("    </frame>");
            sb.append(lineSeparator);
        }
        sb.append("  </exception>");
        sb.append(lineSeparator);
    }

    /**
     * Return the header string for a set of XML formatted records.
     *
     * @param   h  The target handler (can be null)
     * @return  a valid XML string
     */
    public String getHead(Handler h) {
        StringBuffer sb = new StringBuffer();
        String encoding;
        sb.append("<?xml version=\"1.0\"");

        if (h != null) {
            encoding = h.getEncoding();
        } else {
            encoding = null;
        }

        if (encoding == null) {
            // Figure out the default encoding.
            encoding = Converters.getDefaultEncodingName();
        }
        // Try to map the encoding name to a canonical name.
        try {
            Charset cs = Charset.forName(encoding);
            encoding = cs.name();
        } catch (Exception ex) {
            // We hit problems finding a canonical name.
            // Just use the raw encoding name.
        }

        sb.append(" encoding=\"");
        sb.append(encoding);
        sb.append("\"");
        sb.append(" standalone=\"no\"?>");
        sb.append(lineSeparator);
//        sb.append("<!DOCTYPE log SYSTEM \"logger.dtd\">" + lineSeparator);
        sb.append("<log>");
        sb.append(lineSeparator);
        return sb.toString();
    }

    /**
     * Return the tail string for a set of XML formatted records.
     *
     * @param   h  The target handler (can be null)
     * @return  a valid XML string
     */
    public String getTail(Handler h) {
        return "</log>" + lineSeparator;
    }
}
