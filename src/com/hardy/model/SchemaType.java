package com.hardy.model;

/**
 * Author: Hardy
 * Date:   2019/2/25
 * Description:
 **/
public enum SchemaType {

    BOOLEAN("boolean"),
    INTEGER("integer"),
    NUMBER("number"),
    STRING("string"),
    ARRAY("array"),
    OBJECT("object");


    public final String val;


    SchemaType(String val) {
        this.val = val;
    }
}
