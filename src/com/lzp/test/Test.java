package com.lzp.test;

import com.lzp.test.type.ParseUtils;
import com.lzp.test.type.ResourceTypes;


public class Test {
    public static void main(String[] args) {
//        byte[] srcs = FileUtils.readBinaryFile("resources1.arsc");
//        byte[] newSrcs = reWritePackeID(srcs, 100);
//        FileUtils.createResourcesarscFile("D:/Workspace/Java/Test", "new_resource.arsc", newSrcs);

//        byte[] srcs = FileUtils.readBinaryFile("new_resource.arsc");
//        parse(srcs);

        byte[] src = FileUtils.readBinaryFile("/Users/lillian/Workspace/Android/Tests/resarscparse/amf1.xml");
        parseAndroidManifest(src);
    }

    private static byte[] reWritePackeID(byte[] srcs, int packageId) {
        ParseUtils.parseResTable_header(srcs);
        ParseUtils.parseResStringPool_header(srcs);
        byte[] newSrcs = ParseUtils.reWritePackeId(srcs, packageId);
        return newSrcs;
    }

    private static void parse(byte[] srcs) {
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
                System.out.println("----------parse typeSpec--------------");
                ResourceTypes.ResTable_typeSpec table_typeSpec = ParseUtils.parseResTable_typeSpec(srcs);
                System.out.println(table_typeSpec.toString());
                System.out.println("-------------------------------");
            } else {//ResTable_type
                System.out.println("----------parse type--------------");
                ResourceTypes.ResTable_type resTable_type = ParseUtils.parseResTable_type(srcs);
                System.out.println("-------------------------------");
            }
        }
    }
}
