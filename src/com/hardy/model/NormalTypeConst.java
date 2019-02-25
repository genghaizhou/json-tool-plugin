package com.hardy.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Hardy
 * Date:   2019/2/25
 * Description:
 **/
public class NormalTypeConst {

    private static final Map<String, Object> normalTypes = new HashMap<>();

    static {
        normalTypes.put("Boolean", false);
        normalTypes.put("Byte", 0);
        normalTypes.put("Short", 0);
        normalTypes.put("Integer", 0);
        normalTypes.put("Long", 0);
        normalTypes.put("Float", 0.0);
        normalTypes.put("Double", 0.0);
        normalTypes.put("String", "");
        normalTypes.put("BigDecimal", "0.0");
        normalTypes.put("Date", "");
    }

    public static boolean isNormalType(String typeName) {
        return normalTypes.containsKey(typeName);
    }

    public static Object get(String typeName) {
        return normalTypes.get(typeName);
    }
}
