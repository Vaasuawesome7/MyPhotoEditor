package com.example.myphotoeditor;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;

import android.annotation.SuppressLint;
import android.app.SharedElementCallback;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.transition.Fade;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import yuku.ambilwarna.AmbilWarnaDialog;

public class EditorPage extends AppCompatActivity implements ChangePaintThicknessDialog.ChangePaintThicknessDialogListener {

    // FIELDS
    private static ActionBar mActionBar;
    private ArrayList<String> mFileNames;
    private MyViewPager mViewPager;
    private Button mEdit, mPaint, mRotate, mChooseColor, mChooseThickness, mSave, mDone, mCancel, mCrop, mSetCrop, mCancelCrop, mUndo;
    private int mDefColor;
    private Bitmap mCurrentBitmap;
    public static boolean mSaved, mWasSaved;
    private Animation mAnimationEnter, mAnimationExit;

    // OVERRIDDEN METHODS
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor_page);
        supportPostponeEnterTransition();

        mAnimationEnter = AnimationUtils.loadAnimation(this, R.anim.bottom_to_top);
        mAnimationExit = AnimationUtils.loadAnimation(this, R.anim.top_to_bottom);
        mSaved = false;
        mWasSaved = false;

//        Window w = getWindow();
//        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
//        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        mFileNames = MainActivity.getFileNames();
        mDefColor = ContextCompat.getColor(this, R.color.white);

        getWindow().getSharedElementEnterTransition().setDuration(Constants.TRANSITION_DURATION);
        getWindow().getSharedElementReturnTransition().setDuration(Constants.TRANSITION_DURATION);

        mActionBar = getSupportActionBar();
        assert mActionBar != null;
        mActionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.special_transparent)));
        mActionBar.setDisplayHomeAsUpEnabled(true);

        Fade fade = new Fade();
        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(android.R.id.navigationBarBackground, true);
        getWindow().setEnterTransition(fade);
        getWindow().setExitTransition(fade);

        load(setTransition());

        loadButtons();
        setMyViewPager();
    }

    private void setMyViewPager() {
        mViewPager = findViewById(R.id.image_viewPager);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);

        mViewPager.setAdapter(adapter);
        mViewPager.setCurrentItem(MainActivity.position);

        if (getCurrentView() != null)
            mCurrentBitmap = Bitmap.createBitmap(getCurrentView().getImageBitmap());

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                MyImageView imageView = getCurrentView();
                if (positionOffset > 0.00000000000001) {
                    imageView.setScrolling(true);
                }
                if (positionOffset == 0) {
                    imageView.setScrolling(false);
                }
            }

            @Override
            public void onPageSelected(int position) {
                mActionBar.setTitle(mFileNames.get(position));
                MainActivity.position = position;
                MyImageView imageView = getCurrentView();
                imageView.setScrolling(false);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

    }

    @Override
    public void onBackPressed() {
        MyImageView imageView = getCurrentView();
        if (imageView != null) {
            if (imageView.getPaintMode()) {
                exitPaintMode();
                imageView.disablePaintMode();
            } else if (imageView.getCropMode()) {
                exitCropMode();
                imageView.disableCropMode();
            } else if (imageView.getEditingMode()) {
                imageView.resetDegrees();
                exitEditMode();
                imageView.disablePaintMode();
                imageView.disableCropMode();
            } else {
                if (mSaved) {
                    String load = "Loading the album";
                    Toast toast = Toast.makeText(this, load, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                super.onBackPressed();
                finishAfterTransition();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        vibrate();
        if (item.getItemId() == android.R.id.home) {
            finishAfterTransition();
            return true;
        }
        return false;
    }


    @Override
    public void finishAfterTransition() {
        mActionBar.hide();

        setEnterSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                View view = getCurrentView();
                if (view == null)
                    return;

                names.clear();
                sharedElements.clear();

                String transitionName = ViewCompat.getTransitionName(view);
                names.add(transitionName);
                sharedElements.put(transitionName, view);

                setExitSharedElementCallback((SharedElementCallback) null);
            }
        });

        setResult(RESULT_OK);

        super.finishAfterTransition();
    }

    // MY METHODS
    public void rotate(View view) {
        vibrate();
        MyImageView currentView = getCurrentView();
        if (currentView != null) {
            currentView.rotate();
            if (currentView.getRotationDegrees() % 360 != 0 || MyImageView.mHasBeenCropped || MyImageView.mHasBeenPainted) {
                if (mSave.getVisibility() != View.VISIBLE) {
                    mSave.startAnimation(mAnimationEnter);
                    mSave.setVisibility(View.VISIBLE);
                }
            } else {
                mSave.startAnimation(mAnimationExit);
                mSave.setVisibility(View.GONE);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void save(View view) {

        vibrate();

        String saved = "Image saved in Gallery";
        Toast saveToast = Toast.makeText(this, saved, Toast.LENGTH_SHORT);
        saveToast.setText(saved);
        saveToast.setDuration(Toast.LENGTH_SHORT);
        saveToast.setGravity(Gravity.CENTER, 0, 0);

        MyImageView currentView = getCurrentView();
        if (currentView != null) {
            BitmapDrawable drawable = (BitmapDrawable) currentView.getDrawable();
            Bitmap bitmap = drawable.getBitmap();
            saveImage(bitmap);
            saveToast.show();
        }

        exitEditMode();

        if (currentView != null)
            currentView.disablePaintMode();
        if (currentView != null)
            currentView.disableCropMode();

        mSaved = true;
        mWasSaved = true;
    }

    private void saveImage(Bitmap bitmap) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        String folderName = Constants.MY_DIRECTORY;
        File myDirectory = new File(root + "/" + folderName);
        if (!myDirectory.exists())
            myDirectory.mkdirs();
        StringBuilder stringBuilder = new StringBuilder(Constants.MY_DIRECTORY);
        String rev = stringBuilder.reverse().toString();
        String imageName = Constants.MY_DIRECTORY + "_" + System.currentTimeMillis() + "_" + rev + ".jpg";
        File image = new File(myDirectory, imageName);

        try {
            FileOutputStream outputStream = new FileOutputStream(image);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(image);
            scanIntent.setData(contentUri);
            sendBroadcast(scanIntent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void paint(View view) {
        vibrate();
        MyImageView currentView = getCurrentView();
        enterPaintMode();
        if (currentView != null)
            currentView.paint();
    }

    public void chooseColor(View view) {
        vibrate();
        MyImageView currentView = getCurrentView();
        if (currentView != null) {

            AmbilWarnaDialog colorDialog = new AmbilWarnaDialog(this, mDefColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override
                public void onCancel(AmbilWarnaDialog dialog) {

                }

                @Override
                public void onOk(AmbilWarnaDialog dialog, int color) {
                    mDefColor = color;
                    currentView.setPaintColor(color);
                }
            });
            colorDialog.show();
        }
    }

    public void cancel(View view) {
        vibrate();
        MyImageView currentView = getCurrentView();
        exitPaintMode();
        if (currentView != null) {
            currentView.cancel();
            if (MyImageView.mHasBeenPainted || MyImageView.mHasBeenCropped || currentView.getRotationDegrees() % 360 != 0) {
                mSave.startAnimation(mAnimationEnter);
                mSave.setVisibility(View.VISIBLE);
            }
        }
    }

    public void done(View view) {
        vibrate();
        MyImageView currentView = getCurrentView();
        exitPaintMode();
        if (currentView != null) {
            currentView.done();
            if (MyImageView.mHasBeenPainted || MyImageView.mHasBeenCropped || currentView.getRotationDegrees() % 360 != 0) {
                mSave.startAnimation(mAnimationEnter);
                mSave.setVisibility(View.VISIBLE);
            }
        }
    }

    private void enterPaintMode() {
        mChooseColor.startAnimation(mAnimationEnter);
        mChooseColor.setVisibility(View.VISIBLE);
        mChooseThickness.startAnimation(mAnimationEnter);
        mChooseThickness.setVisibility(View.VISIBLE);
        mCancel.startAnimation(mAnimationEnter);
        mCancel.setVisibility(View.VISIBLE);
        mSave.startAnimation(mAnimationExit);
        mSave.setVisibility(View.GONE);
        mCrop.startAnimation(mAnimationExit);
        mCrop.setVisibility(View.GONE);
        mPaint.startAnimation(mAnimationExit);
        mPaint.setVisibility(View.GONE);
        mRotate.startAnimation(mAnimationExit);
        mRotate.setVisibility(View.GONE);
    }

    private void exitPaintMode() {
        mChooseColor.startAnimation(mAnimationExit);
        mChooseColor.setVisibility(View.GONE);
        mChooseThickness.startAnimation(mAnimationExit);
        mChooseThickness.setVisibility(View.GONE);
        mCancel.startAnimation(mAnimationExit);
        mCancel.setVisibility(View.GONE);
        mUndo.startAnimation(mAnimationExit);
        mUndo.setVisibility(View.GONE);
        mDone.startAnimation(mAnimationExit);
        mDone.setVisibility(View.GONE);
        mCrop.startAnimation(mAnimationEnter);
        mCrop.setVisibility(View.VISIBLE);
        mPaint.startAnimation(mAnimationEnter);
        mPaint.setVisibility(View.VISIBLE);
        mRotate.startAnimation(mAnimationEnter);
        mRotate.setVisibility(View.VISIBLE);

    }

    private void enterCropMode() {
        mCancelCrop.startAnimation(mAnimationEnter);
        mCancelCrop.setVisibility(View.VISIBLE);
        mSetCrop.startAnimation(mAnimationEnter);
        mSetCrop.setVisibility(View.VISIBLE);
        mSave.startAnimation(mAnimationExit);
        mSave.setVisibility(View.GONE);
        mCrop.startAnimation(mAnimationExit);
        mCrop.setVisibility(View.GONE);
        mPaint.startAnimation(mAnimationExit);
        mPaint.setVisibility(View.GONE);
        mRotate.startAnimation(mAnimationExit);
        mRotate.setVisibility(View.GONE);
    }

    private void exitCropMode() {
        mSetCrop.startAnimation(mAnimationExit);
        mSetCrop.setVisibility(View.GONE);
        mCancelCrop.startAnimation(mAnimationExit);
        mCancelCrop.setVisibility(View.GONE);
        mCrop.startAnimation(mAnimationEnter);
        mCrop.setVisibility(View.VISIBLE);
        mPaint.startAnimation(mAnimationEnter);
        mPaint.setVisibility(View.VISIBLE);
        mRotate.startAnimation(mAnimationEnter);
        mRotate.setVisibility(View.VISIBLE);
    }

    public void undo(View view) {
        vibrate();
        MyImageView currentView = getCurrentView();
        if (currentView != null)
            currentView.undo();
    }

    @SuppressLint("ShowToast")
    public void crop(View view) {
        MyImageView currentView = getCurrentView();
        if (currentView != null) {
            RectF rect = currentView.getImageRect();
            if (rect.height() < 100 || rect.width() < 100) {
                String notPossible = "Cannot crop images of small height/width";
                Toast toast = Toast.makeText(this, notPossible, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            vibrate();
            enterCropMode();
            currentView.crop();
        }
    }

    public void setCrop(View view) {
        vibrate();
        exitCropMode();
        MyImageView currentView = getCurrentView();
        if (currentView != null) {
            currentView.setCrop();
            if (MyImageView.mHasBeenCropped || MyImageView.mHasBeenPainted || currentView.getRotationDegrees()%360!=0) {
                mSave.startAnimation(mAnimationEnter);
                mSave.setVisibility(View.VISIBLE);
            }
        }
    }

    public void cancelCrop(View view) {
        vibrate();
        exitCropMode();
        MyImageView currentView = getCurrentView();
        if (currentView != null) {
            currentView.cancelCrop();
            if (MyImageView.mHasBeenCropped || MyImageView.mHasBeenPainted || currentView.getRotationDegrees()%360!=0) {
                mSave.startAnimation(mAnimationEnter);
                mSave.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadButtons() {
        mEdit = findViewById(R.id.button_edit);
        mPaint = findViewById(R.id.paint);
        mRotate = findViewById(R.id.rotate);
        mChooseColor = findViewById(R.id.chooseColor);
        mSave = findViewById(R.id.save);
        mDone = findViewById(R.id.done);
        mCancel = findViewById(R.id.cancel);
        mCrop = findViewById(R.id.crop);
        mSetCrop = findViewById(R.id.setCrop);
        mCancelCrop = findViewById(R.id.cancelCrop);
        mUndo = findViewById(R.id.button_undo);
        mChooseThickness = findViewById(R.id.button_changeThickness);
    }

    public static ActionBar getMyActionBar() {
        return mActionBar;
    }

    private void load(String info) {
        mActionBar.setTitle(info);
    }

    private String setTransition() {
        return getIntent().getExtras().getString(Constants.IMAGE_NAME);
    }

    private MyImageView getCurrentView() {
        try {
            return mViewPager.findViewWithTag(MainActivity.position);
        } catch (NullPointerException | IndexOutOfBoundsException exception) {
            return null;
        }
    }

    public void edit(View view) {
        vibrate();
        if (getCurrentView() != null) {
            mCurrentBitmap = Bitmap.createBitmap(getCurrentView().getImageBitmap());
            getCurrentView().resetDegrees();
        }
        enterEditMode();
    }

    private void enterEditMode() {
        mEdit.startAnimation(mAnimationExit);
        mEdit.setVisibility(View.GONE);
        mCrop.startAnimation(mAnimationEnter);
        mCrop.setVisibility(View.VISIBLE);
        mPaint.startAnimation(mAnimationEnter);
        mPaint.setVisibility(View.VISIBLE);
        mRotate.startAnimation(mAnimationEnter);
        mRotate.setVisibility(View.VISIBLE);

        MyImageView currentImage = getCurrentView();
        if (currentImage != null) {
            currentImage.setEditingMode(true);
            mViewPager.disableScroll(true);
        }

        mActionBar.hide();
    }

    private void exitEditMode() {
        MyImageView.mHasBeenPainted = false;
        MyImageView.mHasBeenCropped = false;
        mEdit.startAnimation(mAnimationEnter);
        mEdit.setVisibility(View.VISIBLE);
        mCrop.startAnimation(mAnimationExit);
        mCrop.setVisibility(View.GONE);
        mPaint.startAnimation(mAnimationExit);
        mPaint.setVisibility(View.GONE);
        if (mSave.getVisibility() == View.VISIBLE)
            mSave.startAnimation(mAnimationExit);
        mSave.setVisibility(View.GONE);
        mRotate.startAnimation(mAnimationExit);
        mRotate.setVisibility(View.GONE);


        MyImageView currentImage = getCurrentView();
        if (currentImage != null) {
            currentImage.setRotation(0);
            currentImage.setImageBitmap(mCurrentBitmap);
            currentImage.setEditingMode(false);
            mViewPager.disableScroll(false);
        }

        mActionBar.show();
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(Constants.VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(Constants.VIBRATION_DURATION);
        }
    }

    @Override
    public void applyThickness(float thickness) {
        MyImageView imageView = getCurrentView();
        if (imageView != null) {
            imageView.setCurrentPaintThickness(thickness);
        }
    }

    public void changeThickness(View view) {
        vibrate();
        MyImageView imageView = getCurrentView();
        if (imageView!=null) {
            ChangePaintThicknessDialog dialog = new ChangePaintThicknessDialog(mDefColor, imageView.getCurrentPaintThickness());
            dialog.show(getSupportFragmentManager(), "Change Thickness");
        }
    }
}