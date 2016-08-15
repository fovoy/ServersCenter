package com.fovoy.concurrent;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by zxz.zhang on 16/8/15.
 */
public final class Files {

    private static final Logger log = LoggerFactory.getLogger(Files.class);

    private Files() {
    }

    /**
     * 删除一个文件夹和该文件夹下所有文件。
     *
     * @param file 要删除的目录
     */
    public static boolean deleteTree(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++)
                    deleteTree(files[i]);
            }
        }
        return file.delete();
    }

    /**
     * 判断源文件是否比目标文件新，如果目标文件不存在也返回true.
     *
     * @param source 更改日期
     * @param other
     * @return
     */
    public static boolean isModified(File source, File other) {
        if (!other.exists())
            return true;
        return (source.lastModified() > other.lastModified());
    }

    /**
     * 从Reader读入全部字符,按照换行符拆分成数组返回.注: 返回的数组中已不包含换行符.
     *
     * @param reader A BufferedReader
     * @return 读入的全部文字。
     * @throws IOException
     */
    public static String[] readLines(BufferedReader reader) throws IOException {
        ArrayList<String> list = new ArrayList<String>();
        try {
            String line = null;
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
        } finally {
            Closer.close(reader);
        }
        String[] lines = new String[list.size()];
        list.toArray(lines);
        return lines;
    }

    /**
     * 读入全部字符,按照换行符拆分成数组返回.注: 返回的数组中已不包含换行符.
     *
     * @param reader A Reader
     * @return 读入的全部文字。
     * @throws IOException
     */
    public static String[] readLines(Reader reader) throws IOException {
        BufferedReader br;
        if (reader instanceof BufferedReader)
            br = (BufferedReader) reader;
        else
            br = new BufferedReader(reader);
        return readLines(br);
    }

    /**
     * 从文件中读入全部字符,按照换行符拆分成数组返回.注: 返回的数组中已不包含换行符.<br/>
     * @param file A File
     * @return 读入的全部文字。
     * @throws IOException
     */
    public static String[] readLines(File file) throws IOException {
        return readLines(new BufferedReader(new FileReader(file)));
    }

    /**
     * 从文件中按指定字符集读入全部字符,按照换行符拆分成数组返回.注: 返回的数组中已不包含换行符.
     *
     * @param file A File
     * @param charset 指定字符集
     * @return 读入的全部文字。
     * @throws IOException
     */
    public static String[] readLines(File file, String charset) throws IOException {
        return readLines(new FileInputStream(file), charset);
    }

    /**
     * 从字节流按指定字符集读入全部字符,按照换行符拆分成数组返回.注: 返回的数组中已不包含换行符.
     *
     * @param in A InputStream
     * @param charset 指定字符集
     * @return 读入的全部文字。
     * @throws IOException
     */
    public static String[] readLines(InputStream in, String charset) throws IOException {
        return readLines(new BufferedReader(new InputStreamReader(in, charset)));
    }

    public static String readString(File file, String charset) throws IOException {
        return readString(new FileInputStream(file), charset);
    }

    public static String readString(InputStream in, String charset) throws IOException {
        return readString(new InputStreamReader(in, charset));
    }

    public static String readString(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        try {
            char[] buf = new char[1024];
            for (int i = 0; (i = reader.read(buf)) != -1;)
                sb.append(buf, 0, i);
        } finally {
            Closer.close(reader);
        }
        return sb.toString();
    }

    /**
     * 将字节流缓存至字节组。
     *
     * @param in A InputStream
     * @return byte array
     * @throws IOException
     */
    public static byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte buf[] = new byte[1024];
            for (int i = 0; (i = in.read(buf)) != -1;)
                baos.write(buf, 0, i);
        } finally {
            Closer.close(in);
        }
        return baos.toByteArray();
    }

    /**
     * 将文件二进制内容读至字节组。
     *
     * @param file A File
     * @return byte array
     * @throws IOException
     */
    public static byte[] readBytes(File file) throws IOException {
        return readBytes(new FileInputStream(file));
    }

    @SuppressWarnings("unchecked")
    public static <T> T readObject(File file) throws IOException, ClassNotFoundException {

        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(new FileInputStream(file));
            return (T) is.readObject();
        } finally {
            Closer.close(is);
        }
    }

    /**
     * 将输入流(in) 分批写入到输出流(out)
     *
     * @param bufSize 缓冲大小
     * @return 读入/写出的字节长度
     * @throws IOException
     */
    public static int copyRange(InputStream in, OutputStream out, int bufSize) throws IOException {

        int count = 0;
        try {
            byte buffer[] = new byte[bufSize];
            int len = 0;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                count += len;
            }
        } finally {
            Closer.close(in);
            Closer.close(out);
        }
        return count;
    }

    /**
     * 将输入流(reader) 分批写入到输出流(writer)
     *
     * @param bufSize 缓冲大小
     * @return 读入/写出的字符数
     * @throws IOException
     */
    public static int copyRange(Reader reader, Writer writer, int bufSize) throws IOException {

        int count = 0;
        try {
            char buffer[] = new char[bufSize];
            int len = 0;
            while ((len = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, len);
                count += len;
            }
        } finally {
            Closer.close(reader);
            Closer.close(writer);
        }
        return count;
    }

    public static File getResource(String resourceName) {
        URL url = Files.class.getClassLoader().getResource(resourceName);

        Preconditions.checkArgument(url != null, "resource %s not found.", resourceName);

        File file = new File(url.getFile());

        Preconditions.checkArgument(file.exists(), "resource %s not found.", file);

        return file;
    }

    /**
     * 从Reader 中依次读入sectionLines行数的字符,交由handler处理,直到文件处理完毕或handler取消为止.<br/>
     * 此方法用于处理大型文本或对内存占用要求苛刻的应用.
     *
     * <pre>
     * Files.readString(
     *         new BufferedReader(new InputStreamReader(new FileInputStream(&quot;c:/windows/WindowsUpdate.log&quot;), &quot;UTF-8&quot;)),
     *         new Files.SectionHandler() {
     *             int count = 0;
     *
     *             &#064;Override
     *             public boolean processSection(String[] lines) {
     *                 for (String line : lines) {
     *                     if (++count &gt;= 15)
     *                         return false;
     *                     System.out.println(line);
     *                 }
     *                 return true;
     *             }
     *         }, 20);
     * </pre>
     *
     * @param reader A BufferedReader
     * @param handler 用于处理片段的处理器
     * @param sectionLines 每个片段的行数
     * @return 总共处理过的行数
     * @throws IOException
     */
    public static int readString(BufferedReader reader, SectionHandler handler, int sectionLines) throws IOException {
        int count = 0;
        try {
            ArrayList<String> list = new ArrayList<String>(sectionLines);
            String line = null;
            while ((line = reader.readLine()) != null) {
                list.add(line);
                if (list.size() >= sectionLines) {
                    count += list.size();
                    String[] arr = new String[list.size()];
                    list.toArray(arr);
                    list.clear();
                    if (!handler.processSection(arr))
                        break;
                }
            }
            if (list.size() > 0) {
                count += list.size();
                String[] arr = new String[list.size()];
                list.toArray(arr);
                list.clear();
                handler.processSection(arr);
            }
        } finally {
            Closer.close(reader);
        }
        return count;
    }

    /**
     * @since 2.0
     */
    public static interface SectionHandler {
        boolean processSection(String[] s);
    }

    public static String getExtension(File theFile) {

        String path = theFile.getPath();
        int dot = path.lastIndexOf('.');
        if (dot == -1)
            return null;
        return path.substring(dot + 1);
    }

    private static OutputStream blackhole = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
        }
    };

    public static OutputStream getBlackHole() {
        return blackhole;
    }

    public static void zip(File zipFile, File... files) throws IOException {
        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(new FileOutputStream(zipFile));

            for (File file : files)
                addToZip(null, file, zip);
        } finally {
            Closer.close(zip);
        }
    }

    private static void addToZip(String parentPath, File file, ZipOutputStream zip) throws IOException {

        String path = parentPath == null ? file.getName() : parentPath + "/" + file.getName();

        log.debug("addZip: {}", path);

        String isoPath = new String(path.getBytes("GB18030")); // fix version

        if (file.isDirectory()) {
            zip.putNextEntry(new ZipEntry(isoPath + "/"));
            for (File sub : file.listFiles()) {
                addToZip(path, sub, zip);
            }
        } else {
            zip.putNextEntry(new ZipEntry(isoPath));
            InputStream in = new FileInputStream(file);
            try {
                byte buffer[] = new byte[81920];
                int len = 0;
                while ((len = in.read(buffer)) != -1)
                    zip.write(buffer, 0, len);
            } finally {
                Closer.close(in);
            }
        }
    }
}
