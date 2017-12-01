package sixue.naivereader;

import android.content.Context;
import android.test.mock.MockContext;
import android.util.Log;

import org.junit.Test;

import sixue.naivereader.jyl.JylAuthor;
import sixue.naivereader.jyl.JylBook;
import sixue.naivereader.jyl.JylProvider;

import static org.junit.Assert.assertEquals;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    private Context context;

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testJylProvider() {
        context = new MockContext();
        JylProvider jylProvider = new JylProvider(context);
        jylProvider.startDownloadAuthors();
        for (JylAuthor author : jylProvider.getAuthors()) {
            jylProvider.startDownloadBooks(author);
            for (JylBook book : jylProvider.getBooks(author)) {
                Log.i("hehe", "book:"+book.getTitle());
            }
        }
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {

        }
    }
}