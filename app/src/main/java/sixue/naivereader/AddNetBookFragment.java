package sixue.naivereader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.data.Book;
import sixue.naivereader.provider.NetProvider;
import sixue.naivereader.provider.NetProviderCollections;

public class AddNetBookFragment extends Fragment {

    private List<Book> list;
    private BroadcastReceiver receiver;

    public AddNetBookFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
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
                ImageView cover = (ImageView) view.findViewById(R.id.cover);
                TextView title = (TextView) view.findViewById(R.id.title);
                TextView author = (TextView) view.findViewById(R.id.author);
                TextView source = (TextView) view.findViewById(R.id.source);

                Book book = list.get(i);
                SmartDownloader downloader = new SmartDownloader(getContext(), book);
                if (downloader.coverIsDownloaded()) {
                    Bitmap bm = BitmapFactory.decodeFile(book.getCoverSavePath());
                    cover.setImageBitmap(bm);
                } else {
                    cover.setImageBitmap(downloader.getNoCoverBitmap());
                }
                title.setText(book.getTitle());
                author.setText(book.getAuthor());
                source.setText(getString(R.string.sources, book.getSources().size()));
                return view;
            }
        };
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                list.clear();

                for (final NetProvider provider : NetProviderCollections.getProviders(getContext())) {
                    if (!provider.isActive()) {
                        continue;
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            List<Book> books = provider.search(searchText.getText().toString(), getContext());
                            for (Book book : books) {
                                Book b = findSameBook(book);
                                if (b != null) {
                                    b.getSources().addAll(book.getSources());
                                } else {
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
                        }
                    }).start();
                }
            }
        });
        listBooks.setAdapter(myAdapter);
        listBooks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Book book = list.get(i);
                Book b = BookLoader.getInstance().findBook(book.getId());
                if (b != null) {
                    BookLoader.getInstance().bookBubble(b);
                } else {
                    BookLoader.getInstance().addBook(book);
                }
                getActivity().finish();
            }
        });

        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String bookId = intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID);
                for (Book book : list) {
                    if (book.getId().equals(bookId)) {
                        myAdapter.notifyDataSetChanged();
                        break;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.ACTION_DOWNLOAD_COVER_FINISH);
        getContext().registerReceiver(receiver, filter);

        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(receiver);
    }

    private Book findSameBook(Book book) {
        for (Book b : list) {
            if (book.getTitle().equals(b.getTitle())) {
                return b;
            }
        }
        return null;
    }
}
