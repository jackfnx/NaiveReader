package sixue.naviereader;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

public class OpenTextActivity extends AppCompatActivity {

    private MyAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_text);

        ListView lvFiles = (ListView) findViewById(R.id.lv_files);
        myAdapter = new MyAdapter();
        myAdapter.setCurrentDir(Environment.getExternalStorageDirectory());
        lvFiles.setAdapter(myAdapter);
        lvFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                File file = (File) view.getTag();
                if (file != null) {
                    if (file.isDirectory()) {
                        myAdapter.setCurrentDir(file);
                    } else {
                        Intent data = new Intent();
                        data.putExtra("name", file.getName());
                        data.putExtra("path", file.getAbsolutePath());
                        setResult(RESULT_OK, data);
                        finish();
                    }
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            File currentDir = myAdapter.getCurrentDir();
            if (currentDir.compareTo(Environment.getExternalStorageDirectory()) > 0) {
                myAdapter.setCurrentDir(currentDir.getParentFile());
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private class MyAdapter extends BaseAdapter {
        private File currentDir;
        private File[] files;

        @Override
        public int getCount() {
            return files.length;
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
            tv.setText(files[i].isDirectory() ? "<" + files[i].getName() + ">" : files[i].getName());
            tv.setTag(files[i]);
            return view;
        }

        void setCurrentDir(File currentDir) {
            this.currentDir = currentDir;
            this.files = currentDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    if (s.startsWith(".")) {
                        return false;
                    }
                    File f = new File(file.getAbsoluteFile() + "/" + s);
                    if (f.isDirectory()) {
                        return true;
                    }
                    if (s.endsWith(".txt")) {
                        return true;
                    }
                    return false;
                }
            });
            Arrays.sort(this.files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return f1.getName().compareTo(f2.getName());
                }
            });
            notifyDataSetChanged();
        }

        File getCurrentDir() {
            return currentDir;
        }
    }
}
