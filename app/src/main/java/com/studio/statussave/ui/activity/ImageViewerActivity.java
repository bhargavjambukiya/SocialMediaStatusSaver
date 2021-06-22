package com.studio.statussave.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.github.chrisbanes.photoview.PhotoView;
import com.studio.statussave.R;
import com.studio.statussave.adapters.AdapterImagesViewer;
import com.studio.statussave.adapters.RecyclerInstances;
import com.studio.statussave.data.FilesData;
import com.studio.statussave.filesoperations.FileOperations;

import java.io.IOException;

public class ImageViewerActivity extends BaseActivity implements View.OnClickListener {

    String path;
    int position;
    char contentType;
    PhotoView mPhotoView;
    private ImageView imageViewSaveORDelete, imageViewShareFile;
    private ViewPager viewPagerImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_activity_image_viewer);

        setUpToolbar();
        try {
            path = getIntent().getExtras().getString("path");
            position = getIntent().getExtras().getInt("position");
            contentType = getIntent().getExtras().getChar("contentType");
        } catch (NullPointerException e) {
            Toast.makeText(this, getString(R.string.imageviewer_error), Toast.LENGTH_SHORT).show();
        }

        initializeComponents();
        AdapterImagesViewer myCustomPagerAdapter;
        if (!FilesData.getRecentOrSaved().equals("recent")) {
            imageViewSaveORDelete.setImageResource(R.drawable.ic_delete);
            myCustomPagerAdapter = new AdapterImagesViewer(ImageViewerActivity.this, FilesData.getSavedFilesImages());
        } else {
            myCustomPagerAdapter = new AdapterImagesViewer(ImageViewerActivity.this, FilesData.getWhatsAppFilesImages());
        }
        viewPagerImages.setAdapter(myCustomPagerAdapter);
        viewPagerImages.setCurrentItem(position);

        viewPagerImages.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int pos) {
                if (!FilesData.getRecentOrSaved().equals("recent")) {
                    imageViewSaveORDelete.setImageResource(R.drawable.ic_delete);
                    path = FilesData.getSavedFilesImages().get(pos).getAbsolutePath();
                } else {
                    path = FilesData.getWhatsAppFilesImages().get(pos).getAbsolutePath();
                }
                position = pos;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    private void initializeComponents() {
        mPhotoView = findViewById(R.id.imageviewer);
        imageViewSaveORDelete = findViewById(R.id.imageViewSaveORDelete);
        imageViewShareFile = findViewById(R.id.imageViewShareFile);
        viewPagerImages = findViewById(R.id.viewPagerImages);

        imageViewShareFile.setOnClickListener(this);
        imageViewSaveORDelete.setOnClickListener(this);
        // mPhotoView.setImageURI(Uri.parse(path));
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolimageviewer);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imageViewSaveORDelete:
                if (FilesData.getRecentOrSaved().equals("recent")) {
                    try {
                        FileOperations.saveAndRefreshFiles(FilesData.getWhatsAppFilesImages().get(position));
                        Toast.makeText(ImageViewerActivity.this, getString(R.string.image_saved), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(ImageViewerActivity.this, getString(R.string.image_save_failed), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    FileOperations.deleteAndRefreshFiles(FilesData.getSavedFilesImages().get(position));
                    RecyclerInstances.savedImageAdapter.notifyDataSetChanged();
                    RecyclerInstances.savedImageRecyclerview.setAdapter(RecyclerInstances.savedImageAdapter);
                    Toast.makeText(ImageViewerActivity.this, getString(R.string.image_deleted), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.imageViewShareFile:
                FileOperations.shareFile(FilesData.getWhatsAppFilesImages().get(position), ImageViewerActivity.this, contentType);
                break;
        }
    }
}
