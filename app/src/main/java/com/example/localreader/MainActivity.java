package com.example.localreader;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localreader.adapter.BookShelfAdapter;
import com.example.localreader.entity.Book;
import com.example.localreader.entity.Config;
import com.example.localreader.util.BookShelfUtil;
import com.example.localreader.util.BookUtil;
import com.example.localreader.util.FileUtil;
import com.example.localreader.util.PageFactory;

import org.litepal.LitePal;

import java.io.File;
import java.util.List;

/**
 * @author xialijuan
 * @date 2020/12/05
 */
public class MainActivity extends AppCompatActivity {

    private List<Book> books;
    private BookShelfAdapter adapter;
    private RecyclerView bookShelfRv;
    private LinearLayout bottomLayout;
    private LinearLayout deleteLayout;
    private LinearLayout cancelLayout;
    private LinearLayout selectLayout;
    private LinearLayout detailLayout;
    private ImageView deleteImg;
    private TextView deleteTv;
    private ImageView selectImg;
    private TextView selectTv;
    private ImageView detailImg;
    private TextView detailTv;
    private PopupWindow popupWindow;
    private TextView filePathTv;
    private TextView fileSizeTv;
    private TextView fileTimeTv;
    private View view;
    private LayoutInflater inflater;
    private boolean isHideBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //创建LitePal数据库
        LitePal.getDatabase();
        Config.createConfig(this);
        PageFactory.createPageFactory(this);

        //申请读取文件权限
        verifyStoragePermissions(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //引入popup_window_book_detail布局，否则弹出详细信息时，会找不到子控件
        inflater = getLayoutInflater();
        view = inflater.inflate(R.layout.popup_window_book_detail, null);

        setTitle(getString(R.string.main_toolbar_title));

        initViews();
        initData();
    }

    private void initViews() {
        bookShelfRv = findViewById(R.id.rv_book_shelf);
        bottomLayout = findViewById(R.id.ll_main_bottom);

        deleteLayout = findViewById(R.id.ll_main_bottom_delete);
        cancelLayout = findViewById(R.id.ll_main_bottom_cancel);
        selectLayout = findViewById(R.id.ll_main_bottom_select_all);
        detailLayout = findViewById(R.id.ll_main_bottom_detail);

        deleteImg = findViewById(R.id.iv_main_bottom_delete);
        deleteTv = findViewById(R.id.tv_main_bottom_delete);

        selectImg = findViewById(R.id.iv_main_bottom_select_all);
        selectTv = findViewById(R.id.tv_main_bottom_select_all);

        detailImg = findViewById(R.id.iv_main_bottom_detail);
        detailTv = findViewById(R.id.tv_main_bottom_detail);

        filePathTv = view.findViewById(R.id.tv_file_path);
        fileSizeTv = view.findViewById(R.id.tv_file_size);
        fileTimeTv = view.findViewById(R.id.tv_file_time);

        deleteLayout.setOnClickListener(mDeleteBookListener);
        cancelLayout.setOnClickListener(mCancelListener);
        selectLayout.setOnClickListener(mSelectListener);
        detailLayout.setOnClickListener(mDetailListener);
    }

    private void initData() {
        books = LitePal.findAll(Book.class);
        bookShelfRv.setLayoutManager(new GridLayoutManager(MainActivity.this, 3));
        adapter = new BookShelfAdapter(books, this);

        bookShelfRv.setAdapter(adapter);

        adapter.setOnItemClickListener(mOnItemClickListener);
        adapter.setItemLongClickListener(mItemLongClickListener);
        adapter.setOnItemClickSelectListener(mItemSelectedClickListener);
        adapter.setImportListener(mImportListener);
    }

    private void changeBtnState() {
        // 选中个数
        int selectSize = adapter.getSelectSize();
        // 可选的最大个数
        int maxSize = books.size();
        if (selectSize == maxSize && selectSize != 0) {
            selectTv.setText(getResources().getString(R.string.main_no_select_all));
            selectImg.setImageResource(R.drawable.main_bottom_select_all);
        } else if (books.size() == 0) {
            selectImg.setImageResource(R.drawable.main_bottom_no_select);
            selectTv.setTextColor(ContextCompat.getColor(this, R.color.main_bottom_freeze_color));
        } else {
            selectTv.setText(getResources().getString(R.string.main_select_all));
            selectImg.setImageResource(R.drawable.main_bottom_select_all_cancel);
        }

        if (selectSize == 0) {
            deleteImg.setImageResource(R.drawable.main_bottom_no_delete);
            deleteTv.setTextColor(ContextCompat.getColor(this, R.color.main_bottom_freeze_color));
        } else {
            deleteImg.setImageResource(R.drawable.main_bottom_delete);
            deleteTv.setTextColor(Color.BLACK);
        }

        if (selectSize == 1) {
            detailImg.setImageResource(R.drawable.main_bottom_detail);
            detailTv.setTextColor(Color.BLACK);
        } else {
            detailImg.setImageResource(R.drawable.main_bottom_no_detail);
            detailTv.setTextColor(ContextCompat.getColor(this, R.color.main_bottom_freeze_color));
        }
    }

    private View.OnClickListener mDeleteBookListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (adapter.getSelectSize() == 0) {
                return;
            }
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setTitle("警告");
            dialog.setMessage("确认删除选中的书籍？");
            dialog.setCancelable(false);//设置为false时，点击返回键无效
            dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    List<Book> selectBooks = adapter.getSelectBook();
                    if (books != null) {
                        books.clear();
                        BookUtil.deleteBookmarks(selectBooks);
                        books.addAll(BookShelfUtil.deleteBooks(selectBooks));
                    }
                    if (books.size() == 0) {
                        hideBottomLayout();
                    }
                    changeBtnState();
                    refreshData();
                }
            });
            dialog.setNegativeButton("取消", null);
            dialog.show();
        }
    };
    private View.OnClickListener mCancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hideBottomLayout();
            //设置为默认状态
            selectTv.setText(getResources().getString(R.string.main_select_all));
            selectImg.setImageResource(R.drawable.main_bottom_select_all_cancel);
            detailImg.setImageResource(R.drawable.main_bottom_detail);
            detailTv.setTextColor(Color.BLACK);
        }
    };
    private View.OnClickListener mSelectListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (getResources().getString(R.string.main_select_all).equals(selectTv.getText())) {
                adapter.selectAll();
            } else {
                adapter.unSelectAll();
            }
            changeBtnState();
        }
    };
    private View.OnClickListener mDetailListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (adapter.getSelectSize() == 1) {
                Book book = adapter.getSelectBook().get(0);
                File file = FileUtil.getFileByName(book.getBookName());
                filePathTv.setText(file.getPath());
                fileSizeTv.setText(FileUtil.formatFileSize(file.length()));
                fileTimeTv.setText(FileUtil.formatFileTime(file.lastModified()));
                popupWindow();
            }
        }
    };

    /**
     * 打开图书
     */
    private View.OnClickListener mOnItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int bookId = (Integer) v.getTag();
            Book book = BookShelfUtil.queryBookById(bookId);
            Intent intent = new Intent(MainActivity.this, ReadActivity.class);
            intent.putExtra("book_data", book);
            startActivity(intent);
        }
    };

    /**
     * 导入图书
     */
    private View.OnClickListener mImportListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!adapter.isShowItem()) {//底部菜单弹出来后，不能添加图书
                startActivity(new Intent(MainActivity.this, ImportActivity.class));
            }
        }
    };

    /**
     * 长按图书监听
     */
    private View.OnLongClickListener mItemLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            int bookId = (Integer) v.getTag();
            showBottomLayout();
            adapter.bookSelect(bookId);
            changeBtnState();
            return false;//返回false：执行完长按事件后，还要执行单击事件
        }
    };

    /**
     * 监听选中图书个数
     */
    private View.OnClickListener mItemSelectedClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            adapter.selectBook((Integer) v.getTag());
            changeBtnState();
        }
    };

    /**
     * 显示底部菜单
     */
    private void showBottomLayout() {
        isHideBottom = true;
        Animation mAnimationBottomIn = AnimationUtils.loadAnimation(MainActivity.this, R.anim.bottom_in);
        bottomLayout.setVisibility(View.VISIBLE);
        bottomLayout.startAnimation(mAnimationBottomIn);
    }

    /**
     * 隐藏底部菜单
     */
    private void hideBottomLayout() {
        isHideBottom = false;
        // 隐藏复选框
        adapter.showState(false);
        Animation mAnimationBottomOut = AnimationUtils.loadAnimation(MainActivity.this, R.anim.bottom_out);
        bottomLayout.setVisibility(View.GONE);
        bottomLayout.startAnimation(mAnimationBottomOut);
    }

    /**
     * 导入时直接隐藏，无需动画
     */
    public void directHideBottom() {
        adapter.showState(false);
        bottomLayout.setVisibility(View.GONE);
    }

    public void refreshData() {
        adapter.notifyDataSetChanged();
    }

    /**
     * 弹出详细信息菜单
     */
    private void popupWindow() {
        // 获取屏幕宽高
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        popupWindow = new PopupWindow(view, 2 * width / 3, WindowManager.LayoutParams.WRAP_CONTENT);
        // 点击空白处，隐藏popup窗口
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable(getResources(), (Bitmap) null));
        // 创建当前界面的一个参数对象
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 0.3f;
        getWindow().setAttributes(params);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.alpha = 1.0f;
                getWindow().setAttributes(params);
            }
        });
        popupWindow.showAtLocation(inflater.inflate(R.layout.activity_main, null), Gravity.CENTER, 0, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.title_import_book, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                startActivity(new Intent(this, ImportActivity.class));
                directHideBottom();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isHideBottom) {
                hideBottomLayout();
            } else {
                exit();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    private long exitTime = 0;
    private void exit() {
        long endTime = 2000;
        //两秒内如果没有再次按下，则不会退出
        if ((System.currentTimeMillis() - exitTime) > endTime) {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.back_app), Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
            System.exit(0);
        }
    }

    /**
     * 动态申请SD卡读写的权限，Android6.0之后系统对权限的管理更加严格了，不但要在AndroidManifest中添加，还要在应用运行的时候动态申请
     */
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static String[] PERMISSION_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE"};

    public static void verifyStoragePermissions(Activity activity) {
        try {
            int permission = ActivityCompat.checkSelfPermission(activity, "android.permission.WRITE_EXTERNAL_STORAGE");
            // 判断是否已经授予权限
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSION_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // 已被授予权限
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    // 拒绝授予权限，弹出框，让用户去应用详情页手动设置权限
                    new AlertDialog.Builder(this)
                            .setTitle("警告")
                            .setMessage("存储权限是必须的，若拒绝，则部分功能无法正常运行！")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Uri uri = Uri.parse("package:" + getPackageName());
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
                                    startActivity(intent);
                                }
                            }).show();
                }
                break;
            }
            default:
                break;
        }
    }
}
