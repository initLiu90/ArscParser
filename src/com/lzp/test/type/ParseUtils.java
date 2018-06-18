package com.lzp.test.type;

import com.lzp.test.Utils;

import java.util.ArrayList;

public class ParseUtils {
    private static int resStringPoolChunkOffset;//字符串池的偏移值
    private static int packageChunkOffset;//包内容的偏移值
    private static int keyStringPoolChunkOffset;//key字符串池的偏移值
    private static int typeStringPoolChunkOffset;//类型字符串池的偏移值

    private static int resTypeOffset;

    private static ArrayList<String> resStringList = new ArrayList<>();//所有字符串池
    private static ArrayList<String> keyStringList = new ArrayList<>();//所有资源key的集合（资源的文件名不带后缀）
    private static ArrayList<String> typeStringList = new ArrayList<>();//所有

    //资源包的id和类型id
    private static int packId;
    private static int resTypeId;

    private static int curTypeEntryCounts = 0;
    private static int entryCounts = 0;//辅助获取资源key值的

    /**
     * 解析ResTable_header头信息
     *
     * @param src
     * @return
     */
    public static ResourceTypes.ResTable_header parseResTable_header(byte[] src) {
        ResourceTypes.ResTable_header resTable_header = new ResourceTypes.ResTable_header();
        resTable_header.header = parseResChunk_header(src, 0);

        byte[] packageCountByte = Utils.copyByte(src, resTable_header.header.getSize(), 4);
        resTable_header.packageCount = Utils.byte2int(packageCountByte);

        resStringPoolChunkOffset = resTable_header.header.headerSize;
        return resTable_header;
    }

    public static ResourceTypes.ResStringPool_header parseResStringPool_header(byte[] src) {

        ResourceTypes.ResStringPool_header resStringPool_header = parseStringPollChunk(src, resStringPoolChunkOffset, resStringList);

        packageChunkOffset = resStringPoolChunkOffset + resStringPool_header.header.size;
        return resStringPool_header;
    }

    /**
     * 解析ResTable_package package信息
     *
     * @param src
     * @return
     */
    public static ResourceTypes.ResTable_package parseResTable_package(byte[] src) {
        ResourceTypes.ResTable_package resTable_package = new ResourceTypes.ResTable_package();
        resTable_package.header = parseResChunk_header(src, packageChunkOffset + 0);

        byte[] idBytes = Utils.copyByte(src, packageChunkOffset + resTable_package.header.getSize(), 4);
        resTable_package.id = Utils.byte2int(idBytes);
        packId = resTable_package.id;
        System.out.println("packageId:" + Integer.toHexString(packId));

        byte[] nameBytes = Utils.copyByte(src, packageChunkOffset + resTable_package.header.getSize() + 4, 2 * 128);
        String packageName = new String(nameBytes);
        resTable_package.name = Utils.filterStringNull(packageName).toCharArray();

        byte[] typeStringsBytes = Utils.copyByte(src, packageChunkOffset + resTable_package.header.getSize() + 4 + 2 * 128, 4);
        resTable_package.typeStrings = Utils.byte2int(typeStringsBytes);

        byte[] lastPublicTypeBytes = Utils.copyByte(src, packageChunkOffset + resTable_package.header.getSize() + 4 + 2 * 128 + 4, 4);
        resTable_package.lastPublicType = Utils.byte2int(lastPublicTypeBytes);

        byte[] keyStringsBytes = Utils.copyByte(src, packageChunkOffset + resTable_package.header.getSize() + 4 + 2 * 128 + 4 + 4, 4);
        resTable_package.keyStrings = Utils.byte2int(keyStringsBytes);

        byte[] lastPublicKeyBytes = Utils.copyByte(src, packageChunkOffset + resTable_package.header.getSize() + 4 + 2 * 128 + 4 + 4 + 4, 4);
        resTable_package.lastPublicKey = Utils.byte2int(lastPublicKeyBytes);

        byte[] typeIdOffsetsBytes = Utils.copyByte(src, packageChunkOffset + resTable_package.header.getSize() + 4 + 2 * 128 + 4 + 4 + 4 + 4, 4);
        resTable_package.typeIdOffset = Utils.byte2int(typeIdOffsetsBytes);

        typeStringPoolChunkOffset = packageChunkOffset + resTable_package.typeStrings;
        keyStringPoolChunkOffset = packageChunkOffset + resTable_package.keyStrings;

        return resTable_package;
    }

    public static ResourceTypes.ResStringPool_header parseTypeStringPoolChunk(byte[] src) {
        return parseStringPollChunk(src, typeStringPoolChunkOffset, typeStringList);
    }

    public static ResourceTypes.ResStringPool_header parseKeyStringPoolChunk(byte[] src) {
        ResourceTypes.ResStringPool_header resStringPool_header = parseStringPollChunk(src, keyStringPoolChunkOffset, keyStringList);
        resTypeOffset = keyStringPoolChunkOffset + resStringPool_header.header.size;
        return resStringPool_header;
    }

    public static ResourceTypes.ResTable_typeSpec parseResTable_typeSpec(byte[] src) {
        ResourceTypes.ResTable_typeSpec resTable_typeSpec = new ResourceTypes.ResTable_typeSpec();
        resTable_typeSpec.header = parseResChunk_header(src, resTypeOffset);

        int offset = resTypeOffset + resTable_typeSpec.header.getSize();

        byte[] idByte = Utils.copyByte(src, offset, 1);
        resTable_typeSpec.id = idByte[0];
        resTypeId = resTable_typeSpec.id;

        byte[] res0Byte = Utils.copyByte(src, offset + 1, 1);
        resTable_typeSpec.res0 = res0Byte[0];

        byte[] res1Byte = Utils.copyByte(src, offset + 1 + 1, 2);
        resTable_typeSpec.res1 = Utils.byte2Short(res1Byte);

        byte[] entryCountByte = Utils.copyByte(src, offset + 1 + 1 + 2, 4);
        resTable_typeSpec.entryCount = Utils.byte2int(entryCountByte);
        curTypeEntryCounts = resTable_typeSpec.entryCount;
        entryCounts += resTable_typeSpec.entryCount;

        resTypeOffset = resTypeOffset + resTable_typeSpec.header.size;
        return resTable_typeSpec;
    }


    private static ResourceTypes.ResStringPool_header parseStringPollChunk(byte[] src, int offset, ArrayList<String> stringList) {
        ResourceTypes.ResStringPool_header resStringPool_header = new ResourceTypes.ResStringPool_header();

        resStringPool_header.header = parseResChunk_header(src, offset);

        int offsetIn = offset + resStringPool_header.header.getSize();

        //字符串的个数
        byte[] stringCountByte = Utils.copyByte(src, offsetIn, 4);
        resStringPool_header.stringCount = Utils.byte2int(stringCountByte);

        //样式的个数
        byte[] styleCountByte = Utils.copyByte(src, offsetIn + 4, 4);
        resStringPool_header.styleCount = Utils.byte2int(styleCountByte);

        //字符串的格式:utf-8/utf-16
        byte[] flagByte = Utils.copyByte(src, offsetIn + 4 + 4, 4);
        resStringPool_header.flags = Utils.byte2int(flagByte);
        boolean isUtf8 = resStringPool_header.flags == ResourceTypes.ResStringPool_header.UTF8_FLAG;

        //字符串内容的开始位置
        byte[] stringStartByte = Utils.copyByte(src, offsetIn + 4 + 4 + 4, 4);
        resStringPool_header.stringStart = Utils.byte2int(stringStartByte);

        //样式内容的开始位置
        byte[] styleStartByte = Utils.copyByte(src, offset + 4 + 4 + 4 + 4, 4);
        resStringPool_header.stylesStart = Utils.byte2int(styleStartByte);

        //获取字符串内容的索引数组和样式内容的索引数组
        int[] stringIndexArray = new int[resStringPool_header.stringCount];
        int[] styleIndexArray = new int[resStringPool_header.styleCount];

        int stringIndex = offsetIn + 4 * 5;
        for (int i = 0; i < resStringPool_header.stringCount; i++) {
            stringIndexArray[i] = Utils.byte2int(Utils.copyByte(src, stringIndex + i * 4, 4));
        }

        int styleIndex = stringIndex + 4 * resStringPool_header.stringCount;
        for (int i = 0; i < resStringPool_header.styleCount; i++) {
            styleIndexArray[i] = Utils.byte2int(Utils.copyByte(src, styleIndex + i * 4, 4));
        }

        //每个字符串的头两个字节的最后一个字节是字符串的长度
        //这里获取所有字符串的内容
//        int stringContentIndex = styleIndex + resStringPool_header.styleCount * 4;
        int stringContentIndex = offset + resStringPool_header.stringStart;
        int index = 0;
        while (index < resStringPool_header.stringCount) {
            byte[] lenByte = Utils.copyByte(src, stringContentIndex, 2);
            int len = (lenByte[0] & 0x7F);
            len = computeLengthOffset(len, isUtf8);
            if (len != 0) {
                String val = "";
                try {
                    byte[] stringBytes = Utils.copyByte(src, stringContentIndex + 2, len);
                    val = new String(stringBytes, "utf-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stringList.add(val);
            } else {
                stringList.add("");
            }
            if (isUtf8) {
                stringContentIndex += (len + 3);
            } else {
                stringContentIndex += (len + 2 + 2);
            }
            index++;
        }
        for (String str : stringList) {
            System.out.println("str:" + str);
        }

        return resStringPool_header;
    }

    public static ResourceTypes.ResTable_type parseResTable_type(byte[] src) {
        ResourceTypes.ResTable_type resTable_type = new ResourceTypes.ResTable_type();
        //解析头部信息
        resTable_type.header = parseResChunk_header(src, resTypeOffset);

        int offset = (resTypeOffset + resTable_type.header.getSize());

        //解析type的id值
        byte[] idByte = Utils.copyByte(src, offset, 1);
        resTable_type.id = (byte) (idByte[0] & 0xff);

        //解析res0字段的值
        byte[] res0Byte = Utils.copyByte(src, offset + 1, 1);
        resTable_type.res0 = (byte) (res0Byte[0] & 0xff);

        //解析res1字段的值
        byte[] res1Byte = Utils.copyByte(src, offset + 1 + 1, 2);
        resTable_type.res1 = Utils.byte2Short(res1Byte);

        byte[] entryCountByte = Utils.copyByte(src, offset + 1 + 1 + 2, 4);
        resTable_type.entryCount = Utils.byte2int(entryCountByte);

        byte[] entriesStartByte = Utils.copyByte(src, offset + 1 + 1 + 2 + 4, 4);
        resTable_type.entriesStart = Utils.byte2int(entriesStartByte);

        ResourceTypes.ResTable_config resTable_config = new ResourceTypes.ResTable_config();
        resTable_config = parseResTable_config(Utils.copyByte(src, offset + 1 + 1 + 2 + 4 + 4, resTable_config.getSize()));

//        System.out.println("config:" + resTable_config);

//        System.out.println("res type info:" + resTable_type);

        String typeName = typeStringList.get(resTable_type.id - 1);
        System.out.println("type_name:" + typeName);


        //这里开始解析后面对应的ResEntry和ResValue
        int entryAryOffset = resTypeOffset + resTable_type.entriesStart;
        ResourceTypes.ResTable_entry[] resTable_entries = new ResourceTypes.ResTable_entry[resTable_type.entryCount];
        ResourceTypes.Res_value[] res_values = new ResourceTypes.Res_value[resTable_type.entryCount];

        for (int i = 0; i < resTable_type.entryCount; i++) {
            int resId = getResId(i);
//            System.out.println("resId:0x" + Integer.toHexString(resId));
            System.out.println("R." + typeName + "." + getKeyName(i) + "=0x" + Integer.toHexString(resId));


            resTable_entries[i] = parseResTable_entry(src, entryAryOffset);
            entryAryOffset += resTable_entries[i].getSize();

            if (resTable_entries[i].flags == ResourceTypes.ResTable_entry.Flag.FLAG_COMPLEX) {//ResTable_map_entry
                ResourceTypes.ResTable_map_entry resTable_map_entry = new ResourceTypes.ResTable_map_entry();
                resTable_map_entry.parent = parseResTable_ref(src, entryAryOffset);

                byte[] countBytes = Utils.copyByte(src, entryAryOffset + resTable_map_entry.parent.getSize(), 4);
                resTable_map_entry.count = Utils.byte2int(countBytes);

                entryAryOffset += resTable_map_entry.size;

                for (int j = 0; j < resTable_map_entry.count; j++) {
                    ResourceTypes.ResTable_map resTable_map = new ResourceTypes.ResTable_map();
                    resTable_map.name = parseResTable_ref(src, entryAryOffset);
                    resTable_map.value = parseRes_value(src, entryAryOffset + resTable_map.name.getSize());
                    System.out.print("resTable_map:" + resTable_map.toString());
                    entryAryOffset += resTable_map.getSize();
                }

            } else {//Res_value
                res_values[i] = parseRes_value(src, entryAryOffset);
                entryAryOffset += res_values[i].getSize();
                System.out.println("value:" + res_values[i].toString());
            }
            System.out.println("******************************");
        }
        resTypeOffset += resTable_type.header.size;

        return resTable_type;
    }

    private static ResourceTypes.ResTable_ref parseResTable_ref(byte[] src, int start) {
        ResourceTypes.ResTable_ref resTable_ref = new ResourceTypes.ResTable_ref();
        byte[] identBytes = Utils.copyByte(src, start, 4);
        resTable_ref.ident = Utils.byte2int(identBytes);
        return resTable_ref;
    }

    private static ResourceTypes.ResTable_entry parseResTable_entry(byte[] src, int start) {
        ResourceTypes.ResTable_entry entry = new ResourceTypes.ResTable_entry();

        byte[] sizeBytes = Utils.copyByte(src, start, 2);
        entry.size = Utils.byte2Short(sizeBytes);

        byte[] flagsBytes = Utils.copyByte(src, start + 2, 2);
        entry.flags = Utils.byte2Short(flagsBytes);

        entry.key = parseResStringPool_ref(src, start + 2 + 2);
        return entry;
    }

    private static ResourceTypes.Res_value parseRes_value(byte[] src, int start) {
        ResourceTypes.Res_value res_value = new ResourceTypes.Res_value();

        byte[] sizeBytes = Utils.copyByte(src, start, 2);
        res_value.size = Utils.byte2Short(sizeBytes);

        byte[] res0Bytes = Utils.copyByte(src, start + 2, 1);
        res_value.res0 = (byte) (res0Bytes[0] & 0xff);

        byte[] dataTypeBytes = Utils.copyByte(src, start + 2 + 1, 1);
        res_value.dataType = (byte) (dataTypeBytes[0] & 0xff);

        byte[] dataBytes = Utils.copyByte(src, start + 2 + 1 + 1, 4);
        res_value.data = Utils.byte2int(dataBytes);

        return res_value;
    }

    private static ResourceTypes.ResStringPool_ref parseResStringPool_ref(byte[] src, int start) {
        ResourceTypes.ResStringPool_ref resStringPool_ref = new ResourceTypes.ResStringPool_ref();

        byte[] indexBytes = Utils.copyByte(src, start, 4);
        resStringPool_ref.index = Utils.byte2int(indexBytes);
        return resStringPool_ref;
    }

    public static ResourceTypes.ResTable_config parseResTable_config(byte[] src) {
        ResourceTypes.ResTable_config config = new ResourceTypes.ResTable_config();

        byte[] sizeByte = Utils.copyByte(src, 0, 4);
        config.size = Utils.byte2int(sizeByte);

        //以下结构是Union
        byte[] mccByte = Utils.copyByte(src, 4, 2);
        config.mcc = Utils.byte2Short(mccByte);
        byte[] mncByte = Utils.copyByte(src, 6, 2);
        config.mnc = Utils.byte2Short(mncByte);
        byte[] imsiByte = Utils.copyByte(src, 4, 4);
        config.imsi = Utils.byte2int(imsiByte);

        //以下结构是Union
        byte[] languageByte = Utils.copyByte(src, 8, 2);
        config.language = languageByte;
        byte[] countryByte = Utils.copyByte(src, 10, 2);
        config.country = countryByte;
        byte[] localeByte = Utils.copyByte(src, 8, 4);
        config.locale = Utils.byte2int(localeByte);

        //以下结构是Union
        byte[] orientationByte = Utils.copyByte(src, 12, 1);
        config.orientation = orientationByte[0];
        byte[] touchscreenByte = Utils.copyByte(src, 13, 1);
        config.touchscreen = touchscreenByte[0];
        byte[] densityByte = Utils.copyByte(src, 14, 2);
        config.density = Utils.byte2Short(densityByte);
        byte[] screenTypeByte = Utils.copyByte(src, 12, 4);
        config.screenType = Utils.byte2int(screenTypeByte);

        //以下结构是Union
        byte[] keyboardByte = Utils.copyByte(src, 16, 1);
        config.keyboard = keyboardByte[0];
        byte[] navigationByte = Utils.copyByte(src, 17, 1);
        config.navigation = navigationByte[0];
        byte[] inputFlagsByte = Utils.copyByte(src, 18, 1);
        config.inputFlags = inputFlagsByte[0];
        byte[] inputPad0Byte = Utils.copyByte(src, 19, 1);
        config.inputPad0 = inputPad0Byte[0];
        byte[] inputByte = Utils.copyByte(src, 16, 4);
        config.input = Utils.byte2int(inputByte);

        //以下结构是Union
        byte[] screenWidthByte = Utils.copyByte(src, 20, 2);
        config.screenWidth = Utils.byte2Short(screenWidthByte);
        byte[] screenHeightByte = Utils.copyByte(src, 22, 2);
        config.screenHeight = Utils.byte2Short(screenHeightByte);
        byte[] screenSizeByte = Utils.copyByte(src, 20, 4);
        config.screenSize = Utils.byte2int(screenSizeByte);

        //以下结构是Union
        byte[] sdVersionByte = Utils.copyByte(src, 24, 2);
        config.sdVersion = Utils.byte2Short(sdVersionByte);
        byte[] minorVersionByte = Utils.copyByte(src, 26, 2);
        config.minorVersion = Utils.byte2Short(minorVersionByte);
        byte[] versionByte = Utils.copyByte(src, 24, 4);
        config.version = Utils.byte2int(versionByte);

        //以下结构是Union
        byte[] screenLayoutByte = Utils.copyByte(src, 28, 1);
        config.screenLayout = screenLayoutByte[0];
        byte[] uiModeByte = Utils.copyByte(src, 29, 1);
        config.uiMode = uiModeByte[0];
        byte[] smallestScreenWidthDpByte = Utils.copyByte(src, 30, 2);
        config.smallestScreenWidthDp = Utils.byte2Short(smallestScreenWidthDpByte);
        byte[] screenConfigByte = Utils.copyByte(src, 28, 4);
        config.screenConfig = Utils.byte2int(screenConfigByte);

        //以下结构是Union
        byte[] screenWidthDpByte = Utils.copyByte(src, 32, 2);
        config.screenWidthDp = Utils.byte2Short(screenWidthDpByte);
        byte[] screenHeightDpByte = Utils.copyByte(src, 34, 2);
        config.screenHeightDp = Utils.byte2Short(screenHeightDpByte);
        byte[] screenSizeDpByte = Utils.copyByte(src, 32, 4);
        config.screenSizeDp = Utils.byte2int(screenSizeDpByte);

        byte[] localeScriptByte = Utils.copyByte(src, 36, 4);
        config.localeScript = localeScriptByte;

        byte[] localeVariantByte = Utils.copyByte(src, 40, 8);
        config.localeVariant = localeVariantByte;
        return config;
    }

    /**
     * 解析ResChunk_header头部信息
     *
     * @param src
     */
    private static ResourceTypes.ResChunk_header parseResChunk_header(byte[] src, int start) {
        ResourceTypes.ResChunk_header header = new ResourceTypes.ResChunk_header();

        byte[] typeByte = Utils.copyByte(src, start, 2);
        header.type = Utils.byte2Short(typeByte);

        byte[] headerSizeByte = Utils.copyByte(src, start + 2, 2);
        header.headerSize = Utils.byte2Short(headerSizeByte);

        byte[] tableSizeByte = Utils.copyByte(src, start + 4, 4);
        header.size = Utils.byte2int(tableSizeByte);
        return header;
    }

    public static byte[] reWritePackeId(byte[] src, int packeageId) {
        ResourceTypes.ResTable_package resTable_package = new ResourceTypes.ResTable_package();
        resTable_package.header = parseResChunk_header(src, packageChunkOffset + 0);

        byte[] idBytes = Utils.copyByte(src, packageChunkOffset + resTable_package.header.getSize(), 4);
        resTable_package.id = Utils.byte2int(idBytes);

        byte[] newSrc = new byte[src.length];
        if (packeageId != resTable_package.id) {
            byte[] newIdBytes = Utils.int2ByteArray(packeageId);

            int pidStart = packageChunkOffset + resTable_package.header.getSize();
            int pidEnd = packageChunkOffset + resTable_package.header.getSize() + 4;

            for (int i = 0; i < src.length; i++) {
                if (i >= pidStart && i < pidEnd) {
                    int index = i - pidStart;
                    newSrc[i] = newIdBytes[index];
                } else {
                    newSrc[i] = src[i];
                }
            }
        }
        return newSrc;
    }

    /**
     * 判断是否到文件末尾了
     *
     * @param length
     * @return
     */
    public static boolean isEnd(int length) {
        if (resTypeOffset >= length)
            return true;
        return false;
    }

    /**
     * 判断是否是ResTable_typeSpec类型
     *
     * @param src
     * @return
     */
    public static boolean isTypeSpec(byte[] src) {
        ResourceTypes.ResChunk_header header = parseResChunk_header(src, resTypeOffset);
        if (header.type == 0x0202) {
            return true;
        }
        return false;
    }

    /**
     * 获取资源id
     * 这里高位是packid，中位是restypeid，地位是entryid
     *
     * @param entryid
     * @return
     */
    public static int getResId(int entryid) {
        return (((packId) << 24) | (((resTypeId) & 0xFF) << 16) | (entryid & 0xFFFF));
    }

    private static int computeLengthOffset(int length, boolean isUtf8) {
        return isUtf8 ? length : length * 2;
    }

    private static String getKeyName(int index) {
        try {
            String keyName = keyStringList.get(entryCounts - curTypeEntryCounts + index);
            return keyName;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }
}
