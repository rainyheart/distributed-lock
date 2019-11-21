package org.rainyheart.distributed.lock.api.annotation;

import com.alibaba.fastjson.JSON;

public class JsonBody {
    private String key;
    private String value;

    public JsonBody() {
        super();
    }

    public JsonBody(String key, String value) {
        super();
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        String json = JSON.toJSONString(this);
        return json;
    }
}
