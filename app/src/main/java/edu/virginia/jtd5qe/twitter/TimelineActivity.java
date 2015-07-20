package edu.virginia.jtd5qe.twitter;

/**
 * Created by jackding on 6/11/15.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LruCache;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.io.IOException;
import java.util.ArrayList;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class TimelineActivity extends Activity{

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ResponseList<Status> tweetList;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private DrawerLayout mDrawerLayout;
    private ImageView profilePictureImageView;
    private TextView nameTextView;
    private TextView screennameTextView;
    private ListView mDrawerList;
    private ImageView mDrawerHeader;
    private ActionBarDrawerToggle mDrawerToggle;

    private ConnectivityManager connMgr;
    private NetworkInfo networkInfo;
    private Long sinceID;
    ResponseList<twitter4j.Status> fresh;
    private static AccessToken accessToken;
    private static Twitter twitter;
    private static RequestToken requestToken;
    static String CONSUMER_KEY = "nF8GoCuOJDuYA6WTYUPRJEFgZ";
    static String CONSUMER_SECRET = "RlyeN4GXMMNRSZ1FsRZGjRKUGZ74mJfSVlGTCyXZaOAsdkRC9h";
    static String LIST_STATE_KEY = "recyclerViewState";
    private LruCache<String, RoundedBitmapDrawable> profileCache;
    private String screenname;
    private String profileUrl;
    private String bannerUrl;
    private String name;
    private String[] navigation;
    private RoundedBitmapDrawable profilePicture;
    private BitmapDrawable banner;
    private int lastPosition;
    private int updateSize;
    private Parcelable mListState;
    private SharedPreferences mSharedPref;
    ArrayList<DrawerListItem> mDrawerListItems;
    String FILENAME = "tweetlist";





    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPref = getSharedPreferences("data", MODE_PRIVATE);
        String accessTokenString = mSharedPref.getString("accessToken",null);

        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connMgr.getActiveNetworkInfo();

        try {
            tweetList = (ResponseList<Status>) ObjectSerializer.deserialize(mSharedPref.getString(FILENAME, null));
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (accessTokenString == null) {
            Log.e("here","");
            String requestTokenString = mSharedPref.getString("RequestToken",null);
            if (requestTokenString == null) {
                Log.e("222222","2");
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            }
            else {
                login();
                setup();
            }
        } else {
            accessToken = new AccessToken(mSharedPref.getString("accessToken",null), mSharedPref.getString("accessTokenSecret",null));
            profileUrl = mSharedPref.getString("profileUrl", null);
            bannerUrl = mSharedPref.getString("bannerUrl",null);
            name = mSharedPref.getString("name", "");
            screenname = mSharedPref.getString("screenname","");
            login();
            setup();


        }

    }

   @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        mSharedPref = getSharedPreferences("data", MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = mSharedPref.edit();
        if(tweetList != null) {
            try {
                prefEditor.putString(FILENAME, ObjectSerializer.serialize(tweetList));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        prefEditor.commit();

        // Save list state
        if (mLayoutManager != null) {
            mListState = mLayoutManager.onSaveInstanceState();
            savedInstanceState.putParcelable(LIST_STATE_KEY, mListState);
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        // Retrieve list state and list/item positions
        mListState = state.getParcelable(LIST_STATE_KEY);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mListState != null) {
            mLayoutManager.onRestoreInstanceState(mListState);
        }
    }


    private void refreshContent() {
        updateTimeline();
        while (fresh.size() > 0) {
            tweetList.add(0,fresh.get(fresh.size()-1));
            fresh.remove(fresh.size() - 1);
        }
        mAdapter.notifyDataSetChanged();
        mRecyclerView.scrollToPosition(updateSize);
        mSwipeRefreshLayout.setRefreshing(false);

    }

    protected void getTimeline() {
        if (networkInfo != null && networkInfo.isConnected()) {
            TimeLine timeLine = new TimeLine();
            timeLine.execute();
            while (!timeLine.isCancelled()) {

            }
        }
    }

    protected void updateTimeline() {
        if (networkInfo != null && networkInfo.isConnected()) {
            UpdateTimeLine timeLine = new UpdateTimeLine();
            timeLine.execute();
            while (!timeLine.isCancelled()) {

            }
        }
    }

    protected void login() {
        if (networkInfo.isConnected()) {
            Login login = new Login();
            login.execute();
        }

    }

    protected void setup() {

        setContentView(R.layout.activity_timeline);


        navigation = getResources().getStringArray(R.array.navigation);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("Timeline");
        setActionBar(mToolbar);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle("Timeline");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle("Menu");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_timeline_swipe_refresh_layout);

        sinceID = new Long(0);
        if (tweetList == null) {
            getTimeline();
        }
        sinceID = tweetList.get(0).getId();

        mRecyclerView = (RecyclerView) findViewById(R.id.activity_timeline_recycler_view);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new TimelineAdapter(tweetList, accessToken, mRecyclerView.getContext());
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                refreshContent();
            }
        });

        mDrawerHeader = (ImageView) findViewById(R.id.banner_image);
        Glide.with(this).load(bannerUrl).centerCrop().into(mDrawerHeader);

        profilePictureImageView = (ImageView) findViewById(R.id.profile_header_picture);
        Glide.with(this).load(profileUrl).placeholder(R.drawable.profile_placeholder).transform(new CircleTransform(this)).into(profilePictureImageView);
        profilePictureImageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("Screenname", screenname);
                intent.putExtra("AccessToken", accessToken.getToken());
                intent.putExtra("AccessTokenSecret", accessToken.getTokenSecret());
                startActivity(intent);

            }
        });

        nameTextView = (TextView) findViewById(R.id.name);
        nameTextView.setText(name);
        screennameTextView = (TextView) findViewById(R.id.handle);
        screennameTextView.setText("@"+screenname);
        mDrawerList = (ListView) findViewById(R.id.drawer_list);
        mDrawerListItems = new ArrayList<DrawerListItem>();
        mDrawerListItems.add(new DrawerListItem("Timeline", R.drawable.ic_timeline));

        mDrawerListItems.add(new DrawerListItem("Favorites", R.drawable.ic_favorite_icon));
        mDrawerListItems.add(new DrawerListItem("Mentions", R.drawable.ic_at));
        mDrawerListItems.add(new DrawerListItem("Direct Messages", R.drawable.ic_message_text));
        mDrawerListItems.add(new DrawerListItem("Settings", R.drawable.ic_settings));

        mDrawerList.setAdapter(new DrawerListAdapter(this, mDrawerListItems));

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (mDrawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private class Login extends AsyncTask {

        public void getAccessToken() {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
            configurationBuilder.setOAuthConsumerKey(CONSUMER_KEY);
            configurationBuilder.setOAuthConsumerSecret(CONSUMER_SECRET);
            Configuration configuration = configurationBuilder.build();
            TwitterFactory twitterFactory = new TwitterFactory(configuration);
            twitter = twitterFactory.getInstance();
            if (accessToken == null) {
                try {
                    String url = mSharedPref.getString("verifier",null);
                    String verifier = url.substring((url.indexOf("oauth_verifier=") + 15), url.length());
                    requestToken = new RequestToken(mSharedPref.getString("RequestToken",null), mSharedPref.getString("RequestTokenSecret",null));
                    accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            }
            twitter.setOAuthAccessToken(accessToken);
            SharedPreferences.Editor prefEditor = mSharedPref.edit();
            prefEditor.putString("accessToken", accessToken.getToken());

            prefEditor.putString("accessTokenSecret", accessToken.getTokenSecret());
            prefEditor.putString("RequestToken", null);


            try {
                screenname = twitter.getScreenName();
                User user = twitter.showUser(screenname);
                profileUrl = user.getBiggerProfileImageURL();

                String[] splitUrl = profileUrl.split("\\.");
                String imageType = splitUrl[splitUrl.length - 1];
                profileUrl = profileUrl.split("_bigger."+imageType)[0]+"."+imageType;
                bannerUrl = user.getProfileBannerRetinaURL();
                name = user.getName();

                ;

            }catch (Exception e) {
                e.printStackTrace();
            }

            prefEditor.putString("bannerUrl", bannerUrl);
            prefEditor.putString("profileUrl", profileUrl);
            prefEditor.putString("screenname", screenname);
            prefEditor.putString("name", name);
            prefEditor.commit();

            this.cancel(false);
        }

        protected Object doInBackground(Object[] params) {
            getAccessToken();
            return null;
        }

    }

    private class TimeLine extends AsyncTask {

        public void getHomeTimeline() {
            try {
                tweetList = (twitter.getHomeTimeline(new Paging(1, 100)));
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            this.cancel(false);
        }

        protected Object doInBackground(Object[] params) {
            getHomeTimeline();
            return null;
        }
    }

    private class UpdateTimeLine extends AsyncTask {

        public void updateHomeTimeline() {
            try {
                fresh = twitter.getHomeTimeline(new Paging(sinceID));
                if (fresh.size() > 0) {
                    sinceID = fresh.get(0).getId();
                }
                updateSize = fresh.size();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            this.cancel(false);
        }

        protected Object doInBackground(Object[] params) {
            updateHomeTimeline();
            return null;
        }
    }

    public static class CircleTransform extends BitmapTransformation {
        public CircleTransform(Context context) {
            super(context);
        }

        @Override protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
            return circleCrop(pool, toTransform);
        }

        private static Bitmap circleCrop(BitmapPool pool, Bitmap source) {
            if (source == null) return null;

            int size = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            // TODO this could be acquired from the pool too
            Bitmap squared = Bitmap.createBitmap(source, x, y, size, size);

            Bitmap result = pool.get(size, size, Bitmap.Config.ARGB_8888);
            if (result == null) {
                result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(result);
            Paint paint = new Paint();
            paint.setShader(new BitmapShader(squared, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
            paint.setAntiAlias(true);
            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);
            return result;
        }

        @Override public String getId() {
            return getClass().getName();
        }
    }

}


