package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.util.PathFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.thymeleaf.util.ArrayUtils;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

@Controller
public class StaticFileController
{
    private static Logger logger = LoggerFactory.getLogger(StaticFileController.class);

    private static final int CHUNKSIZE = 8192;

    private static final String[] INLINE_MIMES =
    {
        "text/plain",
        "text/html",
        "text/javascript",
        "image/png",
        "image/jpeg",
        "image/svg+xml",
    };


    @RequestMapping(value = "/static/**", method = RequestMethod.GET)
    public void getStaticFile(HttpServletRequest request,
                              HttpServletResponse response) throws IOException
    {
        String requestUrl = request.getServletPath().substring("/static/".length());
        respondWithFile("/static/" + requestUrl, response, null, false);
    }


    @RequestMapping(value = "/static/**", method = RequestMethod.HEAD)
    public void getStaticFileHead(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException
    {
        String requestUrl = request.getServletPath().substring("/static/".length());
        respondWithFile("/static/" + requestUrl, response, null, true);
    }


    @RequestMapping(value = "/favicon.*",
                    method = {RequestMethod.GET, RequestMethod.HEAD})
    public void getFavicon(HttpServletRequest request,
                              HttpServletResponse response) throws IOException
    {
        respondWithFile("/static/img/favicon.png", response, "image/png", false);
    }


    private void respondWithFile(String requestUrl, HttpServletResponse response, String forceMime, boolean headersOnly) throws IOException
    {
        requestUrl = requestUrl.replaceAll("//+", "/");
        if (requestUrl.startsWith("/")) { requestUrl = requestUrl.substring(1); }

        File inFile = new File("./external/" + requestUrl);

        if (!inFile.isFile())
        {
            inFile = new File(PathFinder.getExecutablePath() + "/external/" + requestUrl);
        }

        URL inUrl;

        if (inFile.isFile())
        {
            inUrl = inFile.toURI().toURL();
        }
        else
        {
            inUrl = getClass().getResource("/WEB-INF/" + requestUrl);
        }

        if (inUrl == null)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);

            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "inline");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.getOutputStream().write(String.format("404 - resource \"%s\" not found", requestUrl).getBytes());
        }
        else
        {
            URLConnection inConnection = inUrl.openConnection();
            long mtime = inConnection.getLastModified();

            String  mime   = forceMime == null ? guessMimeType(requestUrl) : forceMime;
            Boolean inline = ArrayUtils.contains(INLINE_MIMES, mime);

            if (mime.startsWith("text/"))
            {
                mime += "; charset=UTF-8";
            }

            Date date = new Date(mtime);
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            response.setContentType(mime);
            response.setHeader("Content-Disposition", inline ?  "inline" : "attachment");
            response.setHeader("Cache-Control", "public, max-age=3600");
            response.setHeader("Last-Modified", dateFormat.format(date));
            response.setHeader("Content-length", String.valueOf(inConnection.getContentLengthLong()));

            if (!headersOnly)
            {
                InputStream inStream = inConnection.getInputStream();
                byte[] b = new byte[CHUNKSIZE];

                OutputStream out = response.getOutputStream();

                while (true)
                {
                    int readBytes = inStream.read(b);
                    if (readBytes <= 0) { break; }

                    if (readBytes == CHUNKSIZE) { out.write(b); }
                    else { out.write(Arrays.copyOfRange(b, 0, readBytes)); }
                }

                inStream.close();
            }
        }
    }


    private String guessMimeType(String name)
    {
        MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();
        return typeMap.getContentType(name);
    }
}