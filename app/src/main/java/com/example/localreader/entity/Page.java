package com.example.localreader.entity;

import java.util.List;

/**
 * @author xialijuan
 * @date 2021/1/9
 */
public class Page {
    private long begin;
    private long end;
    private List<String> lines;

    public long getBegin() {
        return begin;
    }

    public void setBegin(long begin) {
        this.begin = begin;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }
}