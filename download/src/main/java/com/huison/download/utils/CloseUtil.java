package com.huison.download.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by huisonma on 2019/5/15.
 */
public class CloseUtil {

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
