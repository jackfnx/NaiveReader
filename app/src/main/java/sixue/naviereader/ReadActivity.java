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
    private List<String> lines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        Utils.verifyStoragePermissions(this);
        lines = Utils.readText("test.txt");
        if (lines == null) {
            lines = new ArrayList<>();
            lines.add("Can't open file.");
        }

        attachTextArea();
    }

    private void attachTextArea() {
        List<TextView> lineViews = new ArrayList<>();
        LinearLayout layout = (LinearLayout) findViewById(R.id.textArea);
        if (layout != null) {
            int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            layout.measure(spec, spec);
            int totalHeight = layout.getMeasuredHeight();
            int totalWidth = layout.getMeasuredWidth();

            TextView example = generateLine();
            example.measure(spec, spec);
            int oneHeight = example.getMeasuredHeight();
            int oneWidth = example.getMeasuredWidth();
            int lineLength = totalWidth / oneWidth;

            for (int i = 0; i < totalHeight / oneHeight; i++) {
                TextView lineView = generateLine();
                layout.addView(lineView);
                lineViews.add(lineView);

                View sep = new View(this);
                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
                sep.setLayoutParams(lp2);
                sep.setBackgroundColor(0xFFCCCCCC);
                layout.addView(sep);
            }

            for (int i = 0, k = 0; i < lines.size() && i < 10 && k < lineViews.size(); i++) {
                String paragraphs = lines.get(i);
                int j = 0;
                for (; ((j + 1) * lineLength) < paragraphs.length() && k < lineViews.size(); j++) {
                    String line = paragraphs.substring(j * lineLength, (j + 1) * lineLength);
                    Log.i(TAG, "line:" + line);
                    lineViews.get(k).setText(line);
                    k++;
                }
                String lastLine = paragraphs.substring(j * lineLength);
                Log.i(TAG, "line:" + lastLine);
                lineViews.get(k).setText(lastLine);
                k++;
            }
        }
    }

    private TextView generateLine() {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(lp1);
        tv.setTextSize(18.0f);
        tv.setText("空");
        tv.setTextColor(0xFF000000);
        tv.setMaxLines(1);
        return tv;
    }
}
