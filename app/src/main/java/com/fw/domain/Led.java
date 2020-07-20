package com.fw.domain;

import java.io.Serializable;

/**
 * @author yqf
 */
public class Led implements Serializable {

    private Integer index;
    private Boolean status;

    public Led(Integer index, Boolean status) {
        this.index = index;
        this.status = status;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Led{" +
                "index=" + index +
                ", status=" + status +
                '}';
    }
}
