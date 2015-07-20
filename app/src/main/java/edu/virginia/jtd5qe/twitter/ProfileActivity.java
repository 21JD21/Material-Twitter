package edu.virginia.jtd5qe.twitter;

/**
 * Created by jackding on 6/12/15.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toolbar;

import java.io.InputStream;
import java.net.URL;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class ProfileActivity extends Activity {

    private static AccessToken accessToken;
    private static Twitter twitter;
    private static String screenname;
    private static String ownerScreenname;
    private RoundedBitmapDrawable profilePicture;
    private String profileUrl;
    private String bannerUrl;
    private String name;
    private BitmapDrawable banner;
    private ResponseList<Status> tweetList;
    private ConnectivityManager connMgr;
    private NetworkInfo networkInfo;
    private Long sinceID;
    ResponseList<twitter4j.Status> fresh;
    private LruCache<String, RoundedBitmapDrawable> profileCache;
    private User owner;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private DrawerLayout mDrawerLayout;
    private ImageView profilePictureImageView;
    private TextView nameTextView;
    private TextView screennameTextView;
    private TextView bigNameTextView;
    private TextView bigScreennameTextView;
    private ListView mDrawerList;
    private String[] navigation;
    private View mDrawerHeader;
    private ImageView ProfilePictureImageViewMainScreen;
    private LinearLayout BannerLayoutMainScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        accessToken = new AccessToken(getIntent().getStringExtra("AccessToken"), getIntent().getStringExtra("AccessTokenSecret"));
        screenname = (String) getIntent().getStringExtra("Screenname");
        ownerScreenname = (String) getIntent().getStringExtra("OwnerScreenname");
        navigation = getResources().getStringArray(R.array.navigation);


        Toolbar mToolbar = (Toolbar) findViewById(R.id.activity_profile_toolbar);
        mToolbar.setTitle("Profile Summary");
        setActionBar(mToolbar);
        getActionBar().setDisplayHomeAsUpEnabled(true);


        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connMgr.getActiveNetworkInfo();
        sinceID = new Long(0);

        if (networkInfo != null && networkInfo.isConnected()) {
            Info info = new Info();
            info.execute();
            while( !info.isCancelled()) {

            }
        }

        sinceID = tweetList.get(0).getId();

        mRecyclerView = (RecyclerView) findViewById(R.id.activity_profile_recycler_view);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ProfileAdapter(tweetList, accessToken, tweetList.get(0).getUser(), mRecyclerView.getContext());
        mRecyclerView.setAdapter(mAdapter);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_profile_swipe_refresh_layout);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                refreshContent();
            }
        });

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));

    }

    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id==android.R.id.home) {
            finish();
        }
        return true;
    }

    private void refreshContent() {
        updateTimeline();
        while (fresh.size() > 0) {
            tweetList.add(0,fresh.get(fresh.size()-1));
            fresh.remove(fresh.size() - 1);
        }
        mAdapter.notifyDataSetChanged();
        mSwipeRefreshLayout.setRefreshing(false);

    }

    protected void updateTimeline() {
        if (networkInfo != null && networkInfo.isConnected()) {
            UpdateTimeLine timeLine = new UpdateTimeLine();
            timeLine.execute();
            while (!timeLine.isCancelled()) {

            }
        }
    }

    private class Info extends AsyncTask {
        public void getInfo(){

            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
            configurationBuilder.setOAuthConsumerKey(TimelineActivity.CONSUMER_KEY);
            configurationBuilder.setOAuthConsumerSecret(TimelineActivity.CONSUMER_SECRET);
            Configuration configuration = configurationBuilder.build();
            TwitterFactory twitterFactory = new TwitterFactory(configuration);
            twitter = twitterFactory.getInstance();
            twitter.setOAuthAccessToken(accessToken);

            try {
                owner = twitter.showUser(ownerScreenname);
                profileUrl = owner.getBiggerProfileImageURL();

                String[] splitUrl = profileUrl.split("\\.");
                String imageType = splitUrl[splitUrl.length - 1];
                profileUrl = profileUrl.split("_bigger."+imageType)[0]+"."+imageType;

                Bitmap profpic = BitmapFactory.decodeStream((InputStream) new URL(profileUrl).getContent());
                profpic = ThumbnailUtils.extractThumbnail(profpic, Math.min(profpic.getWidth(), profpic.getHeight()), Math.min(profpic.getWidth(), profpic.getHeight()));
                RoundedBitmapDrawable output = RoundedBitmapDrawableFactory.create(getResources(), profpic);
                output.setTargetDensity(profpic.getDensity());
                output.setAntiAlias(true);
                output.setCornerRadius(Math.min(profpic.getWidth(), profpic.getHeight())/2.0f);

                profilePicture = output;


                bannerUrl = owner.getProfileBannerMobileRetinaURL();
                if(bannerUrl != null) {
                    Bitmap bannerBitmap = BitmapFactory.decodeStream((InputStream) new URL(bannerUrl).getContent());
                    banner = new BitmapDrawable(bannerBitmap);
                }


                tweetList = twitter.getUserTimeline(screenname, (new Paging(1, 15)));


            }
            catch(Exception e) {
                e.printStackTrace();
            }

            this.cancel(false);

        }
        protected Object doInBackground(Object[] params) {
            getInfo();
            return null;
        }
    }

    private class UpdateTimeLine extends AsyncTask {

        public void updateHomeTimeline() {
            try {
                if(twitter != null) {
                    fresh = twitter.getUserTimeline(screenname, new Paging(sinceID));
                    if (fresh.size() > 0) {
                        sinceID = fresh.get(0).getId();
                    }
                }
                else{

                }
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
}
