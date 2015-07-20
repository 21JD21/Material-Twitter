package edu.virginia.jtd5qe.twitter;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LruCache;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toolbar;

import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


/**
 * Created by jackding on 6/29/15.
 */
public class SearchActivity extends Activity {

    private RecyclerView.Adapter mAdapter;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private java.util.List<Status> tweetList;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ImageView profilePictureImageView;
    private TextView nameTextView;
    private TextView screennameTextView;

    private ConnectivityManager connMgr;
    private NetworkInfo networkInfo;
    private Long sinceID;
    private List<twitter4j.Status>  fresh;
    private static AccessToken accessToken;
    private static Twitter twitter;
    private static RequestToken requestToken;
    static String CONSUMER_KEY = "nF8GoCuOJDuYA6WTYUPRJEFgZ";
    static String CONSUMER_SECRET = "RlyeN4GXMMNRSZ1FsRZGjRKUGZ74mJfSVlGTCyXZaOAsdkRC9h";
    private LruCache<String, RoundedBitmapDrawable> profileCache;
    private String screenname;
    private String searchString;
    private String profileUrl;
    private String bannerUrl;
    private String name;
    private String[] navigation;
    private RoundedBitmapDrawable profilePicture;
    private BitmapDrawable banner;
    private Query query;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        accessToken = new AccessToken(getIntent().getStringExtra("AccessToken"), getIntent().getStringExtra("AccessTokenSecret"));
        screenname = (String) getIntent().getStringExtra("Screenname");
        searchString = (String) getIntent().getStringExtra("SearchString");


        query = new Query(searchString);
        query.setCount(50);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(profileUrl);
        setActionBar(mToolbar);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        profileCache = new LruCache<String, RoundedBitmapDrawable>((int) (Runtime.getRuntime().maxMemory())/16);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_timeline_swipe_refresh_layout);

        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            Search search = new Search();
            search.execute();
            while( !search.isCancelled()) {

            }
        }
        sinceID = new Long(0);
        sinceID = tweetList.get(0).getId();


        mRecyclerView = (RecyclerView) findViewById(R.id.activity_timeline_recycler_view);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new SearchAdapter(tweetList, profileCache, accessToken, mRecyclerView.getContext());
        mRecyclerView.setAdapter(mAdapter);
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
        updateSearch();
        while (fresh.size() > 0) {
            tweetList.add(0,fresh.get(fresh.size()-1));
            fresh.remove(fresh.size() - 1);
        }
        mAdapter.notifyDataSetChanged();
        mSwipeRefreshLayout.setRefreshing(false);

    }

    protected void updateSearch() {
        if (networkInfo != null && networkInfo.isConnected()) {
            UpdateSearch search = new UpdateSearch();
            search.execute();
            while (!search.isCancelled()) {

            }
        }
    }

    private class Search extends AsyncTask {
        public void getSearch(){

            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
            configurationBuilder.setOAuthConsumerKey(TimelineActivity.CONSUMER_KEY);
            configurationBuilder.setOAuthConsumerSecret(TimelineActivity.CONSUMER_SECRET);
            Configuration configuration = configurationBuilder.build();
            TwitterFactory twitterFactory = new TwitterFactory(configuration);
            twitter = twitterFactory.getInstance();
            twitter.setOAuthAccessToken(accessToken);

            try {
                QueryResult result = (twitter.search(query));
                tweetList = result.getTweets();


            }
            catch(Exception e) {
                if(twitter != null) {
                    Log.e("null","null");
                }
                e.printStackTrace();
            }

            this.cancel(false);

        }
        protected Object doInBackground(Object[] params) {
            getSearch();
            return null;
        }
    }

    private class UpdateSearch extends AsyncTask {

        public void updateSearch() {
            try {
                if(twitter != null) {
                    query.setSinceId(sinceID);
                    QueryResult result = (twitter.search(query));
                    fresh = result.getTweets();
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
            updateSearch();
            return null;
        }
    }
}
