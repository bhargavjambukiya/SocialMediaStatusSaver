package com.studio.statusvault.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.studio.statusvault.data.FilesData;
import com.studio.statusvault.data.repository.StatusMediaItem;
import com.studio.statusvault.data.repository.StatusRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoriesViewModel extends AndroidViewModel {

    private final StatusRepository repository;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<ArrayList<StatusMediaItem>> recentImages = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<StatusMediaItem>> recentVideos = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<StatusMediaItem>> recentImagesStatusFolderOnly =
            new MutableLiveData<>();
    private final MutableLiveData<ArrayList<StatusMediaItem>> recentVideosStatusFolderOnly =
            new MutableLiveData<>();
    private final MutableLiveData<ArrayList<File>> savedImages = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<File>> savedVideos = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public StoriesViewModel(@NonNull Application application) {
        super(application);
        repository = new StatusRepository(application);
    }

    public void loadStoriesLists() {
        loading.postValue(true);
        ioExecutor.execute(() -> {
            try {
                repository.refreshListsForCurrentMode();

                if ("recent".equals(FilesData.getRecentOrSaved())) {
                    recentImages.postValue(new ArrayList<>(repository.getRecentImages()));
                    recentVideos.postValue(new ArrayList<>(repository.getRecentVideos()));
                    recentImagesStatusFolderOnly.postValue(
                            new ArrayList<>(repository.getRecentImagesFromStatusFolderOnly()));
                    recentVideosStatusFolderOnly.postValue(
                            new ArrayList<>(repository.getRecentVideosFromStatusFolderOnly()));
                } else {
                    savedImages.postValue(new ArrayList<>(repository.getSavedImages()));
                    savedVideos.postValue(new ArrayList<>(repository.getSavedVideos()));
                }
            } finally {
                loading.postValue(false);
            }
        });
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ioExecutor.shutdown();
    }

    public LiveData<ArrayList<StatusMediaItem>> getRecentImages() {
        return recentImages;
    }

    public LiveData<ArrayList<StatusMediaItem>> getRecentVideos() {
        return recentVideos;
    }

    /**
     * Same as merged recent lists but without mirrored copies — only WhatsApp {@code .Statuses} items.
     * Used by the standalone Images/Videos tabs; the combined “All” tab uses {@link #getRecentImages()}
     * / {@link #getRecentVideos()}.
     */
    public LiveData<ArrayList<StatusMediaItem>> getRecentImagesStatusFolderOnly() {
        return recentImagesStatusFolderOnly;
    }

    public LiveData<ArrayList<StatusMediaItem>> getRecentVideosStatusFolderOnly() {
        return recentVideosStatusFolderOnly;
    }

    public LiveData<ArrayList<File>> getSavedImages() {
        return savedImages;
    }

    public LiveData<ArrayList<File>> getSavedVideos() {
        return savedVideos;
    }
}