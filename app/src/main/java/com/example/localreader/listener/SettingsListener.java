package com.example.localreader.listener;

/**
 * @author xialijuan
 * @date 2021/01/05
 */
public interface SettingsListener {
    /**
     * 改变亮度
     *
     * @param brightness 亮度值
     */
    void changeSystemBright(float brightness);

    /**
     * 改变字号
     *
     * @param fontSize 字号大小
     */
    void changeFontSize(int fontSize);

    /**
     * 换读书背景
     *
     * @param bg 背景颜色
     */
    void changeBookBg(int bg);
}