package sixue.naivereader;


import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

public class AddLocalTreeFragment extends Fragment {


    public AddLocalTreeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_add_local_tree, container, false);

        ListView lvFiles = (ListView) v.findViewById(R.id.lv_files);
        final MyAdapter myAdapter = new MyAdapter(Environment.getExternalStorageDirectory());
        lvFiles.setAdapter(myAdapter);
        lvFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                File file = (File) view.getTag();
                if (file != null) {
                    if (file.isDirectory()) {
                        myAdapter.setCurrentDir(file);
                        myAdapter.notifyDataSetChanged();
                    } else {
                        BookLoader.getInstance().addBook(Utils.createBook(file));
                        getActivity().finish();
                    }
                }
            }
        });
        return v;
    }

    private class MyAdapter extends BaseAdapter {
        private File[] children;

        public MyAdapter(File root) {
            setCurrentDir(root);
        }

        @Override
        public int getCount() {
            return children.length;
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
            File f = children[i];
            tv.setText(f.isDirectory() ? "<" + f.getName() + ">" : f.getName());
            tv.setTag(f);
            return view;
        }

        public void setCurrentDir(File currentDir) {
            if (!currentDir.isDirectory()) {
                this.children = new File[]{};
            } else {
                this.children = currentDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return !file.getName().startsWith(".") &&
                                (file.isDirectory() ||
                                        file.getName().toLowerCase().endsWith(".txt"));

                    }
                });
                Arrays.sort(this.children, new Comparator<File>() {
                    @Override
                    public int compare(File l, File r) {
                        if (l.isDirectory() == r.isDirectory()) {
                            return l.getName().compareTo(r.getName());
                        } else if (l.isDirectory()) {
                            return 1;
                        } else { // r.isDirectory()
                            return -1;
                        }
                    }
                });
            }
        }
    }
}
