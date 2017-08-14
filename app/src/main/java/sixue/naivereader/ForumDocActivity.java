package sixue.naivereader;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ForumDocActivity extends AppCompatActivity {

    private ListView posts;
    private MyAdapter myAdapter;
    private String keyword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum_doc);

        keyword = getIntent().getStringExtra("keyword");

        posts = (ListView) findViewById(R.id.posts);
        myAdapter = new MyAdapter();
        posts.setAdapter(myAdapter);
        myAdapter.startSearch();
    }

    private class MyAdapter extends BaseAdapter {
        private List<String> postItems;

        public MyAdapter() {
            postItems = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return postItems.size();
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
                view = new TextView(ForumDocActivity.this);
            }
            TextView tv = (TextView) view;
            tv.setText(postItems.get(i));
            return view;
        }

        public void startSearch() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Document doc = Jsoup.connect("http://www.sexinsex.net/bbs/forum-96-1.html").timeout(5000).get();
                        String text = doc.body().select("#forum_96").html();//.select("#htmlContent").html().replace("<br>", "").replace("&nbsp;", " ");
                        Log.i(ForumDocActivity.class.toString(), text);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
