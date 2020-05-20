package com.example.wifisocket;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends Activity {
    private EditText et1, et2;
    private TextView tv4;
    private Socket socket;
    DataOutputStream writeSocket;
    private DataInputStream readSocket;
    private Handler mHandler = new Handler();

    private ConnectivityManager cManager;
    private ServerSocket serverSocket;
    private static final int PICK_FROM_CAMERA = 0;
    private static final int PICK_FROM_ALBUM = 1;
    private static final int CROP_FROM_iMAGE = 2;
    private static final int REQUEST_IMAGE = 3;
    Uri mImageCaptureUri;
    NetworkInfo wifi;
    private ImageView iv_UserPhoto;
    String absoultePath;
    private String dataUtil;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et1 = (EditText) findViewById(R.id.editText1);  //IP 입력창
        et2 = (EditText) findViewById(R.id.editText2);  //PORT 입력창
        tv4 = (TextView) findViewById(R.id.textView4);  //IP 주소 보여주는 텍스트뷰
        cManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        iv_UserPhoto = (ImageView) this.findViewById(R.id.imageView);
    }

    @SuppressWarnings("deprecation")

    public void OnClick(View v) {
        switch (v.getId()) {
            case R.id.button1:
                (new Connect()).start();
                break;
            case R.id.button2:
                (new Disconnect()).start();
                break;
            case R.id.button3:
                (new SetServer()).start();
                break;
            case R.id.button4:
                (new CloseServer()).start();
                break;
            case R.id.button5:
                wifi = cManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                //서버와 클라이언트가 연결되었을때 서버의 IP주소 가져옴
                if (wifi.isConnected()) {
                    WifiManager wManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    WifiInfo info = wManager.getConnectionInfo(); //현재 와이파이가 활성인 경우 동작정보 반환
                    tv4.setText("IP Address : " + Formatter.formatIpAddress(info.getIpAddress()));// IP주소 반환
                } else { //연결 안됐을 때
                    tv4.setText("Disconnected");
                }

                break;
            case R.id.button6:
                (new SendImage()).start();
            case R.id.button7:
                (new Recevie()).start();
            case R.id.btn_UploadPicture:
                DialogInterface.OnClickListener albumListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
                        startActivityForResult(intent, PICK_FROM_ALBUM);
                    }
                };
                DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                };
                new AlertDialog.Builder(this)
                        .setTitle("업로드할 이미지 선택")
                        .setNeutralButton("앨범선택", albumListener)
                        .setNegativeButton("취소", cancelListener)
                        .show();
        }
    }
// 스레드란 프로세스내에서 실제로 작업을 수행하는 주체
// 안드로이드의 UI는 기본적으로 메인스레드를 주축으로하는 싱글 스레드 모델로 동작하므로, 메인 스레드에서는 긴 작업을 피해야 합니다.
// 즉 긴 작업은 여분의 다른 스레드에서 실행하고 UI를 바꿀 때는 UI 스레드로 접근하도록 스레드가나 통신 방법을 사용해야합니다.
// 이 때 사용하는 것이 Message나 Runnable 객체를 받아와 다른 곳으로 전달해주는 Handler 클래스입니다.
// 이미지를 받아와 UI에 표시해줄 때, AsyncTask와 더불어 많이 사용하고 있죠.

    class Connect extends Thread {
        public void run() {   //JVM(자바 가상 머신 자바와 운영체제 중재해주는 역할함)에 의해 호출됨 이 메소드 종료시 스레드도 종료됨, 스레드 코드
            Log.d("Connect", "Run Connect"); //메세지 송신
            String ip = null; //IP 초기화
            int port = 0; //PORT 초기화

            try {
                ip = et1.getText().toString(); //ip값 입력받고
                port = Integer.parseInt(et2.getText().toString()); //PORT값 입력받고
            } catch (Exception e) {
                final String recvInput = "정확히 입력하세요!";
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        setToast(recvInput); //예외시 정확히 입력하세요 문구가 하단에 출력
                    }
                });
            }
            try {
                socket = new Socket(ip, port);   // 소켓생성하고 지정된 ip port에서 대기하는 원격 응용프로그램의 소켓에 연결
                writeSocket = new DataOutputStream(socket.getOutputStream()); //소켓이 서버 데이터로 전송
                readSocket = new DataInputStream(socket.getInputStream()); //소켓이 상대편으로부터 받은 데이터를 읽을 수 있음

                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        setToast("연결에 성공하였습니다."); //문구 하단 출력
                    }

                });
                (new RecvSocket()).start();
            } catch (Exception e) {
                final String recvInput = "연결에 실패하였습니다.";
                Log.d("Connect", e.getMessage());
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        setToast(recvInput);// 예외시 문구 출력
                    }

                });

            }

        }
    }

    class Disconnect extends Thread {
        public void run() {
            try {
                if (socket != null) {
                    socket.close(); //소켓닫기
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            setToast("연결이 종료되었습니다.");
                        }
                    });

                }

            } catch (Exception e) {
                final String recvInput = "연결에 실패하였습니다."; //예외 처리
                Log.d("Connect", e.getMessage());
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        setToast(recvInput);
                    }

                });

            }

        }
    }

    class SetServer extends Thread {

        public void run() {
            try {
                int port = Integer.parseInt(et2.getText().toString()); //포트값 저장
                serverSocket = new ServerSocket(port); //그 포트값 받아서 소켓생성
                final String result = "서버 포트 " + port + " 가 준비되었습니다.";

                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        setToast(result); // 문구 출력
                    }
                });

                socket = serverSocket.accept();
                writeSocket = new DataOutputStream(socket.getOutputStream());
                readSocket = new DataInputStream(socket.getInputStream());

                while (true) {
                    byte[] b = new byte[100];
                    int ac = readSocket.read(b, 0, b.length); //상대편으로부터 데이터 받아옴
                    String input = new String(b, 0, b.length); // 데이터 String화?
                    final String recvInput = input.trim();// 문자열 반환

                    if (ac == -1){
                        break;
                    }

                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            setToast(recvInput);
                        }

                    });
                }
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        setToast("연결이 종료되었습니다.");
                    }

                });
                serverSocket.close();
                socket.close();
            } catch (Exception e) {
                final String recvInput = "서버 준비에 실패하였습니다.";
                Log.d("SetServer", e.getMessage());
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        setToast(recvInput);
                    }

                });

            }

        }
    }

    class RecvSocket extends Thread {

        public void run() {
            try {
                readSocket = new DataInputStream(socket.getInputStream());

                while (true) {
                    byte[] b = new byte[100];
                    int ac = readSocket.read(b, 0, b.length);
                    String input = new String(b, 0, b.length);
                    final String recvInput = input.trim();

                    if (ac == -1){
                        break;}

                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            setToast(recvInput);
                        }

                    });
                }
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        setToast("연결이 종료되었습니다.");
                    }

                });
            } catch (Exception e) {
                final String recvInput = "연결에 문제가 발생하여 종료되었습니다..";
                Log.d("SetServer", e.getMessage());
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        setToast(recvInput);
                    }

                });

            }

        }
    }

    class CloseServer extends Thread {
        public void run() {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                    socket.close();

                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            setToast("서버가 종료되었습니다..");
                        }
                    });
                }
            } catch (Exception e) {
                final String recvInput = "서버 준비에 실패하였습니다.";
                Log.d("SetServer", e.getMessage());
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        setToast(recvInput);
                    }

                });

            }

        }
    }

    class SendImage extends Thread {
        public void run(){


                }

        }

    class Recevie extends Thread {
        public void run() {
            try {
                Socket socket = new Socket("IP주소", 9999);

                BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());

                while(true) {
                    byte[] imagebuffer = null;
                    int size = 0;

                    byte[] buffer = new byte[10240];

                    int read;
                    while((read = bis.read(buffer)) != -1) {
                        if (imagebuffer == null) {
                            //처음 4byte에서 비트맵이미지의 총크기를 추출해 따로 저장한다
                            byte[] sizebuffer = new byte[4];
                            System.arraycopy(buffer, 0, sizebuffer, 0, sizebuffer.length);
                            size = getInt(sizebuffer);
                            read -= sizebuffer.length;

                            //나머지는 이미지버퍼 배열에 저장한다
                            imagebuffer = new byte[read];
                            System.arraycopy(buffer, sizebuffer.length, imagebuffer, 0, read);
                        }
                        else {
                            //이미지버퍼 배열에 계속 이어서 저장한다
                            byte[] preimagebuffer = imagebuffer.clone();
                            imagebuffer = new byte[read + preimagebuffer.length];
                            System.arraycopy(preimagebuffer, 0, imagebuffer, 0, preimagebuffer.length);

                            System.arraycopy(buffer, 0, imagebuffer, imagebuffer.length - read, read);
                        }

                        //이미지버퍼 배열에 총크기만큼 다 받아졌다면 이미지를 저장하고 끝낸다
                        if(imagebuffer.length >= size) {
                            Bundle bundle = new Bundle();
                            bundle.putByteArray("Data", imagebuffer);

                            Message msg = mResultHandler.obtainMessage();
                            msg.setData(bundle);
                            mResultHandler.sendMessage(msg);

                            imagebuffer = null;
                            size = 0;
                        }

                    }
                }

                socket.close();
                bis.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //byte배열을 숫자로 바꾼다
    private int getInt(byte[] data) {
        int s1 = data[0] & 0xFF;
        int s2 = data[1] & 0xFF;
        int s3 = data[2] & 0xFF;
        int s4 = data[3] & 0xFF;

        return ((s1 << 24) + (s2 << 16) + (s3 << 8) + (s4 << 0));
    }
    //이미지뷰에 비트맵을 넣는다
    public Handler mResultHandler = new Handler() {
        public void handleMessage(Message msg) {
            byte[] data = msg.getData().getByteArray("Data");

            ((ImageView) findViewById(R.id.imageView).setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
        }

    }


    //토스트생성
    void setToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        byte[] image = null;
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case PICK_FROM_ALBUM: {
                // 이후의 처리가 카메라와 같으므로 일단  break없이 진행합니다.
                // 실제 코드에서는 좀더 합리적인 방법을 선택하시기 바랍니다.
                mImageCaptureUri = data.getData();
                Log.d("SmartWheel", mImageCaptureUri.getPath().toString());
            }
            case PICK_FROM_CAMERA: {
                // 이미지를 가져온 이후의 리사이즈할 이미지 크기를 결정합니다.
                // 이후에 이미지 크롭 어플리케이션을 호출하게 됩니다.
                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(mImageCaptureUri, "image/*");
                // CROP할 이미지를 200*200 크기로 저장
                intent.putExtra("outputX", 200); // CROP한 이미지의 x축 크기
                intent.putExtra("outputY", 200); // CROP한 이미지의 y축 크기
                intent.putExtra("aspectX", 1); // CROP 박스의 X축 비율
                intent.putExtra("aspectY", 1); // CROP 박스의 Y축 비율
                intent.putExtra("scale", true);
                intent.putExtra("return-data", true);
                startActivityForResult(intent, CROP_FROM_iMAGE);
                startActivityForResult(intent, REQUEST_IMAGE);// CROP_FROM_CAMERA case문 이동
                break;

            }
            case CROP_FROM_iMAGE: {
                // 크롭이 된 이후의 이미지를 넘겨 받습니다.
                // 이미지뷰에 이미지를 보여준다거나 부가적인 작업 이후에
                // 임시 파일을 삭제합니다.
                if (resultCode != RESULT_OK) {
                    return;
                }
                final Bundle extras = data.getExtras();
                // CROP된 이미지를 저장하기 위한 FILE 경로
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/SmartWheel/" + System.currentTimeMillis() + ".jpg";
                if (extras != null) {
                    Bitmap photo = extras.getParcelable("data"); // CROP된 BITMAP
                    iv_UserPhoto.setImageBitmap(photo); // 레이아웃의 이미지칸에 CROP된 BITMAP을 보여줌
                    storeCropImage(photo, filePath); // CROP된 이미지를 외부저장소, 앨범에 저장한다.
                    absoultePath = filePath;
                    break;
                }
                // 임시 파일 삭제
                File f = new File(mImageCaptureUri.getPath());
                if (f.exists()) {
                    f.delete();
                }
            }
            case REQUEST_IMAGE: {
                try {
                    InputStream is = getContentResolver().openInputStream(data.getData());
                    image = dataUtil.getBytes(is);
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
                placeInfo.setImage(image);
                break;
            }
        }
    }
    public byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024; // 버퍼 크기
        byte[] buffer = new byte[bufferSize]; // 버퍼 배열

        int len = 0;

        // InputStream에서 읽어올 게 없을 때까지 바이트 배열에 쓴다.
        while ((len = is.read(buffer)) != -1)
            byteBuffer.write(buffer, 0, len);

        return byteBuffer.toByteArray();
    }

    private void storeCropImage(Bitmap bitmap, String filePath) {
        // SmartWheel 폴더를 생성하여 이미지를 저장하는 방식이다.
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/SmartWheel";
        File directory_SmartWheel = new File(dirPath);
        if(!directory_SmartWheel.exists()) { // SmartWheel 디렉터리에 폴더가 없다면 (새로 이미지를 저장할 경우에 속한다.)
            directory_SmartWheel.mkdir();
        }
        File copyFile = new File(filePath);

        try {
            copyFile.createNewFile();
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(copyFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            // sendBroadcast를 통해 Crop된 사진을 앨범에 보이도록 갱신한다.
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(copyFile)));
            out.flush();
            out.close();
        } catch (Exception e) {
        }
    }


}









