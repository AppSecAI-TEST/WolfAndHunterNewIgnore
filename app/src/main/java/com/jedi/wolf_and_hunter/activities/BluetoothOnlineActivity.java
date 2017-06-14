package com.jedi.wolf_and_hunter.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.jedi.wolf_and_hunter.R;
import com.jedi.wolf_and_hunter.myObj.PlayerInfo;
import com.jedi.wolf_and_hunter.myViews.characters.BaseCharacterView;
import com.jedi.wolf_and_hunter.utils.BluetoothController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BluetoothOnlineActivity extends Activity {
    String myName;
    String myMac;
    boolean isAcceptStop;
    BluetoothOnlineActivity.MyHandler myHandler;
    boolean isRoomOwner = false;
    BluetoothAdapter bluetoothAdapter;
    SimpleAdapter discoveredDeviceAdapter;
    SimpleAdapter joinedPlayerAdapter;
    ListView discoverDevicesListView;
    ListView joinedPlayerListView;
    List<Map<String, String>> discoveredDeviceInfoList;
    List<Map<String, String>> joinedPlayerDeviceSetInfoList;
    Set<BluetoothDevice> discoveredDevices;
    BluetoothDevice serverDevice;
    //    Set<String> playerDeviceMacs;
    BluetoothController bluetoothController;
    Timer timerForRefreshPlayerListView;
    Set<BluetoothDevice> needToBondDeviceSet;
    Set<BluetoothDevice> joinedPlayerDeviceSet;
    boolean isLoopSearching;
    private BluetoothServerSocket bluetoothServerSocket;


    class MyHandler extends Handler {
        /**
         * Subclasses must implement this to receive messages.
         *
         * @param msg
         */
        public static final int REFRESH_PLAYER_LIST_VIEW = 1;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case REFRESH_PLAYER_LIST_VIEW:
                    Toast.makeText(BluetoothOnlineActivity.this, "等待对方设备确认配对。。。", Toast.LENGTH_SHORT);
                    boolean needToNotify = false;
                    Set<BluetoothDevice> removeDevices = null;
                    for (BluetoothDevice needToBondDevice : needToBondDeviceSet) {
                        int state = needToBondDevice.getBondState();
                        if (state == BluetoothDevice.BOND_BONDED) {
//                            playerDeviceMacs.add(needToBondDevice.getAddress());
                            Map<String, String> dataMap = new HashMap<String, String>();
                            dataMap.put("name", needToBondDevice.getName());
                            dataMap.put("mac", needToBondDevice.getAddress());
                            joinedPlayerDeviceSetInfoList.add(dataMap);
                            needToNotify = true;
                            if (removeDevices == null)
                                removeDevices = new HashSet<BluetoothDevice>();
                            removeDevices.add(needToBondDevice);
                            joinedPlayerDeviceSet.add(needToBondDevice);
                        }
                    }
                    if (removeDevices != null) {
                        for (BluetoothDevice device : removeDevices) {
                            needToBondDeviceSet.remove(device);
                        }
                        removeDevices.clear();
                    }
                    for (BluetoothDevice joinedPlayerDevice : joinedPlayerDeviceSet) {
                        if (joinedPlayerDevice.getAddress().equals(myMac))
                            continue;
                        if (joinedPlayerDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                            if (removeDevices == null)
                                removeDevices = new HashSet<BluetoothDevice>();
                            removeDevices.add(joinedPlayerDevice);
                            Map<String, String> removeMap = null;
                            for (Map<String, String> deviceMap : joinedPlayerDeviceSetInfoList) {
                                String mac = deviceMap.get("mac");
                                if (mac.equals(joinedPlayerDevice.getAddress())) {
                                    removeMap = deviceMap;
                                    break;
                                }
                            }
                            if (removeMap != null) {
                                joinedPlayerDeviceSetInfoList.remove(removeMap);
                                joinedPlayerDeviceSet.remove(joinedPlayerDevice);
//                                playerDeviceMacs.remove(joinedPlayerDevice.getAddress());
                            }
                            needToNotify = true;
                        }
                    }
                    if (needToNotify) {
                        joinedPlayerAdapter.notifyDataSetChanged();
                    }

                    break;
            }
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            String action = intent.getAction();
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 1);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            Toast.makeText(context, "蓝牙已关闭", Toast.LENGTH_SHORT).show();

                            return;
                        case BluetoothAdapter.STATE_ON:
                            Toast.makeText(context, "蓝牙已打开", Toast.LENGTH_SHORT).show();
                            return;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Toast.makeText(context, "正在打开蓝牙", Toast.LENGTH_SHORT).show();
                            return;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Toast.makeText(context, "正在关闭蓝牙", Toast.LENGTH_SHORT).show();
                            return;

                    }
                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Toast.makeText(context, "开始搜索设备", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
//                    if (isLoopSearching)
//                        searchDevice(null);
//                    else
//                    Toast.makeText(context, "结束搜索设备", Toast.LENGTH_SHORT).show();

                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, 1);
                    switch (scanMode) {
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                            Intent in = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                            in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                            startActivity(in);
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                            Toast.makeText(context, "本设备开启允许被发现", Toast.LENGTH_SHORT).show();
                    }

                    break;
                case BluetoothDevice.ACTION_FOUND:
                    Toast.makeText(context, "发现设备", Toast.LENGTH_SHORT).show();
                    //从Intent中获取设备的BluetoothDevice对象
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    serverDevice = device;
                    String deviceMAC = device.getAddress();
                    boolean hasDevice = false;
                    for (Map<String, String> deviceMap : discoveredDeviceInfoList) {
                        if (deviceMap.get("mac").equals(deviceMAC)) {
                            hasDevice = true;
                            break;
                        }
                    }
                    if (hasDevice == false) {
                        Map<String, String> dataMap = new HashMap<String, String>();
                        dataMap.put("name", device.getName());
                        dataMap.put("mac", device.getAddress());
                        discoveredDeviceInfoList.add(dataMap);
                        discoveredDeviceAdapter.notifyDataSetChanged();
                        discoveredDevices.add(device);
                    }

                    break;

            }


        }
    };

    public void searchDevice(View view) {
        isLoopSearching = true;
        if (bluetoothAdapter.isEnabled() == false) {
            Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
            return;
        }
//        Intent in = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
//        startActivity(in);
        bluetoothController.startDiscovery();

    }

    private void paireDevice(BluetoothDevice device) {
        try {
            Method createBondMethod = BluetoothDevice.class
                    .getMethod("createBond");
            createBondMethod.invoke(device);
        } catch (Exception e) {
            // TODO: handle exception
            e.getStackTrace();

        }
    }

    class DiscoverDeviceListViewOnItemClickListener implements AdapterView.OnItemClickListener {


        /**
         * Callback method to be invoked when an item in this AdapterView has
         * been clicked.
         * <p>
         * Implementers can call getItemAtPosition(position) if they need
         * to access the data associated with the selected item.
         *
         * @param parent   The AdapterView where the click happened.
         * @param view     The view within the AdapterView that was clicked (this
         *                 will be a view provided by the adapter)
         * @param position The position of the view in the adapter.
         * @param id       The row id of the item that was clicked.
         */
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            LinearLayout layout = (LinearLayout) view;
            TextView macTextView = (TextView) layout.getChildAt(1);
            TextView nameTextView = (TextView) layout.getChildAt(0);
            String mac = macTextView.getText().toString();
            if (mac.equals(myMac)) {
                return;
            }
            BluetoothDevice thisDevice = null;
            for (BluetoothDevice device : discoveredDevices) {
                if (device.getAddress().equals(mac)) {
                    thisDevice = device;
                    break;
                }
            }
            if (thisDevice == null) {
                Toast.makeText(BluetoothOnlineActivity.this, "找不到设备", Toast.LENGTH_SHORT).show();
            } else {
                if (joinedPlayerDeviceSet.contains(thisDevice) == false) {
                    paireDevice(thisDevice);
                    if (isRoomOwner) {
                        needToBondDeviceSet.add(thisDevice);
                    }
                }
            }

        }
    }

    class JoinedPlayerListViewOnItemClickListener implements AdapterView.OnItemClickListener {
        Context context;

        public JoinedPlayerListViewOnItemClickListener(Context context) {
            this.context = context;
        }

        /**
         * Callback method to be invoked when an item in this AdapterView has
         * been clicked.
         * <p>
         * Implementers can call getItemAtPosition(position) if they need
         * to access the data associated with the selected item.
         *
         * @param parent   The AdapterView where the click happened.
         * @param view     The view within the AdapterView that was clicked (this
         *                 will be a view provided by the adapter)
         * @param position The position of the view in the adapter.
         * @param id       The row id of the item that was clicked.
         */
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            LinearLayout layout = (LinearLayout) view;
            TextView macTextView = (TextView) layout.getChildAt(1);
            TextView nameTextView = (TextView) layout.getChildAt(0);
            String mac = macTextView.getText().toString();
            if (mac.equals(myMac)) {
                if (isRoomOwner == false) {
                    new AlertDialog.Builder(context).setTitle("提示")//设置对话框标题

                            .setMessage("是否成为房主（注意，房主断开连接将导致该局游戏结束）")//设置显示的内容

                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {//添加确定按钮
                                @Override

                                public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件

                                    isRoomOwner = true;

                                }

                            }).setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加返回按钮


                        @Override

                        public void onClick(DialogInterface dialog, int which) {//响应事件


                        }

                    }).show();//在按键响应事件中显示此对话框
                } else {
                    new AlertDialog.Builder(context).setTitle("提示")//设置对话框标题

                            .setMessage("是否放弃房主资格（房间内玩家将解散）")//设置显示的内容

                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {//添加确定按钮
                                @Override

                                public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件

                                    isRoomOwner = false;

                                }

                            }).setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加返回按钮


                        @Override

                        public void onClick(DialogInterface dialog, int which) {//响应事件


                        }

                    }).show();//在按键响应事件中显示此对话框
                }

                return;
            }


        }
    }


    public class AcceptThread extends Thread {


        public AcceptThread() {
            try {
                BluetoothServerSocket tmp = null;
                tmp = bluetoothController.mBluetoothAdapter.listenUsingRfcommWithServiceRecord("BluetoothServer", UUID.fromString(BluetoothController.mUUID));
                bluetoothServerSocket = tmp;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();
            BluetoothSocket socket = null;
            bluetoothController.cancelDiscovery();
            //不断监听直到返回连接或者发生异常
            try {
                while (isAcceptStop == false) {
                    //据说处于查找状态会影响性能哦

                    //启连接请求，这是一个阻塞方法，必须放在子线程
                    socket = bluetoothServerSocket.accept();
                    InputStream is = null;
                    OutputStream os = null;
                    is = socket.getInputStream();
                    StringBuffer str = new StringBuffer();
                    byte[] buff = new byte[1024];

                    while (is.read(buff) !=-1) {
                        str.append(new String(buff, 0, buff.length));
                    }
                    Log.i("------serverR-------", str.toString());
                    String s = "return";
                    buff = s.getBytes();
                    os = socket.getOutputStream();
                    os.write(buff);
                    os.flush();
//                    OutputStream os = null;
//                    //建立了连接
//                    if (socket != null) {
//                        //管理连接(在一个独立的线程里进行)
//                        manageConnectedSocket(socket);
//                        os = socket.getOutputStream();
//                        os.write(1);
//                        os.flush();
//                        os.close();
//                        try {
//                            socket.close();//关闭连接
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//
//                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                cancel();
                BluetoothController.startDiscovery();
            }
        }

        /**
         * 取消正在监听的接口
         */
        public void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void manageConnectedSocket(BluetoothSocket socket) {
            myHandler.sendEmptyMessage(0);
        }

    }


    class ConnectThread extends Thread {
        private BluetoothDevice serverDevice;
        private BluetoothSocket socket;

        public ConnectThread(BluetoothDevice device) {
            serverDevice = device;

            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(bluetoothController.mUUID)); //应该是这里导致Service discovery failed问题

            } catch (IOException e) {
                Log.d("BLUETOOTH_CLIENT", e.getMessage());
            }
            socket = tmp;
        }

        @Override
        public void run() {
            super.run();
            //取消搜索因为搜索会让连接变慢
            bluetoothController.mBluetoothAdapter.cancelDiscovery();
            OutputStream os = null;
            InputStream is = null;

            try {
//                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(mUUID));
                //通过socket连接设备，这是一个阻塞操作，知道连接成功或发生异常
                socket.connect();
                String s = "join";
                byte[] buff = s.getBytes();
                os = socket.getOutputStream();
                os.write(buff);
                os.flush();

                is = socket.getInputStream();
                StringBuffer str = new StringBuffer();
                buff = new byte[1024];

                while (is.read(buff) > 0) {
                    str.append(new String(buff, 0, buff.length));
                }
                Log.i("------client-------", str.toString());
            } catch (IOException e) {
                e.printStackTrace();
                //无法连接，关闭socket并且退出

            } finally {
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }


            //管理连接(在独立的线程)
            // manageConnectedSocket(mmSocket);
        }

        /**
         * 取消正在进行的链接，关闭socket
         */
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    BluetoothOnlineActivity.AcceptThread acceptThread;
    BluetoothOnlineActivity.ConnectThread connectThread;

    public void runAccept(View view) {
        if (acceptThread != null && acceptThread.getState() != Thread.State.TERMINATED) {
            Toast.makeText(this, "服务只能启动一个", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "开始接受请求", Toast.LENGTH_LONG).show();
        acceptThread = new BluetoothOnlineActivity.AcceptThread();
        acceptThread.start();
    }

    public void runConnect(View view) {
        if (serverDevice == null || connectThread != null && connectThread.getState() != Thread.State.TERMINATED)
            return;
        connectThread = new ConnectThread(serverDevice);
        connectThread.start();


    }

    public void startGame(View view) {
        if (joinedPlayerDeviceSet == null || joinedPlayerDeviceSet.size() == 0)
            Toast.makeText(this, "请先找到配对玩家", Toast.LENGTH_SHORT);
        ArrayList<PlayerInfo> playerInfos = new ArrayList<PlayerInfo>();
        for (BluetoothDevice device : joinedPlayerDeviceSet) {
            PlayerInfo playerInfo = new PlayerInfo(true, 1, BaseCharacterView.CHARACTER_TYPE_HUNTER, 1, device);
            playerInfos.add(playerInfo);
        }
        Intent i = new Intent(this, BluetoothOnlineGameBaseAreaActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("playerInfos", playerInfos);
        i.putExtras(bundle);
        startActivity(i);
    }

    class RefreshPlayerListViewTask extends TimerTask {

        @Override
        public void run() {

            myHandler.sendEmptyMessage(MyHandler.REFRESH_PLAYER_LIST_VIEW);

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_online);
        discoveredDevices = new HashSet<BluetoothDevice>();
        needToBondDeviceSet = new HashSet<BluetoothDevice>();
        joinedPlayerDeviceSet = new HashSet<BluetoothDevice>();
//        playerDeviceMacs = new HashSet<String>();
        bluetoothController = new BluetoothController(this);
        bluetoothAdapter = BluetoothController.mBluetoothAdapter;
        myHandler = new BluetoothOnlineActivity.MyHandler();


        myName = bluetoothAdapter.getName();
        //6.0以上系统用adapter只会获取到20.00.00.00.00,一下是网上找到的神方法


        myMac = android.provider.Settings.Secure.getString(this.getContentResolver(), "bluetooth_address");
        if (myMac == null)
            myMac = bluetoothAdapter.getAddress();
        BluetoothDevice myDevice = bluetoothAdapter.getRemoteDevice(myMac);
        joinedPlayerDeviceSet.add(myDevice);
        Map<String, String> myInfoMap = new HashMap<String, String>();
        myInfoMap.put("name", "（本机）" + myName);
        myInfoMap.put("mac", myMac);


        discoverDevicesListView = (ListView) findViewById(R.id.list_view_devices);
        discoverDevicesListView.setOnItemClickListener(new BluetoothOnlineActivity.DiscoverDeviceListViewOnItemClickListener());
        discoveredDeviceInfoList = new ArrayList<Map<String, String>>();
        discoveredDeviceAdapter = new SimpleAdapter(this, discoveredDeviceInfoList, R.layout.online_user_list_item, new String[]{"name", "mac"}, new int[]{R.id.device_info_name, R.id.device_info_mac});
        discoverDevicesListView.setAdapter(discoveredDeviceAdapter);


        joinedPlayerDeviceSetInfoList = new ArrayList<Map<String, String>>();
        joinedPlayerListView = (ListView) findViewById(R.id.list_view_joined_player);
        joinedPlayerListView.setOnItemClickListener(new BluetoothOnlineActivity.JoinedPlayerListViewOnItemClickListener(this));
        joinedPlayerAdapter = new SimpleAdapter(this, joinedPlayerDeviceSetInfoList, R.layout.online_user_list_item, new String[]{"name", "mac"}, new int[]{R.id.device_info_name, R.id.device_info_mac});
        joinedPlayerListView.setAdapter(joinedPlayerAdapter);
        joinedPlayerDeviceSetInfoList.add(myInfoMap);
        joinedPlayerAdapter.notifyDataSetChanged();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
//        filter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);//这条没用，不明原因，我猜是不广播这个？
//        filter.addAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);//这条没用，不明原因，我猜是不广播这个？
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(receiver, filter);

        if (bluetoothAdapter.isEnabled()) {
//            Toast.makeText(this, "蓝牙可用,本设备可见10秒", Toast.LENGTH_LONG).show();
            Intent in = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(in);
        } else {
            Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }


        timerForRefreshPlayerListView = new Timer();
        timerForRefreshPlayerListView.schedule(new RefreshPlayerListViewTask(), 100, 1000);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if (acceptThread != null && acceptThread.getState() != Thread.State.TERMINATED) {
            isAcceptStop = true;
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if (connectThread != null && connectThread.getState() != Thread.State.TERMINATED) {
            connectThread.interrupt();
            try {
                connectThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        timerForRefreshPlayerListView.cancel();
//        bluetoothController.closeDiscoverableTimeout();
        Intent in = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
        startActivity(in);
        bluetoothController.cancelDiscovery();
    }
}
