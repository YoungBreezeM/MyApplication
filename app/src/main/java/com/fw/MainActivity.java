package com.fw;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.widget.*;
import androidx.core.content.ContextCompat;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fw.domain.Body;
import com.fw.domain.Led;
import com.fw.domain.ReadThread;
import com.fw.domain.SendThread;
import com.fw.factory.LedThreadFactory;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author yqf
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ThreadPoolExecutor threadPoolExecutor;
    private Socket socket = null;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (socket != null && socket.isConnected()) {
                    try {
                        socket.close();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    Snackbar.make(view, "未连接...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    showInputDialog();
                }


            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        createdThreadPool();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();


        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_connect) {
            showInputDialog();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showInputDialog() {
        final EditText edit = new EditText(this);
        edit.setHint(R.string.connect_ip);
        final EditText editText = new EditText(this);
        editText.setHint(R.string.connect_port);

        AlertDialog.Builder editDialog = new AlertDialog.Builder(this);
        editDialog.setTitle(getString(R.string.connect_title));
        editDialog.setIcon(R.mipmap.ic_launcher_round);

        //设置视图
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(edit);
        linearLayout.addView(editText);

        //设置dialog布局
        editDialog.setView(linearLayout);

        //设置按钮
        editDialog.setPositiveButton(
                getString(R.string.dialog_btn_confirm_text),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean conn = connectServer("192.168.1.108", 8090);
                        String msg = "连接失败";
                        if (conn) {
                            msg = "连接成功";
                            connectGuard();

                            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                            ColorStateList colorStateList = ContextCompat.getColorStateList(getApplicationContext(), R.color.colorPrimary);
                            fab.setBackgroundTintMode(PorterDuff.Mode.SRC_ATOP);
                            fab.setBackgroundTintList(colorStateList);

                        }
                        Toast.makeText(MainActivity.this,
                                msg, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });

        editDialog.create().show();
    }


    /**
     * 连接服务器
     */
    private boolean connectServer(String ip, final Integer port) {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            socket = null;

            threadPoolExecutor.execute(() -> {
                try {
                    socket = new Socket(ip, port);
                    objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
                    objectInputStream = new ObjectInputStream(this.socket.getInputStream());
                } catch (IOException e) {
                    try {
                        if (socket != null) {
                            socket.close();
                            System.out.println("无法连接服务器");
                        }
                    } catch (IOException ex) {
                        System.out.println("客户端异常" + ex.getMessage());
                    }
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }

            });
            countDownLatch.await();
            initLED();
            return socket != null && socket.isConnected();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 创建线程池
     */
    private void createdThreadPool() {
        threadPoolExecutor = new ThreadPoolExecutor(
                10,
                100,
                5,
                TimeUnit.MICROSECONDS,
                new LinkedBlockingQueue<Runnable>(10),
                new LedThreadFactory("led")
        );
    }

    /**
     * 连接守护线程,发送心跳
     */
    private void connectGuard() {
        ScheduledExecutorService service = Executors
                .newSingleThreadScheduledExecutor();

        service.scheduleAtFixedRate(() -> {
            try {
                if (socket != null) {
                    socket.sendUrgentData(0xFF);
                }
            } catch (Exception e) {
                String msg = "断开连接..." + e.getMessage();
                System.out.println(msg);

                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                ColorStateList colorStateList = ContextCompat.getColorStateList(getApplicationContext(), R.color.colorFail);
                fab.setBackgroundTintMode(PorterDuff.Mode.SRC_ATOP);
                fab.setBackgroundTintList(colorStateList);

                Snackbar.make(fab, msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                socket = null;
                service.shutdown();
            }

        }, 0, 1000, TimeUnit.MILLISECONDS);


    }

    /**
     * 向服务器写数据
     */
    private void write(Body body) {
        if (socket != null) {
            threadPoolExecutor.execute(new SendThread(objectOutputStream, body));
        }
    }


    /**
     * 读取数据
     */
    private String read() {

        Future<Object> msg;

        return null;
    }

    /**
     * 初始化led开关
     */
    private void initLED() throws ExecutionException, InterruptedException {

        Body body = new Body("get", null, "/getLedNum", 200);
        write(body);
        Future submit = threadPoolExecutor.submit(new ReadThread(objectInputStream));


        String msg = JSON.toJSONString(submit.get());

        Body res = JSON.parseObject(msg, Body.class);

        int num = Integer.parseInt(res.getMsg().toString());

        Body changeLedReq = new Body("post", null, "/changeLed", 200);


        LinearLayout linearLayout = findViewById(R.id.ledBox);
        linearLayout.removeAllViews();


        for (int i = 0; i < num; i++) {
            Switch aSwitch = new Switch(this);
            aSwitch.setText("led" + i);
            aSwitch.setId(i);

            aSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {

                    changeLedReq.setMsg(new Led(buttonView.getId(),true));
                    write(changeLedReq);
                    System.out.println(buttonView.getId() + "开启");
                } else {
                    changeLedReq.setMsg(new Led(buttonView.getId(),false));
                    write(changeLedReq);
                    System.out.println(buttonView.getId() + "关闭");
                }
            });
            linearLayout.addView(aSwitch);
        }
    }

}
