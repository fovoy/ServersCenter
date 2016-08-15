package com.fovoy.io;

import com.fovoy.FovoyServer;
import com.fovoy.concurrent.Closer;
import com.fovoy.concurrent.TimeTask;
import com.fovoy.util.Strings;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zxz.zhang on 16/8/15.
 */
public class CachedRemoteFile extends TimeTask {

    private static final Logger logger = LoggerFactory.getLogger(CachedRemoteFile.class);

    public static final File CACHE_HOME = new File(FovoyServer.getFovoyStore());
    private static final File TMP_HOME = new File(CACHE_HOME, "tmp");

    private static final String CHECKSUM_FILE_EXT = ".md5";
    private static final String CHECKSUM_ALGORITHM = "MD5";
    private static final int BUFFER_SIZE = 4096;

    private final List<Listener> listeners = new ArrayList<Listener>();

    private final File file;
    private final String location;
    private final boolean autoUpdate;
    private final long updateInterval;

    private String checksum;
    private long lastChecked = 0;

    static {
        if (!TMP_HOME.exists()) {
            if (!TMP_HOME.mkdirs()) {
                throw new RuntimeException("Failed to create " + TMP_HOME);
            }
        } else if (!TMP_HOME.isDirectory()) {
            throw new IllegalStateException(TMP_HOME + " must be a directory.");
        }
    }

    public CachedRemoteFile(String filename, String location) {
        this(filename, location, false);
    }

    public CachedRemoteFile(String filename, String location, boolean autoUpdate) {
        this(filename, location, autoUpdate, 1000 * 60 * 60); // 1 hour
    }
    /**
     * 缓存的远程文件
     *
     * @param filename 本地缓存文件名
     * @param location 远程文件地址
     * @param autoUpdate 是否自动更新
     * @param updateInterval 更新频率(ms)
     */
    public CachedRemoteFile(String filename, String location, boolean autoUpdate, long updateInterval) {
        this.file = new File(CACHE_HOME, filename);
        this.location = location;
        this.autoUpdate = autoUpdate;
        this.checksum = checksumOf(file);
        this.updateInterval = updateInterval;

        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("无法创建目录" + parent.getAbsolutePath());
        }

        if (autoUpdate && needUpdate()) {
            runTask();
        }
    }
    public File getFile() {
        if (autoUpdate && needUpdate()) {
            weakUp();
        }
        return file;
    }

    public String getLocation() {
        return location;
    }

    public String getChecksum() {
        return checksum;
    }

    private boolean needUpdate() {

        final long current = System.currentTimeMillis();

        if (lastChecked + updateInterval >= current) {
            return false;
        }

        lastChecked = current;
        return true;
    }

    public boolean update() {

        logger.debug("from {}", location);

        String remotesum = checksumOfRemote(locationOfChecksum(location));
        if (Strings.isEmpty(remotesum)) {
            return false;
        }

        if (remotesum.equals(this.checksum)) {
            return true;
        }

        File tmp = new File(TMP_HOME, file.getName() + '_' + remotesum);

        String filesum = download(tmp, location);

        // 计算出的checksum与签名文件不匹配
        if (!remotesum.equals(filesum)) {
            logger.warn("签名不匹配, 远程签名={} 临时文件签名={} 临时文件={} 来自 {}", remotesum, filesum, tmp, location);
            return false;
        }

        // 移动覆盖
        if (!tmp.renameTo(file)) {
            logger.warn("文件移动失败, 尝试拷贝 {} => {}", tmp, file);
            try {
                Files.copy(tmp, file);
                tmp.delete();
            } catch (IOException e) {
                logger.error("copy failed", e);
                return false;
            }
        }

        // 更新本地签名
        this.checksum = remotesum;

        for (final Listener listener : listeners) {
            exector.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onUpdate(CachedRemoteFile.this);
                }
            });
        }

        return true;
    }

    @Override
    protected void runTask() {
        update();
    }

    private static String locationOfChecksum(String location) {
        StringBuilder url = new StringBuilder(location.length() + 4);

        int idx = location.indexOf('?');
        if (idx < 0) {
            // no query
            return url.append(location).append(CHECKSUM_FILE_EXT).toString();
        }

        url.append(location.substring(0, idx));
        url.append(CHECKSUM_FILE_EXT);
        url.append(location.substring(idx)); // query
        return url.toString();
    }

    private static String checksumOfRemote(String location) {

        InputStream is = null;

        try {
            is = connect(location).getInputStream();
            String line = new BufferedReader(new InputStreamReader(is)).readLine();

            if (Strings.isEmpty(line)) {
                logger.warn("远程签名为空 {}", location);
                return null;
            }

            // 签名相同，不必更新
            int idx = line.indexOf('\t');
            if (idx < 0) {
                idx = line.indexOf(' ');
            }

            return line.substring(0, idx);
        } catch (IOException e) {
            logger.warn("读取远程签名失败 {}", location);
            return null;
        } finally {
            Closer.close(is);
        }
    }

    private static String download(File local, String location) {

        InputStream is = null;
        OutputStream os = null;

        try {
            MessageDigest digest = digestOf(CHECKSUM_ALGORITHM);
            is = connect(location).getInputStream();
            os = new FileOutputStream(local);

            int count = 0;
            byte buf[] = new byte[BUFFER_SIZE];

            while ((count = is.read(buf)) > 0) {
                os.write(buf, 0, count);
                digest.update(buf, 0, count);
            }

            return Strings.encodeHex(digest.digest());
        } catch (IOException e) {
            logger.error("下载文件 {} => {}", location, local);
            return null;
        } finally {
            Closer.close(is, os);
        }
    }

    public static String checksumOf(File file) {

        InputStream is = null;

        try {
            MessageDigest digest = digestOf(CHECKSUM_ALGORITHM);

            is = new FileInputStream(file);
            int count = 0;
            byte buf[] = new byte[BUFFER_SIZE];

            while ((count = is.read(buf)) > 0) {
                digest.update(buf, 0, count);
            }
            return Strings.encodeHex(digest.digest());
        } catch (FileNotFoundException e) {
            return "";
        } catch (IOException e) {
            logger.error("", e);
        } finally {
            Closer.close(is);
        }

        return "";
    }

    private static MessageDigest digestOf(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static URLConnection connect(String location) throws IOException {
        URLConnection conn = new URL(location).openConnection();
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        return conn;
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }


    public static interface Listener {
        void onUpdate(CachedRemoteFile cache);
    }
}
