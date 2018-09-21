package com.linminitools.myrsync;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    // Adapter for the viewpager using FragmentPagerAdapter

    private final List<Fragment> mFragmentList = new java.util.ArrayList<>();
    private final List<String> mFragmentTitleList = new ArrayList<>();

    ViewPagerAdapter(FragmentManager manager) {
        super(manager);
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    void addFragment(Fragment fragment, String title) {
        mFragmentList.add(fragment);
        mFragmentTitleList.add(title);

    }

    void refresh_adapter(){
        mFragmentList.clear();
        mFragmentTitleList.clear();
        this.addFragment(new tab1(), "Overview");
        this.addFragment(new tab2(), "Configurations");
        this.addFragment(new tab3(), "Schedulers");
        this.addFragment(new tab4(), "Log");

    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentTitleList.get(position);
    }

}



