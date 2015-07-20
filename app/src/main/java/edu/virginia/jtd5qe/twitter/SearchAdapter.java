package edu.virginia.jtd5qe.twitter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.util.List;

import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created by jackding on 6/29/15.
 * Same as timeline adapter but uses java.util.List instead of a twitter4j ResponseList
 * Because that what twitter4j gives when you use twitter.search(query);
 */
public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {


        private List<twitter4j.Status> mTweetList;
        private LruCache<String, RoundedBitmapDrawable> mProfileCache;
        private String pictureUrl;
        private String tweetPhotoUrl;
        private RoundedBitmapDrawable profilePic;
        private Bitmap tweetPhoto;
        private Context context;
        private final AccessToken mAccessToken;
        private String mOwnerScreenname;
        static String CONSUMER_KEY = "nF8GoCuOJDuYA6WTYUPRJEFgZ";
        static String CONSUMER_SECRET = "RlyeN4GXMMNRSZ1FsRZGjRKUGZ74mJfSVlGTCyXZaOAsdkRC9h";
        private static Twitter twitter;
        private static Status freshTweet;

        public static class ViewHolder extends RecyclerView.ViewHolder {

            public View mView;
            public TextView mScreennameText;
            public TextView mStatusText;
            public ImageView mProfilePicture;
            public TextView mRetweetUser;
            public TextView mRetweetText;
            public ImageView mTweetPhoto;
            public RelativeLayout mActionLayout;
            public CardView mCardView;
            public EditText mReplyText;
            public boolean addonVisible;
            public ImageView mRetweetButton;
            public ImageView mReplyButton;
            public ImageView mFavoriteButton;
            public ImageView mSendButton;
            public ImageView mShareButton;
            public boolean recentlyRetweeted;

            public ViewHolder (View v) {
                super(v);
                mView = v;
                mCardView = (CardView) v.findViewById(R.id.card_view);
                mStatusText = (TextView) v.findViewById(R.id.tweet_card_status);
                mScreennameText = (TextView) v.findViewById(R.id.tweet_card_screen_name);
                mProfilePicture = (ImageView) v.findViewById(R.id.profile_picture);
                mRetweetUser = (TextView) v.findViewById(R.id.retweet_user);
                mRetweetText = (TextView) v.findViewById(R.id.retweet_text);
                mTweetPhoto = (ImageView) v.findViewById(R.id.tweet_photo);
                mActionLayout = (RelativeLayout) v.findViewById(R.id.actions);
                mReplyText = (EditText) v.findViewById(R.id.reply_edit_text);
                mRetweetButton = (ImageView) v.findViewById(R.id.retweet_icon);
                mReplyButton = (ImageView) v.findViewById(R.id.reply_icon);
                mFavoriteButton = (ImageView) v.findViewById(R.id.favorite_icon);
                mSendButton = (ImageView) v.findViewById(R.id.send_reply_icon);
                mShareButton = (ImageView) v.findViewById(R.id.share_icon);
                addonVisible = false;
                recentlyRetweeted = false;



            }


        }

        public SearchAdapter(List<twitter4j.Status> tweetList,  LruCache<String, RoundedBitmapDrawable> profileCache, AccessToken accessToken, Context context) {
            mTweetList = tweetList;
            mProfileCache = profileCache;
            this.context = context;
            this.mAccessToken = accessToken;
            LoadTwitter load = new LoadTwitter();
            load.execute();
        }



        @Override
        public SearchAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tweet_card, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            Status tweet = mTweetList.get(position);
            String reply = "@" + tweet.getUser().getScreenName();
            if ( tweet.isRetweetedByMe()) {
                holder.mRetweetButton.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_retweet_complete));
            }

            holder.mRetweetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!mTweetList.get(position).isRetweetedByMe()) {
                        Retweet retweet = new Retweet();
                        retweet.setStatusId(mTweetList.get(position).getId());
                        retweet.execute();
                        holder.mRetweetButton.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_retweet_complete));
                        holder.recentlyRetweeted = true;

                    }
                }
            });


            holder.mActionLayout.setVisibility(View.GONE);

            holder.mFavoriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Favorite fav = new Favorite();
                    fav.setStatusId(mTweetList.get(position).getId());
                    fav.execute();
                    holder.mFavoriteButton.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_favorite_icon));
                }
            });

            holder.mCardView.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View view) {

                    if (holder.mActionLayout.getVisibility() == View.GONE) {
                        holder.mActionLayout.setVisibility(View.VISIBLE);
                        holder.addonVisible = true;
                    } else {
                        holder.mActionLayout.setVisibility(View.GONE);
                        holder.addonVisible = false;
                    }
                    Log.e("long ", "click");
                    return true;
                }
            });

            if(mTweetList.get(position).isRetweet() == true) {
                holder.mRetweetUser.setText(tweet.getUser().getName());
                holder.mRetweetText.setVisibility(View.VISIBLE);
                holder.mRetweetUser.setVisibility(View.VISIBLE);
                tweet = tweet.getRetweetedStatus();
            }
            else{
                holder.mRetweetText.setVisibility(View.GONE);
                holder.mRetweetUser.setVisibility(View.GONE);
            }

            String tweetText = tweet.getText();
            SpannableStringBuilder tweetSpannableString = new SpannableStringBuilder();
            tweetSpannableString.append(tweetText);
            holder.mScreennameText.setText(tweet.getUser().getScreenName());

            final String screennametweet = tweet.getUser().getScreenName();
            UserMentionEntity[] userMentionEntities = tweet.getUserMentionEntities();
            if (userMentionEntities != null) {
                for (final UserMentionEntity ume: userMentionEntities) {
                    reply += " @" + ume.getScreenName();
                    int start = ume.getStart();
                    int end = ume.getEnd();
                    tweetSpannableString.setSpan(new ClickableSpan() {

                        @Override
                        public void onClick(View widget) {
                            Intent profileIntent = new Intent(context, ProfileActivity.class);
                            profileIntent.putExtra("Screenname", ume.getScreenName());
                            profileIntent.putExtra("AccessToken", mAccessToken.getToken());
                            profileIntent.putExtra("AccessTokenSecret", mAccessToken.getTokenSecret());
                            profileIntent.putExtra("OwnerScreenname", mAccessToken.getScreenName());
                            context.startActivity(profileIntent);

                        }
                        @Override
                        public void updateDrawState(TextPaint ds) {
                            ds.setColor(context.getResources().getColor(R.color.colorAccent));
                            ds.setUnderlineText(true);
                        }

                    },start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            holder.mReplyText.setText(reply);

            HashtagEntity[] hashtagEntities = tweet.getHashtagEntities();
            if (hashtagEntities != null) {
                tweetSpannableString = hashtagSetup(tweetSpannableString, hashtagEntities, screennametweet);
            }

            MediaEntity[] tweetMediaEntities = tweet.getMediaEntities();
            boolean containsPhoto = false;
            if (tweetMediaEntities != null) {
                for (MediaEntity me : tweetMediaEntities ) {
                    String type = me.getType();
                    int start = me.getStart();
                    int end = me.getEnd();
                    tweetSpannableString.replace(start, end, "", 0,0);
                    if (type.contentEquals("photo")) {
                        containsPhoto = true;
                        tweetPhotoUrl = me.getMediaURL();
                        holder.mTweetPhoto.setVisibility(View.VISIBLE);
                        Glide.with(context).load(tweetPhotoUrl).placeholder(R.drawable.placeholder).into(holder.mTweetPhoto);

                    }
                }
                if(!containsPhoto) {
                    holder.mTweetPhoto.setVisibility(View.GONE);
                }

            }

            URLEntity[] entities = tweet.getURLEntities();
            if (entities != null) {
                tweetSpannableString = linkSetup(tweetSpannableString, entities);
            }

            holder.mStatusText.setText(tweetSpannableString);
            holder.mStatusText.setMovementMethod(LinkMovementMethod.getInstance());
            holder.mStatusText.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View view) {

                    if (holder.mActionLayout.getVisibility() == View.GONE) {
                        holder.mActionLayout.setVisibility(View.VISIBLE);
                    } else {
                        holder.mActionLayout.setVisibility(View.GONE);
                    }
                    Log.e("long ", "click");
                    return true;
                }
            });

            String url = tweet.getUser().getBiggerProfileImageURL();
            pictureUrl = url;
            String[] splitUrl = pictureUrl.split("\\.");
            String imageType = splitUrl[splitUrl.length - 1];
            pictureUrl = pictureUrl.split("_bigger." + imageType)[0] + "." + imageType;
            Glide.with(context).load(pictureUrl).placeholder(R.drawable.profile_placeholder).transform(new CircleTransform(context)).into(holder.mProfilePicture);

            holder.mProfilePicture.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                    intent.putExtra("Screenname", screennametweet);
                    intent.putExtra("AccessToken", mAccessToken.getToken());
                    intent.putExtra("AccessTokenSecret", mAccessToken.getTokenSecret());
                    intent.putExtra("OwnerScreenname", mAccessToken.getScreenName());
                    context.startActivity(intent);

                }
            });
            final String finReply = reply;
            holder.mSendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SendTweet sendTweet = new SendTweet();
                    sendTweet.setStatus(holder.mReplyText.getText().toString());
                    sendTweet.execute();
                    holder.mReplyText.setText(finReply);
                }
            });

        }

        private SpannableStringBuilder hashtagSetup(SpannableStringBuilder tweetSpannableString, HashtagEntity[] hash, String screenname) {
            SpannableStringBuilder spannable = tweetSpannableString;
            HashtagEntity[] hashtagEntities = hash;
            final String screennametweet = screenname;

            for (HashtagEntity he : hashtagEntities) {
                int start = he.getStart();
                int end = he.getEnd();
                final String hashtag = he.getText();
                tweetSpannableString.setSpan(new ClickableSpan() {

                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent(context, SearchActivity.class);
                        intent.putExtra("Screenname", screennametweet);
                        intent.putExtra("AccessToken", mAccessToken.getToken());
                        intent.putExtra("AccessTokenSecret", mAccessToken.getTokenSecret());
                        intent.putExtra("SearchString", hashtag);
                        context.startActivity(intent);

                    }
                    @Override
                    public void updateDrawState(TextPaint ds) {
                        ds.setColor(context.getResources().getColor(R.color.colorAccent));
                        ds.setUnderlineText(true);
                    }

                }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            return new SpannableStringBuilder (spannable);
        }

        private SpannableStringBuilder linkSetup(SpannableStringBuilder tweetSpannableString, URLEntity[] urls) {
            SpannableStringBuilder spannable = tweetSpannableString;
            URLEntity[] urlEntities = urls;
            int offset = 0;
            int lastReplaceEnd = 250;

            for(final URLEntity url : urlEntities) {
                int start = url.getStart();
                int end = url.getEnd();
                if (lastReplaceEnd < start) {
                    start += offset;
                    end += offset;
                }
                String displayUrl = url.getDisplayURL();
                tweetSpannableString.replace(start, end, displayUrl, 0, displayUrl.length());
                tweetSpannableString.setSpan(new ClickableSpan() {

                    @Override
                    public void onClick(View widget) {

                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.getExpandedURL()));
                        context.startActivity(browserIntent);

                    }
                    @Override
                    public void updateDrawState(TextPaint ds) {
                        ds.setColor(context.getResources().getColor(R.color.colorAccent));
                        ds.setUnderlineText(true);


                    }

                }, start, start + displayUrl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                offset = displayUrl.length() - (end - start);
                lastReplaceEnd = end;
            }

            return new SpannableStringBuilder(spannable);

        }

        @Override
        public int getItemCount() {
            return mTweetList.size();
        }

        @Override
        public long getItemId(int position) {
            return mTweetList.get(position).getId();
        }


        private class LoadTwitter extends AsyncTask {

            public void loadTwitter() {
                ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
                configurationBuilder.setOAuthConsumerKey(CONSUMER_KEY);
                configurationBuilder.setOAuthConsumerSecret(CONSUMER_SECRET);
                Configuration configuration = configurationBuilder.build();
                TwitterFactory twitterFactory = new TwitterFactory(configuration);
                twitter = twitterFactory.getInstance();
                twitter.setOAuthAccessToken(mAccessToken);
            }

            protected Object doInBackground(Object[] params) {
                loadTwitter();
                this.cancel(false);
                return null;
            }
        }

        private class Retweet extends AsyncTask {

            private long statusId;

            public void retweet() {
                if (twitter != null) {
                    try {
                        freshTweet = twitter.retweetStatus(statusId);
                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }
                }
            }

            public void setStatusId(long id) {
                this.statusId = id;
            }

            protected Object doInBackground(Object[] params) {
                retweet();
                this.cancel(false);
                return null;
            }
        }

        private class DeleteRetweet extends AsyncTask {

            private long statusId;
            private long originalId;

            public void deleteRetweet(){
                if (twitter != null) {
                    try {
                        twitter.destroyStatus(statusId);
                        freshTweet = twitter.showStatus(originalId);
                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Log.e("null","twitter");
                }
            }

            public void setId(long id, long original){
                this.statusId = id;
                this.originalId = original;
            }

            protected Object doInBackground(Object[] Params) {
                deleteRetweet();
                this.cancel(false);
                return null;
            }
        }

        private class Favorite extends AsyncTask {

            private long statusId;

            public void favorite() {
                if (twitter != null) {
                    try {
                        twitter.createFavorite(statusId);
                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Log.e("null","twitter");
                }
            }

            public void setStatusId(long id) {
                this.statusId = id;
            }

            protected Object doInBackground(Object[] params) {
                favorite();
                this.cancel(false);
                return null;
            }
        }

        private class SendTweet extends AsyncTask {

            String status;
            public void tweet() {
                StatusUpdate statusUpdate = new StatusUpdate(status);
                try {
                    twitter.updateStatus(statusUpdate);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            }

            public void setStatus(String status) {
                this.status = status;
            }

            protected Object doInBackground(Object[] params) {
                tweet();
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
