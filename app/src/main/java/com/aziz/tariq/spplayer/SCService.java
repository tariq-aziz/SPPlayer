package com.aziz.tariq.spplayer;

import android.util.Log;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Created by echessa on 6/18/15.
 */
public interface SCService {


    @GET("/tracks?client_id=" + Config.CLIENT_ID)
    public void getRecentTracks(@Query("created_at[from]") String date, Callback<List<Track>> cb);

    //@GET("/resolve?url=http://soundcloud.com/" + MainActivity.username + "&client_id=" + Config.CLIENT_ID)
    @GET("/resolve")
    Call<userObject> getUserId(@QueryMap Map<String, String> options);
    //Call<userObject>getUserId(@Query("username") String username);
    //Call<userObject>getUserId();




    @GET("/users/112331085/favorites?client_id=" + Config.CLIENT_ID)
    Call<List<Track>> getUserTracks();

    //Call<List<Track>> getUserTracks(@Path("id") String id, @Query("client_id") String client_id );
    //public void getUserTracks(@Query("id") int id, Callback<List<Track>> cb);

}