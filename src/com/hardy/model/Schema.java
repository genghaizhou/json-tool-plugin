package com.hardy.model;

/**
 * Author: Hardy
 * Date:   2019/2/25
 * Description:
 **/
public class Schema {
    private String type;

    private KV properties;

    private Schema items;

    private String description;


    public Schema() {
    }

    public Schema(SchemaType type) {
        this.type = type.val;
    }


    public static Schema createObject() {
        Schema schema = new Schema();
        schema.setType(SchemaType.OBJECT.val);
        schema.setProperties(KV.create());
        return schema;
    }

    public static Schema createArray(Schema items) {
        Schema schema = new Schema();
        schema.setType(SchemaType.ARRAY.val);
        schema.setItems(items);
        return schema;
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
}
