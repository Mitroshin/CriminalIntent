package com.developgmail.mitroshin.criminalintent.activity;

import android.content.Intent;
import android.support.v4.app.Fragment;

import com.developgmail.mitroshin.criminalintent.R;
import com.developgmail.mitroshin.criminalintent.model.Crime;
import com.developgmail.mitroshin.criminalintent.fragment.CrimeFragment;
import com.developgmail.mitroshin.criminalintent.fragment.CrimeListFragment;

public class CrimeListActivity extends SingleFragmentActivity
        implements CrimeListFragment.Callbacks, CrimeFragment.Callbacks {

    @Override
    protected Fragment createFragment() {
        return new CrimeListFragment();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_masterdetail;
    }

    @Override
    public void onCrimeSelected(Crime crime) {
        if (findViewById(R.id.details_fragment_container) == null) {
            Intent intent = CrimePagerActivity.newIntent(this, crime.getId());
            startActivity(intent);
        } else {
            Fragment newDetail = CrimeFragment.newInstance(crime.getId());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.details_fragment_container, newDetail)
                    .commit();
        }
    }

    @Override
    public void OnCrimeUpdated(Crime crime) {
        CrimeListFragment listFragment = (CrimeListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        listFragment.updateUI();
    }
}
