package sixue.naviereader;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private GridView gv;
    private FloatingActionButton fab;
    private BaseAdapter gvAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BookList.getInstance().reload(this);

        gv = (GridView) findViewById(R.id.list_books);
        fab = (FloatingActionButton) findViewById(R.id.fab_add);

        gvAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return BookList.getInstance().getBookNum();
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
                    view = LayoutInflater.from(MainActivity.this).inflate(R.layout.gridviewitem_book, viewGroup, false);
                }
                BookList.Book book = BookList.getInstance().getBook(i);
                view.setTag(i);
                TextView tv = (TextView) view.findViewById(R.id.book);
                tv.setText(book.getTitle());
                tv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.fm, 0, 0);
                return view;
            }
        };

        gv.setAdapter(gvAdapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, ReadActivity.class);
                intent.putExtra("position", i);
                startActivity(intent);
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, OpenTextActivity.class);
                startActivityForResult(intent, 0);
            }
        });
    }

    protected void onActivityResult(int requestCode, int responseCode, Intent data) {
        if (requestCode == 0) {
            if (responseCode == RESULT_OK) {
                String name = data.getStringExtra("name");
                String path = data.getStringExtra("path");
                BookList.getInstance().addBook(name, path, this);
                gvAdapter.notifyDataSetChanged();
            }
        }

    }
}
