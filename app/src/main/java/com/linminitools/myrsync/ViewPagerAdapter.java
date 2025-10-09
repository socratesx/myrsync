package com.linminitools.myrsync;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

class ViewPagerAdapter extends FragmentStatePagerAdapter {

    // Adapter for the viewpager using FragmentPagerAdapter

    private final List<Fragment> mFragmentList = new java.util.ArrayList<>();
    private final List<String> mFragmentTitleList = new ArrayList<>();

    ViewPagerAdapter(FragmentManager manager) {
        super(manager, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
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



