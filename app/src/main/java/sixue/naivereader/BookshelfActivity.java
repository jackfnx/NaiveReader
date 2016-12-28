package sixue.naivereader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.data.Book;

public class BookshelfActivity extends AppCompatActivity {

    private MyAdapter myAdapter;
    private boolean isEditMode;
    private List<Book> editList;
    private ActionBar actionBar;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookshelf);

        Utils.verifyPermissions(this);

        BookLoader.getInstance().reload(this);

        isEditMode = false;
        editList = new ArrayList<>();
        actionBar = getSupportActionBar();

        GridView gv = (GridView) findViewById(R.id.gridview_books);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add);
        final SwipeRefreshLayout srl = (SwipeRefreshLayout) findViewById(R.id.srl);

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
        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshAllBooks();
                        srl.setRefreshing(false);
                    }
                });
            }
        });

        receiver = new BroadcastReceiver(){

            @Override
            public void onReceive(Context context, Intent intent) {
                myAdapter.notifyDataSetChanged();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.ACTION_DOWNLOAD_CONTENT_FINISH);
        filter.addAction(Utils.ACTION_DOWNLOAD_COVER_FINISH);
        registerReceiver(receiver, filter);

        refreshAllBooks();
    }

    private void refreshAllBooks() {
        for (int i = 0; i < BookLoader.getInstance().getBookNum(); i++) {
            Book book = BookLoader.getInstance().getBook(i);
            if (!book.isLocal()) {
                SmartDownloader downloader = new SmartDownloader(this, book);
                if (downloader.reloadContent()) {
                    Intent intent = new Intent(Utils.ACTION_DOWNLOAD_CONTENT_FINISH);
                    intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
                    sendBroadcast(intent);
                }

                downloader.startDownloadContent();

                if (!downloader.coverIsDownloaded()) {
                    downloader.startDownloadContent();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
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
                break;
            case R.id.menu_edit:
                Toast.makeText(this, R.string.not_implemented, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_settings:
                Intent intent = new Intent(this, NetProviderManagerActivity.class);
                startActivity(intent);
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
        MenuItem settings = menu.findItem(R.id.menu_settings);

        edit.setVisible(isEditMode);
        delete.setVisible(isEditMode);
        settings.setVisible(!isEditMode);

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

            TextView progress = (TextView) view.findViewById(R.id.progress);
            if (book.isLocal()) {
                int cp = book.getCurrentPosition();
                int wc = book.getWordCount();
                if (wc <= 0) {
                    progress.setText(R.string.read_progress_local_unread);
                } else {
                    progress.setText(getString(R.string.read_progress_local, (float) cp / (float) wc * 100f));
                }
            } else {
                int size = book.getChapterList().size();
                int cp = book.getCurrentChapterIndex();
                if (size <= 0) {
                    progress.setText(R.string.read_progress_net_predownload);
                } else if (cp + 1 == size) {
                    progress.setText(R.string.read_progress_net_allread);
                } else {
                    progress.setText(getString(R.string.read_progress_net, size - cp - 1));
                }
            }

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
                cover.setImageBitmap(downloader.getNoCoverBitmap());
            }
            return view;
        }
    }
}
