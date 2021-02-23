package com.example.audioproj;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private AudioTrack audioTrack;
    private MediaExtractor extractor;
    private MediaCodec codec;
    private ByteBuffer[] codecInputBuffers;
    private ByteBuffer[] codecOutputBuffers;
    
    private boolean sawInputEOS;
    private int mMinBufferSize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonPlayAudioTrack = findViewById(R.id.audioStreamBtn);
        buttonPlayAudioTrack.setOnClickListener(this);

        mMinBufferSize = AudioTrack.getMinBufferSize(4000, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 4000, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize, AudioTrack.MODE_STREAM);



        //playMediaCodec();
    }

    private void playMediaCodec() {
        extractor = new MediaExtractor();

        try {
            extractor.setDataSource(AUDIO_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("LOG_TAG", String.format("TRACKS #: %d", extractor.getTrackCount()));
        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);
        Log.d("LOG_TAG", String.format("MIME TYPE: %s", mime));


        try {
            codec = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        extractor.selectTrack(0); // <= You must select a track. You will read samples from the media from this track!



        int inputBufIndex = codec.dequeueInputBuffer(5000);
        if (inputBufIndex >= 0) {
            ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

            int sampleSize = extractor.readSampleData(dstBuf, 0);
            long presentationTimeUs = 0;
            if (sampleSize < 0) {
                sawInputEOS = true;
                sampleSize = 0;
            } else {
                presentationTimeUs = extractor.getSampleTime();
            }

            codec.queueInputBuffer(inputBufIndex,
                    0, //offset
                    sampleSize,
                    presentationTimeUs,
                    sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
            if (!sawInputEOS) {
                extractor.advance();
            }
        }




    }

    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v.getId() == R.id.audioStreamBtn)
            playSound();
    }

    private void playSound() {
        //InputStream inputStream = getResources().openRawResource(R.raw.piano12);
        Thread thread = new Thread(this::playUrl);
        thread.start();

        /*try {
            //inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    private void playUrl() {
        audioTrack.play();
        int i;
        //int bufferSize = 254;
        byte[] buffer = new byte[mMinBufferSize];
        try {
            InputStream inputStream = new URL(AUDIO_PATH).openStream();
            try {
                while ((i = inputStream.read(buffer)) != -1){


                    //int size = 254;
                    short[] shortArray = new short[mMinBufferSize];

                    G711UCodec.decode(shortArray, buffer, mMinBufferSize, 0);
                    byte[] array = G711UCodec.myShortToByte(shortArray);

                    audioTrack.write(array, 0, i);
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
