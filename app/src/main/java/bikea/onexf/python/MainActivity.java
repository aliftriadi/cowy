package bikea.onexf.python;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        Python python = Python.getInstance();

        PyObject pyObject = python.getModule("script");
        PyObject object = pyObject.callAttr("main", 6.8, 225);

        Log.e("TAG", "onCreate: " + object );
    }
}