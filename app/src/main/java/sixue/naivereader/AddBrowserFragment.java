package sixue.naivereader;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import sixue.naivereader.jyl.JylActivity;


public class AddBrowserFragment extends Fragment {

    public AddBrowserFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_browser, container, false);
        //Button sisButton = (Button) v.findViewById(R.id.browse_forum);
        Button orButton = (Button) v.findViewById(R.id.browse_oldrain);
        orButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), JylActivity.class);
                startActivity(intent);
            }
        });
        return v;
    }
}
