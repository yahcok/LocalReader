package com.example.localreader.adapter;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localreader.R;
import com.example.localreader.entity.Book;
import com.example.localreader.util.BookShelfUtil;
import com.example.localreader.viewholder.BaseViewHolder;
import com.example.localreader.viewholder.BookShelfViewHolder;
import com.example.localreader.viewholder.ImportViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author xialijuan
 * @date 2020/11/12
 */
public class BookShelfAdapter extends RecyclerView.Adapter<BaseViewHolder> {

    private Context context;
    private List<Book> books;
    private final static int OPEN_BOOK = 0;
    private final static int IMPORT_BOOK = 1;
    /**
     * 是否显示底部选项和checkbox
     */
    private boolean isShowItem = false;
    /**
     * 存储选中的图书，类似hashmap
     */
    private SparseBooleanArray mBooleanArray;
    private int[] bg = new int[]{R.drawable.book_shelf_cover_bg_1,
            R.drawable.book_shelf_cover_bg_2, R.drawable.book_shelf_cover_bg_3};
    private View.OnClickListener mOnItemClickListener;
    private View.OnLongClickListener mItemLongClickListener;
    private View.OnClickListener mItemSelectedClickListener;
    private View.OnClickListener mImportListener;
    private String readed = "100.0%";
    private String noRead = "未读";

    public BookShelfAdapter(List<Book> books, Context context) {
        mBooleanArray = new SparseBooleanArray();
        this.context = context;
        this.books = books;
    }

    @Override
    public int getItemViewType(int position) {
        if (books == null || books.size() == 0 || position == books.size()) {
            return IMPORT_BOOK;
        }
        return OPEN_BOOK;
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case IMPORT_BOOK: {
                View view = LayoutInflater.from(context).inflate(R.layout.import_book, parent, false);
                return new ImportViewHolder(view);
            }
            case OPEN_BOOK: {
                View view = LayoutInflater.from(context).inflate(R.layout.recycleview_book_shelf, parent, false);
                return new BookShelfViewHolder(view);
            }
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final BaseViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case OPEN_BOOK:
                BookShelfViewHolder viewHolder = (BookShelfViewHolder) holder;
                Book book = books.get(position);
                String bookName = book.getBookName().split(".txt")[0];
                viewHolder.bookItemTv.setText(bookName);

                if (book.getBookBg() == 0) {
                    // 如果没有封面，则设置封面
                    int random = new Random().nextInt(3);
                    book.setBookBg(random);
                    book.save();
                }
                viewHolder.bookItemTv.setBackgroundResource(bg[book.getBookBg()]);
                viewHolder.bookNameTv.setText(bookName);

                String progress = book.getProgress();

                if (progress.equals(readed)) {
                    progress = "已读完";
                } else if (!progress.equals(noRead)) {
                    progress = "已读" + progress;
                }
                viewHolder.bookProgressTv.setText(progress);

                viewHolder.bookView.setTag(book.getId());
                viewHolder.bookView.setOnLongClickListener(mItemLongClickListener);
                if (isShowItem) {
                    viewHolder.bookSelectIv.setVisibility(View.VISIBLE);
                    viewHolder.bookSelectIv.setSelected(getItemSelected(book.getId()));
                    viewHolder.bookView.setOnClickListener(mItemSelectedClickListener);
                } else {
                    mBooleanArray.clear();
                    viewHolder.bookSelectIv.setVisibility(View.GONE);
                    viewHolder.bookView.setOnClickListener(mOnItemClickListener);
                }
                break;
            case IMPORT_BOOK:
                holder.itemView.setOnClickListener(mImportListener);
                break;
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return books == null ? 1 : books.size() + 1;
    }

    /**
     * 导入图书监听
     * @param mImportListener
     */
    public void setImportListener(View.OnClickListener mImportListener) {
        this.mImportListener = mImportListener;
    }

    /**
     * 点击图书监听
     * @param mOnItemClickListener
     */
    public void setOnItemClickListener(View.OnClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    /**
     * 全选
     */
    public void selectAll() {
        initCheck(true);
    }

    /**
     * 全部选
     */
    public void unSelectAll() {
        initCheck(false);
    }

    public void initCheck(boolean states) {
        if (books != null) {
            for (Book book : books) {
                setItemSelected(book.getId(), states);
            }
            notifyDataSetChanged();
        }
    }

    /**
     * 选中图书
     * @param id
     * @return
     */
    public int selectBook(int id) {
        Book book = BookShelfUtil.queryBookById(id);
        boolean isSelected = getItemSelected(book.getId());
        setItemSelected(book.getId(), !isSelected);
        notifyDataSetChanged();
        return getSelectSize();
    }

    /**
     * 长按图书后自动点击图书监听
     * @param onItemClickListener
     */
    public void setOnItemClickSelectListener(View.OnClickListener onItemClickListener) {
        this.mItemSelectedClickListener = onItemClickListener;
    }

    public void setItemLongClickListener(View.OnLongClickListener itemLongClickListener) {
        this.mItemLongClickListener = itemLongClickListener;
    }

    public void animState(boolean state) {
        isShowItem = state;
        mBooleanArray.clear();
        notifyDataSetChanged();
    }

    public void bookSelect(int id) {
        isShowItem = true;
        setItemSelected(id, true);
        notifyDataSetChanged();
    }


    public void setItemSelected(int id, boolean selected) {
        if (mBooleanArray == null) {
            mBooleanArray = new SparseBooleanArray();
        }
        mBooleanArray.put(id, selected);
    }

    /**
     * 返回图书id是否被选中
     * @param id
     * @return
     */
    public boolean getItemSelected(int id) {
        if (mBooleanArray == null) {
            return false;
        }
        return mBooleanArray.get(id, false);
    }

    /**
     * 获取被选中的id集合
     * @return
     */
    public List<Book> getSelectBook() {
        List<Book> selectBook = new ArrayList<>();
        for (Book book : books) {
            if (getItemSelected(book.getId())) {
                selectBook.add(book);
            }
        }
        return selectBook;
    }

    public int getSelectSize() {
        return getSelectBook().size();
    }

    public boolean isShowItem() {
        return isShowItem;
    }
}
