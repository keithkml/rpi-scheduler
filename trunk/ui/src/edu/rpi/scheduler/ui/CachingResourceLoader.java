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

import edu.rpi.scheduler.schedb.spec.ResourceLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CachingResourceLoader implements ResourceLoader {
    private static final Logger logger
            = Logger.getLogger(CachingResourceLoader.class.getName());

    public static final String SYSPROP_DISALLOWHTTPCOMPRESSION
            = "scheduler.disallowhttpcompression";

    private static long extraTimeoutDelta;
    private static long maxTimeout;
    private static long minTimeout;
    private static long minTimeoutDays;
    private Map<String,Long> lastCached = new ConcurrentHashMap<String, Long>(20);

    static {
        minTimeoutDays = getMillisecondsFromSysProp("scheduler.cache.mintimeoutdays", 3);
        extraTimeoutDelta = getMillisecondsFromSysProp("scheduler.cache.extratimeout", 1);
        minTimeout = getMillisecondsFromSysProp("scheduler.cache.mintimeout", 3);
        maxTimeout = getMillisecondsFromSysProp("scheduler.cache.maxtimeout", 10);
    }

    private static long getMillisecondsFromSysProp(String key, float def) {
        String value = System.getProperty(key);

        float decimal;
        if (value == null) decimal = def;
        else decimal = Float.parseFloat(value);

        long time = (long) (decimal * 1000);
        if (time < 0) time = 0;
        return time;
    }

    private static String getCachedFilenameForURL(String urlstr) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(urlstr.getBytes("UTF-8"));
            StringBuffer sb = new StringBuffer(hash.length*2);
            for (byte b : hash) {
                String str = Integer.toHexString(b);
                if (str.length() > 2) str = str.substring(0, 2);
                for (int i = str.length(); i < 2; i++) sb.append('0');
                sb.append(str);
            }
            int slash = urlstr.lastIndexOf('/');
            if (slash == -1) slash = urlstr.lastIndexOf('\\');
            if (slash != -1) {
                sb.append("-");
                String sub = urlstr.substring(slash);
                sb.append(sub.replaceAll("[^a-zA-Z0-9.-]", ""));
            }
            return sb.toString();
        } catch (Exception e) {
            return urlstr.replaceAll("[^a-zA-Z0-9.]+-", "-");
        }
    }

    public boolean isResourceCached(String urlstr) {
        return getCachedFile(getCachedFilenameForURL(urlstr)) != null;
    }

    public InputStream loadResource(String urlstr)
            throws IOException {
        URL url;
        try {
            url = new URL(urlstr);
        } catch (MalformedURLException urle) {
            try {
                return loadFile(urlstr);
            } catch (FileNotFoundException filee) {
                throw urle;
            }
        }

        long mod = -1;
        long daysold = -1;
        File cachedFile = null;
        String cachedFilename = null;
        try {
            cachedFilename = getCachedFilenameForURL(urlstr);
            cachedFile = getCachedFile(cachedFilename);
            if (cachedFile != null && cachedFile.length() > 0) {
                mod = cachedFile.lastModified();
                long time = System.currentTimeMillis();
                daysold = (time - mod) / 86400000;
                logger.log(Level.FINE, "File " + urlstr + " was cached as "
                        + cachedFilename + ", last modified " + new Date(mod));
                if (time - updateLastCachedTime(cachedFilename, time) < 10*1000) {
                    logger.log(Level.FINE, "File " + urlstr + " was checked "
                            + "less than 10 seconds ago; using cached copy");
                    return new FileInputStream(cachedFile);
                }
            }
        } finally {
            try {
                URLConnection conn = url.openConnection();
                if (mod != -1) {
                    conn.setIfModifiedSince(mod);
                    // wait 1 second for each day old the file is, after 3
                    // days
                    long add = extraTimeoutDelta * (daysold - minTimeoutDays);
                    if (add < 0) add = 0;
                    long maxAdd = maxTimeout - minTimeout;
                    if (add > maxAdd) add = maxAdd;
                    conn.setConnectTimeout((int) (minTimeout + add));

                } else {
                    conn.setConnectTimeout(30000);
                }

                if (!Boolean.getBoolean(SYSPROP_DISALLOWHTTPCOMPRESSION)) {
                    conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
                }
                conn.connect();
                if (mod != -1 && cachedFile != null
                        && conn.getLastModified() < mod) {
                    logger.log(Level.FINER, "File " + urlstr
                            + " has not changed; using cached copy");
                    // we return inside the finally because if an error occurs,
                    // we can log the error and return the cached copy, so the
                    // user can still use the Scheduler
                    return new FileInputStream(cachedFile);
                }

                InputStream oin = conn.getInputStream();
                String encoding = conn.getHeaderField("Content-Encoding");
                InputStream in = oin;
                if (encoding != null) {
                    if (encoding.equalsIgnoreCase("gzip")) {
                        logger.log(Level.FINE, "Using GZIP compression for "
                                + urlstr);
                        in = new GZIPInputStream(oin);

                    } else if (encoding.equalsIgnoreCase("deflate")) {
                        logger.log(Level.FINE, "Using Deflate compression for "
                                + urlstr);
                        in = new InflaterInputStream(oin);
                    }
                }

                if (cachedFilename == null) {
                    logger.log(Level.WARNING, "Couldn't find cached copy for "
                            + urlstr);
                    return conn.getInputStream();

                } else {
                    logger.log(Level.FINER, "File " + urlstr
                            + " has changed; downloading new copy");
                    File tempFile = null;
                    OutputStream fout;
                    File cacheFolder = getCacheFolder();
                    try {
                        try {
                            tempFile = File.createTempFile(
                                    "temp-" + cachedFilename + "-",
                                    ".temp", cacheFolder);

                        } catch (IOException e) {
                            try {
                                logger.log(Level.WARNING,
                                        "Couldn't create temp file", e);
                                tempFile = new File(cacheFolder, ".temp-"
                                        + cachedFilename);
                                tempFile.createNewFile();
                                tempFile.deleteOnExit();
                                
                            } catch (IOException f) {
                                logger.log(Level.WARNING,
                                        "Couldn't create second temp file "
                                        + tempFile.getAbsolutePath(), f);
                                return conn.getInputStream();
                            }
                        }

                        fout = new FileOutputStream(tempFile);

                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Couldn't load " + urlstr, e);
                        return conn.getInputStream();
                    }

                    copyData(in, fout);

                    File finalCachedFile = createCachedFile(cachedFilename);
                    try {
                        try {
                            finalCachedFile.delete();
                        } catch (Exception e) {
                            logger.log(Level.WARNING,
                                    "Couldn't delete old cached file "
                                    + finalCachedFile.getAbsolutePath(), e);
                        }
                        tempFile.renameTo(finalCachedFile);
                        return new FileInputStream(finalCachedFile);

                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Couldn't move temp file "
                                + tempFile.getAbsolutePath()
                                + " to cache folder "
                                + cacheFolder.getAbsolutePath(), e);
                        return new FileInputStream(tempFile);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception while caching; reading "
                        + "from cached file", e);
                // if everything else fails, let's try to read from the
                // cached file
                if (cachedFile != null) return new FileInputStream(cachedFile);
                else return null;
            }
        }
    }

    private long updateLastCachedTime(String cachedFilename, long time) {
        return lastCached.put(cachedFilename, time);
    }

    private void copyData(InputStream in, OutputStream fout)
            throws IOException {
        byte[] buffer = new byte[1024];
        for (;;) {
            int count = in.read(buffer);
            if (count == -1) break;
            fout.write(buffer, 0, count);
        }
        in.close();
        fout.close();
    }

    public boolean removeFromCache(String dburl) {
        File file = getCachedFile(dburl);
        if (file == null) return false;
        else return file.delete();
    }

    private File getCachedFile(String cached) {
        File cachedFile = new File(getCacheFolder(), cached);
        if (cachedFile.exists()) return cachedFile;
        else return null;
    }

    private File createCachedFile(String cached) {
        return new File(getCacheFolder(), cached);
    }

    private File getCacheFolder() {
        File schedulerFolder = UITools.getSchedulerConfigFolder();
        File cacheFolder = new File(schedulerFolder, "cache");
        if (!cacheFolder.exists()) cacheFolder.mkdir();
        return cacheFolder;
    }

    private InputStream loadFile(String name) throws FileNotFoundException {
        return new FileInputStream(name);
    }
}
