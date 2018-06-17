package com.lzp.test.type;

import java.util.Arrays;

public class ResourceTypes {
    private interface Resource {
        /**
         * @return byte count
         */
        int getSize();
    }

    /**
     * struct ResTable_header
     * {
     * struct ResChunk_header header;
     * <p>
     * // The number of ResTable_package structures.
     * uint32_t packageCount;
     * };
     */
    public static class ResTable_header implements Resource {
        ResChunk_header header;
        int packageCount;

        ResTable_header() {
            header = new ResChunk_header();
        }

        @Override
        public int getSize() {
            return header.getSize() + 4;
        }

        @Override
        public String toString() {
            return "ResTable_header{" +
                    "header=" + header.toString() +
                    ", packageCount=" + packageCount +
                    '}';
        }
    }

    /**
     * struct ResStringPool_header
     * {
     * struct ResChunk_header header;
     * <p>
     * // Number of strings in this pool (number of uint32_t indices that follow
     * // in the data).
     * uint32_t stringCount;
     * <p>
     * // Number of style span arrays in the pool (number of uint32_t indices
     * // follow the string indices).
     * uint32_t styleCount;
     * <p>
     * // Flags.
     * enum {
     * // If set, the string index is sorted by the string values (based
     * // on strcmp16()).
     * SORTED_FLAG = 1<<0,
     * <p>
     * // String pool is encoded in UTF-8
     * UTF8_FLAG = 1<<8
     * };
     * uint32_t flags;
     * <p>
     * // Index from header of the string data.
     * uint32_t stringsStart;
     * <p>
     * // Index from header of the style data.
     * uint32_t stylesStart;
     * };
     */
    public static class ResStringPool_header implements Resource {
        public static int UTF8_FLAG = 1 << 8;
        public static int SORTED_FLAG = 1 << 0;

        ResChunk_header header;
        int stringCount;
        int styleCount;
        int flags;
        int stringStart;
        int stylesStart;

        ResStringPool_header() {
            header = new ResChunk_header();
        }

        @Override
        public int getSize() {
            return header.getSize() + 4 * 5;
        }

        @Override
        public String toString() {
            return "ResStringPool_header{" +
                    "header=" + header.toString() +
                    ", stringCount=" + stringCount +
                    ", styleCount=" + styleCount +
                    ", flags=" + flags +
                    ", stringStart=" + stringStart +
                    ", stylesStart=" + stylesStart +
                    '}';
        }
    }

    /**
     * struct ResTable_package
     * {
     * struct ResChunk_header header;
     * <p>
     * // If this is a base package, its ID.  Package IDs start
     * // at 1 (corresponding to the value of the package bits in a
     * // resource identifier).  0 means this is not a base package.
     * uint32_t id;
     * <p>
     * // Actual name of this package, \0-terminated.
     * uint16_t name[128];
     * <p>
     * // Offset to a ResStringPool_header defining the resource
     * // type symbol table.  If zero, this package is inheriting from
     * // another base package (overriding specific values in it).
     * uint32_t typeStrings;
     * <p>
     * // Last index into typeStrings that is for public use by others.
     * uint32_t lastPublicType;
     * <p>
     * // Offset to a ResStringPool_header defining the resource
     * // key symbol table.  If zero, this package is inheriting from
     * // another base package (overriding specific values in it).
     * uint32_t keyStrings;
     * <p>
     * // Last index into keyStrings that is for public use by others.
     * uint32_t lastPublicKey;
     * <p>
     * uint32_t typeIdOffset;
     * };
     */

    public static class ResTable_package implements Resource {
        ResChunk_header header;
        int id;
        char[] name = new char[128];
        int typeStrings;
        int lastPublicType;
        int keyStrings;
        int lastPublicKey;
        int typeIdOffset;

        ResTable_package() {
            header = new ResChunk_header();
        }

        @Override
        public int getSize() {
            return header.getSize() + 4 + 2 * 128 + 4 * 5;
        }

        @Override
        public String toString() {
            return "ResTable_package{" +
                    "header=" + header.toString() +
                    ", id=" + id +
                    ", name=" + Arrays.toString(name) +
                    ", typeStrings=" + typeStrings +
                    ", lastPublicType=" + lastPublicType +
                    ", keyStrings=" + keyStrings +
                    ", lastPublicKey=" + lastPublicKey +
                    ", typeIdOffset=" + typeIdOffset +
                    '}';
        }
    }

    /**
     * struct ResChunk_header
     * {
     * // Type identifier for this chunk.  The meaning of this value depends
     * // on the containing chunk.
     * uint16_t type;
     * <p>
     * // Size of the chunk header (in bytes).  Adding this value to
     * // the address of the chunk allows you to find its associated data
     * // (if any).
     * uint16_t headerSize;
     * <p>
     * // Total size of this chunk (in bytes).  This is the chunkSize plus
     * // the size of any data associated with the chunk.  Adding this value
     * // to the chunk allows you to completely skip its contents (including
     * // any child chunks).  If this value is the same as chunkSize, there is
     * // no data associated with the chunk.
     * uint32_t size;
     * };
     */
    public static class ResChunk_header implements Resource {
        short type;
        short headerSize;
        int size;

        @Override
        public int getSize() {
            return 2 + 2 + 4;
        }

        @Override
        public String toString() {
            return "ResChunk_header{" +
                    "type=" + type +
                    ", headerSize=" + headerSize +
                    ", size=" + size +
                    '}';
        }
    }

    /**
     * struct ResTable_typeSpec
     * {
     * struct ResChunk_header header;
     * <p>
     * // The type identifier this chunk is holding.  Type IDs start
     * // at 1 (corresponding to the value of the type bits in a
     * // resource identifier).  0 is invalid.
     * uint8_t id;
     * <p>
     * // Must be 0.
     * uint8_t res0;
     * // Must be 0.
     * uint16_t res1;
     * <p>
     * // Number of uint32_t entry configuration masks that follow.
     * uint32_t entryCount;
     * <p>
     * enum {
     * // Additional flag indicating an entry is public.
     * SPEC_PUBLIC = 0x40000000
     * };
     * };
     *
     * @author i
     */
    public static class ResTable_typeSpec implements Resource {

        public final static int SPEC_PUBLIC = 0x40000000;

        public ResChunk_header header;
        public byte id;
        public byte res0;
        public short res1;
        public int entryCount;

        public ResTable_typeSpec() {
            header = new ResChunk_header();
        }

        @Override
        public String toString() {
            return "ResTableTypeSpec{" +
                    "header=" + header.toString() +
                    ", id=" + id +
                    ", res0=" + res0 +
                    ", res1=" + res1 +
                    ", entryCount=" + entryCount +
                    '}';
        }

        @Override
        public int getSize() {
            return header.getSize() + 1 + 1 + 2 + 4;
        }
    }

    /**
     * struct ResTable_type
     * {
     * struct ResChunk_header header;
     * <p>
     * enum {
     * NO_ENTRY = 0xFFFFFFFF
     * };
     * <p>
     * // The type identifier this chunk is holding.  Type IDs start
     * // at 1 (corresponding to the value of the type bits in a
     * // resource identifier).  0 is invalid.
     * uint8_t id;
     * <p>
     * // Must be 0.
     * uint8_t res0;
     * // Must be 0.
     * uint16_t res1;
     * <p>
     * // Number of uint32_t entry indices that follow.
     * uint32_t entryCount;
     * <p>
     * // Offset from header where ResTable_entry data starts.
     * uint32_t entriesStart;
     * <p>
     * // Configuration this collection of entries is designed for.
     * ResTable_config config;
     * };
     *
     * @author i
     */
    public static class ResTable_type implements Resource {

        public ResChunk_header header;

        public final static int NO_ENTRY = 0xFFFFFFFF;

        public byte id;
        public byte res0;
        public short res1;
        public int entryCount;
        public int entriesStart;

        public ResTable_config resConfig;

        public ResTable_type() {
            header = new ResChunk_header();
            resConfig = new ResTable_config();
        }

        @Override
        public int getSize() {
            return header.getSize() + 1 + 1 + 2 + 4 + 4;
        }

        @Override
        public String toString() {
            return "header:" + header.toString() + ",id:" + id + ",res0:" + res0 + ",res1:" + res1 + ",entryCount:" + entryCount + ",entriesStart:" + entriesStart;
        }

    }

    public static class ResTable_config implements Resource {

        //uiMode
        public final static int MASK_UI_MODE_TYPE = 0;
        public final static int UI_MODE_TYPE_ANY = 0x00;
        public final static int UI_MODE_TYPE_NORMAL = 0x01;
        public final static int UI_MODE_TYPE_DESK = 0x02;
        public final static int UI_MODE_TYPE_CAR = 0x03;
        public final static int UI_MODE_TYPE_TELEVISION = 0x04;
        public final static int UI_MODE_TYPE_APPLIANCE = 0x05;
        public final static int UI_MODE_TYPE_WATCH = 0x06;
        public final static int MASK_UI_MODE_NIGHT = 0;
        public final static int SHIFT_UI_MODE_NIGHT = 0;
        public final static int UI_MODE_NIGHT_ANY = 0x00;
        public final static int UI_MODE_NIGHT_NO = 0x01;
        public final static int UI_MODE_NIGHT_YES = 0x02;

        //screenLayout
        public final static int MASK_SCREENSIZE = 0;
        public final static int SCREENSIZE_ANY = 0x00;
        public final static int SCREENSIZE_SMALL = 0x01;
        public final static int SCREENSIZE_NORMAL = 0x02;
        public final static int SCREENSIZE_LARGE = 0x03;
        public final static int SCREENSIZE_XLARGE = 0x04;
        public final static int MASK_SCREENLONG = 0;
        public final static int SHIFT_SCREENLONG = 0;
        public final static int SCREENLONG_ANY = 0x00;
        public final static int SCREENLONG_NO = 0x01;
        public final static int SCREENLONG_YES = 0x02;
        public final static int MASK_LAYOUTDIR = 0;
        public final static int SHIFT_LAYOUTDIR = 0;
        public final static int LAYOUTDIR_ANY = 0x00;
        public final static int LAYOUTDIR_LTR = 0x01;
        public final static int LAYOUTDIR_RTL = 0x02;

        /**
         * uint32_t size;
         */
        int size;

        //运营商信息
    /*
    union {
        struct {
            // Mobile country code (from SIM).  0 means "any".
            uint16_t mcc;
            // Mobile network code (from SIM).  0 means "any".
            uint16_t mnc;
        };
        uint32_t imsi;
    };*/

        //这里使用的是union
        short mcc;
        short mnc;

        int imsi;

        //本地化
    /*union {
        struct {
            char language[2];
            char country[2];
        };
        uint32_t locale;
    };*/
        //这里还是使用的union
        byte[] language = new byte[2];
        byte[] country = new byte[2];

        int locale;

        //屏幕属性
        //这里还是采用union结构
        /**
         * union {
         * struct {
         * uint8_t orientation;
         * uint8_t touchscreen;
         * uint16_t density;
         * };
         * uint32_t screenType;
         * };
         */
        byte orientation;
        byte touchscreen;
        short density;

        int screenType;

        //输入属性
        /**
         * union {
         * struct {
         * uint8_t keyboard;
         * uint8_t navigation;
         * uint8_t inputFlags;
         * uint8_t inputPad0;
         * };
         * uint32_t input;
         * };
         */
        //这里还是采用union结构体
        byte keyboard;
        byte navigation;
        byte inputFlags;
        byte inputPad0;

        int input;

        //屏幕尺寸
        /**
         * union {
         * struct {
         * uint16_t screenWidth;
         * uint16_t screenHeight;
         * };
         * uint32_t screenSize;
         * };
         */
        //这里还是采用union结构体
        short screenWidth;
        short screenHeight;

        int screenSize;

        //系统版本
        /**
         * union {
         * struct {
         * uint16_t sdkVersion;
         * // For now minorVersion must always be 0!!!  Its meaning
         * // is currently undefined.
         * uint16_t minorVersion;
         * };
         * uint32_t version;
         * };
         */
        //这里还是采用union结构体
        short sdVersion;
        short minorVersion;

        int version;

        //屏幕配置
        /**
         * union {
         * struct {
         * uint8_t screenLayout;
         * uint8_t uiMode;
         * uint16_t smallestScreenWidthDp;
         * };
         * uint32_t screenConfig;
         * };
         */
        //这里还是采用union结构体
        byte screenLayout;
        byte uiMode;
        short smallestScreenWidthDp;

        int screenConfig;

        //屏幕尺寸
        /**
         * union {
         * struct {
         * uint16_t screenWidthDp;
         * uint16_t screenHeightDp;
         * };
         * uint32_t screenSizeDp;
         * };
         */
        //这里还是采用union结构体
        short screenWidthDp;
        short screenHeightDp;

        int screenSizeDp;

        /**
         * char localeScript[4];
         * char localeVariant[8];
         */
        byte[] localeScript = new byte[4];
        byte[] localeVariant = new byte[8];

        @Override
        public int getSize() {
            return 48;
        }

        @Override
        public String toString() {
            return "size:" + size + ",mcc=" + mcc + ",locale:" + locale + ",screenType:" + screenType + ",input:" +
                    input + ",screenSize:" + screenSize + ",version:" + version + ",sdkVersion:" + sdVersion + ",minVersion:" + minorVersion
                    + ",screenConfig:" + screenConfig + ",screenLayout:" + screenLayout + ",uiMode:" + uiMode +
                    ",smallestScreenWidthDp:" + smallestScreenWidthDp + ",screenSizeDp:" + screenSizeDp;
        }
    }

    public static class ResTable_entry implements Resource {
        short size;
        short flags;
        ResStringPool_ref key;

        ResTable_entry() {
            key = new ResStringPool_ref();
        }

        @Override
        public int getSize() {
            return 2 + 2 + key.getSize();
        }

        @Override
        public String toString() {
            return "ResTable_entry{" +
                    "size=" + size +
                    ", flags=" + flags +
                    ", key=" + key.toString() +
                    '}';
        }

        public interface Flag {
            // If set, this is a complex entry, holding a set of name/value
            // mappings.  It is followed by an array of ResTable_map structures.
            int FLAG_COMPLEX = 0x0001,
            // If set, this resource has been declared public, so libraries
            // are allowed to reference it.
            FLAG_PUBLIC = 0x0002,
            // If set, this is a weak resource and may be overriden by strong
            // resources of the same name/type. This is only useful during
            // linking with other resource tables.
            FLAG_WEAK = 0x0004;
        }
    }

    public static class ResStringPool_ref implements Resource {
        int index;

        @Override
        public int getSize() {
            return 4;
        }

        @Override
        public String toString() {
            return "ResStringPool_ref{" +
                    "index=" + index +
                    '}';
        }
    }

    public static class Res_value implements Resource {
        short size;
        byte res0;
        byte dataType;
        int data;

        @Override
        public int getSize() {
            return 2 + 1 + 1 + 4;
        }

        @Override
        public String toString() {
            return "Res_value{" +
                    "size=" + size +
                    ", res0=" + res0 +
                    ", dataType=" + dataType +
                    ", data=" + data +
                    '}';
        }

        public interface Type {
            int TYPE_NULL = 0x00,
            // The 'data' holds a ResTable_ref, a reference to another resource
            // table entry.
            TYPE_REFERENCE = 0x01,
            // The 'data' holds an attribute resource identifier.
            TYPE_ATTRIBUTE = 0x02,
            // The 'data' holds an index into the containing resource table's
            // global value string pool.
            TYPE_STRING = 0x03,
            // The 'data' holds a single-precision floating point number.
            TYPE_FLOAT = 0x04,
            // The 'data' holds a complex number encoding a dimension value,
            // such as "100in".
            TYPE_DIMENSION = 0x05,
            // The 'data' holds a complex number encoding a fraction of a
            // container.
            TYPE_FRACTION = 0x06,
            // The 'data' holds a dynamic ResTable_ref, which needs to be
            // resolved before it can be used like a TYPE_REFERENCE.
            TYPE_DYNAMIC_REFERENCE = 0x07,
            // The 'data' holds an attribute resource identifier, which needs to be resolved
            // before it can be used like a TYPE_ATTRIBUTE.
            TYPE_DYNAMIC_ATTRIBUTE = 0x08,

            // Beginning of integer flavors...
            TYPE_FIRST_INT = 0x10,

            // The 'data' is a raw integer value of the form n..n.
            TYPE_INT_DEC = 0x10,
            // The 'data' is a raw integer value of the form 0xn..n.
            TYPE_INT_HEX = 0x11,
            // The 'data' is either 0 or 1, for input "false" or "true" respectively.
            TYPE_INT_BOOLEAN = 0x12,

            // Beginning of color integer flavors...
            TYPE_FIRST_COLOR_INT = 0x1c,

            // The 'data' is a raw integer value of the form #aarrggbb.
            TYPE_INT_COLOR_ARGB8 = 0x1c,
            // The 'data' is a raw integer value of the form #rrggbb.
            TYPE_INT_COLOR_RGB8 = 0x1d,
            // The 'data' is a raw integer value of the form #argb.
            TYPE_INT_COLOR_ARGB4 = 0x1e,
            // The 'data' is a raw integer value of the form #rgb.
            TYPE_INT_COLOR_RGB4 = 0x1f,

            // ...end of integer flavors.
            TYPE_LAST_COLOR_INT = 0x1f,

            // ...end of integer flavors.
            TYPE_LAST_INT = 0x1f;
        }

        public interface ComplexData {
            int // Where the unit type information is.  This gives us 16 possible
                    // types, as defined below.
                    COMPLEX_UNIT_SHIFT = 0,
                    COMPLEX_UNIT_MASK = 0xf,

            // TYPE_DIMENSION: Value is raw pixels.
            COMPLEX_UNIT_PX = 0,
            // TYPE_DIMENSION: Value is Device Independent Pixels.
            COMPLEX_UNIT_DIP = 1,
            // TYPE_DIMENSION: Value is a Scaled device independent Pixels.
            COMPLEX_UNIT_SP = 2,
            // TYPE_DIMENSION: Value is in points.
            COMPLEX_UNIT_PT = 3,
            // TYPE_DIMENSION: Value is in inches.
            COMPLEX_UNIT_IN = 4,
            // TYPE_DIMENSION: Value is in millimeters.
            COMPLEX_UNIT_MM = 5,

            // TYPE_FRACTION: A basic fraction of the overall size.
            COMPLEX_UNIT_FRACTION = 0,
            // TYPE_FRACTION: A fraction of the parent size.
            COMPLEX_UNIT_FRACTION_PARENT = 1,

            // Where the radix information is, telling where the decimal place
            // appears in the mantissa.  This give us 4 possible fixed point
            // representations as defined below.
            COMPLEX_RADIX_SHIFT = 4,
                    COMPLEX_RADIX_MASK = 0x3,

            // The mantissa is an integral number -- i.e., 0xnnnnnn.0
            COMPLEX_RADIX_23p0 = 0,
            // The mantissa magnitude is 16 bits -- i.e, 0xnnnn.nn
            COMPLEX_RADIX_16p7 = 1,
            // The mantissa magnitude is 8 bits -- i.e, 0xnn.nnnn
            COMPLEX_RADIX_8p15 = 2,
            // The mantissa magnitude is 0 bits -- i.e, 0x0.nnnnnn
            COMPLEX_RADIX_0p23 = 3,

            // Where the actual value is.  This gives us 23 bits of
            // precision.  The top bit is the sign.
            COMPLEX_MANTISSA_SHIFT = 8,
                    COMPLEX_MANTISSA_MASK = 0xffffff;
        }
    }

    public static class ResTable_map_entry extends ResTable_entry {
        ResTable_ref parent;
        int count;

        public ResTable_map_entry() {
            parent = new ResTable_ref();
        }

        @Override
        public int getSize() {
            return super.getSize() + parent.getSize() + 4;
        }

        @Override
        public String toString() {
            return "ResTable_map_entry{" +
                    "parent=" + parent +
                    ", count=" + count +
                    '}';
        }
    }

    public static class ResTable_ref implements Resource {
        int ident;

        @Override
        public int getSize() {
            return 4;
        }

        @Override
        public String toString() {
            return "ResTable_ref{" +
                    "ident=" + ident +
                    '}';
        }
    }

    public static class ResTable_map implements Resource {
        ResTable_ref name;
        Res_value value;

        public ResTable_map() {
            name = new ResTable_ref();
            value = new Res_value();
        }

        @Override
        public int getSize() {
            return name.getSize() + value.getSize();
        }

        @Override
        public String toString() {
            return "ResTable_map{" +
                    "name=" + name.toString() +
                    ", value=" + value.toString() +
                    '}';
        }
    }
}
