package com.yuninfo.babysafety_teacher.ui.image;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;
import com.yuninfo.babysafety_teacher.R;
import com.yuninfo.babysafety_teacher.activity.BaseNetworkActivity;
import com.yuninfo.babysafety_teacher.annotation.HandleTitleBar;
import com.yuninfo.babysafety_teacher.app.Constant;
import com.yuninfo.babysafety_teacher.bean.CommonResult;
import com.yuninfo.babysafety_teacher.exception.FailGetImageException;
import com.yuninfo.babysafety_teacher.request.UploadFileRequest;
import com.yuninfo.babysafety_teacher.request.action.OnFailSessionObserver;
import com.yuninfo.babysafety_teacher.request.action.OnParseObserver;
import com.yuninfo.babysafety_teacher.ui.widget.PictureCutView;
import com.yuninfo.babysafety_teacher.util.Graphic;
import com.yuninfo.babysafety_teacher.util.NetworkUtil;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by LDM on 2014/6/25. Email : nightkid-s@163.com
 */
@HandleTitleBar(showBackBtn = true, showTitleText = R.string.text_commit_cut_image, showRightText = R.string.text_confirm2)
public class PicCutActivity extends BaseNetworkActivity implements View.OnClickListener, OnParseObserver<CommonResult>, OnFailSessionObserver{

    /**view*/
    private PictureCutView cutView;
    private Bitmap smallBitmap;
    private Bitmap bigBitmap;
    /**logic*/
    public static final String TAG = PicCutActivity.class.getSimpleName();
    public static final String SMALL_PIC_PATH = Constant.SD_HEAD_PIC_PATH.concat("small_avatar.jpg");
    public static final String BIG_PIC_PATH = Constant.SD_HEAD_PIC_PATH.concat("big_avatar.jpg");

    @Override
    public void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setContentView(R.layout.activity_pic_cut);
        cutView = (PictureCutView) findViewById(R.id.cut_view);
        findViewById(R.id.right_text_id).setOnClickListener(this);
        File file = new File(Constant.SD_HEAD_PIC_PATH);
        if(!file.exists()) file.mkdirs();
        if(getIntent().getIntExtra(TAG, Constant.FROM_CAM) == Constant.FROM_CAM){
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(BIG_PIC_PATH))), Constant.FROM_CAM);
        } else if(getIntent().getIntExtra(TAG, Constant.FROM_CAM) == Constant.FROM_ALBUM)
            startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), Constant.FROM_ALBUM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            switch (requestCode) {
                case Constant.FROM_CAM:
                    File file = new File(BIG_PIC_PATH);
                    if (file.exists() && file.length() > 0) cutView.setImageBitmap(bigBitmap = Graphic.compressImage(Graphic.getByOptionsBitmap(BIG_PIC_PATH, true)));
                    else finish();
                    break;
                case Constant.FROM_ALBUM:
                    if(data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();
                                String imgPath = cursor.getString(1); // 0图片编号  1图片文件路径  2图片大小  3图片文件名
                                cursor.close();
                                cutView.setImageBitmap(bigBitmap = Graphic.compressImage(Graphic.getByOptionsBitmap(imgPath, false)));
                            } else throw new FailGetImageException();
                        } else throw new FailGetImageException();
                    } else finish();
                    break;
                default:
                    finish();
            }
        } catch (FailGetImageException e) {
            displayToast(getString(R.string.text_fail_get_image));
            finish();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        File file = new File(BIG_PIC_PATH);
        if(file.exists()) file.delete();
        if(smallBitmap != null) {
            smallBitmap.recycle();
            smallBitmap = null;
        }

        if(bigBitmap != null){
            bigBitmap.isRecycled();
            bigBitmap = null;
        }

    }

    @Override
    public void onClick(View v) {
        try {
            smallBitmap = cutView.cutImageBitmap(SMALL_PIC_PATH);
            if(smallBitmap == null) throw new FileNotFoundException();
            else new UploadFileRequest(SMALL_PIC_PATH, this, this, this);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            displayToast("请正确裁剪大小适中的图片，或检查sd卡是否可用。");
        }
    }

    @Override
    public void onParseSuccess(CommonResult commonResult2) {
        if(NetworkUtil.isConnected(this)){
            //清空大小头像缓存(内存+闪存)
            File imageFile = ImageLoader.getInstance().getDiskCache().get(getApp().getUser().getHeadPic());
            if ( imageFile != null && imageFile.exists()) imageFile.delete();
            imageFile = ImageLoader.getInstance().getDiskCache().get(getApp().getUser().getBigHeadPic());
            if ( imageFile != null && imageFile.exists()) imageFile.delete();
            MemoryCacheUtils.removeFromCache(getApp().getUser().getHeadPic(), ImageLoader.getInstance().getMemoryCache());
            MemoryCacheUtils.removeFromCache(getApp().getUser().getBigHeadPic(), ImageLoader.getInstance().getMemoryCache());
        }
        displayToast("上传成功");
        finish();
    }

    @Override
    public void onFailSession(String errorInfo, int failCode) {
        super.onFailSession(errorInfo, failCode);
        finish();
    }
}
