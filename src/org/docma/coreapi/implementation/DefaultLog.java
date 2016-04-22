/*
 * DefaultLog.java
 * 
 *  Copyright (C) 2014  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.coreapi.implementation;

import org.docma.coreapi.ExportLog;
import org.docma.coreapi.*;

import java.util.*;
import java.text.SimpleDateFormat;
import java.io.*;

/**
 *
 * @author MP
 */
public class DefaultLog implements ExportLog 
{
    private final ArrayList<LogMessage> logList = new ArrayList<LogMessage>(100);
    private DocI18n i18n = null;
    private Locale locale = null;
    private int cnt_Info = 0;
    private int cnt_Warning = 0;
    private int cnt_Error = 0;

    protected DefaultLog()
    {
    }

    public DefaultLog(DocI18n i18n)
    {
        this.i18n = i18n;
    }
    
    public void setLocale(Locale loc)
    {
        this.locale = loc;
    }

    @Override
    public void infoMsg(String message)
    {
        long timestamp = System.currentTimeMillis();
        DocmaLogMessage log_msg = new DocmaLogMessage(timestamp, DocmaLogMessage.SEVERITY_INFO, message);
        logList.add(log_msg);
        cnt_Info++;
    }

    @Override
    public void info(String location, String message_key, Object[] args)
    {
        String message = i18n.getLabel(locale, message_key, args);
        // if (args != null) message = MessageFormat.format(message, args);
        if ((location != null) && !location.equals("")) {
            message = location + ": \n" + message;
        }
        infoMsg(message);
    }

    @Override
    public void info(String message_key, Object[] args)
    {
        info(null, message_key, args);
    }

    @Override
    public void info(String message_key)
    {
        info(message_key, null);
    }

    @Override
    public void warningMsg(String message)
    {
        long timestamp = System.currentTimeMillis();
        DocmaLogMessage log_msg = new DocmaLogMessage(timestamp, DocmaLogMessage.SEVERITY_WARNING, message);
        logList.add(log_msg);
        cnt_Warning++;
    }

    @Override
    public void warning(String location, String message_key, Object[] args)
    {
        String message = i18n.getLabel(locale, message_key, args);
        // if (args != null) message = MessageFormat.format(message, args);
        if ((location != null) && !location.equals("")) {
            message = location + ": \n" + message;
        }
        warningMsg(message);
    }

    @Override
    public void warning(String message_key, Object[] args)
    {
        warning(null, message_key, args);
    }
    
    @Override
    public void warning(String message_key)
    {
        warning(message_key, null);
    }

    @Override
    public void errorMsg(String message)
    {
        long timestamp = System.currentTimeMillis();
        DocmaLogMessage log_msg = new DocmaLogMessage(timestamp, DocmaLogMessage.SEVERITY_ERROR, message);
        logList.add(log_msg);
        cnt_Error++;
    }

    @Override
    public void error(String location, String message_key, Object[] args)
    {
        String message = i18n.getLabel(locale, message_key, args);
        // if (args != null) message = MessageFormat.format(message, args);
        if ((location != null) && !location.equals("")) {
            message = location + ": \n" + message;
        }
        errorMsg(message);
    }

    @Override
    public void error(String message_key, Object[] args)
    {
        error(null, message_key, args);
    }
    
    @Override
    public void error(String message_key)
    {
        error(message_key, null);
    }

    @Override
    public boolean hasError()
    {
        return (cnt_Error > 0);
    }

    @Override
    public boolean hasInfo()
    {
        return (cnt_Info > 0);
    }

    @Override
    public boolean hasWarning()
    {
        return (cnt_Warning > 0);
    }

    @Override
    public int getErrorCount()
    {
        return cnt_Error;
    }

    @Override
    public int getInfoCount()
    {
        return cnt_Info;
    }

    @Override
    public int getWarningCount()
    {
        return cnt_Warning;
    }

    public void clear()
    {
        logList.clear();
        cnt_Info = 0;
        cnt_Warning = 0;
        cnt_Error = 0;
    }

    @Override
    public LogMessage[] getLog(boolean infos, boolean warnings, boolean errors)
    {
        ArrayList resultList;
        if (infos && warnings && errors) {
            resultList = logList;
        } else {
            resultList = new ArrayList(logList.size());
            for (int i=0; i < logList.size(); i++) {
                DocmaLogMessage log_msg = (DocmaLogMessage) logList.get(i);
                if ((log_msg.isInfo() && infos) ||
                    (log_msg.isWarning() && warnings) ||
                    (log_msg.isError() && errors)) {
                    resultList.add(log_msg);
                }
            }
        }
        LogMessage[] arr = new DocmaLogMessage[resultList.size()];
        return (LogMessage[]) resultList.toArray(arr);
    }

    public void storeToXML(OutputStream out) throws IOException
    {
        OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
        // BufferedWriter writer = new BufferedWriter(outwriter);
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<log>\n");
        for (int i=0; i < logList.size(); i++) {
            DocmaLogMessage log_msg = (DocmaLogMessage) logList.get(i);
            // String msg = log_msg.getMessage().replace('\n', ' ').replace('\r', ' ');
            String msg = log_msg.getMessage();
            msg = (msg == null) ? "" : msg.replace("<", "&lt;").replace(">", "&gt;");
            int sev = log_msg.getSeverity();
            String sev_str;
            switch (sev) {
                case DocmaLogMessage.SEVERITY_ERROR:
                    sev_str = "error";
                    break;
                case DocmaLogMessage.SEVERITY_WARNING:
                    sev_str = "warning";
                    break;
                case DocmaLogMessage.SEVERITY_INFO:
                    sev_str = "info";
                    break;
                default:
                    sev_str = "" + sev;
                    break;
            }
            String line = "<message timestamp=\"" + log_msg.getTimestamp() +
                          "\" severity=\"" + sev_str + "\">" + msg + "</message>\n";
            writer.write(line);
        }
        writer.write("</log>\n");
        writer.close();
    }

    public static void loadFromXML(InputStream in, DefaultLog export_log) 
    throws IOException, DocException
    {
        // ArrayList list = new ArrayList(100);
        InputStreamReader inreader = new InputStreamReader(in, "UTF-8");
        BufferedReader reader = new BufferedReader(inreader);

        // Read log file into string
        StringBuilder logstr = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            logstr.append(line).append("\n");
        }
        reader.close();

        // Parse log string and create DocmaLogMessage objects
        final String MESSAGE_PATTERN = "<message ";
        final String TIMESTAMP_PATTERN = " timestamp=\"";
        final String SEVERITY_PATTERN = " severity=\"";
        int searchpos = 0;
        while (true) {
            searchpos = logstr.indexOf(MESSAGE_PATTERN, searchpos);
            if (searchpos < 0) break;

            int tagstart = searchpos;
            int tagend = logstr.indexOf(">", searchpos);
            if (tagend < 0) {
                throw new DocException("Invalid log file format!");
            }
            searchpos = tagend;
            String attribs = logstr.substring(tagstart, tagend);

            long timestamp = 0;
            int p1 = attribs.indexOf(TIMESTAMP_PATTERN);
            if (p1 >= 0) {
                p1 = p1 + TIMESTAMP_PATTERN.length();
                int p2 = attribs.indexOf('"', p1);
                if (p2 < 0) p2 = attribs.length();
                String time_str = attribs.substring(p1, p2);
                try {
                    timestamp = Long.parseLong(time_str);
                } catch (Exception ex) {
                    System.out.println("Warning: Log message has invalid timestamp:" + time_str);
                }
            } else {
                System.out.println("Warning: Log message has no timestamp.");
            }

            int sev = DocmaLogMessage.SEVERITY_INFO;
            p1 = attribs.indexOf(SEVERITY_PATTERN);
            if (p1 >= 0) {
                p1 = p1 + SEVERITY_PATTERN.length();
                int p2 = attribs.indexOf('"', p1);
                if (p2 < 0) p2 = attribs.length();
                String sev_str = attribs.substring(p1, p2);
                if (sev_str.equals("error")) sev = DocmaLogMessage.SEVERITY_ERROR;
                else if (sev_str.equals("warning")) sev = DocmaLogMessage.SEVERITY_WARNING;
                else if (sev_str.equals("info")) sev = DocmaLogMessage.SEVERITY_INFO;
                else {
                    try {
                        sev = Integer.parseInt(sev_str);
                    } catch (Exception ex) {}
                }
            } else {
                System.out.println("Warning: Log message has no severity.");
            }

            int msgend = logstr.indexOf("</message>", tagend);
            if (msgend < 0) {
                throw new DocException("Invalid log file format: missing end tag.");
            }
            String msg = logstr.substring(tagend + 1, msgend);

            DocmaLogMessage logmsg = new DocmaLogMessage(timestamp, sev, msg);
            export_log.addLogMsg(logmsg);

            searchpos = msgend;
        }
    }

    public String toHTMLString()
    {
        return toHTMLString(this.logList);
    }
    
    public static String toHTMLString(List<LogMessage> log_list)
    {
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        StringWriter writer = new StringWriter();
        // writer.write("<html>\n");
        // writer.write("<body>\n");
        for (int i=0; i < log_list.size(); i++) {
            writer.write("<div class=\"log_msg\">\n");
            LogMessage log_msg = log_list.get(i);
            String sev_str;
            writer.write("<div class=\"");
            if (log_msg.isError()) {
                sev_str = "ERROR";
                writer.write("msg_head_error\">");
            }
            else if (log_msg.isWarning()) {
                sev_str = "WARNING";
                writer.write("msg_head_warning\">");
            }
            else if (log_msg.isInfo()) {
                sev_str = "INFO";
                writer.write("msg_head_info\">");
            }
            else {
                if (log_msg instanceof DocmaLogMessage) {
                    int sev = ((DocmaLogMessage) log_msg).getSeverity();
                    sev_str = "SEVERITY: " + sev;
                } else {
                    sev_str = "";
                }
                writer.write("msg_head_info\">");
            }
            Date dt = new Date(log_msg.getTimestamp());
            String dt_str = dateformat.format(dt);
            writer.write(dt_str);
            writer.write(": ");
            writer.write(sev_str);
            writer.write("</div>");
            // writer.write("<br>");
            // String msg = log_msg.getMessage().replace('\n', ' ').replace('\r', ' ');
            // writer.write(msg);
            // writer.write("\n</p>\n");
            writer.write("<pre class=\"msg_content\">");
            String msg = log_msg.getMessage();
            msg = (msg == null) ? "" : msg.replace("<", "&lt;").replace(">", "&gt;");
            writer.write(msg);
            writer.write("</pre></div>\n");
        }
        // writer.write("</body></html>\n");
        try {
            writer.close();
        } catch (IOException ex) {}
        return writer.toString();
    }

    /* --------------  private methods  ---------------------- */

    private void addLogMsg(DocmaLogMessage logmsg)
    {
        logList.add(logmsg);
        if (logmsg.isInfo()) cnt_Info++;
        else if (logmsg.isWarning()) cnt_Warning++;
        else if (logmsg.isError()) cnt_Error++;
    }

}
