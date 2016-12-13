package sixue.naviereader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import sixue.naviereader.data.Book;
import sixue.naviereader.data.Chapter;

public class ContentActivity extends AppCompatActivity {

    private BroadcastReceiver receiver;
    private SmartDownloader downloader;
    private Book book;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        book = BookLoader.getInstance().getBook(0);
        downloader = new SmartDownloader(this, book);

        final ListView listView = (ListView) findViewById(R.id.content);
        final MyAdapter myAdapter = new MyAdapter(book);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_refresh);
        final View loadingCircle = findViewById(R.id.loading_circle);

        listView.setAdapter(myAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(ContentActivity.this, ReadActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(Utils.INTENT_PARA_CHAPTER_INDEX, i);
                intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, 0);
                startActivity(intent);
                finish();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.ACTION_DOWNLOAD_CONTENT_FINISH);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (book.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID))) {
                    loadingCircle.setVisibility(View.GONE);
                    listView.setSelection(book.getCurrentChapterIndex());
                    myAdapter.notifyDataSetChanged();
                }
            }
        };
        registerReceiver(receiver, filter);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingCircle.setVisibility(View.VISIBLE);
                downloader.startDownloadContent();
            }
        });

        if (downloader.reloadContent()) {
            if (book.isLocal()) {
                Intent intent = new Intent(this, ReadActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
            listView.setSelection(book.getCurrentChapterIndex());
        } else {
            loadingCircle.setVisibility(View.VISIBLE);
            downloader.startDownloadContent();
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private class MyAdapter extends BaseAdapter {

        private final Book book;

        public MyAdapter(Book book) {
            this.book = book;
        }

        @Override
        public int getCount() {
            return book.getChapterList().size();
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
                view = new TextView(ContentActivity.this);
                view.setPadding(10, 10, 10, 10);
            }
            Chapter chapter = book.getChapterList().get(i);
            TextView tv = (TextView) view;
            String s = chapter.getTitle();
            if (i == book.getCurrentChapterIndex()) {
                s += "*";
            }
            tv.setText(s);
            return view;
        }
    }

}
