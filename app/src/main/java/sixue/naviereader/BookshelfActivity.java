package sixue.naviereader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sixue.naviereader.data.Book;

public class BookshelfActivity extends AppCompatActivity {

    private MyAdapter myAdapter;
    private boolean isEditMode;
    private List<Book> editList;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookshelf);

        Utils.verifyStoragePermissions(this);

        BookLoader.getInstance().reload(this);

        isEditMode = false;
        editList = new ArrayList<>();
        actionBar = getSupportActionBar();

        GridView gv = (GridView) findViewById(R.id.gridview_books);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add);

        myAdapter = new MyAdapter();
        gv.setAdapter(myAdapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!isEditMode) {
                    BookLoader.getInstance().bookBubble(i);

                    Intent intent = new Intent(BookshelfActivity.this, ReadActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else {
                    View selectIcon = view.findViewById(R.id.select_icon);
                    boolean checked = !selectIcon.isSelected();
                    checkItem(selectIcon, i, checked);
                }
            }
        });
        gv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!isEditMode) {
                    setEditMode(true);
                    View selectIcon = view.findViewById(R.id.select_icon);
                    checkItem(selectIcon, i, true);
                    return true;
                }
                return false;
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isEditMode) {
                    Intent intent = new Intent(BookshelfActivity.this, AddActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        myAdapter.notifyDataSetChanged();
    }

    private void checkItem(View selectIcon, int i, boolean checked) {
        selectIcon.setSelected(checked);
        if (checked) {
            editList.add(BookLoader.getInstance().getBook(i));
        } else {
            editList.remove(BookLoader.getInstance().getBook(i));
        }
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bookshelf, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_delete:
                if (isEditMode) {
                    BookLoader.getInstance().deleteBooks(editList);
                    editList.clear();
                    setEditMode(false);
                    return true;
                }
            case R.id.menu_edit:
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem edit = menu.findItem(R.id.menu_edit);
        MenuItem delete = menu.findItem(R.id.menu_delete);

        edit.setVisible(isEditMode);
        delete.setVisible(isEditMode);

        edit.setEnabled(editList.size() == 1);
        delete.setEnabled(editList.size() >= 1);

        return super.onPrepareOptionsMenu(menu);
    }

    private void setEditMode(boolean editMode) {
        isEditMode = editMode;
        myAdapter.notifyDataSetChanged();
        actionBar.setDisplayShowTitleEnabled(!editMode);
        invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            editList.clear();
            setEditMode(false);
            return;
        }

        super.onBackPressed();
    }

    private class MyAdapter extends BaseAdapter {

        public MyAdapter() {

        }

        @Override
        public int getCount() {
            return BookLoader.getInstance().getBookNum();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(BookshelfActivity.this).inflate(R.layout.gridviewitem_book, viewGroup, false);
            }
            Book book = BookLoader.getInstance().getBook(i);
            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(book.getTitle());
            TextView author = (TextView) view.findViewById(R.id.author);
            author.setText(book.getAuthor());
            View selectIcon = view.findViewById(R.id.select_icon);
            if (!isEditMode) {
                selectIcon.setVisibility(View.INVISIBLE);
            } else {
                selectIcon.setVisibility(View.VISIBLE);
            }
            ImageView cover = (ImageView) view.findViewById(R.id.cover);
            SmartDownloader downloader = new SmartDownloader(BookshelfActivity.this, book);
            if (downloader.coverIsDownloaded()) {
                Bitmap bm = BitmapFactory.decodeFile(book.getCoverSavePath());
                cover.setImageBitmap(bm);
            } else {
                cover.setImageBitmap(BookLoader.getInstance().getNoCover());
            }
            return view;
        }
    }
}
