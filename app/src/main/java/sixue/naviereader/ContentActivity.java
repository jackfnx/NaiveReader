package sixue.naviereader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import sixue.naviereader.data.Book;
import sixue.naviereader.data.BookLoader;
import sixue.naviereader.data.Chapter;

public class ContentActivity extends AppCompatActivity {

    private Book waitingBook;
    private Chapter waitingChapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        final Book book = new Book();
        book.setId("http://www.50zw.la/book_" + 3246);
        book.setTitle("xdzz");
        book.setLocal(false);
        book.setLocalPath("");
        book.setSiteId("http://www.50zw.la");
        book.setSitePara("");
        book.setChapterList(new ArrayList<Chapter>());
        book.setCurrentChapterId("");
        book.setCurrentChapterIndex(0);

        ListView listView = (ListView) findViewById(R.id.content);
        final MyAdapter myAdapter = new MyAdapter(book);

        BookLoader.getInstance().pushContentQueue(book);
        waitingBook = book;

        IntentFilter filter = new IntentFilter();
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (waitingBook != null && waitingBook.getId().equals(intent.getStringExtra("bookId"))) {
                    myAdapter.notifyDataSetChanged();
                    waitingBook = null;
                } else if (waitingChapter != null && waitingChapter.getId().equals(intent.getStringExtra("chapterId"))) {
                    Intent i = new Intent(ContentActivity.this, ReadActivity.class);
                    i.putExtra("path", waitingChapter.getSavePath());
                    i.putExtra("currentPosition", 0);
                    startActivity(i);
                    waitingChapter = null;
                }
            }
        };
        registerReceiver(receiver, filter);

        listView.setAdapter(myAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final Chapter chapter = (Chapter) view.getTag();
                if (!chapter.isDownloaded()) {
                    BookLoader.getInstance().pushChapterQueue(chapter);
                    waitingChapter = chapter;
                } else {
                    Intent intent = new Intent(ContentActivity.this, ReadActivity.class);
                    intent.putExtra("path", chapter.getSavePath());
                    intent.putExtra("currentPosition", 0);
                    startActivity(intent);
                }
            }
        });
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
            view.setTag(chapter);

            TextView tv = (TextView) view;
            tv.setText(chapter.getTitle());
            return view;
        }
    }

}
