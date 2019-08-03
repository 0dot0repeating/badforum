package com.jinotrain.badforum.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.thymeleaf.util.ArrayUtils;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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


    @RequestMapping(value = "/static/**",
                    method = {RequestMethod.GET, RequestMethod.HEAD})
    public void getStaticFile(HttpServletRequest request,
                              HttpServletResponse response) throws IOException
    {
        String requestUrl = request.getServletPath().substring("/static/".length());
        respondWithFile("/static/" + requestUrl, response, null);
    }


    @RequestMapping(value = "/favicon.*",
                    method = {RequestMethod.GET, RequestMethod.HEAD})
    public void getFavicon(HttpServletRequest request,
                              HttpServletResponse response) throws IOException
    {
        respondWithFile("/static/favicon.png", response, "image/png");
    }


    private void respondWithFile(String requestUrl, HttpServletResponse response, String forceMime) throws IOException
    {
        requestUrl = requestUrl.replaceAll("//+", "/");
        if (requestUrl.startsWith("/")) { requestUrl = requestUrl.substring(1); }

        URL inUrl = getClass().getResource("/WEB-INF/" + requestUrl);

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
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "public, max-age=360");
            response.setHeader("Last-Modified", dateFormat.format(date));

            InputStream inFile = inConnection.getInputStream();
            byte[] b = new byte[CHUNKSIZE];

            OutputStream out = response.getOutputStream();

            while (true)
            {
                int readBytes = inFile.read(b);
                if (readBytes <= 0) { break; }

                if (readBytes == CHUNKSIZE) { out.write(b); }
                else { out.write(Arrays.copyOfRange(b, 0, readBytes)); }
            }

            inFile.close();
        }
    }


    private String guessMimeType(String name)
    {
        MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();
        return typeMap.getContentType(name);
    }
}