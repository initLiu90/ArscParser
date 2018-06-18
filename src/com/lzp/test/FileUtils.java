package com.lzp.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileUtils {
    public static byte[] readResourcearscFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) return null;
        File file = new File(filePath);
        if (!file.exists() || file.isDirectory()) {
            throw new RuntimeException("Invalide file path:" + filePath);
        }
        byte[] srcBytes = null;
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
                bos.flush();
            }
            srcBytes = bos.toByteArray();

            fis.close();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return srcBytes;
    }

    public static void createResourcesarscFile(String filePath, String fileName, byte[] srcs) {
        if (TextUtils.isEmpty(filePath)) throw new RuntimeException("invalide file path:" + filePath);
        if (srcs == null || srcs.length == 0) throw new RuntimeException("no content");

        File file = new File(filePath);
        if (file.isDirectory()) {
            if (!file.exists()) {
                file.mkdirs();
            }
            if (!file.exists()) {
                throw new RuntimeException("counld not create dir:" + filePath);
            }

            fileName = TextUtils.isEmpty(fileName) ? "resources.arsc" : fileName;

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(new File(filePath, fileName));
                fos.write(srcs);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
