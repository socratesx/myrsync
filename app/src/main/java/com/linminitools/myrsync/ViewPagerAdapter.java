package com.linminitools.myrsync;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

class ViewPagerAdapter extends FragmentStatePagerAdapter {

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

    private void addFragment(Fragment fragment, String title) {
        mFragmentList.add(fragment);
        mFragmentTitleList.add(title);

    }

    void refresh_adapter(){
        mFragmentList.clear();
        mFragmentTitleList.clear();

        FragmentInitializer tab1 = new FragmentInitializer(new tab1());
        FragmentInitializer tab2 = new FragmentInitializer(new tab2());
        FragmentInitializer tab3 = new FragmentInitializer(new tab3());
        FragmentInitializer tab4 = new FragmentInitializer(new tab4());

        // Add Fragments to adapter one by one
        this.addFragment(tab1.fHolder, "Overview");
        this.addFragment(tab2.fHolder, "Configurations");
        this.addFragment(tab3.fHolder, "Schedulers");
        this.addFragment(tab4.fHolder, "Log");

    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentTitleList.get(position);
    }

}



