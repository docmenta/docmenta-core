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

import java.util.*;
import java.text.SimpleDateFormat;
import java.io.*;

import org.docma.coreapi.ExportLog;
import org.docma.coreapi.*;
import org.docma.plugin.LogLevel;
import org.docma.util.XMLParser;

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

    
    /* ----------- Interface ExportLog ---------- */

    @Override
    public void infoMsg(String message)
    {
        long timestamp = System.currentTimeMillis();
        DocmaLogMessage log_msg = new DocmaLogMessage(timestamp, LogLevel.INFO, message);
        logList.add(log_msg);
        cnt_Info++;
    }

    @Override
    public void info(String location, String message_key, Object[] args)
    {
        String message = formatMsg(location, message_key, args);
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
        DocmaLogMessage log_msg = new DocmaLogMessage(timestamp, LogLevel.WARNING, message);
        logList.add(log_msg);
        cnt_Warning++;
    }

    @Override
    public void warning(String location, String message_key, Object[] args)
    {
        String message = formatMsg(location, message_key, args);
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
        DocmaLogMessage log_msg = new DocmaLogMessage(timestamp, LogLevel.ERROR, message);
        logList.add(log_msg);
        cnt_Error++;
    }

    @Override
    public void error(String location, String message_key, Object[] args)
    {
        String message = formatMsg(location, message_key, args);
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


    /* ----------- Other public methods ---------- */

    public void add(LogLevel level, String generator, String msg, Object[] args)
    {
        add(level, generator, null, msg, args);
    }
    
    public void add(LogLevel level, String generator, String location, String msg, Object[] args)
    {
        long timestamp = System.currentTimeMillis();
        String message = formatMsg(location, msg, args);
        DocmaLogMessage log_msg = new DocmaLogMessage(timestamp, level, message, generator);
        addLogMsg(log_msg);
    }
    
    public void clear()
    {
        logList.clear();
        cnt_Info = 0;
        cnt_Warning = 0;
        cnt_Error = 0;
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
            LogLevel lev = log_msg.getLevel();
            if (lev == null) {
                lev = LogLevel.INFO;
            }
            String gen = log_msg.getGenerator();
            StringBuilder line = new StringBuilder(msg.length() + 150);
            line.append("<message timestamp=\"")
                .append(log_msg.getTimestamp())
                .append("\" severity=\"").append(lev.name()).append("\"");
            if ((gen != null) && !gen.equals("")) {
                line.append(" generator=\"").append(gen).append("\"");
            }
            line.append(">").append(msg) .append("</message>\n");
            writer.write(line.toString());
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
        final String MESSAGE_PATTERN = "<message";
        final String ATT_TIMESTAMP = "timestamp";
        final String ATT_SEVERITY = "severity";
        final String ATT_GENERATOR = "generator";
        int searchpos = 0;
        List<String> attNames = new ArrayList();
        List<String> attValues = new ArrayList();
        while (true) {
            searchpos = logstr.indexOf(MESSAGE_PATTERN, searchpos);
            if (searchpos < 0) break;

            int tagstart = searchpos;
            int attstart = tagstart + MESSAGE_PATTERN.length();
            
            if (attstart >= logstr.length()) {
                throw new DocException("Invalid log file format: unexpected end of log.");
            }
            char ch = logstr.charAt(attstart);
            if (! Character.isWhitespace(ch)) {  // <message has to be followed by whitespace
                searchpos = attstart;
                continue;
            }
            
            int tagend = XMLParser.parseTagAttributes(logstr, attstart, attNames, attValues); // logstr.indexOf(">", searchpos);
            if (tagend < 0) {
                throw new DocException("Invalid log file format!");
            }
            searchpos = tagend;  // in next loop continue with next message

            long timestamp = 0;
            int idxTime = attNames.indexOf(ATT_TIMESTAMP);
            if (idxTime >= 0) {   // timestamp attribute exists
                String timeStr = attValues.get(idxTime);
                try {
                    timestamp = Long.parseLong(timeStr);
                } catch (Exception ex) {
                    System.out.println("Warning: Log message has invalid timestamp:" + timeStr);
                }
            } else {
                System.out.println("Warning: Log message has no timestamp.");
            }

            LogLevel lev = LogLevel.INFO;
            int idxSeverity = attNames.indexOf(ATT_SEVERITY);
            if (idxSeverity >= 0) {  // severity attribute exists
                String sev_str = attValues.get(idxSeverity).toUpperCase();
                if (sev_str.equals("INFO")) lev = LogLevel.INFO;
                else if (sev_str.equals("WARNING")) lev = LogLevel.WARNING;
                else if (sev_str.equals("ERROR")) lev = LogLevel.ERROR;
            } else {
                System.out.println("Warning: Log message has no severity.");
            }
            
            String generator = null;
            int idxGenerator = attNames.indexOf(ATT_GENERATOR);
            if (idxGenerator >= 0) {  // generator attribute exists
                generator = attValues.get(idxGenerator);
            }

            int msgend = logstr.indexOf("</message>", tagend);
            if (msgend < 0) {
                throw new DocException("Invalid log file format: missing end tag.");
            }
            String msg = logstr.substring(tagend + 1, msgend);

            DocmaLogMessage logmsg = new DocmaLogMessage(timestamp, lev, msg, generator);
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
                    LogLevel lev = ((DocmaLogMessage) log_msg).getLevel();
                    sev_str = lev.name();
                } else {
                    sev_str = "";
                }
                writer.write("msg_head_info\">");
            }
            Date dt = new Date(log_msg.getTimestamp());
            String dt_str = dateformat.format(dt);
            
            String generator = null;
            if (log_msg instanceof DocmaLogMessage) {
                generator = ((DocmaLogMessage) log_msg).getGenerator();
            }
            
            writer.write(dt_str);
            writer.write(": ");
            writer.write(sev_str);
            if ((generator != null) && !generator.equals("")) {
                writer.write(" [");
                writer.write(generator);
                writer.write("]");
            }
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

    private String formatMsg(String location, String msg, Object[] args)
    {
        String message = i18n.getLabel(locale, msg, args);
        if ((message == null) || message.equals("")) {  // msg is no ressource key?
            message = msg;
        }
        if ((location != null) && !location.equals("")) {
            return location + ": \n" + message;
        } else {
            return message;
        }
    }
}
