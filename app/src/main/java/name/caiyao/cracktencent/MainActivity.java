package name.caiyao.cracktencent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import name.caiyao.cracktencent.utils.Utils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(){
            @Override
            public void run() {
                Utils.init(MainActivity.this);
                Utils.copyDatabase(MainActivity.this);
            }
        }.start();
    }
}
