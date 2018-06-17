package com.lzp.test;

import com.lzp.test.type.ParseUtils;
import com.lzp.test.type.ResourceTypes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;


public class Test {
    public static void main(String[] args) {
        byte[] srcs = readResourcearscFile("resources.arsc");
        ResourceTypes.ResTable_header resTable_header = ParseUtils.parseResTable_header(srcs);
//        System.out.println(resTable_header.toString());

        System.out.println("----------parse res stringpool--------------");
        ResourceTypes.ResStringPool_header resStringPool_header = ParseUtils.parseResStringPool_header(srcs);
//        System.out.println(resStringPool_header.toString());
        System.out.println("------------------------");

        ResourceTypes.ResTable_package resTable_package = ParseUtils.parseResTable_package(srcs);
//        System.out.println(resTable_package.toString());
//        System.out.println("------------------------");

        System.out.println("----------parse type stringpool--------------");
        ParseUtils.parseTypeStringPoolChunk(srcs);
        System.out.println("------------------------");

        System.out.println("----------parse key stringpool--------------");
        ParseUtils.parseKeyStringPoolChunk(srcs);
        System.out.println("------------------------");

        int resCount = 0;
        while (!ParseUtils.isEnd(srcs.length)) {
            resCount++;
            boolean isTypeSpec = ParseUtils.isTypeSpec(srcs);
            if (isTypeSpec) {
                ResourceTypes.ResTable_typeSpec table_typeSpec = ParseUtils.parseResTable_typeSpec(srcs);
                System.out.println(table_typeSpec.toString());
//                System.out.println("-------------------------------");
            }else{//ResTable_type
                ResourceTypes.ResTable_type resTable_type = ParseUtils.parseResTable_type(srcs);
//                System.out.println(resTable_type.toString());
            }
        }
    }

    private static byte[] readResourcearscFile(String filePath) {
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
}
