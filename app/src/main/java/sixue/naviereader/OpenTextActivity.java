package sixue.naviereader;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OpenTextActivity extends AppCompatActivity {

    private String sdcard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_text);

        ListView lvFiles = (ListView) findViewById(R.id.lv_files);
        MyAdapter myAdapter = new MyAdapter(Environment.getExternalStorageDirectory());
        sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
        lvFiles.setAdapter(myAdapter);
        lvFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                File file = (File) view.getTag();
                if (file != null) {
                    Intent data = new Intent();
                    data.putExtra(Utils.INTENT_PARA_BOOKNAME, file.getName());
                    data.putExtra(Utils.INTENT_PARA_BOOKPATH, file.getAbsolutePath().substring(sdcard.length()));
                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        });
    }

    private class MyAdapter extends BaseAdapter {
        private List<File> files;

        public MyAdapter(File root) {
            this.files = new ArrayList<>();
            startLoadFiles(root);
        }

        private void startLoadFiles(final File root) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    loadFiles(root);
                }
            }).start();
        }

        private void loadFiles(File dir) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    loadFiles(file);
                } else if (file.getName().endsWith(".txt")) {
                    final File f = file;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            files.add(f);
                            notifyDataSetChanged();
                        }
                    });
                }
            }
        }

        @Override
        public int getCount() {
            return files.size();
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
                view = new TextView(OpenTextActivity.this);
                view.setPadding(10, 10, 10, 10);
            }
            TextView tv = (TextView) view;
            tv.setText(files.get(i).getAbsolutePath().substring(sdcard.length()));
            tv.setTag(files.get(i));
            return view;
        }
    }
}
