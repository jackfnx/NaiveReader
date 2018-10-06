package sixue.naivereader;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.provider.NetProvider;
import sixue.naivereader.provider.NetProviderCollections;

public class NetProviderManagerActivity extends AppCompatActivity {

    private List<NetProvider> netProviders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_provider_manager);

        netProviders = new ArrayList<>(NetProviderCollections.getProviders(this));

        ListView listView = findViewById(R.id.list_net_providers);
        listView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return netProviders.size();
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
                if (view == null)
                    view = LayoutInflater.from(NetProviderManagerActivity.this).inflate(R.layout.listviewitem_provider, viewGroup, false);

                final TextView name = view.findViewById(R.id.name);
                final Switch sw = view.findViewById(R.id.sw);

                final NetProvider netProvider = netProviders.get(i);
                name.setText(netProvider.getProviderName());
                sw.setChecked(netProvider.isActive());
                sw.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        netProvider.setActive(sw.isChecked());
                        NetProviderCollections.saveSettings(NetProviderManagerActivity.this);
                    }
                });
                sw.setChecked(netProvider.isActive());

                return view;
            }
        });

        Button cg = findViewById(R.id.garbage_button);
        cg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BookLoader.getInstance().clearGarbage();
                Toast.makeText(NetProviderManagerActivity.this, "[GARBAGE] clear.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
