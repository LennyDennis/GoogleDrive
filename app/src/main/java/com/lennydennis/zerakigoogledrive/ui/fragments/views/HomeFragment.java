package com.lennydennis.zerakigoogledrive.ui.fragments.views;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;
import com.lennydennis.zerakigoogledrive.R;
import com.lennydennis.zerakigoogledrive.databinding.HomeFragmentBinding;
import com.lennydennis.zerakigoogledrive.drive.DriveServiceHelper;
import com.lennydennis.zerakigoogledrive.drive.GoogleDriveFileHolder;
import com.lennydennis.zerakigoogledrive.ui.fragments.viewmodels.HomeFragmentsViewModel;
import com.lennydennis.zerakigoogledrive.util.PathUtil;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.ammarptn.gdriverest.DriveServiceHelper.getGoogleDriveService;

public class HomeFragment extends Fragment implements PickiTCallbacks {

    private static final String TAG = "MainActivity";
    private static final int READ_PERMISSION = 0;
    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE_SIGN_IN = 100;
    private static final String FOLDER_ID = "Folder ID";

    PickiT pickiT;
    private GoogleSignInClient mGoogleSignInClient;
    private DriveServiceHelper mDriveServiceHelper;
    private Uri mFileUri;
    private String mFileOriginalName;
    private String mAccountName;
    private String mInternalFilePath;
    private SharedPreferences mSharedPreferences;

    private HomeFragmentsViewModel mViewModel;
    private HomeFragmentBinding mHomeFragmentBinding;
    private String mFolderName;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mHomeFragmentBinding = HomeFragmentBinding.inflate(inflater,container,false);
        pickiT = new PickiT(getContext(), this, requireActivity());


        mHomeFragmentBinding.selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    selectFile();
                } else {
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION);
                }
            }
        });

        mHomeFragmentBinding.uploadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFileUri != null && mFileOriginalName != null) {
                    createFolder();
                } else {
                    Toast.makeText(requireActivity(), "Select a file", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mHomeFragmentBinding.viewFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                String folderId = mSharedPreferences.getString("Folder Id", "");
                String folderId = "1U-rGrlZZO1xNGiP77Mc8fNTZ0R1y_Ccj";
                mFolderName = "Session One";
                if (folderId != null && mFolderName != null) {
                    Navigation.findNavController(view).navigate(HomeFragmentDirections.actionHomeFragmentToGdriveDebugViewFragment().setFolderId(folderId).setFolderName(mFolderName));
                    viewFiles(folderId);
                } else {
                    Toast.makeText(getContext(), "Create files", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return mHomeFragmentBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(HomeFragmentsViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override
    public void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());

        if (account == null) {
            signIn();
        } else {
            mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getContext(), account, "appName"));
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
        return GoogleSignIn.getClient(getContext(), signInOptions);
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        Log.d(TAG, "Signed in as " + googleSignInAccount.getEmail());
                        mAccountName = googleSignInAccount.getEmail();
                        mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getContext(), googleSignInAccount, "appName"));
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectFile();
        } else {
            Toast.makeText(requireActivity(), "Provide permission", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectFile() {
        Intent fileIntent = new Intent();
        fileIntent.setType("*/*");
        fileIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(fileIntent, REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == RESULT_OK && data != null) {
                    handleSignInResult(data);
                }
                break;
            case REQUEST_CODE:
                if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {

                    mFileUri = data.getData();

                    pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);

                    Cursor returnCursor = requireActivity().getContentResolver().query(mFileUri, null, null, null, null);
                    int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    returnCursor.moveToFirst();
                    mFileOriginalName = returnCursor.getString(nameIndex);

                    mHomeFragmentBinding.selectFile.setVisibility(View.INVISIBLE);
                    mHomeFragmentBinding.uploadFile.setVisibility(View.VISIBLE);
                    mHomeFragmentBinding.fileName.setText(mFileOriginalName);
                } else {
                    Toast.makeText(requireActivity(), "Please select a file", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void createFolder() {
        mFolderName = "Session One";
        if (mDriveServiceHelper == null) {
            return;
        }
        // you can provide  folder id in case you want to save this file inside some folder.
        // if folder id is null, it will save file to the root
        mDriveServiceHelper.createFolderIfNotExist(mFolderName, null)
                .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                    @Override
                    public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                        Gson gson = new Gson();
                        Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));

                        mSharedPreferences = requireActivity().getSharedPreferences(FOLDER_ID, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putString("Folder Id", googleDriveFileHolder.getId());
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
        String mimeType = requireActivity().getContentResolver().getType(fileUri);
        String folderId = mSharedPreferences.getString("Folder Id", "");

        if (mDriveServiceHelper == null) {
            return;
        }

        mDriveServiceHelper.uploadFile(file, mimeType, folderId)
                .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                    @Override
                    public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                        Gson gson = new Gson();
                        Toast.makeText(requireActivity(), mFileOriginalName + " uploaded successfully", Toast.LENGTH_SHORT).show();
                        mHomeFragmentBinding.selectFile.setVisibility(View.VISIBLE);
                        mHomeFragmentBinding.uploadFile.setVisibility(View.INVISIBLE);
                        Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));
                        mHomeFragmentBinding.fileName.setText("File name");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: " + e.getMessage());
                    }
                });
    }

    private void viewFiles(String folderId) {
        mDriveServiceHelper.queryFiles(folderId)
                .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
                    @Override
                    public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {
                        Gson gson = new Gson();
                        Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolders));
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
                mInternalFilePath = PathUtil.getPath(getContext(), mFileUri);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
}