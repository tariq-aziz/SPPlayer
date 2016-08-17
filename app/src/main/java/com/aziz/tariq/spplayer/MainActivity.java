package com.aziz.tariq.spplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private List<Track> mListItems;
    private SCTrackAdapter mAdapter;
    private TextView mSelectedTrackTitle;
    private ImageView mSelectedTrackImage;
    private MediaPlayer mMediaPlayer;
    private ImageView mPlayerControl;
    private int curPos;
    private int userId;
    public static final String username = "tariquhziz";

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    private List peers = new ArrayList();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Config.API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener(){

            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });

        WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {

                // Out with the old, in with the new.
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                // If an AdapterView is backed by this data, notify it
                // of the change.  For instance, if you have a ListView of available
                // peers, trigger an update.
                //((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
                if (peers.size() == 0) {
                    Log.d(TAG, "No devices found");
                    return;
                }
            }
        };

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                togglePlayPause();
            }
        });

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                try {
                    mMediaPlayer.setDataSource(mListItems.get(curPos + 1).getStreamURL() + "?client_id=" + Config.CLIENT_ID);
                    mMediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mListItems = new ArrayList<Track>();
        ListView listView = (ListView) findViewById(R.id.track_list_view);
        mAdapter = new SCTrackAdapter(this, mListItems);
        listView.setAdapter(mAdapter);

        mSelectedTrackTitle = (TextView) findViewById(R.id.selected_track_title);
        mSelectedTrackImage = (ImageView) findViewById(R.id.selected_track_image);
        mPlayerControl = (ImageView) findViewById(R.id.player_control);

        mPlayerControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Track track = mListItems.get(position);
                curPos = position;

                mSelectedTrackTitle.setText(track.getTitle());
                Picasso.with(MainActivity.this).load(track.getArtworkURL()).into(mSelectedTrackImage);

                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                    mMediaPlayer.reset();
                }

                try {
                    mMediaPlayer.setDataSource(track.getStreamURL() + "?client_id=" + Config.CLIENT_ID);
                    //USING PREPAREASYNC GIVES ERROR WHEN YOU SELECT 2 SONGS RIGHT AFTER EACH OTHER,
                    //PREPARE() results in waiting for buffer before starting (slower but more reliable)
                    mMediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        //SCService scService = SoundCloud.getService();
        final SCService scService = retrofit.create(SCService.class);

        //Call<userObject> call = scService.getUserId(Config.API_URL + username, Config.CLIENT_ID);
        //Call<userObject> call = scService.getUserId();
        //Call<userObject> call = scService.getUserId(username);

        Map<String, String> data = new HashMap<>();
        data.put("url", "http://soundcloud.com/" + username);
        data.put("client_id", Config.CLIENT_ID);


        Call<userObject> call = scService.getUserId(data);
        call.enqueue(new Callback<userObject>() {


            @Override
            public void onResponse(Call<userObject> call, Response<userObject> response) {
                userObject user = response.body();
                if (user == null) {
                    Log.d(TAG, "NULL BOYZ");
                } else {
                    userId = user.user_id;
                    Log.d(TAG, "WE DID IT BOYS: " + user.user_id);
                    Call<List<Track>> callTrack = scService.getUserTracks();
                    callTrack.enqueue(new Callback<List<Track>>() {

                        @Override
                        public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
                            List<Track> tracks = response.body();
                            if (tracks == null) {
                                Log.d(TAG, "NULL SONG BOYZ");
                                Log.d(TAG, "USER_ID IS: " + userId);
                            } else {
                                Log.d(TAG, "WE DID IT BOYS- SONG: " + tracks.get(0).getTitle());
                                loadTracks(tracks);
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Track>> call, Throwable t) {
                            Log.d(TAG, "BIG TIME ERROR - SONG: " + t);
                        }
                    });
                }

            }

            @Override
            public void onFailure(Call<userObject> call, Throwable t) {
                Log.d(TAG, "BIG TIME ERROR: " + t);
            }
        });

        /*
        scService.getRecentTracks(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()), new Callback<List<Track>>() {
            @Override
            public void success(List<Track> tracks, Response response) {
                loadTracks(tracks);
            }
=
            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "Error: " + error);
            }
        });
        */

        /*
        scService.getUserId(username, new Callback<userObject>(){

            @Override
            public void success(userObject userObject, Response response) {
                userId = userObject.user_id;
                Log.d(TAG, "USER ID IS: " + userId);

            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "Error getting user id: " + error);
            }
        });
        */

        //Call<List<Track>> callTrack = scService.getUserTracks(Integer.toString(userId));


    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override

    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    private void loadTracks(List<Track> tracks) {
        mListItems.clear();
        mListItems.addAll(tracks);
        mAdapter.notifyDataSetChanged();
    }

    private void togglePlayPause() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPlayerControl.setImageResource(R.drawable.ic_play);
        } else {
            mMediaPlayer.start();
            mPlayerControl.setImageResource(R.drawable.ic_pause);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }


}