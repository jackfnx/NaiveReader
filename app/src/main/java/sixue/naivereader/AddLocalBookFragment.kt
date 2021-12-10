package sixue.naivereader

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import sixue.naivereader.helper.LocalTextLoader.createBook

class AddLocalBookFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_add_local_book, container, false)
        val button = v.findViewById<Button>(R.id.button)
        button.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "text/plain"
            startActivityForResult(intent, REQUEST_CODE_OPEN)
        }
        return v
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_OPEN) {
            val uri = data!!.data
            if (uri != null) {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                this.requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                val b = BookLoader.findBook(uri.toString())
                if (b != null) {
                    BookLoader.bookBubble(b)
                } else {
                    BookLoader.addBook(createBook(uri.toString()))
                }
                val activity = activity
                activity?.finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val REQUEST_CODE_OPEN = 0
    }
}