import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.realtodoapp.databinding.*
import com.example.realtodoapp.model.TodoPackageDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MyRoomFragment : Fragment() {

    lateinit var fragmentMyRoomBinding: FragmentMyRoomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("CheckResult", "CommitPrefEdits")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fragmentMyRoomBinding = FragmentMyRoomBinding.inflate(layoutInflater)

        fragmentMyRoomBinding.oneProgressView.progress = 30f
        fragmentMyRoomBinding.twoProgressView.progress = 40f
        fragmentMyRoomBinding.threeProgressView.progress = 50f
        fragmentMyRoomBinding.fourProgressView.progress = 60f


        val view = fragmentMyRoomBinding.root
        return view
    }
}