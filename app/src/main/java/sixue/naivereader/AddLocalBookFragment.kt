package sixue.naivereader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import sixue.naivereader.helper.LocalTextLoader.createBook

class AddLocalBookFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_add_local_book, container, false)
        val button = v.findViewById<Button>(R.id.button)
        val getTextDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                val takeFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                val b = BookLoader.findBook(uri.toString())
                if (b != null) {
                    BookLoader.bookBubble(b)
                } else {
                    BookLoader.addBook(createBook(uri.toString()))
                }
                activity?.finish()
            }
        }
        button.setOnClickListener {
            getTextDocument.launch(arrayOf("text/plain"))
        }
        return v
    }
}