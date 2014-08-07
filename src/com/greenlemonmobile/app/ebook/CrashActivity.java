
package com.greenlemonmobile.app.ebook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CrashActivity extends Activity implements OnClickListener {

    private TextView title;
    private ViewGroup titlebar;
    private Button exit;
    private Button send;
    private String crashString;
    private TextView error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            crashString = intent.getStringExtra("key0");
        }
        setContentView(R.layout.activity_crash);
        initViews();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode)
            return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.exit:
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
                break;
            case R.id.send:
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, new String[] {
                    "achellies@163.com"
                });
                i.putExtra(Intent.EXTRA_TEXT, crashString);
                i.putExtra(Intent.EXTRA_SUBJECT, "LimeMobile Crash Log");
                try {
                    startActivity(Intent.createChooser(i, getResources().getText(R.string.choose_email_client)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "There are no email clients installed.",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void initViews() {
        titlebar = (ViewGroup) findViewById(R.id.main_titlebar);
        title = (TextView) findViewById(R.id.title);
        exit = (Button) findViewById(R.id.exit);
        exit.setOnClickListener(this);
        error = (TextView) findViewById(R.id.edit_input);
        send = (Button) findViewById(R.id.send);
        send.setOnClickListener(this);
        
        if (!TextUtils.isEmpty(crashString)) {
            error.setText(crashString);
        }
    }

}
