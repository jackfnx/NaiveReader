package sixue.naivereader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import sixue.naivereader.data.Book;
import sixue.naivereader.helper.LocalTextLoader;

public class AddLocalBookFragment extends Fragment {

    private static final int REQUEST_CODE_OPEN = 0;

    public AddLocalBookFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_local_book, container, false);

        Button button = v.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/plain");
                startActivityForResult(intent, REQUEST_CODE_OPEN);
            }
        });

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if ((resultCode == Activity.RESULT_OK) && (requestCode == REQUEST_CODE_OPEN)) {
            Uri uri = data.getData();
            if (uri != null) {
                int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                this.getContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                Book b = BookLoader.getInstance().findBook(uri.toString());
                if (b != null) {
                    BookLoader.getInstance().bookBubble(b);
                } else {
                    BookLoader.getInstance().addBook(LocalTextLoader.createBook(uri.toString()));
                }
                FragmentActivity activity = getActivity();
                if (activity != null) {
                    activity.finish();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
