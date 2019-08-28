package com.huison.download.utils;

import java.io.File;

/**
 * Created by huisonma on 2019/5/15.
 */
public class FileUtil {

    public static boolean deleteFile(String path) {
        return deleteFile(new File(path));
    }

    public static boolean deleteFile(File file) {
        if (file == null) {
            return false;
        }
        if (!file.exists()) {
            return true;
        }
        if (file.isFile()) {
            return file.delete();
        }
        return false;
    }

    public static boolean deleteDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            return true;
        }
        if (dir.isDirectory()) {
            File[] list = dir.listFiles();
            boolean result = true;
            for (File child : list) {
                if (result) {
                    result = deleteDir(child.getAbsolutePath());
                } else {
                    deleteFile(child);
                }
            }
            return result;
        } else {
            return deleteFile(dir);
        }
    }

    public static boolean isFileExists(String path) {
        return isFileExists(new File(path));
    }

    public static boolean isFileExists(File file) {
        if (file == null) {
            return false;
        }
        return file.exists();
    }

    public static File createDir(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static long getFileLength(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file.length();
        }
        return 0;
    }

}
