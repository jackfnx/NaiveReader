package sixue.naviereader;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ReadActivity extends AppCompatActivity {

    private static final String TAG = "ReadActivity";
    private String text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        Utils.verifyStoragePermissions(this);
        text = Utils.readText("test.txt");
        if (text == null) {
            text = "Can't open file.";
        }

        ReaderView readerView = (ReaderView) findViewById(R.id.textArea);
        readerView.setText(text);
    }
}
