package sixue.naviereader;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sixue.naviereader.data.Chapter;

public class ContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        ListView listView = (ListView) findViewById(R.id.content);
        listView.setAdapter(new MyAdapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Chapter chapter = (Chapter) view.getTag();

            }
        });
    }

    private class MyAdapter extends BaseAdapter {
        List<Chapter> chapterList;

        public MyAdapter() {
            chapterList = new ArrayList<>();
            startDownload();
        }

        private void startDownload() {
            final Handler handler = new Handler();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; ; i++) {
                            if (fetchChaptersOfPage(3246, i) <= 0) {
                                break;
                            }
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                notifyDataSetChanged();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        }

        private int fetchChaptersOfPage(int bookId, int pageId) throws IOException {
            Document doc = Jsoup.connect("http://m.50zw.la/chapters_" + bookId + "/" + (pageId + 1)).timeout(5000).get();
            Elements list = doc.body().select("#alllist .last9");

            Document listDoc = Jsoup.parse(list.toString());
            int i = 0;
            for (Element ch : listDoc.select("li.even")) {
                String title = ch.select("a").text();
                String url = ch.select("a").attr("href").replace("/", "").trim();
                Chapter chapter = new Chapter();
                chapter.setTitle(title);
                chapter.setUrl(url);
                chapterList.add(chapter);
                i++;
            }
            return i;
        }

        @Override
        public int getCount() {
            return chapterList.size();
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
            Chapter chapter = chapterList.get(i);
            view.setTag(chapter);

            TextView tv = (TextView) view;
            tv.setText(chapter.getTitle());
            return view;
        }
    }

}
