package com.linminitools.myrsync;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

class ViewPagerAdapter extends FragmentStateAdapter {

    private static final String[] TAB_TITLES = {"Overview", "Configurations", "Log"};

    ViewPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new tab1();
            case 1: return new tab3();
            case 2: return new tab4();
            default: return new tab1();
        }
    }

    @Override
    public int getItemCount() {
        return TAB_TITLES.length;
    }

    static String getTabTitle(int position) {
        return TAB_TITLES[position];
    }
}
