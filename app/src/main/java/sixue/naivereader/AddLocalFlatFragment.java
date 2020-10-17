package sixue.naivereader;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.data.Book;
import sixue.naivereader.helper.LocalTextLoader;

public class AddLocalFlatFragment extends Fragment {
    private String sdcard;
    private boolean running;
    private List<String> excludes;

    public AddLocalFlatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_local_flat, container, false);

        sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
        excludes = new ArrayList<>();
        excludes.add(sdcard + "/Android/data");
        if (getContext() != null) {
            excludes.add(sdcard + "/" + getContext().getPackageName());
        }

        ListView lvFiles = v.findViewById(R.id.lv_files);
        MyAdapter myAdapter = new MyAdapter(Environment.getExternalStorageDirectory());
        lvFiles.setAdapter(myAdapter);
        lvFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                File file = (File) view.getTag();
                if (file != null) {
                    Book b = BookLoader.getInstance().findBook(file.getAbsolutePath());
                    if (b != null) {
                        BookLoader.getInstance().bookBubble(b);
                    } else {
                        BookLoader.getInstance().addBook(LocalTextLoader.createBook(file));
                    }
                    FragmentActivity activity = getActivity();
                    if (activity != null) {
                        activity.finish();
                    }
                }
            }
        });

        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
    }

    private class MyAdapter extends BaseAdapter {
        private List<File> files;

        MyAdapter(File root) {
            this.files = new ArrayList<>();
            running = true;
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

        private void loadFiles(final File file) {
            if (running) {
                if (file.isDirectory()) {
                    if (excludes.contains(file.getAbsolutePath())) {
                        return;
                    }
                    if (file.getName().startsWith(".")) {
                        return;
                    }
                    File[] lf = file.listFiles();
                    if (lf != null) {
                        for (File subFile : lf) {
                            loadFiles(subFile);
                        }
                    }
                } else {
                    if (file.getName().endsWith(".txt")) {
                        FragmentActivity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    files.add(file);
                                    notifyDataSetChanged();
                                }
                            });
                        }
                    }
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
                view = new TextView(getActivity());
                view.setPadding(10, 10, 10, 10);
            }
            TextView tv = (TextView) view;
            tv.setText(files.get(i).getAbsolutePath().substring(sdcard.length()));
            tv.setTag(files.get(i));
            return view;
        }
    }
}
