package edu.virginia.jtd5qe.twitter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by jackding on 7/8/15.
 * From http://codetheory.in/android-navigation-drawer/
 */
public class DrawerListAdapter extends BaseAdapter {

    Context mContext;
    ArrayList<DrawerListItem> mListItems;

    public DrawerListAdapter(Context context, ArrayList<DrawerListItem> navItems) {
        mContext = context;
        mListItems = navItems;
    }

    @Override
    public int getCount() {
        return mListItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mListItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.drawer_list_item, null);
        }
        else {
            view = convertView;
        }

        TextView titleView = (TextView) view.findViewById(R.id.navigation_item);
        ImageView iconView = (ImageView) view.findViewById(R.id.icon);

        titleView.setText( mListItems.get(position).mTitle );
        iconView.setImageResource(mListItems.get(position).mIcon);

        return view;
    }
}
