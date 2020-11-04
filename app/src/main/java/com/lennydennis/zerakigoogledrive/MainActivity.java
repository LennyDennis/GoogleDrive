package com.lennydennis.zerakigoogledrive;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ammarptn.gdriverest.DriveServiceHelper;
import com.ammarptn.gdriverest.GoogleDriveFileHolder;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;


import java.io.File;
import java.net.URISyntaxException;

import static com.ammarptn.gdriverest.DriveServiceHelper.getGoogleDriveService;

public class MainActivity extends AppCompatActivity implements PickiTCallbacks {
    private static final String TAG = "MainActivity";
    private static final int READ_PERMISSION = 0;
    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE_SIGN_IN = 100;
    private static final String FOLDER_ID = "Folder ID";

    Button selectButton, uploadButton;
    TextView mFileName;
    private GoogleSignInClient mGoogleSignInClient;
    private DriveServiceHelper mDriveServiceHelper;
    private Uri mFileUri;
    private String mFileOriginalName;
    private String mAccountName;
    PickiT pickiT;
    private String mInternalFilePath;
    private Intent mResultDataIntent;
    private SharedPreferences mSharedPreferences;
    private Boolean mCreateFolder;

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

        if (account == null) {
            signIn();
        } else {
            mDriveServiceHelper = new com.ammarptn.gdriverest.DriveServiceHelper(getGoogleDriveService(getApplicationContext(), account, "appName"));
            mAccountName = account.getEmail();
        }
    }

    private void signIn() {
        mGoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(com.google.android.gms.drive.Drive.SCOPE_FILE)
                        .requestEmail()
                        .build();
        return GoogleSignIn.getClient(getApplicationContext(), signInOptions);
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        Log.d(TAG, "Signed in as " + googleSignInAccount.getEmail());
                        mAccountName = googleSignInAccount.getEmail();
                        mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), googleSignInAccount, "appName"));
                        Log.d(TAG, "handleSignInResult: " + mDriveServiceHelper);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to sign in.", e);
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pickiT = new PickiT(this, this, this);

        selectButton = findViewById(R.id.select_file);
        uploadButton = findViewById(R.id.upload_file);
        mFileName = findViewById(R.id.file_name);

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    selectFile();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION);
                }
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFileUri != null && mFileOriginalName != null) {
                    createFolder();
                } else {
                    Toast.makeText(MainActivity.this, "Select a file", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectFile();
        } else {
            Toast.makeText(this, "Provide permission", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectFile() {
        Intent fileIntent = new Intent();
        fileIntent.setType("*/*");
        fileIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(fileIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleSignInResult(data);
                }
                break;
            case REQUEST_CODE:
                if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
                    mResultDataIntent = data;
                    mFileUri = data.getData();
                    pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);

                    Cursor returnCursor = getContentResolver().query(mFileUri, null, null, null, null);
                    int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    returnCursor.moveToFirst();
                    mFileOriginalName = returnCursor.getString(nameIndex);

                    selectButton.setVisibility(View.INVISIBLE);
                    uploadButton.setVisibility(View.VISIBLE);
                    mFileName.setText(mFileOriginalName);
                } else {
                    Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void createFolder(){
        String folderName = "Session One";
        if (mDriveServiceHelper == null) {
            return;
        }
        // you can provide  folder id in case you want to save this file inside some folder.
        // if folder id is null, it will save file to the root
        mDriveServiceHelper.createFolderIfNotExist(folderName, null)
                .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                    @Override
                    public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                        Gson gson = new Gson();
                        Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));

                        mSharedPreferences = getSharedPreferences(FOLDER_ID, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putString("Folder Id",googleDriveFileHolder.getId());
                        editor.apply();
                        uploadTheFile(mFileUri, mFileOriginalName);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: " + e.getMessage());
                    }
                });
    }

    public void uploadTheFile(Uri fileUri, String fileName) {
        File file = new File(mInternalFilePath);
        String mimeType = getContentResolver().getType(fileUri);
        String folderId = mSharedPreferences.getString("Folder Id","");

        if (mDriveServiceHelper == null) {
            return;
        }

        mDriveServiceHelper.uploadFile(file, mimeType, folderId)
                .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                    @Override
                    public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                        Gson gson = new Gson();
                        Toast.makeText(MainActivity.this, mFileOriginalName+" uploaded successfully", Toast.LENGTH_SHORT).show();
                        selectButton.setVisibility(View.VISIBLE);
                        uploadButton.setVisibility(View.INVISIBLE);
                        Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));
                        mFileName.setText("File name");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: " + e.getMessage());
                    }
                });
    }

    @Override
    public void PickiTonUriReturned() {

    }

    @Override
    public void PickiTonStartListener() {

    }

    @Override
    public void PickiTonProgressUpdate(int progress) {

    }

    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String Reason) {
        if (wasSuccessful) {
            mInternalFilePath = path;
        } else {
            try {
                mInternalFilePath = PathUtil.INSTANCE.getPath(getApplicationContext(),mFileUri);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
}