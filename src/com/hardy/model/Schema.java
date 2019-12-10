package com.hardy.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Author: Hardy
 * Date:   2019/2/25
 * Description:
 **/
public class Schema<T> {
    // ************** base **************
    private String type;

    private KV properties;

    private Schema items;

    private String description;

    private List<String> required;
    // ***********************************

    // string 约束
    private Integer minLength; // size > ?
    private Integer maxLength; // size < ?
    private String pattern;   // 正则

    //  约束

    // integer / number 约束
    private T minimum; // >= ?
    private T maximum; // <= ?
    private Boolean exclusiveMinimum; // 是否排除最小值 ?
    private Boolean exclusiveMaximum; // 是否排除最大值 ?


    public static Schema createObject() {
        Schema schema = new Schema<>();
        schema.setType(SchemaType.OBJECT.val);
        schema.setProperties(KV.create());
        return schema;
    }

    public static Schema createArray(Schema items) {
        Schema schema = new Schema<>();
        schema.setType(SchemaType.ARRAY.val);
        schema.setItems(items);
        return schema;
    }

    public static Schema createBasic(String typeName) {
        String type = typeName.toLowerCase();

        if ("boolean".equals(type))
            return new Schema<>(SchemaType.BOOLEAN);
        else if (Arrays.asList("char", "string", "bigdecimal", "date").contains(type))
            return new Schema<>(SchemaType.STRING);
        else if (Arrays.asList("byte", "short", "int", "long", "integer", "biginteger").contains(type))
            return new Schema<Integer>(SchemaType.INTEGER);
        else
            return new Schema<Double>(SchemaType.NUMBER);
    }

    public Schema() {
    }

    private Schema(SchemaType type) {
        this.type = type.val;
    }

    public void addRequire(String fieldName) {
        if (required == null)
            required = new ArrayList<>();

        required.add(fieldName);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public KV getProperties() {
        return properties;
    }

    public void setProperties(KV properties) {
        this.properties = properties;
    }

    public Schema getItems() {
        return items;
    }

    public void setItems(Schema items) {
        this.items = items;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public T getMinimum() {
        return minimum;
    }

    public void setMinimum(T minimum) {
        this.minimum = minimum;
    }

    public T getMaximum() {
        return maximum;
    }

    public void setMaximum(T maximum) {
        this.maximum = maximum;
    }

    public boolean getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public void setExclusiveMinimum(boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public boolean getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public void setExclusiveMaximum(boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }
}
