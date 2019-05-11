package sixue.naivereader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.data.Book;
import sixue.naivereader.data.BookKind;
import sixue.naivereader.data.Chapter;
import sixue.naivereader.data.Source;
import sixue.naivereader.helper.LocalTextLoader;
import sixue.naivereader.provider.NetProvider;
import sixue.naivereader.provider.NetProviderCollections;

public class ContentActivity extends AppCompatActivity {

    private static final int MAX_SUMMARY_LENGTH = 40;
    private BroadcastReceiver receiver;
    private SmartDownloader downloader;
    private Book book;
    private List<String> providerIds;
    private String localText;
    private List<Integer> localChapterNodes;
    private int currentLocalChapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        providerIds = new ArrayList<>();
        book = BookLoader.getInstance().getBook(0);
        downloader = new SmartDownloader(this, book);
        localChapterNodes = new ArrayList<>();

        final ListView listView = findViewById(R.id.content);
        final MyAdapter myAdapter = new MyAdapter(book);
        final SwipeRefreshLayout srl = findViewById(R.id.srl);

        listView.setAdapter(myAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(ContentActivity.this, ReadActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                if (book.getKind() == BookKind.Online) {
                    int index = book.getChapterList().size() - i - 1;
                    intent.putExtra(Utils.INTENT_PARA_CHAPTER_INDEX, index);
                    intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, 0);
                } else if (book.getKind() == BookKind.Packet) {
                    intent.putExtra(Utils.INTENT_PARA_CHAPTER_INDEX, i);
                    intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, 0);
                } else if (book.getKind() == BookKind.LocalText) {
                    intent.putExtra(Utils.INTENT_PARA_CHAPTER_INDEX, 0);
                    intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, localChapterNodes.get(i));
                }
                startActivity(intent);
                finish();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.ACTION_DOWNLOAD_CONTENT_FINISH);
        filter.addAction(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH);
        filter.addAction(Utils.ACTION_DOWNLOAD_ALL_CHAPTER_FINISH);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                switch (action) {
                    case Utils.ACTION_DOWNLOAD_CONTENT_FINISH:
                        if (book.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID))) {
                            srl.setRefreshing(false);
                            listView.setSelection(book.getChapterList().size() - book.getCurrentChapterIndex() - 1);
                            myAdapter.notifyDataSetChanged();
                        }
                        break;
                    case Utils.ACTION_DOWNLOAD_CHAPTER_FINISH:
                        if (book.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID))) {
                            myAdapter.notifyDataSetChanged();
                        }
                        break;
                    case Utils.ACTION_DOWNLOAD_ALL_CHAPTER_FINISH:
                        if (book.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID))) {
                            Toast.makeText(ContentActivity.this, R.string.msg_batch_download_finish, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        registerReceiver(receiver, filter);

        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (book.getKind() == BookKind.Online) {
                    downloader.startDownloadContent();
                }
            }
        });

        if (book.buildHelper().reloadContent(this)) {
            if (book.getKind() == BookKind.LocalText) {
                localText = Utils.readText(book.getLocalPath());
                localChapterNodes = LocalTextLoader.calcChapterNodes(localText);
                currentLocalChapter = 0;
                for (int i = 0; i < localChapterNodes.size(); i++) {
                    int node = localChapterNodes.get(i);
                    int next = (i + 1) < localChapterNodes.size() ? localChapterNodes.get(i + 1) : Integer.MAX_VALUE;
                    if (book.getCurrentPosition() >= node && book.getCurrentPosition() < next) {
                        currentLocalChapter = i;
                        break;
                    }
                }
                listView.setSelection(currentLocalChapter);
            } else if (book.getKind() == BookKind.Online) {
                listView.setSelection(book.getChapterList().size() - book.getCurrentChapterIndex() - 1);
            } else if (book.getKind() == BookKind.Packet) {
                listView.setSelection(book.getCurrentChapterIndex());
            }
        } else {
            srl.setRefreshing(true);
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

        MyAdapter(Book book) {
            this.book = book;
        }

        @Override
        public int getCount() {
            return book.getKind() == BookKind.LocalText ? localChapterNodes.size() : book.getChapterList().size();
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
                view = LayoutInflater.from(ContentActivity.this).inflate(R.layout.listviewitem_content, viewGroup, false);
                view.setPadding(20, 20, 20, 20);
            }
            TextView title = view.findViewById(R.id.title);
            TextView summary = view.findViewById(R.id.summary);
            if (book.getKind() == BookKind.LocalText) {
                int node = localChapterNodes.get(i);
                int length = localText.length();

                int end = localText.indexOf('\n', node);
                end = end < 0 ? length : end;
                String s = localText.substring(node, end);

                if (i == currentLocalChapter) {
                    s += "*";
                }
                title.setText(s);

                int sumStart = node + s.length();
                int sumEnd = sumStart + MAX_SUMMARY_LENGTH > length ? length : sumStart + MAX_SUMMARY_LENGTH;
                String sum = localText.substring(sumStart, sumEnd).trim().replace('\n', ' ');
                summary.setText(sum);
                summary.setGravity(Gravity.START);

            } else {
                int index;
                if (book.getKind() == BookKind.Online)
                    index = book.getChapterList().size() - i - 1;
                else
                    index = i;
                Chapter chapter = book.getChapterList().get(index);
                String s = chapter.getTitle();
                if (index == book.getCurrentChapterIndex()) {
                    s += "*";
                }
                title.setText(s);
                if (downloader.isDownloaded(chapter)) {
                    summary.setText(R.string.download);
                } else {
                    summary.setText("");
                }
                summary.setGravity(Gravity.END);
            }
            return view;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.content, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuProviders = menu.findItem(R.id.menu_providers);
        if (book.getKind() != BookKind.Online) {
            menuProviders.setVisible(false);
        } else {
            SubMenu subMenu = menuProviders.getSubMenu();

            providerIds.clear();
            subMenu.clear();
            for (Source source : book.getSources()) {
                NetProvider netProvider = NetProviderCollections.findProviders(source.getId(), this);
                if (netProvider != null) {
                    String id = netProvider.getProviderId();
                    String name = netProvider.getProviderName();
                    if (book.getSiteId().equals(id)) {
                        name += "*";
                    }
                    providerIds.add(id);
                    subMenu.add(Menu.NONE, Menu.FIRST + providerIds.size() - 1, providerIds.size(), name);
                }
            }

            subMenu.add(Menu.NONE, Menu.FIRST + providerIds.size(), providerIds.size() + 1, R.string.menu_search_again);
        }

        MenuItem batchDownload = menu.findItem(R.id.menu_batch_download);
        if (book.getKind() != BookKind.Online) {
            batchDownload.setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_batch_download) {
            downloader.startDownloadAllChapter();
            return true;
        }

        int i = menuItem.getItemId() - Menu.FIRST;
        if (i < providerIds.size()) {
            String providerId = providerIds.get(i);
            for (Source source : book.getSources()) {
                if (source.getId().equals(providerId)) {
                    book.setSiteId(source.getId());
                    book.setSitePara(source.getPara());
                    BookLoader.getInstance().save();
                    invalidateOptionsMenu();
                    return true;
                }
            }
            return true;
        } else if (i == providerIds.size()) {
            Toast.makeText(this, R.string.not_implemented, Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }
}
