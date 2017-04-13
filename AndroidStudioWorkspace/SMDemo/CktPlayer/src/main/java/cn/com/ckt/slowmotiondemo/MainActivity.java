package cn.com.ckt.slowmotiondemo;



import android.Manifest;
import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;

import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * 使用SurfaceView和MediaPlayer的本地视频播放器。
 *
 */
public class MainActivity extends Activity implements OnClickListener {

    private SurfaceView surfaceView;
    private Button button_play, button_pause, button_stop, button_replay;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private int currentPosition;
    private boolean isPlaying;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;



     private final  static  float PLAY_SPEED_SLOW = 0.3f;
    private final  static  float PLAY_SPEED_NORMAL = 1.0f;

    private   double PLAY_POSITION1= 0;
    private   double PLAY_POSITION2= 0;

    // Storage Permissions
    private static String[] REQUIRED_PERMISSIONS = {
           Manifest.permission.READ_EXTERNAL_STORAGE,
           Manifest.permission.WRITE_EXTERNAL_STORAGE };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        if (PermissionActivity.checkAndRequestPermission(this, REQUIRED_PERMISSIONS)) {
            finish();
        }

        init();

        initData();
    }

    private void init() {
        button_play = (Button) findViewById(R.id.button_play);
        button_pause = (Button) findViewById(R.id.button_pause);
        button_stop = (Button) findViewById(R.id.button_stop);
        button_replay = (Button) findViewById(R.id.button_replay);

        tvCurrentTime=(TextView)findViewById(R.id.tv_current_time);
        tvTotalTime=(TextView)findViewById(R.id.tv_total_time);

        surfaceView = (SurfaceView) findViewById(R.id.sv);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int process = seekBar.getProgress();
                if (mediaPlayer!=null && mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(process);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {

            }
        });
    }

    private void initData() {
        mediaPlayer = new MediaPlayer();
        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//4.0一下的版本需要加该段代码。

        surfaceView.getHolder().addCallback(new Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                /**
                 * 当点击手机上home键（或其他使SurfaceView视图消失的键）时，调用该方法，获取到当前视频的播放值，currentPosition。
                 * 并停止播放。
                 */
                currentPosition = mediaPlayer.getCurrentPosition();
                stop();
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                /**
                 * 当重新回到该视频应当视图的时候，调用该方法，获取到currentPosition，并从该currentPosition开始继续播放。
                 */
                if (currentPosition > 0) {
                    play(currentPosition);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {

            }
        });

        button_play.setOnClickListener(this);
        button_pause.setOnClickListener(this);
        button_stop.setOnClickListener(this);
        button_replay.setOnClickListener(this);
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_play:
                /**
                 * 播放
                 */
                play(0);

                break;

            case R.id.button_pause:
                /**
                 * 暂停
                 */
                pause();

                break;

            case R.id.button_stop:
                /**
                 * 停止
                 */
                stop();
                break;

            case R.id.button_replay:
                /**
                 * 重播
                 */
                replay();
                break;

            default:
                break;
        }
    }

    private void replay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(0);
        } else {
            play(0);
        }
    }

    private void stop() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.seekTo(0);
        }

    }

    private void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }

    }

    private void play(final int currentPosition) {
        String path = "/storage/emulated/0/test.mp4";//指定视频所在路径。
//		String path = "http://daily3gp.com/vids/family_guy_penis_car.3gp";//指定视频所在路径。
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);//设置视频流类型
        try {

            mediaPlayer.setDisplay(surfaceView.getHolder());
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepareAsync();

            //setspeed
//          mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(PLAY_SPEED_SLOW));
//
//            float speed = mediaPlayer.getPlaybackParams().getSpeed();
            Log.i("tag", "play:speed== " + mediaPlayer.getPlaybackParams().getSpeed());





            mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();
                  final   int duration = mediaPlayer.getDuration();
//                    Log.i("srx", "onPrepared: getDuration==" + duration);
                    seekBar.setMax(duration);
                    mediaPlayer.seekTo(currentPosition);

                    PLAY_POSITION1 = duration*0.25;

                    PLAY_POSITION2 = duration*0.75;



                    //把总时间显示textView上
                    int m = duration / 1000 / 60;
                    int s = duration / 1000 % 60;
                    tvTotalTime.setText( m + ":" + s);
                    tvCurrentTime.setText("00:00");

                    new Thread() {

                        public void run() {
                            isPlaying = true;
                            while (isPlaying) {
                              final   int position = mediaPlayer.getCurrentPosition();
                                seekBar.setProgress(position);
                                final int m = position / 1000 / 60;
                                final int s = position / 1000 % 60;

                                //此方法给定的runable对象，会执行主线程（UI线程中）
                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        tvCurrentTime.setText(m + ":" + s);

                                        if(position > duration){
                                            tvCurrentTime.setText("00:00");
                                        }

                                    }

                                });
                                SystemClock.sleep(1000);
/**
 *  slow motion   test
 *
 */
                                boolean isslowmotion = (position < PLAY_POSITION2) && (position > PLAY_POSITION1);
                                Log.i("tag", "onPrepared: isslowmotion=="+isslowmotion);
                                if(isslowmotion) {
                                    mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(PLAY_SPEED_SLOW));
                                    Log.i("tag", "onPrepared:1111 ");
                                }else{
                                    mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(PLAY_SPEED_NORMAL));
                                    Log.i("tag", "onPrepared:2222 ");
                                }
                            }

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                 }.start();
                }
            });

            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    button_play.setEnabled(true);
                }
            });

            mediaPlayer.setOnErrorListener(new OnErrorListener() {

                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    button_play.setEnabled(true);
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
