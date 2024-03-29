package com.example.localreader;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.localreader.entity.Book;
import com.example.localreader.entity.Bookmark;
import com.example.localreader.entity.Config;
import com.example.localreader.listener.PageListener;
import com.example.localreader.listener.SettingsListener;
import com.example.localreader.listener.TouchListener;
import com.example.localreader.util.PageFactory;
import com.example.localreader.view.PageView;
import com.example.localreader.view.SettingsDialog;
import com.google.android.material.appbar.AppBarLayout;

import org.litepal.LitePal;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by xialijuan on 2020/12/22.
 */
public class ReadActivity extends AppCompatActivity implements View.OnClickListener {

    private Config config;
    private Book book;
    private PageFactory pageFactory;
    /**
     * 是否隐藏底部菜单栏和toolbar
     */
    private boolean hide = false;
    /**
     * 夜间模式
     */
    private boolean night;
    private SettingsDialog settingsDetail;
    private PageView bookPage;
    private TextView showProgressTv;
    private RelativeLayout showProgressRl;
    private SeekBar chapterProgressSb;
    private TextView dayOrNightTv;
    private RelativeLayout readBottomRl;
    private Toolbar toolbar;
    private AppBarLayout appbar;
    private ImageView dayOrNightIv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        initView();
        initData();
    }

    private void initData() {
        Intent intent = getIntent();
        book = (Book) intent.getSerializableExtra("book_data");

        toolbar.setTitle(book.getBookName().split(".txt")[0]);
        // 一定要放在setNavigationOnClickListener的前面，否则点击事件不会被响应
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_title_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        config = Config.getInstance();
        pageFactory = PageFactory.getInstance();

        settingsDetail = new SettingsDialog(this);
        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //初始化屏幕亮度
        if (config.getLight() == 0) {
            float light = (float) ((100 * pageFactory.getBrightness(this)) / 255.0);
            config.setLight((float) (light * 1.0 / 100));
        }
        pageFactory.changeBrightness(this, config.getLight());
        //隐藏
        hideSystemUi();

        pageFactory.setPageWidget(bookPage);

        try {
            pageFactory.openBook(book, ReadActivity.this);
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.read_load_fail), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        settingsDetail.setOnCancelListener(mOnCancelListener);
        settingsDetail.setSettingListener(mSettingsListener);
        pageFactory.setPageListener(mPageListener);
        bookPage.setTouchListener(mTouchListener);
        initDayOrNight();
    }

    DialogInterface.OnCancelListener mOnCancelListener = dialog -> hideSystemUi();

    SettingsListener mSettingsListener = new SettingsListener() {
        @Override
        public void changeLight(float brightness) {
            pageFactory.changeBrightness(ReadActivity.this, brightness);
        }

        @Override
        public void changeFontSize(int fontSize) {
            pageFactory.changeFontSize(fontSize);
        }

        @Override
        public void changeBookBg(int type) {
            pageFactory.changeBookBg(type);
        }
    };

    PageListener mPageListener = new PageListener() {
        @Override
        public void changeProgress(float progress) {
            Message message = new Message();
            message.what = 1;
            message.obj = progress;
            mHandler.sendMessage(message);
        }
    };


    private Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                float progress = (float) msg.obj;
                setSeekBarProgress(progress);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!hide) {
            hideSystemUi();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pageFactory.initData();
        bookPage = null;
    }

    TouchListener mTouchListener = new TouchListener() {
        @Override
        public void center() {
            if (hide) {
                hideReadSetting();
            } else {
                showReadSetting();
            }
        }

        @Override
        public boolean upPage() {
            if (hide) {
                return false;
            }
            pageFactory.upPage();
            return !pageFactory.isFirstPage();
        }

        @Override
        public boolean nextPage() {
            if (hide) {
                return false;
            }
            pageFactory.nextPage();
            return !pageFactory.isLastPage();
        }

        @Override
        public void cancel() {
            pageFactory.cancelPage();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.read_title_add_bookmark, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        // 添加书签
        if (id == R.id.action_add_bookmark) {
            if (pageFactory.getCurrentPage() != null) {
                List<Bookmark> bookmarkList = LitePal.where("bookPath = ? and firstIndex = ?", pageFactory.getBookPath(), pageFactory.getCurrentPage().getFirstIndex() + "").find(Bookmark.class);

                if (!bookmarkList.isEmpty()) {
                    Toast.makeText(ReadActivity.this, "该书签已存在", Toast.LENGTH_SHORT).show();
                } else {
                    Bookmark bookmark = new Bookmark();
                    StringBuilder word = new StringBuilder();
                    for (String line : pageFactory.getCurrentPage().getLines()) {
                        word.append(line);
                    }
                    try {
                        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm ss");
                        String time = sf.format(new Date());
                        bookmark.setTime(time);
                        bookmark.setFirstIndex(pageFactory.getCurrentPage().getFirstIndex());
                        bookmark.setPartContent(word.toString());
                        bookmark.setBookPath(pageFactory.getBookPath());
                        bookmark.save();

                        Toast.makeText(ReadActivity.this, "书签添加成功", Toast.LENGTH_SHORT).show();
                    } catch (SQLException e) {
                        Toast.makeText(ReadActivity.this, "该书签已存在", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(ReadActivity.this, "添加书签失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 沉浸式阅读
     */
    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void showSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }


    float pro;

    SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            pro = (float) (progress / 10000.0);
            showProgress(pro);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            pageFactory.changeProgress(pro);
        }
    };

    /**
     * 拖动进度条时显示书本进度
     *
     * @param progress 书本进度
     */
    public void showProgress(float progress) {
        if (showProgressRl.getVisibility() != View.VISIBLE) {
            showProgressRl.setVisibility(View.VISIBLE);
        }
        setProgress(progress);
    }

    public void initDayOrNight() {
        night = config.isNight();
        if (night) {
            dayOrNightIv.setImageResource(R.drawable.read_day_mode);
            dayOrNightTv.setText(getResources().getString(R.string.read_day_mode));
        } else {
            dayOrNightIv.setImageResource(R.drawable.read_night_mode);
            dayOrNightTv.setText(getResources().getString(R.string.read_night_mode));
        }
    }

    /**
     * 改变显示模式
     */
    public void changeDayOrNight() {
        if (night) {
            night = false;
            dayOrNightTv.setText(getResources().getString(R.string.read_night_mode));
            dayOrNightIv.setImageResource(R.drawable.read_night_mode);
        } else {
            night = true;
            dayOrNightTv.setText(getResources().getString(R.string.read_day_mode));
            dayOrNightIv.setImageResource(R.drawable.read_day_mode);
        }
        config.setNight(night);
        pageFactory.setDayOrNight(night);
    }

    private void setProgress(float progress) {
        DecimalFormat decimalFormat = new DecimalFormat("00.00");
        String p = decimalFormat.format(progress * 100.0);
        showProgressTv.setText(String.format(getString(R.string.read_progress), p));
    }

    public void setSeekBarProgress(float progress) {
        chapterProgressSb.setProgress((int) (progress * 10000));
    }

    /**
     * 显示功能栏动画
     */
    private void showReadSetting() {
        hide = true;
        showProgressRl.setVisibility(View.GONE);
        showSystemUi();
        Animation bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_in);
        Animation topAnim = AnimationUtils.loadAnimation(this, R.anim.top_in);
        readBottomRl.startAnimation(bottomAnim);
        appbar.startAnimation(topAnim);
        readBottomRl.setVisibility(View.VISIBLE);
        appbar.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏功能栏动画
     */
    private void hideReadSetting() {
        hide = false;
        Animation bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_out);
        Animation topAnim = AnimationUtils.loadAnimation(this, R.anim.top_out);
        if (readBottomRl.getVisibility() == View.VISIBLE) {
            readBottomRl.startAnimation(bottomAnim);
        }
        if (appbar.getVisibility() == View.VISIBLE) {
            appbar.startAnimation(topAnim);
        }
        readBottomRl.setVisibility(View.GONE);
        appbar.setVisibility(View.GONE);
        hideSystemUi();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_read_up_chapter:
                pageFactory.preChapter();
                break;
            case R.id.tv_read_next_chapter:
                pageFactory.nextChapter();
                break;
            case R.id.tv_read_catalog:
                Intent intent = new Intent(ReadActivity.this, CatalogActivity.class);
                intent.putExtra("book_data", book);
                startActivity(intent);
                break;
            case R.id.ll_read_day_or_night:
                changeDayOrNight();
                break;
            case R.id.ll_read_setting:
                hideReadSetting();
                settingsDetail.show();
                break;
            default:
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (hide) {
                hideReadSetting();
                return true;
            }
            if (settingsDetail.isShowing()) {
                settingsDetail.hide();
                return true;
            }

            //退出阅读前保存阅读进度
            List<Book> books = LitePal.where("bookPath = ?", pageFactory.getBookPath()).find(Book.class);
            if (books.size() == 1) {
                Book openedBook = books.get(0);
                openedBook.setProgress(pageFactory.getProgress());
                openedBook.save();
            }
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initView() {
        bookPage = findViewById(R.id.bookPage);
        showProgressTv = findViewById(R.id.tv_show_progress);
        showProgressRl = findViewById(R.id.rl_show_progress);
        TextView upTv = findViewById(R.id.tv_read_up_chapter);
        chapterProgressSb = findViewById(R.id.sb_chapter_progress);
        TextView nextTv = findViewById(R.id.tv_read_next_chapter);
        LinearLayout catalogTv = findViewById(R.id.tv_read_catalog);
        LinearLayout dayOrNightLayout = findViewById(R.id.ll_read_day_or_night);
        dayOrNightTv = findViewById(R.id.tv_read_day_or_night);
        dayOrNightIv = findViewById(R.id.iv_read_day_or_night);
        LinearLayout settingLayout = findViewById(R.id.ll_read_setting);
        readBottomRl = findViewById(R.id.rl_read_bottom);
        toolbar = findViewById(R.id.toolbar);
        appbar = findViewById(R.id.appbar);

        chapterProgressSb.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        upTv.setOnClickListener(this);
        nextTv.setOnClickListener(this);
        catalogTv.setOnClickListener(this);
        dayOrNightLayout.setOnClickListener(this);
        settingLayout.setOnClickListener(this);
    }
}