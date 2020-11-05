package com.lennydennis.zerakigoogledrive.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.ammarptn.debug.gdrive.lib.ui.gdrivedebugview.GdriveDebugViewFragment;
import com.ammarptn.debug.gdrive.lib.ui.gdrivedebugview.viewObject.CreateFolderFragment;
import com.lennydennis.zerakigoogledrive.R;
import com.lennydennis.zerakigoogledrive.ui.fragments.viewobject.FileInfoDialogFragment;

import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity implements FileInfoDialogFragment.OnFragmentInteractionListener {
    private static final String TAG = "MainActivity";
    private GdriveDebugViewFragment mGdriveDebugViewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGdriveDebugViewFragment = new GdriveDebugViewFragment();

    }

    @Override
    public void onDelete(@NotNull String driveId) {
        mGdriveDebugViewFragment.onDelete(driveId);
    }

    @Override
    public void onDownload(@NotNull String driveId) {
        mGdriveDebugViewFragment.onDownload(driveId);
    }
}