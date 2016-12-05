package sixue.naviereader;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import sixue.naviereader.data.Book;

public class AddNetBookActivity extends AppCompatActivity {

    private List<Book> list;
    private BaseAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_net_book);

        list = new ArrayList<>();

        final Button search = (Button) findViewById(R.id.search);
        final EditText searchText = (EditText) findViewById(R.id.search_text);
        final ListView listBooks = (ListView) findViewById(R.id.list_books);
        final Handler handler = new Handler();

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                list.clear();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String t = searchText.getText().toString();
                            String key = URLEncoder.encode(t, "GB2312");
                            String url = "http://www.50zw.la/modules/article/search.php?searchkey=" + key;
                            Connection.Response response = Jsoup.connect(url).followRedirects(true).timeout(5000).execute();
                            if (!url.equals(response.url().toString())) {
                                Book book = new Book();
                                book.setId(response.url().toString());
                                book.setTitle(t);
                                book.setAuthor("--");
                                book.setLocal(false);
                                list.add(book);
                            } else {
                                Document doc = response.parse();

                                Elements elements = doc.body().select("table.grid");
                                for (Element tr : Jsoup.parse(elements.toString()).select("tr")) {
                                    Elements tds = tr.select("td.odd");
                                    Elements a = tds.select("a");
                                    if (tds.size() == 0) {
                                        continue;
                                    }
                                    Book book = new Book();
                                    String title = a.text();
                                    String id = a.attr("href").trim();
                                    String author = tds.get(1).text();
                                    book.setId(id);
                                    book.setTitle(title);
                                    book.setAuthor(author);
                                    book.setLocal(false);
                                    list.add(book);
                                }
                            }
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    myAdapter.notifyDataSetChanged();
                                }
                            });
                            Log.i(AddNetBookActivity.this.getClass().toString(), list.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        myAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return list.size();
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
                    view = AddNetBookActivity.this.getLayoutInflater().inflate(R.layout.listviewitem_book, null);
                }
                TextView title = (TextView) view.findViewById(R.id.title);
                TextView author = (TextView) view.findViewById(R.id.author);
                Book book = list.get(i);
                title.setText(book.getTitle());
                author.setText(book.getAuthor());
                return view;
            }
        };
        listBooks.setAdapter(myAdapter);
        listBooks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Book book = list.get(i);
                BookLoader.getInstance().addBook(book);
                finish();
            }
        });
    }
}
