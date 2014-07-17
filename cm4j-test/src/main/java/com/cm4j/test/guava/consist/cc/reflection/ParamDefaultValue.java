package com.cm4j.test.guava.consist.cc.reflection;

/**
 * Created by yanghao on 14-7-15.
 */
public enum ParamDefaultValue {
    CHAR(char.class){
        @Override
        public Object translate(String param) {
            return param.charAt(0);
        }
    },
    STRING(String.class){
        @Override
        public Object translate(String param) {
            return param;
        }
    },
    BYTE0(byte.class) {
        @Override
        public Object translate(String param) {
            return new Byte(param);
        }
    },
    BYTE(Byte.class) {
        @Override
        public Object translate(String param) {
            return new Byte(param);
        }
    },

    SHORT0(short.class) {
        @Override
        public Object translate(String param) {
            return new Short(param);
        }
    },
    SHORT(Short.class) {
        @Override
        public Object translate(String param) {
            return new Short(param);
        }
    },

    INT0(int.class) {
        @Override
        public Object translate(String param) {
            return new Integer(param);
        }
    },
    INT(Integer.class) {
        @Override
        public Object translate(String param) {
            return new Integer(param);
        }
    },

    LONG0(long.class) {
        @Override
        public Object translate(String param) {
            return new Long(param);
        }
    },
    LONG(Long.class) {
        @Override
        public Object translate(String param) {
            return new Long(param);
        }
    },
    ;

    private Class clazz;

    ParamDefaultValue(Class clazz) {
        this.clazz = clazz;
    }

    public abstract Object translate(String param);

    public static ParamDefaultValue get(Class clazz) {
        ParamDefaultValue[] values = ParamDefaultValue.values();
        for (ParamDefaultValue value : values) {
            if (value.clazz.isAssignableFrom(clazz)) {
                return value;
            }
        }
        return null;
    }

    static {
//        defaultValMap.put(char.class, 'x');
//        defaultValMap.put(String.class, "");
//        defaultValMap.put(byte.class, 0);
//        defaultValMap.put(Byte.class, 0);
//        defaultValMap.put(short.class, 0);
//        defaultValMap.put(Short.class, 0);
//        defaultValMap.put(int.class, 0);
//        defaultValMap.put(Integer.class, 0);
//        defaultValMap.put(long.class, 0);
//        defaultValMap.put(Long.class, 0);
    }
}
