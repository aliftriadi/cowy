package bikea.onexf.python;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import bikea.onexf.python.tutorial.TutorialActivity;

public class SplashActivity extends AppCompatActivity {

    private int timeLoading = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent home=new Intent(SplashActivity.this, TutorialActivity.class);
                startActivity(home);
                finish();
            }
        }, timeLoading);
    }
}