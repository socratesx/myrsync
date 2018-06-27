package com.linminitools.mysync;

import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import static android.support.v4.content.ContextCompat.startActivity;
import static com.linminitools.mysync.MainActivity.configs;
import java.util.ArrayList;

class customAdapter extends BaseAdapter {

        Context context;
        String[] data;
        private static LayoutInflater inflater = null;

        public customAdapter(Context context, ArrayList<?> data) {
        this.context = context;
        this.data=new String[data.size()];
        for (Object c : data) {
            if (c.getClass()==RS_Configuration.class) {
                this.data[data.indexOf(c)] = ((RS_Configuration) c).name;
            }
            if (c.getClass()==Scheduler.class){
                this.data[data.indexOf(c)] = ((Scheduler) c).name;
            }
        }
        inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
        // TODO Auto-generated method stub
        return data.length;
        }

        @Override
        public Object getItem(int position) {
        // TODO Auto-generated method stub
        return data[position];
        }

        @Override
        public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (vi == null)
            vi = inflater.inflate(R.layout.row,null);
            TextView text = (TextView) vi.findViewById(R.id.list_item_id);
            text.setText(data[position]);
            vi.findViewById(R.id.bt_edit).setTag(R.id.bt_edit,position);
            vi.findViewById(R.id.bt_delete).setTag(R.id.bt_delete,position);

        return vi;
        }
}