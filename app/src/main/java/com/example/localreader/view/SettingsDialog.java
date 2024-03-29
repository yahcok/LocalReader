package com.example.localreader.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.localreader.R;
import com.example.localreader.entity.Config;
import com.example.localreader.listener.SettingsListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Created by xialijuan on 2021/01/05.
 */
public class SettingsDialog extends Dialog implements View.OnClickListener {

    private final String TAG = "SettingsDialog";
    private Config config;
    private int fontSizeMin;
    private int fontSizeMax;
    private int currentFontSize;
    private SeekBar brightnessSb;
    private TextView showSizeTv;
    private SettingsListener mSettingsListener;

    public SettingsDialog(@NonNull Context context) {
        this(context, R.style.read_setting_popup);
    }

    public SettingsDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setGravity(Gravity.BOTTOM);
        setContentView(R.layout.popup_settings_layout);

        fontSizeMin = (int) getContext().getResources().getDimension(R.dimen.read_min_text_size);
        fontSizeMax = (int) getContext().getResources().getDimension(R.dimen.read_max_text_size);

        config = Config.getInstance();

        initView();

        // 初始化进度条的位置
        changeBrightnessProgress((int) (config.getLight() * 100));

        // 初始化字体大小
        currentFontSize = (int) config.getFontSize();
        showSizeTv.setText(String.valueOf(currentFontSize));

        // 拖动亮度进度条使数据和进度条位置一样
        brightnessSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                changeBrightnessProgress(progress);
                Log.d(TAG, progress + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /**
     * 设置背景
     *
     * @param bg 背景颜色
     */
    private void setBookBg(int bg) {
        config.setBookBg(bg);
        if (mSettingsListener != null) {
            mSettingsListener.changeBookBg(bg);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_less_font_size:
                lessFontSize();
                break;
            case R.id.tv_more_font_size:
                addFontSize();
                break;
            case R.id.fb_bg_white:
                setBookBg(Config.BOOK_BG_WHITE);
                break;
            case R.id.fb_bg_yellow:
                setBookBg(Config.BOOK_BG_YELLOW);
                break;
            case R.id.fb_bg_gray:
                setBookBg(Config.BOOK_BG_GRAY);
                break;
            case R.id.fb_bg_green:
                setBookBg(Config.BOOK_BG_GREEN);
                break;
            case R.id.fb_bg_blue:
                setBookBg(Config.BOOK_BG_BLUE);
                break;
            default:
                break;
        }
    }

    /**
     * 变大书本字号
     */
    private void addFontSize() {
        if (currentFontSize < fontSizeMax) {
            currentFontSize += 1;
            showSizeTv.setText(String.valueOf(currentFontSize));
            config.setFontSize(currentFontSize);
            if (mSettingsListener != null) {
                mSettingsListener.changeFontSize(currentFontSize);
            }
        }
    }

    /**
     * 变小书本字号
     */
    private void lessFontSize() {
        if (currentFontSize > fontSizeMin) {
            currentFontSize -= 1;
            showSizeTv.setText(String.valueOf(currentFontSize));
            config.setFontSize(currentFontSize);
            if (mSettingsListener != null) {
                mSettingsListener.changeFontSize(currentFontSize);
            }
        }
    }

    /**
     * 改变亮度进度条位置
     *
     * @param brightness 进度条的值
     */
    private void changeBrightnessProgress(int brightness) {
        Log.d("brightness", brightness + "");
        brightnessSb.setProgress(brightness);
        float light = (float) (brightness / 100.0);
        config.setLight(light);
        if (mSettingsListener != null) {
            mSettingsListener.changeLight(light);
        }
    }

    public void setSettingListener(SettingsListener settingsListener) {
        this.mSettingsListener = settingsListener;
    }

    private void initView() {
        brightnessSb = findViewById(R.id.sb_brightness);
        showSizeTv = findViewById(R.id.tv_show_font_size);
        TextView lessSizeTv = findViewById(R.id.tv_less_font_size);
        TextView moreSizeTv = findViewById(R.id.tv_more_font_size);
        FloatingActionButton whiteBgFb = findViewById(R.id.fb_bg_white);
        FloatingActionButton yellowBgFb = findViewById(R.id.fb_bg_yellow);
        FloatingActionButton grayBgFb = findViewById(R.id.fb_bg_gray);
        FloatingActionButton greenBgFb = findViewById(R.id.fb_bg_green);
        FloatingActionButton blueBgFb = findViewById(R.id.fb_bg_blue);

        lessSizeTv.setOnClickListener(this);
        moreSizeTv.setOnClickListener(this);
        whiteBgFb.setOnClickListener(this);
        yellowBgFb.setOnClickListener(this);
        grayBgFb.setOnClickListener(this);
        greenBgFb.setOnClickListener(this);
        blueBgFb.setOnClickListener(this);
    }
}
