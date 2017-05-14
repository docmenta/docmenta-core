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
import org.docma.plugin.LogEntry;
import org.docma.plugin.LogLevel;
import org.docma.util.XMLParser;

/**
 *
 * @author MP
 */
public class DefaultLog implements ExportLog 
{
    private final List<LogMessage> logList = Collections.synchronizedList(new ArrayList<LogMessage>(100));
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

    public int getLogCount()
    {
        return logList.size();
    }
    
    public LogMessage[] getLog()
    {
        return logList.toArray(new LogMessage[logList.size()]);
    }
    
    public LogMessage[] getLog(int fromIndex, int toIndex)
    {
        if (toIndex > logList.size()) {
            toIndex = logList.size();
        }
        if (toIndex <= fromIndex) {
            return new LogMessage[0];
        }
        LogMessage[] res = new LogMessage[toIndex - fromIndex];
        for (int i = 0; i < res.length; i++) {
            res[i] = logList.get(fromIndex + i);
        }
        return res;
    }
    
    @Override
    public LogMessage[] getLog(boolean infos, boolean warnings, boolean errors)
    {
        List resultList;
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
    
    public void addHeader(int level, String msg, Object[] args)
    {
        long timestamp = System.currentTimeMillis();
        String message = formatMsg(null, msg, args);
        DocmaLogMessage log_msg = new DocmaLogMessage(timestamp, LogLevel.INFO, message);
        log_msg.setType("header" + ((level < 0) ? 0 : level));
        addLogMsg(log_msg);
    }
    
    public void addPreformatted(String title, String txt)
    {
        long timestamp = System.currentTimeMillis();
        DocmaLogMessage log_msg = new DocmaLogMessage(timestamp, LogLevel.INFO, txt);
        log_msg.setType("pre");
        if ((title != null) && !title.equals("")) {
            log_msg.setTitle(title);
        }
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
            String msgType = log_msg.getType();
            String headline = log_msg.getTitle();
            StringBuilder line = new StringBuilder(msg.length() + 150);
            line.append("<message timestamp=\"")
                .append(log_msg.getTimestamp())
                .append("\" severity=\"").append(lev.name()).append("\"");
            if ((gen != null) && !gen.equals("")) {
                line.append(" generator=\"").append(gen).append("\"");
            }
            if ((msgType != null) && !msgType.equals("")) {
                line.append(" type=\"").append(msgType).append("\"");
            }
            if ((headline != null) && !headline.equals("")) {
                line.append(" title=\"").append(toXMLAttribute(headline)).append("\"");
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
        final String ATT_TYPE = "type";
        final String ATT_TITLE = "title";
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

            String msgType = null;
            int idxType = attNames.indexOf(ATT_TYPE);
            if (idxType >= 0) {  // type attribute exists
                msgType = attValues.get(idxType);
            }
            
            String msgTitle = null;
            int idxTitle = attNames.indexOf(ATT_TITLE);
            if (idxTitle >= 0) {  // title attribute exists
                msgTitle = attValues.get(idxTitle);
            }

            int msgend = logstr.indexOf("</message>", tagend);
            if (msgend < 0) {
                throw new DocException("Invalid log file format: missing end tag.");
            }
            String msg = logstr.substring(tagend + 1, msgend);

            DocmaLogMessage logmsg = new DocmaLogMessage(timestamp, lev, msg, generator);
            if (msgType != null) {
                logmsg.setType(msgType);
            }
            if (msgTitle != null) {
                logmsg.setTitle(msgTitle);
            }
            export_log.addLogMsg(logmsg);

            searchpos = msgend;
        }
    }

    public String toHTMLString()
    {
        return toHTMLString(logList.toArray(new LogEntry[logList.size()]));
    }
    
    public static String toHTMLString(List<LogEntry> log_entries)
    {
        return toHTMLString(log_entries.toArray(new LogEntry[log_entries.size()]));
    }
    
    public static String toHTMLString(LogEntry[] log_arr)
    {
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        StringWriter writer = new StringWriter();
        for (LogEntry log_msg : log_arr) {
            String msgType = log_msg.getType();
            String msgTitle = log_msg.getTitle();
            String msg = log_msg.getMessage();
            msg = (msg == null) ? "" : msg.replace("<", "&lt;").replace(">", "&gt;");
            if ((msgType != null) && msgType.startsWith("header")) {
                String head = (msgTitle != null) ? msgTitle : msg;
                writeHeader(writer, msgType, head);
                continue;
            }
            writer.write("<div class=\"log_msg\">\n");
            LogLevel level = log_msg.getLevel();
            if (level == null) {
                level = LogLevel.INFO;
            }
            
            boolean isPre = (msgType != null) && msgType.equals("pre");
            boolean hasTitle = (msgTitle != null) && !msgTitle.equals("");
            if (hasTitle || (level != LogLevel.INFO) || !isPre) {
                String sev_str = level.name();
                writer.write("<div class=\"msg_head_");
                writer.write(sev_str.toLowerCase());
                writer.write("\">");
                
                if (hasTitle) {
                    if (level != LogLevel.INFO) {
                        writer.write(sev_str);
                        writer.write(": ");
                    }
                    writer.write(msgTitle);
                } else {
                    Date dt = new Date(log_msg.getTimestamp());
                    String dt_str = dateformat.format(dt);
                    String generator = log_msg.getGenerator();

                    writer.write("[");
                    writer.write(dt_str);
                    writer.write("] ");
                    writer.write(sev_str);
                    if ((generator != null) && !generator.equals("")) {
                        writer.write(" \"");
                        writer.write(generator);
                        writer.write("\"");
                    }
                }
                writer.write("</div>");
            }
            // writer.write("<br>");
            // String msg = log_msg.getMessage().replace('\n', ' ').replace('\r', ' ');
            // writer.write(msg);
            // writer.write("\n</p>\n");
            if (msg.equals("")) {
                writer.write("</div>\n");
            } else {
                writer.write("<pre class=\"");
                if (isPre) {
                    writer.write("msg_pre\">");
                } else {
                    writer.write("msg_content\">");
                }
                writer.write(msg);
                writer.write("</pre></div>\n");
            }
        }
        try {
            writer.close();
        } catch (IOException ex) {}
        return writer.toString();
    }

    /* --------------  package local methods  ---------------------- */

    void addLogMsg(DocmaLogMessage logmsg)
    {
        logList.add(logmsg);
        if (logmsg.isInfo()) cnt_Info++;
        else if (logmsg.isWarning()) cnt_Warning++;
        else if (logmsg.isError()) cnt_Error++;
    }

    /* --------------  private methods  ---------------------- */

    private String toXMLAttribute(String str)
    {
        return str.replace('"', '\'').replace('\n', ' ').replace('\r', ' ').replace('\f', ' ');
    }
    
    private static void writeHeader(StringWriter writer, String msgType, String msg)
    {
        writer.write("<div class=\"log_");
        writer.write(msgType);
        writer.write("\">");
        writer.write(msg);
        writer.write("</div>\n");
    }
    
    private String formatMsg(String location, String msg, Object[] args)
    {
        String message = i18n.getLabel(locale, msg, args);
        if ((message == null) || message.equals("")) {  // msg is no ressource key?
            message = msg;
        }
        if ((location != null) && !location.equals("")) {
            if (message.endsWith(".")) {
                message = message.substring(0, message.length() - 1);
            }
            return message + ": \n" + location;
        } else {
            return message;
        }
    }
}
