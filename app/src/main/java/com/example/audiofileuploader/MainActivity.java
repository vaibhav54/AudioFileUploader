package com.example.audiofileuploader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public
class MainActivity extends AppCompatActivity {

    Button selectFile;
    Button download;
    Button upload;
    TextView progress;
    Uri audioUri;

    FirebaseStorage storage;
    FirebaseDatabase database;
    StorageReference storageReference;
    StorageReference ref;

    ProgressDialog progressDialog;

    @Override
    protected
    void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage=FirebaseStorage.getInstance();
        database=FirebaseDatabase.getInstance();

        selectFile = findViewById(R.id.btn_selector);
        upload = findViewById(R.id.btn_upload);
        progress = findViewById(R.id.txt_progress);

        download = findViewById(R.id.btn_download);


        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public
            void onClick(View view) {
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)==
                        PackageManager.PERMISSION_GRANTED){
                    selectAudioFile();
                }else{
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);

                }
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public
            void onClick(View view) {
                if(audioUri!=null)
                    uploadAudioFile(audioUri);
                else
                    Toast.makeText(MainActivity.this,"Select a file", Toast.LENGTH_SHORT).show();
            }
        });

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public
            void onClick(View view) {
                downloadFile();
                Toast.makeText(MainActivity.this,"downloading statred", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadFile() {
        StorageReference storageReference = storage.getReference();
        ref=storageReference.child("uploads/audio.mp3");

        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public
            void onSuccess(Uri uri) {

                String url = uri.toString();
                downloadFileFromFirebase(MainActivity.this,"audio",".mp3", DIRECTORY_DOWNLOADS, uri);
            }


        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public
            void onFailure(@NonNull Exception e) {

            }
        });
    }

    private
    void downloadFileFromFirebase(Context context, String fileName, String fileExtension, String destinationDirectory, Uri url) {

        DownloadManager downloadManager =(DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri =Uri.parse(String.valueOf(url));
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
        request.setDestinationInExternalFilesDir(context, destinationDirectory,fileName + fileExtension);
        downloadManager.enqueue(request);
    }

    private
    void uploadAudioFile(Uri audioUri) {

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("Uploading file");
        progressDialog.setProgress(0);
        progressDialog.show();

        final String fileName = System.currentTimeMillis()+"";

        StorageReference storageReference = storage.getReference();

        storageReference.child("uploads/audio.mp3").child(fileName).putFile(audioUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public
                    void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> downloadUrl = taskSnapshot.getStorage().getDownloadUrl();
                        DatabaseReference databaseReference = database.getReference();
                        databaseReference.child(fileName).setValue(downloadUrl)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public
                                    void onComplete(@NonNull Task<Void> task) {

                                        if(task.isSuccessful()){
                                            Toast.makeText(MainActivity.this,"File Successfully uploaded", Toast.LENGTH_SHORT).show();
                                        }
                                        else
                                            Toast.makeText(MainActivity.this,"File Not Successfully uploaded", Toast.LENGTH_SHORT).show();
                                    }

                                });
                    }


                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public
            void onFailure(@NonNull Exception e) {

                Toast.makeText(MainActivity.this,"File Not Successfully uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public
            void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {

                int currentProgress = (int) ((int) 100*taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                progressDialog.setProgress(currentProgress);
            }
        });

    }

    @Override
    public
    void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 1000 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            selectAudioFile();
        }else{
            Toast.makeText(MainActivity.this,"please provide permission",Toast.LENGTH_SHORT).show();

        }
    }

    private
    void selectAudioFile() {

        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Audio "), 1001);

    }

    @Override
    protected
    void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            progress.setText("A file selected " + data.getData().getLastPathSegment());
            audioUri = data.getData();
        } else {
            Toast.makeText(MainActivity.this, "Please select a file", Toast.LENGTH_SHORT).show();
        }

    }
}