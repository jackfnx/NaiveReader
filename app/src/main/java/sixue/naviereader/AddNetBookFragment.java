package sixue.naviereader;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
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

public class AddNetBookFragment extends Fragment {

    private List<Book> list;

    public AddNetBookFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_add_net_book, container, false);

        list = new ArrayList<>();

        final Button search = (Button) v.findViewById(R.id.search);
        final EditText searchText = (EditText) v.findViewById(R.id.search_text);
        final ListView listBooks = (ListView) v.findViewById(R.id.list_books);

        final BaseAdapter myAdapter = new BaseAdapter() {
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
                    view = getActivity().getLayoutInflater().inflate(R.layout.listviewitem_book, viewGroup, false);
                }
                TextView title = (TextView) view.findViewById(R.id.title);
                TextView author = (TextView) view.findViewById(R.id.author);
                Book book = list.get(i);
                title.setText(book.getTitle());
                author.setText(book.getAuthor());
                return view;
            }
        };
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
                                book.setAuthor("*");
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
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    myAdapter.notifyDataSetChanged();
                                }
                            });
                            Log.i(AddNetBookFragment.this.getClass().toString(), list.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        listBooks.setAdapter(myAdapter);
        listBooks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Book book = list.get(i);
                BookLoader.getInstance().addBook(book);
                getActivity().finish();
            }
        });

        return v;
    }
}
