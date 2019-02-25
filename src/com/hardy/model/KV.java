package com.hardy.model;

import java.util.LinkedHashMap;

/**
 * Author: Hardy
 * Date:   2019/2/22
 * Description:
 **/
public class KV extends LinkedHashMap<String, Object> {

    public static KV create() {
        return new KV();
    }

    public KV set(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
