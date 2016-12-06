package sixue.naviereader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        final Book book = BookLoader.getInstance().getBook(0);
        SmartDownloader downloader = new SmartDownloader(this, book);

        ListView listView = (ListView) findViewById(R.id.content);
        final MyAdapter myAdapter = new MyAdapter(book);

        listView.setAdapter(myAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(ContentActivity.this, ReadActivity.class);
                intent.putExtra(Utils.INTENT_PARA_CHAPTER_INDEX, i);
                intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, 0);
                startActivity(intent);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.ACTION_DOWNLOAD_CONTENT_FINISH);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (book.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID))) {
                    myAdapter.notifyDataSetChanged();
                }
            }
        };
        registerReceiver(receiver, filter);

        if (downloader.reloadContent()) {
            if (book.isLocal()) {
                Intent intent = new Intent(this, ReadActivity.class);
                startActivity(intent);
                finish();
            }
        } else {
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
            tv.setText(chapter.getTitle());
            return view;
        }
    }

}
