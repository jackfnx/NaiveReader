package sixue.naivereader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.data.Book;
import sixue.naivereader.provider.NetProvider;
import sixue.naivereader.provider.NetProviderCollections;

public class AddNetBookFragment extends Fragment {
    private static final String TAG = AddNetBookFragment.class.getSimpleName();

    private Context context;
    private List<Book> list;
    private BroadcastReceiver receiver;

    public AddNetBookFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_add_net_book, container, false);

        list = new ArrayList<>();

        final Button search = v.findViewById(R.id.search);
        final EditText searchText = v.findViewById(R.id.search_text);
        final ListView listBooks = v.findViewById(R.id.list_books);

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
                    view = inflater.inflate(R.layout.listviewitem_book, viewGroup, false);
                }
                ImageView cover = view.findViewById(R.id.cover);
                TextView title = view.findViewById(R.id.title);
                TextView author = view.findViewById(R.id.author);
                TextView source = view.findViewById(R.id.source);

                Book book = list.get(i);
                cover.setImageBitmap(book.buildHelper().loadCoverBitmap(context));
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
                            FragmentActivity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        myAdapter.notifyDataSetChanged();
                                    }
                                });
                            }
                            Log.i(TAG, list.toString());
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
                FragmentActivity activity = getActivity();
                if (activity != null) {
                    activity.finish();
                }
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

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.context = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.ACTION_DOWNLOAD_COVER_FINISH);
        context.registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        context.unregisterReceiver(receiver);
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
