package com.fndroid.machinelearningdemo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.hanuor.onyx.Onyx;
import com.hanuor.onyx.hub.OnTaskCompletion;
import com.hanuor.onyx.toolbox.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Button mButton = null;
    private Button btn_album = null;
    private ImageView mImageView;
    private TextView mTextView;
    private RequestQueue mRequestQueue;

    private static final int PHOTO_REQUEST_GALLERY = 1;// 从相册中选择

    public String URL = "http://cdn-img.instyle.com/sites/default/files/styles/622x350/public/images/2016/04/040116-emma-roberts-lead.jpg?itok=OHMonJFa";
//    public static final String URL = "http://bcs.img.r1.91.com/data/upload/2014/10_15/9/201410150922428540.jpg";
//    private static final String URL = "http://caopic.146cao.com/pic/newspic/2016-9/303701-11-1731.jpg"; //PL
//    private static final String URL = "http://caopic.146cao.com/pic/newspic/2017-8/e35078998845d2d98a5bbfda867faeac-258.jpg";
    public static final String TRANSLATION_URL = "http://api.fanyi.baidu" +
            ".com/api/trans/vip/translate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mRequestQueue = Volley.newRequestQueue(this);
        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    private void initViews() {
        mButton = (Button) findViewById(R.id.main_btn_analyse);
        btn_album = (Button) findViewById(R.id.photo_btn);
        mImageView = (ImageView) findViewById(R.id.main_iv_pic);
        mTextView = (TextView) findViewById(R.id.main_tv_result);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: ---" + URL);
                Onyx.with(MainActivity.this).fromURL(URL).getTagsfromApi(new OnTaskCompletion() {
                    @Override
                    public void onComplete(ArrayList<String> response) {
                        try {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < response.size(); i++) {
                                String str = response.get(i);
                                sb.append(str);
                                sb.append(";");
                            }
                            sb.deleteCharAt(sb.length() - 1);
                            translation(sb.toString());
                        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        btn_album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 激活系统图库，选择一张图片
                Intent intent1 = new Intent(Intent.ACTION_PICK);
                intent1.setType("image/*");
                // 开启一个带有返回值的Activity，请求码为PHOTO_REQUEST_GALLERY
                startActivityForResult(intent1, PHOTO_REQUEST_GALLERY);
            }
        });
//        Glide.with(this).load(URL).into(mImageView);
        getBitmapFromSharedPreferences();
    }


    /**
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PHOTO_REQUEST_GALLERY) {
            // 从相册返回的数据
            if (data != null) {
                // 得到图片的全路径
                Uri uri = data.getData();
                Glide.with(this).load(uri).into(mImageView);
                URL = uri.toString();
                /**
                 * 获得图片
                 */
                Bitmap photoBmp = null;
                if (uri != null) {
                    try {
                        photoBmp = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(),uri);
                        //保存到SharedPreferences
                        saveBitmapToSharedPreferences(photoBmp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
//        else if (requestCode == PHOTO_REQUEST_CUT) {
//            // 从剪切图片返回的数据
//            if (data != null) {
//                Bitmap bitmap = data.getParcelableExtra("data");
//                /**
//                 * 获得图片
//                 */
//                iv_img.setImageBitmap(bitmap);
//                postImage(bitmap);
//                //保存到SharedPreferences
//                saveBitmapToSharedPreferences(bitmap);
//            }
//            try {
//                // 将临时文件删除
//                tempFile.delete();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    //保存图片到SharedPreferences
    private void saveBitmapToSharedPreferences(Bitmap bitmap) {
        // Bitmap bitmap=BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        //第一步:将Bitmap压缩至字节数组输出流ByteArrayOutputStream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        //第二步:利用Base64将字节数组输出流中的数据转换成字符串String
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String imageString = new String(Base64.encodeToString(byteArray, Base64.DEFAULT));
        //第三步:将String保持至SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("testSP", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("image", imageString);
        editor.commit();

    }

    //从SharedPreferences获取图片
    private void getBitmapFromSharedPreferences(){
        SharedPreferences sharedPreferences=getSharedPreferences("testSP", Context.MODE_PRIVATE);
        //第一步:取出字符串形式的Bitmap
        String imageString=sharedPreferences.getString("image", "");
        //第二步:利用Base64将字符串转换为ByteArrayInputStream
        byte[] byteArray=Base64.decode(imageString, Base64.DEFAULT);
        if(byteArray.length==0){
            //
            Toast.makeText(this,"No Image!",Toast.LENGTH_LONG).show();
            mImageView.setImageResource(R.mipmap.ic_launcher);
        }else{
            ByteArrayInputStream byteArrayInputStream=new ByteArrayInputStream(byteArray);

            //第三步:利用ByteArrayInputStream生成Bitmap
            Bitmap bitmap= BitmapFactory.decodeStream(byteArrayInputStream);
            mImageView.setImageBitmap(bitmap);
        }

    }




    private void translation(String input) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        String appid = "20160920000028985";
        String key = "qy53qCdsTFBRCGwNTsxo";
        int salt = new Random().nextInt(10000);
        String result = getMD5(input, appid, key, salt);
        appid = urlEncode(appid);
        input = urlEncode(input);
        String str_salt = urlEncode(Integer.toString(salt));
        String from = urlEncode("en");
        String to = urlEncode("zh");
        String url = TRANSLATION_URL + "?q=" + input + "&from=" + from + "&to=" + to + "&appid="
                + appid + "&salt=" + str_salt + "&sign=" + result;
        StringRequest stringRequest = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                EventBus.getDefault().post(new MessageEvent(response));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        mRequestQueue.add(stringRequest);
    }

    @NonNull
    private String getMD5(String input, String appid, String key, int salt) throws
            NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.reset();
        String s = appid + input + salt + key;
        messageDigest.update(s.getBytes());
        byte[] digest = messageDigest.digest();
        StringBuilder md5StrBuilder = new StringBuilder();

        //将加密后的byte数组转换为十六进制的字符串,否则的话生成的字符串会乱码
        for (int i = 0; i < digest.length; i++) {
            if (Integer.toHexString(0xFF & digest[i]).length() == 1) {
                md5StrBuilder.append("0").append(
                        Integer.toHexString(0xFF & digest[i]));
            } else {
                md5StrBuilder.append(Integer.toHexString(0xFF & digest[i]));
            }
        }
        return md5StrBuilder.toString();
    }

    private String urlEncode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "UTF-8");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        Gson gson = new Gson();
        Result result = gson.fromJson(event.msg, Result.class);
        String dst = result.getTrans_result().get(0).getDst();
        mTextView.setText(dst);
    }
}
