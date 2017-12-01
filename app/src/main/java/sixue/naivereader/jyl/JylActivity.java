package sixue.naivereader.jyl;

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

import sixue.naivereader.R;
import sixue.naivereader.Utils;

public class JylActivity extends AppCompatActivity {

    private static final int AUTHORS_MODE = 0;
    private static final int BOOKS_MODE = 1;
    private JylProvider downloader;
    private ListView listView;
    private MyReceiver receiver;
    private int mode;
    private BaseAdapter authorAdapter;
    private BaseAdapter bookAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        mode = getIntent().getIntExtra("Mode", 0);
        int position = getIntent().getIntExtra("Position", 0);

        listView = (ListView) findViewById(R.id.browse_item_list);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.ACTION_JYL_AUTHORS_DOWNLOAD_FINISH);
        filter.addAction(Utils.ACTION_JYL_AUTHOR_BOOKS_DOWNLOAD_FINISH);
        receiver = new MyReceiver();
        registerReceiver(receiver, filter);

        downloader = new JylProvider(this);
        authorAdapter = new AuthorAdapter();
        bookAdapter = new BookAdapter();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (mode) {
                    case AUTHORS_MODE:
                        Intent intent = new Intent(JylActivity.this, JylActivity.class);
                        intent.putExtra("Mode", BOOKS_MODE);
                        intent.putExtra("Position", i);
                        startActivity(intent);
                        break;
                    case BOOKS_MODE:
                        break;
                }
            }
        });

        switch (mode) {
            case AUTHORS_MODE:
                downloader.startDownloadAuthors();
                break;
            case BOOKS_MODE:
                downloader.startDownloadBooks(JylProvider.getAuthors().get(position));
                break;
        }
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Utils.ACTION_JYL_AUTHORS_DOWNLOAD_FINISH:
                    listView.setAdapter(authorAdapter);
                    authorAdapter.notifyDataSetChanged();
                    break;
                case Utils.ACTION_JYL_AUTHOR_BOOKS_DOWNLOAD_FINISH:
                    listView.setAdapter(bookAdapter);
                    bookAdapter.notifyDataSetChanged();
                    break;
                default:
                    break;
            }
        }
    }

    private class AuthorAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return JylProvider.getAuthors().size();
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
                view = new TextView(JylActivity.this);
            }
            ((TextView) view).setText(JylProvider.getAuthors().get(i).getAuthor());
            return view;
        }
    }

    private class BookAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return JylProvider.getBooks(null).size();
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
                view = new TextView(JylActivity.this);
            }
            JylBook book = JylProvider.getBooks(null).get(i);
            ((TextView) view).setText(book.getSeries() + "_" + book.getTitle());
            return view;
        }
    }
}
