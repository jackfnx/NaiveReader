package sixue.naivereader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sixue.naivereader.data.Book;
import sixue.naivereader.data.Chapter;
import sixue.naivereader.data.Source;
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

        final ListView listView = (ListView) findViewById(R.id.content);
        final MyAdapter myAdapter = new MyAdapter(book);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_refresh);
        final View loadingCircle = findViewById(R.id.loading_circle);

        listView.setAdapter(myAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(ContentActivity.this, ReadActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                if (!book.isLocal()) {
                    intent.putExtra(Utils.INTENT_PARA_CHAPTER_INDEX, i);
                    intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, 0);
                } else {
                    intent.putExtra(Utils.INTENT_PARA_CHAPTER_INDEX, 0);
                    intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, localChapterNodes.get(i));
                }
                startActivity(intent);
                finish();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.ACTION_DOWNLOAD_CONTENT_FINISH);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (book.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID))) {
                    loadingCircle.setVisibility(View.GONE);
                    listView.setSelection(book.getCurrentChapterIndex());
                    myAdapter.notifyDataSetChanged();
                }
            }
        };
        registerReceiver(receiver, filter);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingCircle.setVisibility(View.VISIBLE);
                downloader.startDownloadContent();
            }
        });

        if (downloader.reloadContent()) {
            if (book.isLocal()) {
                calcLocalChapterNodes();
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
            } else {
                listView.setSelection(book.getCurrentChapterIndex());
            }
        } else {
            loadingCircle.setVisibility(View.VISIBLE);
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
            return book.isLocal() ? localChapterNodes.size() : book.getChapterList().size();
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
            TextView title = (TextView) view.findViewById(R.id.title);
            TextView summary = (TextView) view.findViewById(R.id.summary);
            String s;
            if (book.isLocal()) {
                int node = localChapterNodes.get(i);
                int length = localText.length();
                if (localText != null) {
                    int end = localText.indexOf('\n', node);
                    end = end < 0 ? length : end;
                    s = localText.substring(node, end);
                } else {
                    s = "?";
                }
                if (i == currentLocalChapter) {
                    s += "*";
                }

                int sumStart = node + s.length();
                int sumEnd = sumStart + MAX_SUMMARY_LENGTH > length ? length : sumStart + MAX_SUMMARY_LENGTH;
                String sum = localText.substring(sumStart, sumEnd).trim().replace('\n', ' ');
                summary.setText(sum);

            } else {
                Chapter chapter = book.getChapterList().get(i);
                s = chapter.getTitle();
                if (i == book.getCurrentChapterIndex()) {
                    s += "*";
                }
                summary.setText("");
            }
            title.setText(s);
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
        if (book.isLocal()) {
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

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
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

    private void calcLocalChapterNodes() {
        localText = Utils.readText(book.getLocalPath());
        localChapterNodes = new ArrayList<>();
        if (localText == null) {
            return;
        }

        String[] patterns = new String[]{
                "\\b第[一二三四五六七八九十百千零]+[章节篇集卷]\\b",
                "\\b[\\d\\uFF10-\\uFF19]+\\b"
        };
        for (String ps : patterns) {
            Pattern pattern = Pattern.compile(ps);
            Matcher matcher = pattern.matcher(localText);
            while (matcher.find()) {
                localChapterNodes.add(matcher.start());
            }
            if (localChapterNodes.size() != 0) {
                break;
            }
        }
    }
}
